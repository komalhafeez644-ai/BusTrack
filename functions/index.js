const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function triggered whenever a notification document is created in Firestore.
 * 
 * Strict Mutually Exclusive Delivery Strategy:
 * 1. Targeted / Individual notification (recipientId is present):
 *    Delivered strictly via direct FCM token to the recipient's device.
 * 2. Role / Group broadcast notification (recipientRole is present):
 *    Delivered strictly to the corresponding role topic (role_parent, role_driver, etc.).
 * 
 * This guarantees no duplicate FCM notifications are ever delivered to a device.
 */
exports.sendPushOnNotificationCreate = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snap, context) => {
    const notificationId = context.params.notificationId;
    const data = snap.data();

    if (!data) {
      console.log(`Notification ${notificationId} has no data, skipping.`);
      return null;
    }

    const title = data.title || "BusTrack Update";
    const message = data.message || "";
    const type = data.type || "GENERAL";
    const relatedId = data.relatedId || "";
    const recipientId = (data.recipientId || "").trim();
    const recipientRole = (data.recipientRole || "").trim().toLowerCase();

    // Standard Android FCM Payload with high priority and notification channel
    const commonPayload = {
      notification: {
        title: title,
        body: message,
      },
      data: {
        notificationId: notificationId,
        title: title,
        message: message,
        type: type,
        relatedId: relatedId,
        recipientId: recipientId,
        recipientRole: recipientRole,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "bus_track_notifications",
          priority: "high",
          defaultSound: true,
          defaultVibrateTimings: true,
        },
      },
    };

    try {
      // 1. DIRECT TARGETED NOTIFICATION (Individual user)
      if (recipientId !== "") {
        console.log(`Processing direct notification ${notificationId} for user: ${recipientId}`);

        let fcmToken = null;

        // Try lookup in users collection first
        const userDoc = await admin.firestore().collection("users").doc(recipientId).get();
        if (userDoc.exists && userDoc.data().fcmToken) {
          fcmToken = userDoc.data().fcmToken;
        }

        // Check users by email if not found
        if (!fcmToken && recipientId.includes("@")) {
          const userByEmail = await admin.firestore().collection("users")
            .where("email", "==", recipientId)
            .limit(1)
            .get();
          if (!userByEmail.empty && userByEmail.docs[0].data().fcmToken) {
            fcmToken = userByEmail.docs[0].data().fcmToken;
          }
        }

        // If not in users, check drivers collection (by doc ID or uid field)
        if (!fcmToken) {
          const driverDoc = await admin.firestore().collection("drivers").doc(recipientId).get();
          if (driverDoc.exists && driverDoc.data().fcmToken) {
            fcmToken = driverDoc.data().fcmToken;
          } else {
            const driverQuery = await admin.firestore().collection("drivers")
              .where("uid", "==", recipientId)
              .limit(1)
              .get();
            if (!driverQuery.empty && driverQuery.docs[0].data().fcmToken) {
              fcmToken = driverQuery.docs[0].data().fcmToken;
            } else {
              const driverById = await admin.firestore().collection("drivers")
                .where("id", "==", recipientId)
                .limit(1)
                .get();
              if (!driverById.empty && driverById.docs[0].data().fcmToken) {
                fcmToken = driverById.docs[0].data().fcmToken;
              }
            }
          }
        }

        if (!fcmToken) {
          console.log(`No active FCM token found for user ${recipientId}. Message stored in Firestore only.`);
          return null;
        }

        const messageToSend = {
          ...commonPayload,
          token: fcmToken,
        };

        try {
          const response = await admin.messaging().send(messageToSend);
          console.log(`Successfully delivered direct notification ${notificationId} to user ${recipientId}:`, response);
        } catch (sendError) {
          console.error(`Error sending direct notification to token:`, sendError);
          // Clean up expired or unregistered tokens
          if (
            sendError.code === "messaging/registration-token-not-registered" ||
            sendError.code === "messaging/invalid-registration-token"
          ) {
            console.log(`Removing invalid FCM token for user ${recipientId}`);
            await admin.firestore().collection("users").doc(recipientId).update({
              fcmToken: admin.firestore.FieldValue.delete(),
            }).catch(() => {});
          }
        }

        return null;
      }

      // 2. ROLE BROADCAST NOTIFICATION (Topic only)
      if (recipientRole !== "") {
        const topic = `role_${recipientRole}`;
        console.log(`Processing role broadcast ${notificationId} to topic: ${topic}`);

        const messageToSend = {
          ...commonPayload,
          topic: topic,
        };

        const response = await admin.messaging().send(messageToSend);
        console.log(`Successfully broadcast notification ${notificationId} to topic ${topic}:`, response);
        return null;
      }

      console.log(`Notification ${notificationId} had neither recipientId nor recipientRole.`);
      return null;
    } catch (error) {
      console.error(`Unexpected error processing notification ${notificationId}:`, error);
      return null;
    }
  });

/**
 * Creates a tracking request for Admin review. Duplicate tracking decisions
 * are intentionally made by Admin at approval time, not during submission.
 */
exports.submitTrackingRequest = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Please sign in before requesting tracking.");
  }

  const enteredRollNumber = String(data && data.rollNumber ? data.rollNumber : "").trim();
  const parentName = String(data && data.parentName ? data.parentName : "").trim();
  const phone = String(data && data.phone ? data.phone : "").trim();
  const relationship = String(data && data.relationship ? data.relationship : "").trim();
  if (!enteredRollNumber) {
    throw new functions.https.HttpsError("invalid-argument", "Roll Number is required.");
  }

  const db = admin.firestore();
  let studentDocument = (await db.collection("students")
    .where("rollNumber", "==", enteredRollNumber)
    .limit(1)
    .get()).docs[0] || null;
  const canonicalRollNumber = enteredRollNumber.toUpperCase();
  // Keep the internal student document reference for existing app features,
  // while always retaining Roll Number as the request's matching/display key.
  const studentId = studentDocument ? studentDocument.id : enteredRollNumber;
  const parentId = context.auth.uid;
  const requests = db.collection("trackingRequests");
  const requestRef = requests.doc();
  await requestRef.create({
    requestId: requestRef.id,
    parentId,
    studentId,
    rollNumber: canonicalRollNumber,
    status: "PENDING",
    trackingEnabled: false,
    trackingState: "PENDING",
    isSeenByAdmin: false,
    submittedAt: admin.firestore.FieldValue.serverTimestamp(),
    parentName,
    phone,
    relationship,
  });

  return { requestId: requestRef.id, studentId, rollNumber: canonicalRollNumber };
});

exports.updateDriverEmail = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Please sign in before updating a driver.");
  }

  const db = admin.firestore();
  const adminSnapshot = await db.collection("users").doc(context.auth.uid).get();
  if (!adminSnapshot.exists || String(adminSnapshot.data().role || "").toLowerCase() !== "admin") {
    throw new functions.https.HttpsError("permission-denied", "Only an Admin can update a driver's email.");
  }

  const driverUid = String(data && data.driverUid || "").trim();
  const driverDocumentId = String(data && data.driverDocumentId || "").trim();
  const newEmail = String(data && data.email || "").trim().toLowerCase();
  if (!driverUid || !driverDocumentId || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail)) {
    throw new functions.https.HttpsError("invalid-argument", "A driver account and valid email are required.");
  }

  const userRef = db.collection("users").doc(driverUid);
  const driverRef = db.collection("drivers").doc(driverDocumentId);
  const [userSnapshot, driverSnapshot] = await Promise.all([userRef.get(), driverRef.get()]);
  if (!userSnapshot.exists || !driverSnapshot.exists || driverSnapshot.data().uid !== driverUid) {
    throw new functions.https.HttpsError("not-found", "The driver account could not be found.");
  }

  const oldEmail = (await admin.auth().getUser(driverUid)).email;
  if (oldEmail && oldEmail.trim().toLowerCase() === newEmail) {
    return { changed: false };
  }

  await admin.auth().updateUser(driverUid, { email: newEmail });
  try {
    const batch = db.batch();
    batch.update(userRef, { email: newEmail });
    batch.update(driverRef, { email: newEmail });
    await batch.commit();
  } catch (error) {
    if (oldEmail) {
      try {
        await admin.auth().updateUser(driverUid, { email: oldEmail });
      } catch (rollbackError) {
        console.error("Failed to roll back driver Auth email after Firestore update failure:", rollbackError);
      }
    }
    throw new functions.https.HttpsError("internal", "Could not save the driver's email changes.");
  }

  return { changed: true };
});
