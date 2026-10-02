const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

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
