const ONESIGNAL_APP_ID = "59d52980-a287-4a44-bc2e-7c34e565360c";
const FIRESTORE_COLLECTION = "notifications";
const FIRESTORE_PAGE_SIZE = 100;
const MAX_DOCUMENTS_PER_RUN = 100;
const ROLE_TAG_KEY = "role";
const ALLOWED_ROLES = ["parent", "driver", "admin", "principal"];

/** Installs a one-minute polling trigger and starts after existing notifications. */
function installMinuteTrigger() {
  const properties = PropertiesService.getScriptProperties();
  const existing = ScriptApp.getProjectTriggers()
    .filter(function (trigger) { return trigger.getHandlerFunction() === "processPendingNotifications"; });
  existing.forEach(function (trigger) { ScriptApp.deleteTrigger(trigger); });

  if (!properties.getProperty("NOTIFICATION_CURSOR")) {
    initializeNotificationCursor();
  }

  ScriptApp.newTrigger("processPendingNotifications").timeBased().everyMinutes(1).create();
  console.log("OneSignal relay trigger installed. Existing inbox notifications were left untouched.");
}

/** Run once after setting Script Properties to initialize a cursor without replaying history. */
function initializeNotificationCursor() {
  const token = getFirestoreAccessToken();
  let cursor;

  // Documents with a missing or non-timestamp `timestamp` field are skipped.
  // Continue paging in descending order until the newest valid Firestore
  // timestamp is found.
  let scanCursor = null;
  while (!cursor) {
    const page = runNotificationQuery(token, scanCursor, true, FIRESTORE_PAGE_SIZE)
      .map(function (entry) { return entry.document; })
      .filter(function (document) { return Boolean(document); });
    if (page.length === 0) break;

    for (let index = 0; index < page.length; index++) {
      cursor = cursorFromDocument(page[index]);
      if (cursor) break;
    }
    if (!cursor) {
      const nextScanCursor = queryCursorFromDocument(page[page.length - 1]);
      if (!nextScanCursor) break;
      scanCursor = nextScanCursor;
    }
  }

  if (!cursor) {
    const projectId = requiredProperty("FIREBASE_PROJECT_ID");
    cursor = {
      timestamp: new Date().toISOString(),
      name: "projects/" + projectId + "/databases/(default)/documents/notifications/__relay_start__",
    };
  }

  PropertiesService.getScriptProperties().setProperty("NOTIFICATION_CURSOR", JSON.stringify(cursor));
  console.log("Relay cursor initialized. Existing notification documents will not be pushed.");
}

/** Polls Firestore and relays each new notification through OneSignal. */
function processPendingNotifications() {
  const lock = LockService.getScriptLock();
  if (!lock.tryLock(1000)) return;

  try {
    const properties = PropertiesService.getScriptProperties();
    const hasNotificationCursor = Boolean(properties.getProperty("NOTIFICATION_CURSOR"));
    console.log("Diagnostic: NOTIFICATION_CURSOR exists: " + hasNotificationCursor);
    if (!hasNotificationCursor) {
      initializeNotificationCursor();
      return;
    }

    const firestoreToken = getFirestoreAccessToken();
    let storedCursor;
    try {
      storedCursor = JSON.parse(properties.getProperty("NOTIFICATION_CURSOR"));
    } catch (error) {
      storedCursor = null;
    }
    let cursor = normalizeNotificationCursor(storedCursor);
    if (!cursor) {
      console.warn("Stored relay cursor is invalid; reinitializing it from existing notifications.");
      initializeNotificationCursor();
      cursor = normalizeNotificationCursor(JSON.parse(properties.getProperty("NOTIFICATION_CURSOR")));
    } else if (JSON.stringify(storedCursor) !== JSON.stringify(cursor)) {
      properties.setProperty("NOTIFICATION_CURSOR", JSON.stringify(cursor));
    }
    console.log("Diagnostic: cursor timestamp = " + cursor.timestamp);
    let queryCursor = cursor;
    let processedThisRun = 0;

    while (processedThisRun < MAX_DOCUMENTS_PER_RUN) {
      const pendingNotificationDocs = runNotificationQuery(firestoreToken, queryCursor, false, FIRESTORE_PAGE_SIZE)
        .map(function (entry) { return entry.document; })
        .filter(function (document) { return Boolean(document); });
      console.log("Diagnostic: runNotificationQuery returned " + pendingNotificationDocs.length + " document(s).");
      if (pendingNotificationDocs.length === 0) break;

      for (let index = 0; index < pendingNotificationDocs.length && processedThisRun < MAX_DOCUMENTS_PER_RUN; index++) {
        const document = pendingNotificationDocs[index];
        const notification = decodeFields(document.fields || {});
        console.log("Diagnostic notification document: " + JSON.stringify({
          documentId: document.name.split("/").pop(),
          timestamp: notification.timestamp || null,
          recipientIdPresent: Boolean(stringValue(notification.recipientId).trim()),
          recipientRole: stringValue(notification.recipientRole),
        }));
        const nextCursor = cursorFromDocument(document);

        if (!nextCursor) {
          // Skip malformed timestamp documents without touching Firestore. Keep
          // a typed cursor only in memory; NOTIFICATION_CURSOR stays normalized.
          queryCursor = queryCursorFromDocument(document) || queryCursor;
          processedThisRun++;
          continue;
        }

        // Send first, then checkpoint this exact document. Failed requests stop the
        // run so the same event is retried on the next scheduled execution.
        console.log("Diagnostic: calling relayNotification for document " + document.name.split("/").pop() + ".");
        relayNotification(document.name.split("/").pop(), notification, firestoreToken);
        cursor = nextCursor;
        queryCursor = cursor;
        properties.setProperty("NOTIFICATION_CURSOR", JSON.stringify(cursor));
        console.log("Diagnostic last checkpointed tuple: " + JSON.stringify({
          timestamp: cursor.timestamp,
          name: cursor.name,
        }));
        processedThisRun++;
      }
    }
  } catch (error) {
    console.error("OneSignal notification relay failed: " + error.stack);
    throw error;
  } finally {
    console.log("Diagnostic: processPendingNotifications run finished.");
    lock.releaseLock();
  }
}

function relayNotification(notificationId, notification, firestoreToken) {
  const title = stringValue(notification.title) || "BusTrack Update";
  const message = stringValue(notification.message);
  const recipientId = stringValue(notification.recipientId).trim();
  const role = stringValue(notification.recipientRole).trim().toLowerCase();

  if (recipientId) {
    // NotificationModel defines recipientId as a Firebase UID. Email addresses
    // are intentionally rejected rather than guessed as OneSignal identities.
    if (recipientId.indexOf("@") >= 0) {
      console.warn("Skipping notification " + notificationId + ": recipientId is not a Firebase UID.");
      return;
    }
    sendOneSignalPush({
      include_aliases: { external_id: [recipientId] },
      target_channel: "push",
    }, notificationId, title, message, notification);
    return;
  }

  if (role) {
    if (ALLOWED_ROLES.indexOf(role) < 0) {
      console.warn("Skipping notification " + notificationId + ": unsupported role " + role + ".");
      return;
    }

    // Role tags are sourced from Firestore here, never accepted from the app.
    const rolesByUid = findFirebaseUserRoles(firestoreToken);
    Object.keys(rolesByUid).forEach(function (uid) {
      setOneSignalRoleTag(uid, rolesByUid[uid]);
    });

    sendOneSignalPush({
      filters: [{ field: "tag", key: ROLE_TAG_KEY, relation: "=", value: role }],
      target_channel: "push",
    }, notificationId, title, message, notification);
    return;
  }

  console.warn("Skipping notification " + notificationId + ": no recipientId or recipientRole.");
}

function sendOneSignalPush(target, notificationId, title, message, notification) {
  const payload = Object.assign({
    app_id: ONESIGNAL_APP_ID,
    existing_android_channel_id: "bus_track_notifications",
    headings: { en: title },
    contents: { en: message },
    data: {
      notificationId: notificationId,
      type: stringValue(notification.type) || "GENERAL",
      relatedId: stringValue(notification.relatedId),
      recipientRole: stringValue(notification.recipientRole).toLowerCase(),
    },
  }, target);

  const response = UrlFetchApp.fetch("https://api.onesignal.com/notifications", {
    method: "post",
    contentType: "application/json",
    headers: { Authorization: "Key " + requiredProperty("ONESIGNAL_REST_API_KEY") },
    payload: JSON.stringify(payload),
    muteHttpExceptions: true,
  });
  const status = response.getResponseCode();
  if (status < 200 || status >= 300) {
    throw new Error("OneSignal send failed (HTTP " + status + "): " + response.getContentText());
  }
  console.log("OneSignal accepted notification " + notificationId + ": " + response.getContentText());
}

function setOneSignalRoleTag(uid, role) {
  const url = "https://api.onesignal.com/apps/" + encodeURIComponent(ONESIGNAL_APP_ID)
    + "/users/by/external_id/" + encodeURIComponent(uid);
  const response = UrlFetchApp.fetch(url, {
    method: "patch",
    contentType: "application/json",
    headers: { Authorization: "Key " + requiredProperty("ONESIGNAL_REST_API_KEY") },
    payload: JSON.stringify({ properties: { tags: { [ROLE_TAG_KEY]: role } } }),
    muteHttpExceptions: true,
  });
  const status = response.getResponseCode();
  if (status === 404) {
    // This account has not linked a OneSignal subscription yet; later role
    // broadcasts will retry the server-side tag synchronization.
    console.log("OneSignal user not linked yet; role tag deferred for UID " + uid);
    return;
  }
  if (status < 200 || status >= 300) {
    throw new Error("OneSignal role tag update failed for UID " + uid + " (HTTP " + status + ").");
  }
}

function findFirebaseUserRoles(accessToken) {
  const projectId = requiredProperty("FIREBASE_PROJECT_ID");
  const rolesByUid = {};
  listFirestoreCollection("users", projectId, accessToken).forEach(function (document) {
    const fields = decodeFields(document.fields || {});
    const uid = stringValue(fields.uid).trim() || document.name.split("/").pop();
    const rawRole = stringValue(fields.role).trim().toLowerCase();
    const email = stringValue(fields.email).trim().toLowerCase();
    let role = rawRole === "user" || !rawRole ? "parent" : rawRole;
    if (email === "admin@gmail.com" || email === "barlasmaria2@gmail.com") role = "admin";
    if (email === "principal@gmail.com") role = "principal";
    if (ALLOWED_ROLES.indexOf(role) >= 0) rolesByUid[uid] = role;
  });

  listFirestoreCollection("drivers", projectId, accessToken).forEach(function (document) {
    const fields = decodeFields(document.fields || {});
    const uid = stringValue(fields.uid).trim();
    if (uid) rolesByUid[uid] = "driver";
  });
  return rolesByUid;
}

function listFirestoreCollection(collectionId, projectId, accessToken) {
  const documents = [];
  let pageToken = "";
  do {
    let url = firestoreBaseUrl(projectId) + "/" + encodeURIComponent(collectionId) + "?pageSize=1000";
    if (pageToken) url += "&pageToken=" + encodeURIComponent(pageToken);
    const response = authorizedFetch(url, accessToken, "get");
    const result = JSON.parse(response.getContentText());
    documents.push.apply(documents, result.documents || []);
    pageToken = result.nextPageToken || "";
  } while (pageToken);
  return documents;
}

function runNotificationQuery(accessToken, cursor, descending, limit) {
  const order = descending ? "DESCENDING" : "ASCENDING";
  const structuredQuery = {
    from: [{ collectionId: FIRESTORE_COLLECTION }],
    orderBy: [
      { field: { fieldPath: "timestamp" }, direction: order },
      { field: { fieldPath: "__name__" }, direction: order },
    ],
    limit: limit,
  };
  if (cursor) {
    const timestampValue = cursor.timestampValue
      ? (typeof cursor.timestampValue === "string"
        ? { timestampValue: cursor.timestampValue }
        : cursor.timestampValue)
      : { timestampValue: cursor.timestamp };
    if (!timestampValue || typeof timestampValue !== "object"
        || Object.keys(timestampValue).length === 0 || !cursor.name) {
      throw new Error("Invalid Firestore notification query cursor.");
    }

    structuredQuery.startAt = {
      values: [
        timestampValue,
        { referenceValue: cursor.name },
      ],
      // Cursor values follow orderBy exactly: timestamp, then __name__.
      // In Firestore REST, before:false means the position strictly after this tuple.
      before: false,
    };
  }
  console.log("Diagnostic Firestore runQuery cursor: " + JSON.stringify(
    structuredQuery.startAt || { position: "initial" }
  ));
  return firestoreRequest(accessToken, "runQuery", { structuredQuery: structuredQuery });
}

function firestoreRequest(accessToken, method, body) {
  const projectId = requiredProperty("FIREBASE_PROJECT_ID");
  const response = authorizedFetch(
    firestoreBaseUrl(projectId) + ":" + method,
    accessToken,
    "post",
    body
  );
  return JSON.parse(response.getContentText());
}

function authorizedFetch(url, accessToken, method, body) {
  const options = {
    method: method,
    headers: { Authorization: "Bearer " + accessToken },
    muteHttpExceptions: true,
  };
  if (body !== undefined) {
    options.contentType = "application/json";
    options.payload = JSON.stringify(body);
  }
  const response = UrlFetchApp.fetch(url, options);
  const status = response.getResponseCode();
  if (status < 200 || status >= 300) {
    throw new Error("Firestore request failed (HTTP " + status + "): " + response.getContentText());
  }
  return response;
}

function firestoreBaseUrl(projectId) {
  return "https://firestore.googleapis.com/v1/projects/" + encodeURIComponent(projectId)
    + "/databases/(default)/documents";
}

function getFirestoreAccessToken() {
  const cache = CacheService.getScriptCache();
  const cached = cache.get("FIRESTORE_ACCESS_TOKEN");
  if (cached) return cached;

  const serviceEmail = requiredProperty("FIREBASE_SERVICE_ACCOUNT_EMAIL");
  const privateKey = requiredProperty("FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY").replace(/\\n/g, "\n");
  const now = Math.floor(Date.now() / 1000);
  const header = base64UrlEncode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64UrlEncode(JSON.stringify({
    iss: serviceEmail,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = header + "." + claim;
  const signature = Utilities.computeRsaSha256Signature(unsigned, privateKey);
  const assertion = unsigned + "." + base64UrlEncode(signature);
  const response = UrlFetchApp.fetch("https://oauth2.googleapis.com/token", {
    method: "post",
    payload: {
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: assertion,
    },
    muteHttpExceptions: true,
  });
  const status = response.getResponseCode();
  if (status < 200 || status >= 300) {
    throw new Error("Google OAuth token request failed (HTTP " + status + "): " + response.getContentText());
  }
  const token = JSON.parse(response.getContentText()).access_token;
  cache.put("FIRESTORE_ACCESS_TOKEN", token, 3000);
  return token;
}

function base64UrlEncode(value) {
  const bytes = typeof value === "string" ? Utilities.newBlob(value).getBytes() : value;
  return Utilities.base64EncodeWebSafe(bytes).replace(/=+$/g, "");
}

function decodeFields(fields) {
  const result = {};
  Object.keys(fields).forEach(function (key) { result[key] = decodeField(fields[key]); });
  return result;
}

function decodeField(field) {
  if (field.stringValue !== undefined) return field.stringValue;
  if (field.integerValue !== undefined) return Number(field.integerValue);
  if (field.doubleValue !== undefined) return Number(field.doubleValue);
  if (field.booleanValue !== undefined) return field.booleanValue;
  if (field.timestampValue !== undefined) return field.timestampValue;
  if (field.referenceValue !== undefined) return field.referenceValue;
  if (field.nullValue !== undefined) return null;
  if (field.mapValue) return decodeFields(field.mapValue.fields || {});
  if (field.arrayValue) return (field.arrayValue.values || []).map(decodeField);
  return null;
}

function cursorFromDocument(document) {
  const timestamp = document.fields && document.fields.timestamp
    ? document.fields.timestamp.timestampValue
    : null;
  if (typeof timestamp !== "string" || !Number.isFinite(Date.parse(timestamp))) return null;
  return { timestamp: timestamp, name: document.name };
}

function normalizeNotificationCursor(cursor) {
  if (!cursor || typeof cursor !== "object" || typeof cursor.name !== "string" || !cursor.name) return null;

  let timestamp = cursor.timestamp;
  if (timestamp && typeof timestamp === "object") {
    timestamp = timestamp.timestampValue;
  }
  if (typeof timestamp !== "string" && cursor.timestampValue) {
    timestamp = typeof cursor.timestampValue === "string"
      ? cursor.timestampValue
      : cursor.timestampValue.timestampValue;
  }
  if (typeof timestamp !== "string" || !Number.isFinite(Date.parse(timestamp))) return null;

  // Preserve Firestore's exact RFC3339 precision (including nanoseconds). Using
  // Date#toISOString would truncate sub-millisecond digits and replay documents.
  return { timestamp: timestamp, name: cursor.name };
}

function queryCursorFromDocument(document) {
  const timestampValue = document.fields && document.fields.timestamp;
  if (!timestampValue || !document.name) return null;
  return { timestampValue: timestampValue, name: document.name };
}

function stringValue(value) {
  return typeof value === "string" ? value : "";
}

function requiredProperty(name) {
  const value = PropertiesService.getScriptProperties().getProperty(name);
  if (!value) throw new Error("Missing required Apps Script property: " + name);
  return value;
}
