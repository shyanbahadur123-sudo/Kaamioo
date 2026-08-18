# Kaamio Repository Engineering Rules

This document serves as the absolute source of truth for all AI-assisted development within the Kaamio project. All contributions must adhere to these standards.

## 1. Core Architecture
- **Pattern**: strictly MVVM + Clean Architecture (Data, Domain, UI layers).
- **Separation of Concerns**: ViewModels must not contain Android Framework logic (Context, UI).
- **State Management**: Use `StateFlow` and `collectAsStateWithLifecycle` for all UI state.
- **Dependency Injection**: Use Hilt for all dependencies. No manual instantiation of ViewModels or Repositories.

## 2. UI & Design System
- **Framework**: Jetpack Compose only.
- **Material 3**: Use Material 3 tokens for colors, typography, and shapes.
- **Theme**: Adhere to the "Quiet Luxury" monochrome system (Pure Black, Soft Black, Elevated Surfaces).
- **Animations**: Implement spring-based physical motion (e.g., scale on press: 0.96f).
- **Resources**: All strings must be externalized in `strings.xml` (with Nepali support in `values-ne/`).

## 3. Data & Firebase
- **No Mock Data**: Production code must use real Repository calls or local Room/DataStore logic.
- **Repository Pattern**: All data fetching must go through an interface-driven Repository.
- **Offline First**: Use Room for caching mission-critical data.
- **Security**: Always validate Firestore security rules. No hardcoded API keys or Secrets.

## 4. Performance & Stability
- **Compose**: Minimize recompositions using `@Stable`, `remember`, and `derivedStateOf`.
- **Lists**: Always use `LazyColumn` or `LazyVerticalGrid` for dynamic content.
- **Startup**: App must reach interactive state under 2 seconds.
- **Memory**: Proactively check for memory leaks in `DisposableEffect`.

## 5. Completion Checklist (Mandatory)
Before submitting or deploying any change, the following must be verified:
1. [ ] **Build**: `./gradlew assembleDebug` passes without errors.
2. [ ] **Lint**: `./gradlew lint` checked; no critical security or performance warnings.
3. [ ] **Tests**: New logic is covered by Unit/ViewModel tests.
4. [ ] **UI Review**: Layout matches the premium Apple-inspired aesthetic.
5. [ ] **Accessibility**: Content descriptions added for all interactive elements.
6. [ ] **Status**: `PROJECT_STATUS.md` updated with the new changes.
7. [ ] **Report**: Generate a brief summary of what was changed and verified.
