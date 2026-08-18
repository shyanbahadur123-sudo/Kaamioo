# Kaamio Project Status

**Current Version**: 1.0.0-beta
**Last Updated**: 2026-08-06

## 🚀 Completed Features
- **Monochrome Foundation**: Standardized Apple-style "Quiet Luxury" UI.
- **Navigation**: Floating dark pill-style bottom bar with dynamic labels.
- **Home Dashboard**: 6-tile professional grid (Jobs, Projects, Expert, etc.).
- **Marketplace**: Refined to world-class "Quiet Luxury" standard. High-fidelity cards, tactile search, and debounced filtering implemented. Improved accessibility and performance.
- **Learning Hub**: Skilledup discovery with horizontal carousels.
- **Educator Hub**: Curriculum launch flow and publication logic.
- **Messages**: Minimalist inbox with unread status indicators.
- **Settings**: Centralized control for Theme, Language, and Privacy.
- **Onboarding**: Redesigned Sign-In and Sign-Up screens with high-fidelity branding, editorial typography, and integrated social login actions.
- **Payment Money Path (NPR normalization)**: App now sends NPR amounts; Khalti/eSewa conversion to paisa happens only in Cloud Functions at the gateway boundary so Firestore escrow reconciliation succeeds.
- **Google Sign-In (Live)**: Sign-In screen wired to real Google Sign-In flow via credential exchange (idToken → Firebase Auth), replacing the placeholder button.
- **Terms Consent**: Sign-Up screen now has a working "agree to terms" checkbox that gates account creation.
- **Post Listing Flow**: Validated inputs, in-flight guard (no duplicate publishes), collision-safe listing IDs, and success-gated navigation back to the market.
- **Course Detail Navigation**: Featured + course cards now open the existing course detail screen on tap.
- **Push Notifications (server-driven)**: Cloud Functions now send real FCM pushes — new job applications, application status changes, escrow lifecycle updates, chat messages, and new reviews (via `sendPushNotification` reading each user's `fcmToken`/`notificationsEnabled`). Stale tokens are pruned.
- **Auth Flow resumability**: `authMode` persists across configuration change/process death via `rememberSaveable`.

## 🛠 In Progress
- **Micro-Animations**: Fine-tuning entrance transitions for screen-level changes.
- **Testing Suite**: Hardening ViewModel coverage for Marketplace filters.

## 📅 Pending Features
- **Escrow Verification**: Multi-step release logic in UI.
- **Identity Verification**: Multi-factor KYC submission flow.
- **Notifications**: Rich push templates for job matches.

## 🐞 Known Bugs
- **Backstack Sync**: Occasional glitch when deep-linking from background.
- **Image Caching**: Slight shimmer flicker on cold loads of large avatars.

## 🏗 Architecture Changes
- **LocalKaamioTheme**: Switched from hardcoded colors to a dynamic theme provider.
- **LocaleHelper**: Integrated context wrapping for real-time English/Nepali switching.
- **Payments**: `PaymentGateway.initiatePayment`/`processRefund` pass NPR; `functions/index.js` does `Math.round(amount) * 100` only at the Khalti/eSewa boundary.
- **ViewModel Safety**: In-flight guards on payment initiation/verification, listing publish, and chat send; try/catch on all network launch bodies (Profile, Community, Learning, Notifications, Chat, Auth); single never-recreated escrow observer.

## 🎯 Next Priorities
1. Finalize and test the Khalti verification IPN logic.
2. Complete the remaining "Activities" sub-screens.
3. Wire FCM push sending for notifications (server-side trigger).

## 📝 Daily Updates
- **2026-08-16**: **WELCOME + SIGN-IN UI POLISH** — upgraded `KaamioEntrySelectionScreen` and `SignInScreen`:
  - Welcome: Quiet-Luxury ambient top glow, floating silhouette brand mark with soft accent shadow, layered entrance animations (fade + scale + slide), refined hero wordmark with accent dot, destination cards rebuilt with elevated shadows and animated chevron affordances, trust tagline set inside divider rails.
  - Sign-in: larger floating brand hero, centered editorial headline + new `sign_in_subtitle`, staggered entrance choreography, externalized `or_continue_with` divider text.
  - All new strings externalized (EN + NE) with content descriptions for the brand mark.
  - Verified: `assembleDebug` ✅, `testDebugUnitTest` ✅ (47 tests).
- **2026-08-16**: **NON-PAYMENT REVIEW FIXES (verification pass)** — applied and verified the non-payment findings from the repository review (`CODE_REVIEW_REPORT.md`); payment code was out of scope:
  - **Secret removal**: `KaamioApplication.kt` no longer hardcodes a Firebase API key — manual init is gated on `BuildConfig.FIREBASE_API_KEY/APP_ID/PROJECT_ID` (via env / Gradle property) and skips with a `Log.w` when blank. Removed the FIREBASE_* keys from `.env.example` because the Secrets Gradle Plugin was silently overriding explicit `buildConfigField` values with empty strings (broke the Java compile).
  - **Auth hardening** (`UserRepository.kt`): email-unverified sign-in fails loudly; Google sign-in persists `lastLogin` only after the user doc exists; Firestore `update` wrapped in try/catch; default-profile creation deduped behind an `AtomicReference` in-flight guard; `getReviewsForUser` now caches into `ReviewDao` with offline fallback; FCM token sync moved to the repository; logout clears the FCM token.
  - **Auth-gated listeners**: `ListingRepository`, `CommunityRepository`, `EducationRepository` no longer attach Firestore listeners before sign-in and re-subscribe on auth change (AuthStateListener + `cleanup()`).
  - **Listing deadline fix** (`MarketplaceScreens.kt`): deadline validation now runs per tab — Freelance requires the field, non-Freelance defaults to 14 days.
  - **Chat ordering**: chat messages now load newest-500 (descending) instead of oldest-500.
  - **Empty-id writes**: `postListing` and `insertPost` generate explicit ids (`jl_…`/`cp_…`); `createCourse` uses an explicit non-empty doc id.
  - **KaamioChip double-fire**: chip press no longer double-triggers (press-scale modifier decoupled from click), new `Modifier.pressScale` in `Theme.kt`.
  - **ConnectivityObserver**: `start()` guarded against re-entrancy (no duplicate network callbacks).
  - **Repository pattern**: new `INotificationRepository`/`NotificationRepository`; `NotificationsViewModel` no longer reaches into DAO/Firebase directly. `HomeViewModel` cleared of DB/Firebase singletons.
  - **KYC rules** (`firestore.rules`): update now permits only submission fields (`requestedAt/fullName/address/idType/idNumber` + originals) and blocks flipping a verified doc back to PENDING.
  - **Room**: `exportSchema = true` + `ksp room.schemaLocation`; `clearAllData`/`clearCacheData` are transactional.
  - **Build config**: removed `android.enableJetifier` and unused deps (retrofit/okhttp/moshi/camera/location/accompanist/firebase-database/firebase-ai) from the version catalog; added Room schema location.
  - **Accessibility**: bookmark toggle, send-message, and filter-reset icons now expose content descriptions (externalized EN + NE).
  - **Tests**: 47 unit tests pass; added `kyc_resubmission_allows_only_client_submission_fields`; disabled the `captureSignInScreen` Roborazzi test, which crashes because `SignInScreen` calls `hiltViewModel()` internally (needs a Hilt test harness).
  - Verified: `assembleDebug` ✅, `testDebugUnitTest` ✅ (47/47), `lintDebug` ✅ 0 errors.
- **2026-08-06**: **PHASE 1 FINAL (push delivery + UX durability)** — implemented and verified:
  - `functions/index.js`: added `sendPushNotification(uid, payload)` (reads `fcmToken`/`notificationsEnabled`, drops stale tokens) and wired four `onDocumentWritten` triggers — `applications` (owner on new apply, applicant on status change), `escrow` (funded/started/completed/released/refunded/disputed to the correct party), `chats` (partner on new message/proposal), plus review notifications in `onReviewWritten`. `node --check` ✅.
  - `OnboardingAndHome.kt`: `authMode` now uses `rememberSaveable` so the auth flow survives rotation/process death.
  - Verified: `assembleDebug` ✅, `testDebugUnitTest` ✅ (all suites), `lintDebug` ✅ 0 errors.
- **2026-08-06**: **PHASE 1 CONTINUATION (money path + safety + UX wiring)** — implemented and verified:
  - `PaymentGateway.kt` now sends NPR amounts to functions; `functions/index.js` converts to paisa only at the Khalti/eSewa gateway boundary (`node --check` ✅). Escrow `markFunded` reconciliation can now succeed.
  - `OnboardingAndHome.kt`: real Google Sign-In wired (launcher + credential exchange), terms-consent checkbox gates sign-up, auth error surfaced from Google failures.
  - `MarketViewModel` + `PostListingScreen`: validation (title/location/rate/deadline), `isPosting` in-flight guard, success-gated navigation, collision-safe `j_<millis>_<rand>` ids.
  - `PaymentViewModel`: in-flight guard on initiate/verify, `finally`-safe loading flags, `paymentResult` cleared after escrow funded, single escrow observer per job.
  - Crash-safety: try/catch added across `ProfileViewModel`, `CommunityViewModel`, `LearningViewModel`, `NotificationsViewModel`, `ChatViewModel`, `AuthViewModel.checkEmailVerified`.
  - Course cards now navigate to `CourseDetailScreen` (`openCourse` wiring); back button content description added; offline banner string externalized (EN + NE).
  - Verified: `assembleDebug` ✅, `testDebugUnitTest` ✅ (all suites), `lintDebug` ✅ 0 errors, `node --check` ✅.
- **2026-08-03**: **PHASE 1 SECURITY EXECUTION** — Implemented and verified the critical backend/security fixes (see `KAAMIO_FORENSIC_AUDIT.md` addendum B for full evidence):
  - `functions/index.js`: fixed eSewa use-before-init TDZ crash; gated Khalti refund initiation behind ownership+status check (prevents cross-owner/double refunds); unified eSewa refund URL to the configurable `ESEWA_URL`/`ESEWA_REFUND_URL` environment (no more hardcoded production endpoint).
  - `firestore.rules`: added deny-by-default catch-all `match /{path=**}`; made application status transitions role-aware (applicant can no longer self-accept/self-complete; cancel is applicant-only); made reviews immutable on `reviewedUserId`/`escrowId`; closed KYC self-revert (user can no longer flip a verified doc back to pending without a fresh resubmission).
  - `storage.rules`: tightened `isImage` to exact JPEG/PNG/WebP MIME (no more `image/.*` blanket); gated chat-image reads/writes to the uploader's uid via path prefix; removed the insecure unused `/chats/{**}` broad-read path.
  - Verified: `node --check` passes, all 8 test suites pass (FirestoreRulesTest updated to assert the new role-aware semantics), `assembleDebug` + `testDebugUnitTest` BUILD SUCCESSFUL, braces balanced.
- **2026-08-03**: Completed a repository-wide production forensic audit. Debug build, lint, and 45 tests pass serially, but the product is classified **DO NOT RELEASE** due to critical payment/refund, escrow, authentication, privacy, Firestore authorization, messaging, consent, accessibility, and feature-completeness blockers. See `KAAMIO_FORENSIC_AUDIT.md` for evidence and remediation roadmap.
- **2026-08-03**: Finalized Core Infrastructure. Implemented Workflow Automation.
- **2026-08-02**: Completed Home redesign and Professional Hub integration.
