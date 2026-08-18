const admin = require("firebase-admin");
const crypto = require("crypto");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");

admin.initializeApp();

const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;

function requireAuth(request) {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  return request.auth.uid;
}

// Mirrors the client's price parsing: extract the numeric value of a price
// string like "Rs. 12,500" or "NPR 8000". Returns null when not a paid course.
function priceAsNumber(price) {
  if (!price) return null;
  const cleaned = String(price).replace(/[^0-9.]/g, "");
  const n = Number(cleaned);
  return Number.isFinite(n) && n > 0 ? n : null;
}

// Single canonical trust-score formula used everywhere (max 100).
async function computeTrustScoreFor(uid) {
  const userDoc = await db.collection("users").doc(uid).get();
  if (!userDoc.exists) throw new HttpsError("not-found", "User not found");
  const data = userDoc.data() || {};

  let score = 0;
  if (data.isPhoneVerified) score += 15;
  if (data.isGoogleVerified) score += 10;
  if (data.isIdentityVerified) score += 25;
  if (data.profileCompleted) score += 15;
  score += Math.min((data.completedJobsCount || 0) * 5, 20);
  score += Math.min((data.endorsementsCount || 0) * 2, 15);

  const reviewsSnap = await db
    .collection("reviews")
    .where("reviewedUserId", "==", uid)
    .get();
  if (!reviewsSnap.empty) {
    const ratings = reviewsSnap.docs.map((d) => d.data().rating || 0);
    const avg = ratings.reduce((a, b) => a + b, 0) / ratings.length;
    if (avg >= 4.5) score += 10;
    else if (avg >= 4.0) score += 8;
    else if (avg >= 3.5) score += 6;
  }

  return Math.min(score, 100);
}

async function writeTrustScore(uid) {
  const score = await computeTrustScoreFor(uid);
  await db.collection("users").doc(uid).update({ trustScore: score });
  return score;
}

// ----------------------------- PUSH NOTIFICATIONS -----------------------
// Deliver an FCM data message to a user's device. The client persists the
// notification into the user's own notifications doc on receipt, so the server
// only needs to target the device token. Failures (stale tokens, disabled
// notifications) are intentionally non-fatal so a broken token never breaks a
// business operation.
async function sendPushNotification(uid, payload) {
  if (!uid) return;
  let userDoc;
  try {
    userDoc = await db.collection("users").doc(uid).get();
  } catch (err) {
    console.warn("push: could not read user doc", uid, err.message);
    return;
  }
  if (!userDoc.exists) return;
  const user = userDoc.data() || {};
  const token = user.fcmToken;
  if (!token) return;
  if (user.notificationsEnabled === false) return;

  const message = {
    token,
    notification: {
      title: String(payload.title || ""),
      body: String(payload.body || ""),
    },
    data: {
      title: String(payload.title || ""),
      body: String(payload.body || ""),
      screen: String(payload.screen || "home"),
      channel: String(payload.channel || "kaamio_general"),
    },
  };

  try {
    await admin.messaging().send(message);
  } catch (err) {
    if (err.code === "messaging/invalid-registration-token" || err.code === "messaging/registration-token-not-registered") {
      // Stale token from a reinstalled/logged-out device: drop it so the next
      // login pushes a fresh one.
      await db
        .collection("users")
        .doc(uid)
        .update({ fcmToken: FIELD.delete() })
        .catch(() => {});
    }
  }
}

// Notify the listing owner when a candidate applies, and the applicant when
// their application status changes. Both documents are written by the client,
// but the rules already pin party fields, so this trigger only reads.
exports.onApplicationWritten = onDocumentWritten("applications/{applicationId}", async (event) => {
  const after = event.data.after;
  if (!after || !after.exists) return;
  const data = after.data() || {};
  const before = event.data.before;
  const prev = before && before.exists ? before.data() : null;

  const applicantId = data.applicantId;
  const ownerId = data.ownerId;

  if (!before || !before.exists) {
    // New application -> tell the owner.
    if (!applicantId || !ownerId) return;
    let body = "A specialist just applied to your listing.";
    try {
      const listing = await db.collection("listings").doc(data.jobId).get();
      const jobTitle = listing.exists ? (listing.data().title || "") : "";
      if (jobTitle) body = `New application for "${jobTitle}".`;
    } catch (_) {}
    await sendPushNotification(ownerId, {
      title: "New application received",
      body,
      screen: "my-work",
      channel: "kaamio_jobs",
    });
    return;
  }

  const status = data.status;
  const prevStatus = prev.status;
  if (!status || status === prevStatus || !applicantId) return;
  const title = "Application update";
  const body =
    status === "accepted"
      ? "Congratulations! Your application was accepted."
      : status === "rejected"
      ? "Your application was not selected this time."
      : status === "completed"
      ? "Your job was marked completed. Thank you!"
      : status === "cancelled"
      ? "Your application was withdrawn."
      : `Your application status changed to ${status}.`;
  await sendPushNotification(applicantId, { title, body, screen: "my-work", channel: "kaamio_jobs" });
});

// Keep both parties in the loop as an escrow moves through its lifecycle.
exports.onEscrowWritten = onDocumentWritten("escrow/{escrowId}", async (event) => {
  const after = event.data.after;
  if (!after || !after.exists) return;
  const data = after.data() || {};
  const before = event.data.before;
  const prev = before && before.exists ? before.data() : null;
  const status = data.status;
  if (!status || (prev && prev.status === status)) return;

  const employerId = data.employerId;
  const workerId = data.workerId;

  const notify = async (uid, title, body, screen) => {
    if (!uid) return;
    await sendPushNotification(uid, { title, body, screen, channel: "kaamio_payments" });
  };

  switch (status) {
    case "FUNDED":
      await notify(workerId, "Escrow funded", "Payment is secured for your job. You can start work now.", "my-work");
      break;
    case "IN_PROGRESS":
      await notify(employerId, "Work started", "The worker has started on your job.", "my-work");
      break;
    case "COMPLETED":
      await notify(employerId, "Work completed", "The worker marked the job complete. Review and release funds.", "my-work");
      break;
    case "RELEASED":
      await notify(workerId, "Payment released", "Funds have been released to your account.", "my-work");
      break;
    case "REFUNDED":
      await notify(employerId, "Escrow refunded", "Your escrow payment was refunded.", "my-work");
      break;
    case "DISPUTED":
      await notify(employerId, "Escrow in dispute", "A dispute was opened on this escrow.", "my-work");
      await notify(workerId, "Escrow in dispute", "A dispute was opened on this escrow.", "my-work");
      break;
    default:
      break;
  }
});

// Deliver a chat message push to the partner as soon as it lands.
exports.onChatMessageWritten = onDocumentWritten("chats/{messageId}", async (event) => {
  const after = event.data.after;
  if (!after || !after.exists) return;
  if (event.data.before && event.data.before.exists) return; // create-only
  const data = after.data() || {};
  const partnerId = data.partnerId;
  if (!partnerId) return;
  const senderName = data.senderName || data.senderId || "Someone";
  const isProposal = !!data.proposalStatus;
  const text = data.messageText;
  if (isProposal) {
    await sendPushNotification(partnerId, {
      title: "New proposal from " + String(senderName),
      body: String(text || "Open the conversation to review."),
      screen: "chat",
      channel: "kaamio_messages",
    });
  } else {
    await sendPushNotification(partnerId, {
      title: String(senderName),
      body: String(text || "New message"),
      screen: "chat",
      channel: "kaamio_messages",
    });
  }
});

exports.ensureUserProfile = onCall(async (request) => {
  const uid = requireAuth(request);

  const data = request.data || {};
  const displayName = String(data.displayName || "").trim().slice(0, 80);
  const email = String(data.email || "").trim().toLowerCase();
  const phoneNumber = String(data.phoneNumber || "").trim();
  const photoURL = String(data.photoURL || "").trim();

  const year = new Date().getUTCFullYear();
  const random = Math.floor(100000 + Math.random() * 900000);
  const kaamioId = `KAA-${year}-${random}`;

  const userRef = db.collection("users").doc(uid);
  const userDoc = await userRef.get();

  if (!userDoc.exists) {
    await userRef.set(
      {
        uid,
        kaamioId,
        displayName,
        email,
        phoneNumber,
        photoURL,
        role: "User",
        province: "",
        district: "",
        municipality: "",
        userTypes: "",
        skills: "",
        experience: "",
        language: "English",
        verified: false,
        profileCompleted: false,
        isPhoneVerified: !!phoneNumber,
        isGoogleVerified: request.auth.token.firebase?.sign_in_provider === "google.com",
        isIdentityVerified: false,
        trustScore: 0,
        verificationLevel: 0,
        // Privacy is opt-out, not opt-in: a missing privacyEnabled must never
        // make a profile public (rules treat null != true as public).
        privacyEnabled: true,
        notificationsEnabled: true,
        preferredLanguage: "English",
        kycStatus: "",
        createdAt: FIELD.serverTimestamp(),
        lastLogin: FIELD.serverTimestamp(),
        isOnline: true,
        status: "active",
        gender: "",
        dateOfBirth: ""
      },
      { merge: true }
    );
  } else {
    await userRef.set(
      {
        displayName,
        email,
        phoneNumber,
        photoURL,
        lastLogin: FIELD.serverTimestamp(),
        isOnline: true
      },
      { merge: true }
    );
  }

  return { ok: true };
});

exports.updateLastLogin = onCall(async (request) => {
  const uid = requireAuth(request);
  await db.collection("users").doc(uid).set(
    { lastLogin: FIELD.serverTimestamp(), isOnline: true },
    { merge: true }
  );
  return { ok: true };
});

// Single consolidated trust-score endpoint. Recomputes the caller's own score,
// or a target user's score when the caller shares a completed/accepted job or
// is an admin, so a freshly-posted review updates the reviewed user's score.
exports.recomputeTrustScore = onCall(async (request) => {
  const uid = requireAuth(request);
  const targetUid = String(request.data?.targetUid || "");

  if (targetUid && targetUid !== uid) {
    const isAdmin = (await db.collection("users").doc(uid).get()).data()?.role === "admin";
    const sharedWork = await db
      .collection("applications")
      .where("applicantId", "==", uid)
      .where("status", "in", ["accepted", "completed"])
      .get();
    const linked = sharedWork.docs.some((d) => d.data().ownerId === targetUid);
    if (!isAdmin && !linked) {
      throw new HttpsError("permission-denied", "Not authorized to update this user's trust score");
    }
  }

  const score = await writeTrustScore(targetUid || uid);
  return { trustScore: score };
});

// Auto-recompute the reviewed user's trust score AND aggregate rating whenever
// a review is created/updated. Ratings are now computed server-side only, so a
// client can never write its own averageRating/totalReviews.
exports.onReviewWritten = onDocumentWritten("reviews/{reviewId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  const reviewedUid = after?.reviewedUserId || before?.reviewedUserId;
  if (!reviewedUid) return;
  try {
    await writeTrustScore(reviewedUid);

    const reviewsSnap = await db
      .collection("reviews")
      .where("reviewedUserId", "==", reviewedUid)
      .get();
    const ratings = reviewsSnap.docs.map((d) => d.data().rating || 0);
    const total = ratings.length;
    const average = total > 0 ? ratings.reduce((a, b) => a + b, 0) / total : 0;
    await db.collection("users").doc(reviewedUid).set(
      { averageRating: average, totalReviews: total },
      { merge: true }
    );

    if (after && (!before || !before.reviewedUserId)) {
      await sendPushNotification(reviewedUid, {
        title: "New review",
        body: `You received a ${after.rating || 5}-star review. Your trust score was updated.`,
        screen: "trust",
        channel: "kaamio_general",
      });
    }
  } catch (e) {
    console.error("onReviewWritten failed for", reviewedUid, e);
  }
});

// ----------------------------- KHALTI -----------------------------------

exports["khalti-initiatePayment"] = onCall(async (request) => {
  const uid = requireAuth(request);

  const { amount, orderId, productName, customerName, customerEmail, customerPhone } = request.data || {};
  if (!amount || !orderId) throw new HttpsError("invalid-argument", "amount and orderId required");

  const secret = process.env.KHALTI_SECRET_KEY;
  if (!secret) throw new HttpsError("failed-precondition", "Khalti secret key not configured");

  try {
    const body = {
      return_url: `https://kaamio.com/payment/callback?orderId=${orderId}`,
      website_url: "https://kaamio.com",
      // Khalti expects the amount in paisa. The app and Firestore store amounts
      // in NPR everywhere (escrow docs included) so reconciliation in
      // escrow-markFunded compares like-for-like units. Conversion happens only
      // here, at the gateway boundary.
      amount: Math.round(amount) * 100,
      purchase_order_id: orderId,
      purchase_order_name: productName || "Kaamio Service",
      customer_info: {
        name: customerName || "Customer",
        email: customerEmail || "",
        phone: customerPhone || "9800000000",
      },
    };

    const resp = await fetch("https://a.khalti.com/api/v2/epayment/initiate/", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Key ${secret}` },
      body: JSON.stringify(body),
    });
    const json = await resp.json();
    if (!resp.ok) throw new Error(json.detail || "Khalti initiation failed");

    await db.collection("payments").doc(orderId).set(
      {
        uid,
        orderId,
        pidx: json.pidx,
        amount,
        status: "pending",
        gateway: "khalti",
        createdAt: FIELD.serverTimestamp(),
      },
      { merge: true }
    );

    return { success: true, pidx: json.pidx, paymentUrl: json.payment_url };
  } catch (e) {
    throw new HttpsError("internal", e.message || "Khalti initiation error");
  }
});

exports["khalti-verifyPayment"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { pidx } = request.data || {};
  if (!pidx) throw new HttpsError("invalid-argument", "pidx required");

  const secret = process.env.KHALTI_SECRET_KEY;
  if (!secret) throw new HttpsError("failed-precondition", "Khalti secret key not configured");

  try {
    const resp = await fetch("https://a.khalti.com/api/v2/epayment/lookup/", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Key ${secret}` },
      body: JSON.stringify({ pidx }),
    });
    const json = await resp.json();
    if (!resp.ok) throw new Error(json.detail || "Khalti verification failed");

    const completed = json.status === "Completed";
    const transactionId = json.transaction_id || "";

    if (completed) {
      const snapshot = await db.collection("payments").where("pidx", "==", pidx).get();
      snapshot.forEach((doc) => {
        doc.ref.update({ status: "completed", transactionId, verifiedAt: FIELD.serverTimestamp() });
      });
    }

    return { success: completed, status: json.status, transactionId, pidx };
  } catch (e) {
    throw new HttpsError("internal", e.message || "Khalti verification error");
  }
});

exports["khalti-processRefund"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { transactionId, amount } = request.data || {};
  if (!transactionId || !amount) throw new HttpsError("invalid-argument", "transactionId and amount required");

  const secret = process.env.KHALTI_SECRET_KEY;
  if (!secret) throw new HttpsError("failed-precondition", "Khalti secret key not configured");

  // Ownership and status must be verified BEFORE any gateway call: any
  // authenticated user must only be able to refund transactions they own,
  // and a payment that is already refunded must never be refunded twice.
  const snapshot = await db
    .collection("payments")
    .where("uid", "==", uid)
    .get();
  const paymentDoc = snapshot.docs.find(
    (d) => d.data().pidx === transactionId || d.data().transactionId === transactionId
  );
  if (!paymentDoc || !paymentDoc.exists) {
    throw new HttpsError("permission-denied", "Not your payment or payment not found");
  }
  const pay = paymentDoc.data() || {};
  if (pay.status === "refunded" || pay.status === "REFUNDED") {
    return { success: true, status: "refunded", refundId: pay.refundId || "", already: true };
  }

  try {
    const resp = await fetch("https://a.khalti.com/api/v2/epayment/refund/", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Key ${secret}` },
      body: JSON.stringify({ transaction_id: transactionId, amount: Math.round(amount) * 100 }),
    });
    const json = await resp.json();
    if (!resp.ok) throw new Error(json.detail || "Khalti refund failed");

    await paymentDoc.ref.update({ status: "refunded", refundId: json.refund_id, refundedAt: FIELD.serverTimestamp() });

    return { success: true, status: "refunded", refundId: json.refund_id };
  } catch (e) {
    throw new HttpsError("internal", e.message || "Khalti refund error");
  }
});

// ----------------------------- ESEWA ------------------------------------

// eSewa mandates the response signature: SHA256 of the signed fields joined
// with commas. Missing this meant payments could never be validated server-side.
function esewaSignature(fields) {
  const secret = process.env.ESEWA_SECRET_KEY || "";
  return crypto.createHmac("sha256", secret).update(fields).digest("base64");
}

exports["esewa-initiatePayment"] = onCall(async (request) => {
  const uid = requireAuth(request);

  const { amount, orderId, productName } = request.data || {};
  if (!amount || !orderId) throw new HttpsError("invalid-argument", "amount and orderId required");

  const esewaUrl = process.env.ESEWA_URL || "https://rc-epay.esewa.com.np/api/epay/main/v2/form";
  const merchantCode = process.env.ESEWA_MERCHANT_CODE || "";
  const transactionUuid = `${orderId}-${Date.now()}`;
  const signedFieldNames = "total_amount,transaction_uuid,product_code";
  // Must be a string (eSewa expects a string total) and declared BEFORE the
  // signature is computed to avoid a TDZ "Cannot access 'body' before
  // initialization" reference inside the object literal.
  const totalAmount = amount.toString();

  const body = {
    amount: totalAmount,
    tax_amount: "0",
    total_amount: totalAmount,
    transaction_uuid: transactionUuid,
    product_code: merchantCode,
    product_service_charge: "0",
    product_delivery_charge: "0",
    product_name: productName || "Kaamio Service",
    success_url: `https://kaamio.com/payment/esewa/success?orderId=${orderId}`,
    failure_url: `https://kaamio.com/payment/esewa/failure?orderId=${orderId}`,
    signed_field_names: signedFieldNames,
    signature: esewaSignature(`${totalAmount},${transactionUuid},${merchantCode}`),
  };

  await db.collection("payments").doc(orderId).set(
    {
      uid,
      orderId,
      transactionUuid,
      amount,
      status: "pending",
      gateway: "esewa",
      createdAt: FIELD.serverTimestamp(),
    },
    { merge: true }
  );

  return { success: true, transactionId: transactionUuid, paymentUrl: esewaUrl, formData: body };
});

exports["esewa-verifyPayment"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { transactionId } = request.data || {};
  if (!transactionId) throw new HttpsError("invalid-argument", "transactionId required");

  try {
    const esewaUrl = process.env.ESEWA_URL || "https://rc-epay.esewa.com.np/api/epay/transaction/status/";
    const merchantCode = process.env.ESEWA_MERCHANT_CODE || "KAAMIO";

    const resp = await fetch(`${esewaUrl}?product_code=${merchantCode}&transaction_uuid=${transactionId}`, {
      method: "GET",
    });
    const json = await resp.json();
    const completed = json.status === "COMPLETE";
    const refId = json.ref_id || "";

    if (completed) {
      const snapshot = await db.collection("payments").where("transactionUuid", "==", transactionId).get();
      snapshot.forEach((doc) => {
        doc.ref.update({ status: "completed", refId, verifiedAt: FIELD.serverTimestamp() });
      });
    }

    return { success: completed, status: json.status, refId };
  } catch (e) {
    throw new HttpsError("internal", e.message || "ESewa verification error");
  }
});

exports["esewa-processRefund"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { transactionId, amount } = request.data || {};
  if (!transactionId) throw new HttpsError("invalid-argument", "transactionId required");

  const merchantCode = process.env.ESEWA_MERCHANT_CODE || "";
  const secret = process.env.ESEWA_SECRET_KEY || "";
  if (!merchantCode || !secret) {
    throw new HttpsError("failed-precondition", "eSewa merchant credentials not configured");
  }

  try {
    const paymentSnap = await db
      .collection("payments")
      .where("transactionUuid", "==", transactionId)
      .get();
    const paymentDoc = paymentSnap.docs[0];
    if (!paymentDoc || !paymentDoc.exists) {
      throw new HttpsError("not-found", "Payment not found");
    }
    const pay = paymentDoc.data() || {};
    if (pay.uid !== uid) throw new HttpsError("permission-denied", "Not your payment");

    const refId = pay.refId || "";
    const refundAmount = amount ? Math.round(amount).toString() : (pay.amount ? String(Math.round(pay.amount)) : "0");

    const body = new URLSearchParams({
      refundAmount,
      referenceId: refId,
      transactionId,
    });

    // Must point at the SAME environment as initiation/verification. Hardcoded
    // production here while init/verify use ESEWA_URL caused dev refunds to
    // always fail (and mixed-env token mismatches).
    const esewaRefundUrl = process.env.ESEWA_REFUND_URL
      || `${(process.env.ESEWA_URL || "https://rc-epay.esewa.com.np").replace(/\/form$/, "").replace(/\/transaction\/status\/$/, "")}/api/merchant/refund`;

    const resp = await fetch(esewaRefundUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Basic ${Buffer.from(`${merchantCode}:${secret}`).toString("base64")}`,
      },
      body: body.toString(),
    });

    let refundId = "";
    let ok = resp.ok;
    if (ok) {
      const json = await resp.json().catch(() => ({}));
      refundId = json.refundId || "";
      // eSewa returns an error payload inside a 200 body on failure.
      if (json.refundId && json.status === "REFUNDED") {
        ok = true;
      } else if (json.error || json.message) {
        ok = false;
      }
    }

    if (!ok) {
      await paymentDoc.ref.update({ status: "REFUND_PENDING", refundAttemptedAt: FIELD.serverTimestamp() });
      throw new HttpsError("internal", "eSewa refund rejected");
    }

    await paymentDoc.ref.update({ status: "refunded", refundId, refundedAt: FIELD.serverTimestamp() });
    return { success: true, status: "refunded", refundId };
  } catch (e) {
    if (e instanceof HttpsError) throw e;
    throw new HttpsError("internal", e.message || "ESewa refund error");
  }
});

// ----------------------------- ESCROW -----------------------------------
// Server-authoritative state machine. Clients create an escrow, then all
// transitions (fund, start work, release, refund, dispute) happen through
// these functions after verification — never via direct Firestore writes.

exports["escrow-createEscrow"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { jobId, workerId, amount } = request.data || {};
  if (!jobId || !workerId || !amount) throw new HttpsError("invalid-argument", "jobId, workerId, and amount required");
  if (typeof amount !== "number" || amount <= 0) throw new HttpsError("invalid-argument", "amount must be a positive number");
  if (workerId === uid) throw new HttpsError("invalid-argument", "Cannot escrow with yourself");

  const escrowId = `escrow_${jobId}_${workerId}`;
  const escrowRef = db.collection("escrow").doc(escrowId);
  const existing = await escrowRef.get();
  if (existing.exists) {
    return { success: true, escrowId, status: existing.data().status };
  }

  await escrowRef.set({
    jobId,
    employerId: uid,
    workerId,
    amount,
    status: "PENDING_FUNDING",
    createdAt: FIELD.serverTimestamp(),
  });
  return { success: true, escrowId, status: "PENDING_FUNDING" };
});

// Called after Khalti/eSewa verification succeeds. Only the employer may fund,
// and ONLY a completed payment whose orderId matches this escrow — that has not
// already been consumed by another escrow/course — may move it forward.
exports["escrow-markFunded"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { escrowId, transactionId } = request.data || {};
  if (!escrowId) throw new HttpsError("invalid-argument", "escrowId required");
  if (!transactionId) throw new HttpsError("invalid-argument", "transactionId (pidx) required");

  const escrowRef = db.collection("escrow").doc(escrowId);
  const escrowDoc = await escrowRef.get();
  if (!escrowDoc.exists) throw new HttpsError("not-found", "Escrow not found");
  const data = escrowDoc.data() || {};
  if (data.employerId !== uid) throw new HttpsError("permission-denied", "Only the employer can fund escrow");
  if (data.status !== "PENDING_FUNDING") {
    throw new HttpsError("failed-precondition", "Escrow is not awaiting funding");
  }

  const payments = await db
    .collection("payments")
    .where("uid", "==", uid)
    .where("status", "==", "completed")
    .get();
  const payment = payments.docs.find(
    (d) => (d.data().pidx === transactionId || d.data().transactionId === transactionId || d.data().refId === transactionId)
  );
  if (!payment) throw new HttpsError("failed-precondition", "Payment not verified");

  const pay = payment.data() || {};
  if (pay.orderId && pay.orderId !== escrowId) {
    throw new HttpsError("failed-precondition", "Payment does not match this escrow");
  }
  if (pay.consumedFor && pay.consumedFor !== escrowId) {
    throw new HttpsError("failed-precondition", "Payment already used for another order");
  }

  // Amount reconciliation: a completed payment of any size must not be able to
  // fund a different-value escrow (e.g. Rs. 1 funding a Rs. 100,000 escrow).
  if (Math.round(Number(pay.amount)) !== Math.round(Number(data.amount))) {
    throw new HttpsError("failed-precondition", "Payment amount does not match the escrow amount");
  }

  await payment.ref.update({ consumedFor: escrowId, consumedAt: FIELD.serverTimestamp() });
  await escrowRef.update({
    status: "FUNDED",
    fundingTransactionId: transactionId,
    fundedAt: FIELD.serverTimestamp(),
  });
  return { success: true, status: "FUNDED" };
});

// Worker signals work has started: FUNDED -> IN_PROGRESS.
exports["escrow-startWork"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { escrowId } = request.data || {};
  if (!escrowId) throw new HttpsError("invalid-argument", "escrowId required");

  const escrowDoc = await db.collection("escrow").doc(escrowId).get();
  if (!escrowDoc.exists) throw new HttpsError("not-found", "Escrow not found");
  const data = escrowDoc.data() || {};
  if (data.workerId !== uid) throw new HttpsError("permission-denied", "Only the worker can start work");
  if (data.status !== "FUNDED") throw new HttpsError("failed-precondition", "Escrow is not funded");

  await escrowDoc.ref.update({ status: "IN_PROGRESS", startedAt: FIELD.serverTimestamp() });
  return { success: true, status: "IN_PROGRESS" };
});

exports["escrow-markComplete"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { escrowId } = request.data || {};
  if (!escrowId) throw new HttpsError("invalid-argument", "escrowId required");

  const escrowDoc = await db.collection("escrow").doc(escrowId).get();
  if (!escrowDoc.exists) throw new HttpsError("not-found", "Escrow not found");
  const data = escrowDoc.data() || {};
  if (data.workerId !== uid) throw new HttpsError("permission-denied", "Only the worker can mark work complete");
  if (data.status !== "IN_PROGRESS") throw new HttpsError("failed-precondition", "Work must be in progress");

  await escrowDoc.ref.update({ status: "COMPLETED", completedAt: FIELD.serverTimestamp() });
  return { success: true, status: "COMPLETED" };
});

exports["escrow-releaseFunds"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { escrowId } = request.data || {};
  if (!escrowId) throw new HttpsError("invalid-argument", "escrowId required");

  const escrowDoc = await db.collection("escrow").doc(escrowId).get();
  if (!escrowDoc.exists) throw new HttpsError("not-found", "Escrow not found");
  const data = escrowDoc.data() || {};
  if (data.employerId !== uid) throw new HttpsError("permission-denied", "Only the employer can release funds");
  if (!["FUNDED", "IN_PROGRESS", "COMPLETED"].includes(data.status)) {
    throw new HttpsError("failed-precondition", "Escrow cannot be released in current state");
  }

  await escrowDoc.ref.update({ status: "RELEASED", releasedAt: FIELD.serverTimestamp() });

  // Reward the worker: completed job count + trust score recompute.
  await db.collection("users").doc(data.workerId).set(
    { completedJobsCount: FIELD.increment(1) },
    { merge: true }
  );
  await writeTrustScore(data.workerId).catch(() => {});

  return { success: true, status: "RELEASED" };
});

exports["escrow-refund"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { escrowId } = request.data || {};
  if (!escrowId) throw new HttpsError("invalid-argument", "escrowId required");

  const escrowDoc = await db.collection("escrow").doc(escrowId).get();
  if (!escrowDoc.exists) throw new HttpsError("not-found", "Escrow not found");
  const data = escrowDoc.data() || {};
  if (data.employerId !== uid) throw new HttpsError("permission-denied", "Only the employer can refund");
  if (!["FUNDED", "IN_PROGRESS", "COMPLETED"].includes(data.status)) {
    throw new HttpsError("failed-precondition", "Escrow cannot be refunded in current state");
  }

  // Real money must move before we mark anything refunded. No fake refunds.
  const pidx = data.fundingTransactionId;
  if (!pidx) throw new HttpsError("failed-precondition", "No gateway transaction to refund");

  const secret = process.env.KHALTI_SECRET_KEY;
  if (!secret) throw new HttpsError("failed-precondition", "Khalti secret key not configured");

  try {
    const resp = await fetch("https://a.khalti.com/api/v2/epayment/refund/", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Key ${secret}` },
      body: JSON.stringify({ transaction_id: pidx, amount: Math.round(Number(data.amount)) * 100 }),
    });
    const json = await resp.json();
    if (!resp.ok) throw new Error(json.detail || "Khalti refund failed");

    await escrowDoc.ref.update({
      status: "REFUNDED",
      refundId: json.refund_id,
      refundedAt: FIELD.serverTimestamp(),
    });

    // Release the payment record so it cannot be re-consumed.
    const payments = await db.collection("payments").where("pidx", "==", pidx).get();
    payments.forEach((doc) => {
      doc.ref.update({ status: "refunded", refundId: json.refund_id, refundedAt: FIELD.serverTimestamp() });
    });

    return { success: true, status: "REFUNDED", refundId: json.refund_id };
  } catch (e) {
    throw new HttpsError("internal", e.message || "Refund could not be processed");
  }
});

exports["escrow-dispute"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { escrowId, reason } = request.data || {};
  if (!escrowId) throw new HttpsError("invalid-argument", "escrowId required");

  const escrowDoc = await db.collection("escrow").doc(escrowId).get();
  if (!escrowDoc.exists) throw new HttpsError("not-found", "Escrow not found");
  const data = escrowDoc.data() || {};
  if (data.employerId !== uid && data.workerId !== uid) {
    throw new HttpsError("permission-denied", "Only escrow participants can dispute");
  }
  if (data.status === "RELEASED" || data.status === "REFUNDED") {
    throw new HttpsError("failed-precondition", "Escrow already settled");
  }

  await escrowDoc.ref.update({
    status: "DISPUTED",
    disputeReason: reason || "",
    disputedBy: uid,
    disputedAt: FIELD.serverTimestamp(),
  });
  return { success: true, status: "DISPUTED" };
});

// ----------------------------- CHAT -------------------------------------
// Chat messages are immutable from clients. Proposal status changes are the
// only allowed mutation and are enforced server-side.
exports["chat-updateProposal"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { messageId, status } = request.data || {};
  if (!messageId || !status) throw new HttpsError("invalid-argument", "messageId and status required");
  if (!["PENDING", "ACCEPTED", "REJECTED", "COMPLETED"].includes(status)) {
    throw new HttpsError("invalid-argument", "Invalid proposal status");
  }

  const msgRef = db.collection("chats").doc(messageId);
  const msgDoc = await msgRef.get();
  if (!msgDoc.exists) throw new HttpsError("not-found", "Message not found");
  const data = msgDoc.data() || {};
  const participants = data.participantIds || [];
  if (!participants.includes(uid)) {
    throw new HttpsError("permission-denied", "Not a participant in this chat");
  }
  const currentStatus = data.proposalStatus || "PENDING";
  if (currentStatus !== "PENDING") {
    throw new HttpsError("failed-precondition", "Proposal already handled");
  }

  await msgRef.update({
    proposalStatus: status,
    updatedAt: FIELD.serverTimestamp(),
    updatedBy: uid,
  });
  return { success: true, proposalStatus: status };
});

// ----------------------------- COURSES ----------------------------------
// Premium courses can only be unlocked after a verified gateway payment bound
// to THIS course that has not been consumed elsewhere. The client never marks
// a course unlocked on its own.
exports["course-unlock"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const { courseId, transactionId } = request.data || {};
  if (!courseId) throw new HttpsError("invalid-argument", "courseId required");
  if (!transactionId) throw new HttpsError("invalid-argument", "transactionId (pidx) required");

  const courseRef = db.collection("courses").doc(courseId);
  const courseDoc = await courseRef.get();
  if (!courseDoc.exists) throw new HttpsError("not-found", "Course not found");
  const course = courseDoc.data() || {};

  // Idempotent: already unlocked for this user.
  const unlockedBy = Array.isArray(course.unlockedBy) ? course.unlockedBy : [];
  if (unlockedBy.includes(uid)) return { success: true };

  const payments = await db
    .collection("payments")
    .where("uid", "==", uid)
    .where("status", "==", "completed")
    .get();
  const payment = payments.docs.find(
    (d) => (d.data().pidx === transactionId || d.data().transactionId === transactionId || d.data().refId === transactionId)
  );
  if (!payment) throw new HttpsError("failed-precondition", "Payment not verified");

  const pay = payment.data() || {};
  const orderId = pay.orderId || "";
  const isCourseOrder = orderId === courseId || orderId.startsWith(`course_${courseId}_`);
  if (pay.orderId && !isCourseOrder) {
    throw new HttpsError("failed-precondition", "Payment does not match this course");
  }
  if (pay.consumedFor && pay.consumedFor !== courseId) {
    throw new HttpsError("failed-precondition", "Payment already used for another order");
  }

  // Amount reconciliation: the completed payment must cover the course price.
  const coursePrice = priceAsNumber(course.price);
  if (coursePrice != null && Math.round(Number(pay.amount)) < Math.round(coursePrice)) {
    throw new HttpsError("failed-precondition", "Payment amount is less than the course price");
  }

  await payment.ref.update({ consumedFor: courseId, consumedAt: FIELD.serverTimestamp() });
  await courseRef.update({ unlockedBy: FIELD.arrayUnion(uid) });
  return { success: true };
});

// ----------------------------- KYC --------------------------------------
// Admin-only review of a submitted KYC document. Flips the user's identity
// flag and recomputes their trust score; never done client-side.
exports["kyc-review"] = onCall(async (request) => {
  const uid = requireAuth(request);
  const admin = await db.collection("users").doc(uid).get();
  if (admin.data()?.role !== "admin") {
    throw new HttpsError("permission-denied", "Admins only");
  }

  const { targetUid, approved, reason } = request.data || {};
  if (!targetUid) throw new HttpsError("invalid-argument", "targetUid required");

  const userRef = db.collection("users").doc(targetUid);
  const kycRef = db.collection("kyc").doc(targetUid);

  if (approved) {
    await userRef.update({
      isIdentityVerified: true,
      verified: true,
      verificationLevel: 3,
    });
    await kycRef.set(
      { status: "verified", reviewedBy: uid, reviewedAt: FIELD.serverTimestamp() },
      { merge: true }
    );
  } else {
    await kycRef.set(
      {
        status: "resubmit",
        reviewReason: reason || "",
        reviewedBy: uid,
        reviewedAt: FIELD.serverTimestamp(),
      },
      { merge: true }
    );
  }

  await writeTrustScore(targetUid).catch(() => {});
  return { success: true, approved: !!approved };
});

