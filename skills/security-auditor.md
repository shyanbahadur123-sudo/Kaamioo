# Security Auditor Skill

Identify and report security vulnerabilities within the Kaamio application and backend configuration.

## Audit Points
- **Firebase Security**: Analyze `firestore.rules` and `storage.rules`. Ensure `request.auth != null` is enforced.
- **Data Protection**: Check for sensitive information in logs or public state.
- **Authentication**: Verify the integrity of the login/signup flow and token management.
- **Input Validation**: Ensure all user-provided data is sanitized before being written to Firestore.
- **Secrets Management**: Audit the project for hardcoded keys in `BuildConfig` or `KaamioApplication`.

## Report
List findings by severity and provide a mitigation plan for each.
