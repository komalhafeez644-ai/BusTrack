# OneSignal push relay (Google Apps Script)

This relay polls Firestore's existing `notifications` collection once per minute and sends push requests through OneSignal. It never writes notification documents, so the current Firestore inbox and its schema remain the source of truth. The Apps Script time trigger avoids Firebase Cloud Functions and Blaze. Delivery latency is normally under one minute, subject to Apps Script scheduling and free-account quotas.

## Security boundary

- The Android app contains only the public OneSignal App ID. Its authenticated Firebase UID is linked to OneSignal as the External ID by `MyApp`.
- The OneSignal REST API key and Firebase service-account private key belong only in the standalone Apps Script project's **Script Properties**. Do not add them to this repository, Android resources, or app code.
- Create a dedicated Google service account and grant it only the Firestore read-only role (`Cloud Datastore Viewer`) on the BusTrack Firebase project. The relay reads `notifications`, `users`, and `drivers`; it does not write Firestore.
- Only the Apps Script owner/editors can access Script Properties. Keep the script private and limit editors.
- Role tags are synchronized server-side from Firestore's trusted user records. The app does not set role tags.

## One-time setup

1. In Google Apps Script, create a **standalone** project and copy `Code.gs` into its editor. Keep the Apps Script project private. If editing the manifest, use the checked-in `appsscript.json` contents so external requests and installable triggers are authorized.
2. In **Project Settings → Script Properties**, add:
   - `FIREBASE_PROJECT_ID` — the Firebase project ID.
   - `FIREBASE_SERVICE_ACCOUNT_EMAIL` — the dedicated service account email.
   - `FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY` — the `private_key` from that service account's JSON key. Paste it only into Script Properties; newlines may be literal or `\n`.
   - `ONESIGNAL_REST_API_KEY` — the app REST API key from OneSignal Keys & IDs.
3. Grant the service account `Cloud Datastore Viewer` on the Firebase project. No billing account or Firebase Cloud Functions deployment is required.
4. Run `installMinuteTrigger` once from the Apps Script editor and approve its Google permissions. The first run records the newest existing notification as the cursor, so old inbox entries are not replayed.
5. Check **Executions** in Apps Script after the next minute. A successful OneSignal response is logged with the notification document ID.
6. In Firebase Console, delete the previously deployed `sendPushOnNotificationCreate` function if it exists. The local source no longer exports it, but changing this repository cannot delete an already deployed function; leaving it deployed can cause duplicate pushes.

OneSignal also needs the Android FCM service-account JSON configured under **Settings → Push & In-App → Google Android (FCM)**. This is OneSignal's downstream transport credential and is separate from the read-only Firestore service-account key used by this relay. Keep both server credentials out of the Android app and repository.

## Delivery behavior

- A non-empty `recipientId` is sent to OneSignal `external_id` using that Firebase UID.
- A non-empty `recipientRole` is validated against `parent`, `driver`, `admin`, or `principal`. The relay reads the matching Firebase UIDs, synchronizes their OneSignal `role` tag, then targets that tag.
- The script advances its cursor only after OneSignal accepts a request. If an API request fails, the notification is retried on the next trigger. The script does not insert a second in-app notification.
- Notification `timestamp` is assigned by Firestore server time when queued notifications sync. This preserves the existing field/schema and lets the relay resume safely even after offline sync.
- OneSignal Dashboard/manual messages continue to work independently of this relay.

## Free-tier limits

Google currently allows time-driven triggers as frequently as every minute and lists 20,000 URL Fetch calls/day for consumer accounts. Quotas can change and depend on the account type. A high-volume app may exceed free limits; check Apps Script's Executions page for failures.
