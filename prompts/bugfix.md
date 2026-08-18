# Bugfix Protocol

Follow this structured approach to resolve any reported bugs or crashes.

## 1. Reproduction
- Extract the stack trace or error logs from Logcat.
- Identify the exact screen or state that triggers the issue.
- If possible, create a failing Unit Test to simulate the regression.

## 2. Root Cause Analysis (RCA)
- Determine if the issue is Architectural (State management), Data (Firebase/Room), or UI (Compose lifecycle).
- Investigate side-effects in `LaunchedEffect` or `DisposableEffect`.

## 3. Implementation
- Apply the fix following the Kaamio design language.
- Ensure the fix doesn't introduce new recomposition loops.
- Use `SnackbarBroker` to inform the user of recoverable errors.

## 4. Verification
- Confirm the fix by running the reproduction steps.
- Run the full test suite to ensure no regressions.
- Verify the build with `./gradlew assembleDebug`.
