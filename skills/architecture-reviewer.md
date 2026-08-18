# Architecture Integrity Reviewer Skill

Ensure the project maintains structural discipline as it scales.

## Core Rules
- **Layer Isolation**: Data layer must never talk to UI directly.
- **Contract Driven**: Use interfaces for repositories to support testability.
- **Immutable State**: ViewModels should only expose `StateFlow` and process intentions via functions.
- **Dependency Management**: No circular dependencies. Strict use of Constructor Injection via Hilt.
