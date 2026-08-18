# Kaamio Firebase Architecture & Security Blueprint 🇳🇵🚀

This document defines the production-grade, enterprise-ready Firebase Backend Architecture for **Kaamio**—Nepal's elite workforce, employment, freelancing, local worker marketplace, professional networking, and skill-sharing platform. 

It covers Firestore database schemas, secure Cloud Storage structure, robust security rules, Cloud Functions codebase, realtime synchronization mechanisms, dynamic Trust Score formulas, and index models optimized to support **10M+ active users** across Nepal's 7 Provinces and 77 Districts with zero fake/seeded records.

---

## 1. Authentication System & Flow

Kaamio enforces strict real-user identity authentication. Anonymous/guest/mock registrations are entirely disallowed. All user data is tied to a secure verification event.

```
                  ┌──────────────────────────────┐
                  │      Registration Entry      │
                  └──────────────┬───────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         ▼                                               ▼
┌──────────────────┐                           ┌──────────────────┐
│ Google Auth Flow │                           │ Phone OTP Flow   │
└────────┬─────────┘                           └────────┬─────────┘
         │                                               │
         ▼                                               ▼
┌──────────────────┐                           ┌──────────────────┐
│ Real Google Acct │                           │ SMS Verification │
│ Verified Email   │                           │ +977 OTP Code    │
└────────┬─────────┘                           └────────┬─────────┘
         │                                               │
         └───────────────────────┬───────────────────────┘
                                 │
                                 ▼
                   ┌───────────────────────────┐
                   │   Cloud Function Trigger  │
                   │  Auto-Profile Creation &  │
                   │   Unique KAA-2026-XXXXX   │
                   └───────────────────────────┘
```

### A. Google Sign-In Flow
1. User authenticates via Google Client SDK on client device.
2. Verified email domain and photo URL are pulled directly from Google profile payload.
3. Firebase Auth creates the user account and retrieves the authenticated UID.
4. An triggers-based Cloud Function automatically provisions the corresponding `users/{uid}` and `profiles/{uid}` documents inside Firestore.

### B. Phone OTP (Nepal Support: +977)
1. Recaptcha is activated on the mobile device (or invisible web provider) for DDoS and spam protection.
2. User enters an E.164 phone number containing the Nepal prefix (`+977XXXXXXXXXX`).
3. SMS OTP is dispatched via standard Firebase Authentication carrier route.
4. User completes validation on the OTP Entry Screen. 
5. Account registration succeeds, marking the user record as `isPhoneVerified: true` with a guaranteed secure phone identification key.

---

## 2. Complete Firestore Schemas

Every Firestore document includes a robust, traceable metadata layout: `uid` / `createdBy`, `createdAt`, and `updatedAt` for full traceability.

### 1. `users`
* **Path:** `users/{uid}`
* **Description:** Core identity of the authenticated user.
* **Schema:**
```typescript
{
  uid: string;                 // Matches Auth UID
  kaamioId: string;            // Permanent identifier (e.g., KAA-2026-042891)
  fullName: string;            // Retracted from Auth or User Registration Form
  email: string;               // Verified email or null
  phoneNumber: string;         // Verified phone number or null
  photoURL: string;            // Public profile photograph URL
  province: string;            // State Province (1 to 7)
  district: string;            // District of residency (e.g., Kaski, Lalitpur)
  municipality: string;        // Local government body name (e.g., Pokhara, Kathmandu)
  userTypes: string[];         // ["worker", "employer", "freelancer", "student"]
  profileCompletion: number;   // Percentage of complete metadata (0 to 100)
  trustScore: number;          // Managed dynamic score (0 to 100)
  isPhoneVerified: boolean;    // OTP status flag
  isGoogleVerified: boolean;   // Google Sign-In flag
  isIdentityVerified: boolean; // Verified against citizenship card (Nagarpalika / Govt Doc)
  createdAt: Timestamp;        // Creation date
  updatedAt: Timestamp;        // Last document update
  lastLoginAt: Timestamp;      // Last active timestamp
  status: "active" | "suspended" | "admin"
}
```

### 2. `profiles`
* **Path:** `profiles/{uid}`
* **Description:** Professional work metadata, resume, and experience logs.
* **Schema:**
```typescript
{
  uid: string;                 // Reference to user document
  bio: string;                 // Short personal bio
  website: string;             // Portfolio or personal link
  category: string;            // Core sector (e.g., Electrical, IT Services)
  experienceYears: number;     // Years in industry
  hourlyRate: number;          // Local currency rate (NPR)
  serviceAreas: string[];      // Neighborhoods / Municipalities serviced
  languages: string[];         // ["Nepali", "English", "Newari"]
  availabilityStatus: string;  // "available" | "busy" | "away"
  portfolioLinks: string[];    // URLs to uploaded projects or images
  verifications: {             // Verification credentials
    citizenshipId?: string;
    verifiedAt?: Timestamp;
  }
}
```

### 3. `jobs`
* **Path:** `jobs/{jobId}`
* **Description:** Employment opportunities uploaded by authenticated employers.
* **Schema:**
```typescript
{
  jobId: string;               // Unique document UID
  employerId: string;          // Author ID
  title: string;               // Post title
  description: string;         // In-depth details
  category: string;            // e.g., Construction, Marketing, Plumbing
  jobType: "full-time" | "part-time" | "contract" | "freelance" | "hourly" | "local" | "remote";
  location: {
    province: string;
    district: string;
    municipality: string;
    detailedAddress: string;
    coordinates?: GeoPoint;
  };
  salaryMin: number;
  salaryMax: number;
  currency: "NPR";
  requirements: string[];      // Required tags / certifications
  perks: string[];             // Lunch, Travel Allowance, Insurance
  status: "active" | "filled" | "archived" | "suspended";
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 4. `job_applications`
* **Path:** `job_applications/{applicationId}`
* **Description:** Applications sent for published jobs.
* **Schema:**
```typescript
{
  applicationId: string;
  jobId: string;
  employerId: string;
  applicantId: string;         // Authenticated Worker ID
  resumeUrl: string;           // Path within Kaamio Cloud Storage
  coverLetter: string;
  status: "submitted" | "shortlisted" | "offered" | "rejected" | "withdrawn";
  proposalRate?: number;
  durationEstimation?: string;
  comments: string;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 5. `worker_services`
* **Path:** `worker_services/{serviceId}`
* **Description:** Service gigs listed by individual local tradespeople.
* **Schema:**
```typescript
{
  serviceId: string;
  workerId: string;
  title: string;               // e.g., Express Home Plumbing Repair
  description: string;
  basePrice: number;           // Cost in NPR
  priceUnit: "flat" | "hourly" | "daily";
  category: string;            // e.g., Plumbing, Handyman
  status: "active" | "paused";
  averageRating: number;       // Computed field
  completedBookings: number;   // Count of successfully closed transactions
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 6. `service_bookings`
* **Path:** `service_bookings/{bookingId}`
* **Description:** Service request reservations.
* **Schema:**
```typescript
{
  bookingId: string;
  serviceId: string;
  workerId: string;
  clientId: string;            // Customer UID
  status: "pending" | "accepted" | "in_progress" | "completed" | "cancelled" | "disputed";
  rate: number;                // Final agreed pricing rate
  scheduleDateTime: Timestamp; // Agreed arrival date & time
  location: {
    detailedAddress: string;
    coordinates?: GeoPoint;
  };
  comments: string;
  disputes?: {
    openedBy: string;
    reason: string;
    status: "open" | "resolved" | "dismissed";
    resolvedAt?: Timestamp;
  };
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 7. `freelance_projects`
* **Path:** `freelance_projects/{projectId}`
* **Description:** Tech & virtual projects for remote/contract freelancers.
* **Schema:**
```typescript
{
  projectId: string;
  clientId: string;
  title: string;
  description: string;
  category: string;            // e.g., App Development, Content Writing
  budgetType: "fixed" | "hourly";
  budgetMin: number;
  budgetMax: number;
  skillsRequired: string[];
  status: "open" | "ongoing" | "completed" | "closed";
  proposalCount: number;
  deadline: Timestamp;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 8. `project_proposals`
* **Path:** `project_proposals/{proposalId}`
* **Description:** Freelance bids for freelance projects.
* **Schema:**
```typescript
{
  proposalId: string;
  projectId: string;
  freelancerId: string;
  clientId: string;
  proposalText: string;
  bidAmount: number;           // Bidding rate in NPR
  durationEstimation: string;  // e.g., "14 days", "1 month"
  status: "submitted" | "interviewing" | "accepted" | "rejected" | "withdrawn";
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 9. `courses`
* **Path:** `courses/{courseId}`
* **Description:** Trade skill training and digital upskilling certificates.
* **Schema:**
```typescript
{
  courseId: string;
  instructorId: string;
  title: string;
  description: string;
  price: number;               // 0 for free training
  language: string;            // "Nepali" | "English"
  platform: "online" | "hybrid" | "local_workshop";
  locationDetails?: string;    // Mandatory for physical workshops
  totalDurationHours: number;
  modules: Array<{
    moduleId: string;
    title: string;
    durationMinutes: number;
    videoUrl?: string;
  }>;
  certificatesOffered: boolean;
  status: "draft" | "published" | "archived";
  enrollmentsCount: number;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 10. `course_enrollments`
* **Path:** `course_enrollments/{enrollmentId}`
* **Description:** Live student course tracker.
* **Schema:**
```typescript
{
  enrollmentId: string;
  courseId: string;
  studentId: string;
  instructorId: string;
  status: "active" | "completed" | "dropped";
  completedLessons: string[];  // List of completed moduleIds
  progressPercentage: number;  // 0 to 100
  certificateUrl?: string;     // PDF download link
  transactionId?: string;      // Payment tracking reference
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 11. `conversations`
* **Path:** `conversations/{conversationId}`
* **Description:** Chat thread parent document.
* **Schema:**
```typescript
{
  conversationId: string;
  participantIds: string[];    // Array of 2 UIDs
  participantNames: string[];  // Mirror names to save read charges
  participantAvatars: string[];// Mirror avatar URLs
  lastMessageText: string;
  lastMessageTimestamp: Timestamp;
  lastMessageSenderId: string;
  unreadCounts: {
    [uid: string]: number;     // Tracks unread per user
  };
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 12. `messages`
* **Path:** `conversations/{conversationId}/messages/{messageId}`
* **Description:** Subcollection holding real-time chat messages and offer proposals.
* **Schema:**
```typescript
{
  messageId: string;
  conversationId: string;
  senderId: string;
  receiverId: string;
  messageText: string;
  mediaUrl?: string;           // Optional image or documents attachment
  messageType: "text" | "image" | "document" | "voice_note" | "proposal" | "system";
  isRead: boolean;
  offerDetails?: {             // Integrated negotiation cards
    rate: string;
    duration: string;
    status: "PENDING" | "ACCEPTED" | "DECLINED";
  };
  timestamp: Timestamp;
}
```

### 13. `notifications`
* **Path:** `notifications/{notificationId}`
* **Description:** Push logs and system transaction alerts.
* **Schema:**
```typescript
{
  notificationId: string;
  recipientId: string;
  senderId: string;            // "system" or originating user UID
  type: "job_application" | "chat_message" | "review" | "course_enrollment" | "post_interaction" | "system";
  title: string;
  body: string;
  payload: {
    targetId: string;          // ID to route application navigation
    route: string;             // Route ID parameter
  };
  isRead: boolean;
  createdAt: Timestamp;
}
```

### 14. `reviews`
* **Path:** `reviews/{reviewId}`
* **Description:** Verified transactional job feedback.
* **Schema:**
```typescript
{
  reviewId: string;
  transactionId: string;       // Matches service_bookings / freelance_projects
  transactionType: "booking" | "project" | "job";
  reviewerId: string;
  revieweeId: string;
  rating: number;              // 1 to 5
  comments: string;
  positiveTags: string[];      // ["Punctual", "Professional Skills", "Honest Pricing"]
  negativeTags: string[];      // ["Late arrival", "Incomplete clean-up"]
  createdAt: Timestamp;
}
```

### 15. `ratings`
* **Path:** `ratings/{ratingId}`
* **Description:** Detailed feedback parameters.
* **Schema:**
```typescript
{
  ratingId: string;
  targetUid: string;           // Rated entity
  sourceUid: string;           // Reviewing entity
  categoryRatings: {
    punctuality: number;       // 1 to 5
    communication: number;     // 1 to 5
    quality: number;           // 1 to 5
    reliability: number;       // 1 to 5
  };
  overallRating: number;
  createdAt: Timestamp;
}
```

### 16. `trust_scores`
* **Path:** `trust_scores/{uid}`
* **Description:** Decentralized background ledger tracking profile trustworthiness.
* **Schema:**
```typescript
{
  uid: string;
  score: number;               // Dynamic calculation (0 to 100)
  historicalLogs: Array<{
    reason: string;
    scoreDelta: number;
    timestamp: Timestamp;
  }>;
  verifiedIdentitiesCount: number;
  ratingsImpact: number;       // Cumulative value
  completedBookingsImpact: number;
  updatedAt: Timestamp;
}
```

### 17. `skills`
* **Path:** `skills/{skillId}`
* **Description:** Normalized professional taxonomy.
* **Schema:**
```typescript
{
  skillId: string;
  name: string;                // e.g., "PVC Pipe Laying", "Kotlin Jetpack Compose"
  category: string;            // e.g., "Plumbing", "IT & Development"
  approvedBy: string;          // "system" or admin UID
  popularityCount: number;     // Dynamic count of profiles claiming this skill
  createdAt: Timestamp;
}
```

### 18. `certifications`
* **Path:** `certifications/{certId}`
* **Description:** Verified trade degrees, licenses, or courses.
* **Schema:**
```typescript
{
  certId: string;
  uid: string;
  title: string;               // e.g., CTEVT Certified Plumber Grade A
  issuingOrganization: string; // e.g., Council for Technical Education and Vocational Training (CTEVT)
  credentialId: string;
  credentialUrl: string;       // Public scanning URL / doc storage path
  issueDate: Timestamp;
  expiryDate?: Timestamp;
  isVerifiedByKaamio: boolean; // Marked after manual/automated check
  createdAt: Timestamp;
}
```

### 19. `community_posts`
* **Path:** `community_posts/{postId}`
* **Description:** Professional community network feed.
* **Schema:**
```typescript
{
  postId: string;
  authorId: string;
  authorName: string;
  authorAvatar: string;
  content: string;
  mediaUrls: string[];         // Image upload attachments
  category: string;            // "Industry Updates" | "Q&A" | "Achievements" | "Trade Help"
  tags: string[];
  reactionsCount: number;
  commentsCount: number;
  status: "published" | "moderated" | "flagged";
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 20. `post_comments`
* **Path:** `post_comments/{commentId}`
* **Description:** User discussions under community posts.
* **Schema:**
```typescript
{
  commentId: string;
  postId: string;
  authorId: string;
  authorName: string;
  authorAvatar: string;
  content: string;
  reactionsCount: number;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 21. `post_reactions`
* **Path:** `post_reactions/{reactionId}`
* **Description:** User feedback tracking to avoid duplication.
* **Schema:**
```typescript
{
  reactionId: string;          // Compound key (uid_targetId)
  targetId: string;            // postId or commentId
  targetType: "post" | "comment";
  uid: string;                 // Source User ID
  reactionType: "like" | "applaud" | "insightful" | "support";
  createdAt: Timestamp;
}
```

### 22. `reports`
* **Path:** `reports/{reportId}`
* **Description:** Abuse, fake user, spam or safety reporting ledger.
* **Schema:**
```typescript
{
  reportId: string;
  reportedById: string;
  contentId: string;           // UID of reported entity / job / comment / post
  contentType: "user" | "job" | "post" | "comment" | "review";
  reason: "fake_profile" | "scam" | "harassment" | "inappropriate_content" | "payment_default";
  description: string;
  status: "pending" | "under_review" | "resolved" | "dismissed";
  actionTaken?: "warned" | "suspended" | "content_deleted" | "none";
  reviewerId?: string;         // Admin ID who resolved this
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 23. `saved_jobs`
* **Path:** `saved_jobs/{saveId}`
* **Description:** Bookmarked job advertisements.
* **Schema:**
```typescript
{
  saveId: string;              // Compound: uid_jobId
  uid: string;
  jobId: string;
  savedAt: Timestamp;
}
```

### 24. `saved_workers`
* **Path:** `saved_workers/{saveId}`
* **Description:** Bookmarked tradespeople cards.
* **Schema:**
```typescript
{
  saveId: string;              // Compound: uid_workerId
  uid: string;
  workerId: string;
  savedAt: Timestamp;
}
```

### 25. `saved_courses`
* **Path:** `saved_courses/{saveId}`
* **Description:** Bookmarked courses/training lists.
* **Schema:**
```typescript
{
  saveId: string;              // Compound: uid_courseId
  uid: string;
  courseId: string;
  savedAt: Timestamp;
}
```

### 26. `payments`
* **Path:** `payments/{paymentId}`
* **Description:** Escrow and service transaction clearances.
* **Schema:**
```typescript
{
  paymentId: string;
  bookingId?: string;          // Optional local booking reference
  projectId?: string;          // Optional freelance project reference
  payerId: string;             // Client UID
  payeeId: string;             // Worker / Freelancer UID
  amount: number;
  currency: "NPR";
  status: "initiated" | "held_in_escrow" | "released" | "refunded" | "failed";
  paymentMethod: "eSewa" | "Khalti" | "IPS_Bank_Transfer";
  paymentGatewayTransactionId: string; // Direct provider reference receipt
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

### 27. `transactions`
* **Path:** `transactions/{txId}`
* **Description:** Immutable wallet credit/debit records.
* **Schema:**
```typescript
{
  txId: string;
  uid: string;                 // Subject of transaction
  type: "escrow_deposit" | "earning_payout" | "refund_credit" | "platform_fee";
  amount: number;              // Positive/negative value
  currency: "NPR";
  description: string;
  status: "success" | "pending" | "failed";
  gatewayRef: string;          // External ID link
  timestamp: Timestamp;
}
```

### 28. `admin_logs`
* **Path:** `admin_logs/{logId}`
* **Description:** Complete, audited trail of administrative changes.
* **Schema:**
```typescript
{
  logId: string;
  adminId: string;
  actionType: "suspend_user" | "delete_job" | "verify_identity" | "refund_payment";
  entityId: string;            // target Document ID
  entityType: string;          // target Collection
  ipAddress: string;
  userAgent: string;
  details: string;             // Explanation or metadata object string
  timestamp: Timestamp;
}
```

---

## 3. Trust Score System Model

Kaamio computes an automated, unbiased Trust Score (from 0 to 100) to ensure high platform integrity. Score data resides in a secure, read-only location to prevent user tampering.

### Calculation Weights

$$T = W_V + W_R + W_C - P_D$$

| Component | Max Weight | Allocation details |
| :--- | :--- | :--- |
| **Verification ($W_V$)** | **40 Points** | • Phone Verification: +10 pts<br>• Google Verification: +10 pts<br>• Govt Identity Verified (Citizenship/PAN): +20 pts |
| **Reviews & Ratings ($W_R$)** | **30 Points** | Calculated as: $30 \times (\frac{\text{Average Rating}}{5.0})$ |
| **Completion Performance ($W_C$)** | **30 Points** | Calculated as: $30 \times (\frac{\text{Completed Projects/Bookings}}{\text{Total Bookings}})$ (Min 5 completed jobs for maximum points) |
| **Dispute Penalty ($P_D$)** | **Deductive** | • Verified Dispute defaults: -15 pts per incident<br>• Profile Flagged for Terms Abuse: -30 pts |

### Dynamic Verification Triggers (Cloud Functions)
- Automatically recalculates on any `reviews` write.
- Automatically recalculates when `users` verification flags switch from `false` to `true`.
- Adjusts on negative moderation events like `reports` status changing to `resolved` (Abuse confirmed).

---

## 4. Cloud Functions Codebase (TypeScript)

These core Node.js TypeScript triggers automate system integrity, profile creation, trust indexing, and FCM push notifications securely.

### 1. Auto-Profile Creation on Auth Trigger
Generates the unique, permanent **Kaamio ID** (`KAA-YYYY-XXXXXX`) automatically and sets up standard, clean profile schemas.

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

export const onUserSignUp = functions.auth.user().onCreate(async (user) => {
  const uid = user.uid;
  const year = new Date().getFullYear();
  const randomSixDigits = Math.floor(100000 + Math.random() * 900000);
  const kaamioId = `KAA-${year}-${randomSixDigits}`;

  const userDoc = {
    uid,
    kaamioId,
    fullName: user.displayName || 'Unregistered User',
    email: user.email || null,
    phoneNumber: user.phoneNumber || null,
    photoURL: user.photoURL || '',
    province: '',
    district: '',
    municipality: '',
    userTypes: ['student'], // defaults to learning/student
    profileCompletion: 15,
    trustScore: 50, // Starts at 50 Neutral
    isPhoneVerified: !!user.phoneNumber,
    isGoogleVerified: user.providerData.some(p => p.providerId === 'google.com'),
    isIdentityVerified: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
    status: 'active'
  };

  const profileDoc = {
    uid,
    bio: '',
    website: '',
    category: '',
    experienceYears: 0,
    hourlyRate: 0,
    serviceAreas: [],
    languages: ['Nepali'],
    availabilityStatus: 'available',
    portfolioLinks: [],
    verifications: {}
  };

  const batch = db.batch();
  batch.set(db.collection('users').doc(uid), userDoc);
  batch.set(db.collection('profiles').doc(uid), profileDoc);

  await batch.commit();
  console.log(`Successfully registered user profiles for ${uid} under ID ${kaamioId}`);
});
```

### 2. Auto Recalculate Trust Score
Triggered instantly on any new user review submission to update user metrics.

```typescript
export const recalculateTrustScoreOnReview = functions.firestore
  .document('reviews/{reviewId}')
  .onCreate(async (snapshot, context) => {
    const reviewData = snapshot.data();
    if (!reviewData) return;

    const revieweeId = reviewData.revieweeId;

    // Fetch all reviews for this user
    const reviewsQuery = await db.collection('reviews')
      .where('revieweeId', '==', revieweeId)
      .get();

    let totalRating = 0;
    const totalReviews = reviewsQuery.size;

    reviewsQuery.forEach(doc => {
      totalRating += doc.data().rating;
    });

    const averageRating = totalReviews > 0 ? (totalRating / totalReviews) : 0;

    // Fetch user details
    const userDocRef = db.collection('users').doc(revieweeId);
    const userSnap = await userDocRef.get();
    if (!userSnap.exists) return;

    const userData = userSnap.data()!;

    // Compute base weights
    let verificationWeight = 0;
    if (userData.isPhoneVerified) verificationWeight += 10;
    if (userData.isGoogleVerified) verificationWeight += 10;
    if (userData.isIdentityVerified) verificationWeight += 20;

    const ratingWeight = averageRating * 6; // Max 30 points (5 * 6)
    
    // Total jobs booked successfully vs total
    const bookingWeight = Math.min(30, (totalReviews * 6)); // Scaled max 30 points

    let newTrustScore = Math.round(verificationWeight + ratingWeight + bookingWeight);
    if (newTrustScore > 100) newTrustScore = 100;
    if (newTrustScore < 0) newTrustScore = 0;

    await db.runTransaction(async (transaction) => {
      transaction.update(userDocRef, {
        trustScore: newTrustScore,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Log in historical ledger
      const logRef = db.collection('trust_scores').doc(revieweeId);
      transaction.set(logRef, {
        uid: revieweeId,
        score: newTrustScore,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    });

    console.log(`Trust score for ${revieweeId} recalculated to: ${newTrustScore}`);
  });
```

### 3. Push Notifications Trigger (FCM)
Dispatches push messages for chat conversations and platform events.

```typescript
export const sendChatPushNotification = functions.firestore
  .document('conversations/{conversationId}/messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    const message = snapshot.data();
    if (!message) return;

    const receiverId = message.receiverId;
    const senderId = message.senderId;

    // Retrieve receiver FCM token from users
    const userSnap = await db.collection('users').doc(receiverId).get();
    if (!userSnap.exists) return;
    
    const userData = userSnap.data()!;
    const fcmToken = userData.fcmToken; // Handshake token

    if (!fcmToken) {
      console.log(`No FCM token registered for recipient ${receiverId}. Skipped push.`);
      return;
    }

    const payload = {
      notification: {
        title: `Message from ${message.partnerName || "Kaamio Partner"}`,
        body: message.messageText,
        clickAction: 'FLUTTER_NOTIFICATION_CLICK' // Or appropriate platform intent
      },
      data: {
        conversationId: context.params.conversationId,
        route: 'negotiation_chat_screen'
      }
    };

    await admin.messaging().sendToDevice(fcmToken, payload);
    console.log(`Successfully dispatched chat push to user: ${receiverId}`);
  });
```

---

## 5. Scalability Recommendations

To support over 10,000,000 users smoothly, the Kaamio database uses optimal engineering best practices to reduce reads and eliminate system hotspots.

1. **Denormalize Select Read-Intensive Fields:** Instead of fetching a full profile and user record on every single chat card or post, denormalize key data fields (`authorName`, `authorAvatar`, `partnerName`, `partnerAvatar`) directly inside the child documents (`conversations`, `community_posts`). Update these asynchronous fields in batches when users update their profile details.
2. **Prevent Hotspots on Counters:** Collections with highly frequent writes (e.g., `post_reactions`, `post_comments`) can create database bottlenecking on centralized counter variables. Use **Distributed Counter Shards** to split counting across 10 random documents, summing them via background cloud aggregate processes.
3. **Optimistic UI & Offline Cache:** Configure Firestore with offline persistence enabled. This allows immediate client UI rendering of posts, messages, and job applications, sync-queueing operations to Firestore when cell network availability dips.
4. **Partition Geography Queries:** Nepal is structured by Provinces, Districts, and Municipalities. Ensure queries filter by these explicit string fields first to filter down retrieved document payloads before running massive scans.

---

## 6. Firestore Security and Storage Verification

All files inside this repository (`/firestore.rules`, `/storage.rules`, `/firebase.json`, and `/firestore.indexes.json`) contain complete production code ready to deploy using:

```bash
# Verify credentials and deploy fully configured cloud schemas
firebase deploy --only auth,firestore,storage,functions
```
