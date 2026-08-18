# Firebase Configuration Auditor Skill

Verify that the Firebase integration is optimized and adheres to cloud architecture best practices.

## Checklist
1. **Authentication**: Verify Phone, Google, and Email configurations.
2. **Firestore**: Check for efficient querying and indexing. Validate rules.
3. **Storage**: Ensure folders are organized (e.g., `kyc/`, `chat/`) and access is restricted to owners.
4. **Cloud Functions**: Review callable triggers for the Khalti/eSewa payment flow.
5. **Offline Sync**: Verify that Firestore persistence is correctly handling local writes.
