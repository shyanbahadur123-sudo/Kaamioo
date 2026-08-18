# Kaamio Engineering Rules

## Architecture
- MVVM + Clean Architecture only
- Repository pattern
- Kotlin Coroutines + Flow
- Jetpack Compose Material 3

## Code Quality
- No mock data
- No TODOs in production code
- No fallbackToDestructiveMigration
- Prefer immutable state

## Firebase
- Respect Firestore security rules
- Validate all user input
- Never expose admin functionality

## Performance
- Minimize recompositions
- LazyColumn for large lists
- Image caching required
- Startup under 2 seconds

## UX
- Apple-like premium black and white UI
- Accessibility support
- Responsive layouts

## Completion Checklist
After every task:
1. Build project
2. Run lint
3. Run tests
4. Check for crashes
5. Verify UI