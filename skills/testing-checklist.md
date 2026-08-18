# Testing & Verification Checklist

Ensure every logic change is validated by a rigorous testing suite.

## Required Tests
- **Unit Tests**: Pure logic in the Domain layer and utility classes.
- **ViewModel Tests**: Verify state transitions and repository interaction using MockK.
- **Integration Tests**: Verify the interaction between the Data layer and Room/DataStore.
- **UI Tests**: Smoke tests for primary user journeys using Compose Test Rule.

## Verification
- Run `./gradlew test` for all unit tests.
- Check code coverage reports (Kover/JaCoCo).
