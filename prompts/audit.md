# Forensic Project Audit

Perform a full A–Z audit of the Kaamio codebase to evaluate production readiness.

## 1. Architecture Audit
- Verify Clean Architecture boundaries (UI -> Domain <- Data).
- Check Hilt module organization and dependency scoping.
- Evaluate Repository patterns for potential leaks or logic bloat.

## 2. Security Audit
- Audit `firestore.rules` for path safety and whitelist enforcement.
- Check `storage.rules` for valid size and content-type constraints.
- Scan for hardcoded API keys, secrets, or sensitive logs.
- Review payment flow integrity (Escrow state transitions).

## 3. UI/UX & Accessibility
- Check visual consistency with the monochrome design system.
- Test responsive layouts on multiple screen sizes.
- Verify `contentDescription` presence on all Image/Icon elements.
- Validate loading (shimmer) and error state handling.

## 4. Performance Audit
- Profile Compose recompositions in critical screens.
- Check list performance (LazyColumn keys, image caching).
- Evaluate startup time and ANR risks in IO operations.

## 5. Report Generation
Generate a score (0–10) for each category and provide a prioritized list of remediation tasks.
