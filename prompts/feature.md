# Feature Implementation Protocol

Use this prompt when implementing new features to ensure production-grade results.

## Phase 1: Planning
1. Identify the core functionality and its placement in Clean Architecture.
2. Define the UI structure using Jetpack Compose and the "Quiet Luxury" design language.
3. Outline the necessary data model changes and Firestore/Room updates.

## Phase 2: Implementation
1. **Model**: Create or update `@Immutable` data classes.
2. **Domain**: Define the Repository interface and use cases.
3. **Data**: Implement the Repository and local/remote data sources.
4. **UI**: Build the Composable screens and ViewModels.
   - Use `LocalKaamioTheme.current` for colors.
   - Implement `premiumPress` and `bounceClick` for all interactive elements.
   - Add loading/shimmer, empty, and error states.

## Phase 3: Verification
1. Run `./gradlew assembleDebug` to ensure compilation.
2. Perform a `ui_state` check to verify layout hierarchy.
3. Run ViewModel Unit tests with MockK.
4. Audit the code using `skills/code-reviewer.md`.

## Phase 4: Finalization
1. Update `PROJECT_STATUS.md`.
2. Generate a "Feature Implementation Report" including before/after UI screenshots (if possible).
3. Ensure no hardcoded secrets or mock data remain.
