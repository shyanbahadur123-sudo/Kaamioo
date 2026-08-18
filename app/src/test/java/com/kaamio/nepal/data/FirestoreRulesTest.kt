package com.kaamio.nepal.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration-style verification of the Firestore security rules.
 *
 * These tests load the actual firestore.rules shipped with the repo and assert
 * that security-critical policies are present and correctly gated. The rules
 * are the enforcement layer for the server-authoritative design: escrow state
 * transitions, course unlocks, trust/verification fields, and KYC status are
 * reserved for cloud functions, never for clients.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestoreRulesTest {

    companion object {
        private lateinit var rules: String

        @JvmStatic
        @BeforeClass
        fun loadRules() {
            val candidates = listOf(
                File("firestore.rules"),
                File("../firestore.rules"),
                File("../../../firestore.rules")
            )
            val rulesFile = candidates.firstOrNull { it.exists() }
                ?: error("firestore.rules not found under ${File(".").absolutePath}")
            rules = rulesFile.readText()
        }

        private fun has(snippet: String): Boolean =
            rules.replace(Regex("\\s+"), " ").contains(snippet)
    }

    @Test
    fun rules_version_and_service_declared() {
        assertTrue(rules.contains("rules_version = '2'"))
        assertTrue(rules.contains("service cloud.firestore"))
        assertTrue(rules.contains("match /databases/{database}/documents"))
    }

    @Test
    fun user_self_write_requires_owner_and_key_whitelist() {
        assertTrue(has("allow update: if isOwner(uid)"))
        assertTrue(has("userMutableKeysOnly"))
        assertTrue(has("request.auth.uid == uid || resource.data.privacyEnabled != true"))
        assertTrue(has("allow delete: if false"))
    }

    @Test
    fun trust_and_verification_fields_are_not_client_mutable() {
        // Core anti-forgery guarantee: a client can never self-set trust score,
        // identity flags, KYC status, or rating aggregates.
        val mutable = rules.substringAfter("function userMutableKeysOnly()").substringBefore("function isAllowedRole")
        for (forbidden in listOf(
            "trustScore", "isPhoneVerified", "isGoogleVerified", "isIdentityVerified",
            "kycStatus", "completedJobsCount", "totalReviews", "averageRating",
            "endorsementsCount"
        )) {
            assertFalse("$forbidden must not be client-mutable", mutable.contains(forbidden))
        }
    }

    @Test
    fun users_are_readable_only_when_not_private() {
        assertTrue(has("request.auth.uid == uid || resource.data.privacyEnabled != true"))
    }

    @Test
    fun user_create_seeds_only_neutral_system_fields() {
        assertTrue(has("request.resource.data.uid == uid"))
        assertTrue(has("request.resource.data.trustScore == 0"))
        assertTrue(has("request.resource.data.isIdentityVerified == false"))
        assertTrue(has("request.resource.data.totalReviews == 0"))
        assertTrue(has("request.resource.data.averageRating == 0"))
    }

    @Test
    fun user_role_cannot_escalate_to_admin() {
        assertTrue(has("isAllowedRole"))
        // "admin" is deliberately not part of the allowed client role set.
        assertFalse(has("'admin'"))
    }

    @Test
    fun listings_create_requires_owner_match_and_validation() {
        assertTrue(has("request.resource.data.ownerId == request.auth.uid"))
        assertTrue(has("validString(request.resource.data.title, 2, 120)"))
        assertTrue(has("validString(request.resource.data.company, 1, 80)"))
        assertTrue(has("allow delete: if isAuthenticated() && resource.data.ownerId == request.auth.uid"))
    }

    @Test
    fun listings_update_only_allows_operational_keys() {
        assertTrue(has("affectedKeys().hasOnly(['isApplied', 'isBookmarked'])"))
    }

    @Test
    fun courses_unlock_is_function_only() {
        assertTrue(has("request.resource.data.instructorId == request.auth.uid"))
        // A client can never append itself to unlockedBy.
        assertFalse(has("'unlockedBy'"))
    }

    @Test
    fun community_posts_require_author_match() {
        assertTrue(has("request.resource.data.authorId == request.auth.uid"))
        assertTrue(has("allow delete: if isAuthenticated() && resource.data.authorId == request.auth.uid"))
    }

    @Test
    fun chats_are_participant_scoped_and_immutable_from_clients() {
        assertTrue(has("match /chats/{chatId}"))
        assertTrue(has("request.auth.uid in resource.data.participantIds"))
        assertTrue(has("request.resource.data.participantIds.size() >= 2"))
        assertTrue(has("request.resource.data.participantIds.size() <= 10"))
        assertTrue(has("'isRead', 'proposalStatus', 'updatedAt', 'updatedBy'"))
        assertTrue(has("allow delete: if false"))
    }

    @Test
    fun applications_update_is_status_and_party_restricted() {
        assertTrue(has("match /applications/{applicationId}"))
        assertTrue(has("affectedKeys().hasOnly(['status'])"))
        // Both parties trusted to read, but writes are role-aware.
        assertTrue(has("resource.data.applicantId == request.auth.uid || resource.data.ownerId == request.auth.uid"))
    }

    @Test
    fun applications_applicant_cannot_self_accept() {
        // Owner-only authority for accept/reject/complete; applicant may only cancel.
        assertTrue(has("resource.data.ownerId == request.auth.uid"))
        assertTrue(has("'accepted', 'rejected', 'completed', 'cancelled'"))
        assertTrue(has("request.resource.data.status == 'cancelled'"))
        // The applicant branch must never permit 'accepted'/'completed'.
        val applicantBranch = Regex("applicantId == request\\.auth\\.uid[^;]*;").findAll(rules)
            .map { it.value }.joinToString(" ")
        assertFalse("Applicant must not self-accept", applicantBranch.contains("accepted"))
    }

    @Test
    fun reviews_validate_rating_range_and_no_self_review() {
        assertTrue(has("request.resource.data.reviewerId == request.auth.uid"))
        assertTrue(has("request.resource.data.reviewerId != request.resource.data.reviewedUserId"))
        assertTrue(has("request.resource.data.rating is int"))
        assertTrue(has("request.resource.data.rating >= 1"))
        assertTrue(has("request.resource.data.rating <= 5"))
    }

    @Test
    fun notifications_are_recipient_scoped() {
        assertTrue(has("resource.data.recipientId == request.auth.uid"))
        assertTrue(has("affectedKeys().hasOnly(['read'])"))
    }

    @Test
    fun escrow_is_server_authoritative() {
        assertTrue(has("match /escrow/{escrowId}"))
        assertTrue(has("request.resource.data.employerId == request.auth.uid"))
        assertTrue(has("request.resource.data.amount is number"))
        assertTrue(has("request.resource.data.amount > 0"))
        assertTrue(has("request.resource.data.status == 'PENDING_FUNDING'"))
        assertTrue(has("allow update: if false"))
    }

    @Test
    fun kyc_cannot_self_verify() {
        assertTrue(has("match /kyc/{uid}"))
        assertTrue(has("request.auth.uid == uid"))
        assertTrue(has("request.resource.data.status == 'pending'"))
        // The only client-writable status is "pending"; "verified" and
        // "resubmit" are function-only transitions.
        assertFalse("KYC must not permit client-side verified transitions", has("request.resource.data.status == 'verified'"))
    }

    @Test
    fun kyc_resubmission_allows_only_client_submission_fields() {
        // A reviewed document must not be silently re-pending, and only the
        // submission payload keys (plus uid) are client-writable.
        assertTrue(has("resource.data.status != 'verified'"))
        assertTrue(has("affectedKeys().hasAll(['status'])"))
        assertTrue(has("'documentUrl', 'selfieUrl', 'submittedAt', 'uid'"))
        assertTrue(has("'requestedAt', 'fullName', 'address', 'idType', 'idNumber'"))
        // verifyStatus/verifyNotes are reserved for Cloud Functions.
        assertFalse("Client KYC writes must not touch verifyStatus", has("affectedKeys().hasAny(['verifyStatus'"))
        assertFalse("Client KYC writes must not touch verifyNotes", has("affectedKeys().hasAny(['verifyNotes'"))
    }

    @Test
    fun no_public_anonymous_write_paths() {
        val unauthenticatedWrites = Regex("""allow\s+(create|update|delete)\s*:\s*if\s+true\s*;""")
            .findAll(rules).toList()
        assertTrue("No unauthenticated write rules allowed, found ${unauthenticatedWrites.size}", unauthenticatedWrites.isEmpty())
    }
}
