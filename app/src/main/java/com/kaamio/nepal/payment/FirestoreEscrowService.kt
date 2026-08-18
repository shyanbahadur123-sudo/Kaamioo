package com.kaamio.nepal.payment

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Escrow is server-authoritative. Creation is mirrored to Firestore for local
// reads, but every state transition (fund / start work / release / refund /
// dispute) goes through a verified cloud function so clients can never
// self-settle funds.
class FirestoreEscrowService @javax.inject.Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions
) : EscrowService {

    private val escrowCollection = firestore.collection("escrow")

    override suspend fun createEscrow(transaction: EscrowTransaction): Result<EscrowTransaction> = runCatching {
        val result = firebaseFunctions
            .getHttpsCallable("escrow-createEscrow")
            .call(
                hashMapOf(
                    "jobId" to transaction.jobId,
                    "workerId" to transaction.workerId,
                    "amount" to transaction.amount
                )
            )
            .await()
        val data = result.data as? Map<*, *>
        transaction.copy(
            id = data?.get("escrowId") as? String ?: transaction.id,
            status = EscrowStatus.valueOf(data?.get("status") as? String ?: "PENDING_FUNDING")
        )
    }

    override suspend fun releaseFunds(escrowId: String): Result<EscrowTransaction> = runCatching {
        firebaseFunctions
            .getHttpsCallable("escrow-releaseFunds")
            .call(hashMapOf("escrowId" to escrowId))
            .await()
        readTransaction(escrowId) ?: throw Exception("Escrow not found")
    }

    override suspend fun refundEscrow(escrowId: String): Result<EscrowTransaction> = runCatching {
        firebaseFunctions
            .getHttpsCallable("escrow-refund")
            .call(hashMapOf("escrowId" to escrowId))
            .await()
        readTransaction(escrowId) ?: throw Exception("Escrow not found")
    }

    override suspend fun markFunded(escrowId: String, transactionId: String?): Result<EscrowTransaction> = runCatching {
        firebaseFunctions
            .getHttpsCallable("escrow-markFunded")
            .call(hashMapOf("escrowId" to escrowId, "transactionId" to transactionId))
            .await()
        readTransaction(escrowId) ?: throw Exception("Escrow not found")
    }

    override suspend fun startWork(escrowId: String): Result<EscrowTransaction> = runCatching {
        firebaseFunctions
            .getHttpsCallable("escrow-startWork")
            .call(hashMapOf("escrowId" to escrowId))
            .await()
        readTransaction(escrowId) ?: throw Exception("Escrow not found")
    }

    override suspend fun markComplete(escrowId: String): Result<EscrowTransaction> = runCatching {
        firebaseFunctions
            .getHttpsCallable("escrow-markComplete")
            .call(hashMapOf("escrowId" to escrowId))
            .await()
        readTransaction(escrowId) ?: throw Exception("Escrow not found")
    }

    override suspend fun dispute(escrowId: String, reason: String): Result<EscrowTransaction> = runCatching {
        firebaseFunctions
            .getHttpsCallable("escrow-dispute")
            .call(hashMapOf("escrowId" to escrowId, "reason" to reason))
            .await()
        readTransaction(escrowId) ?: throw Exception("Escrow not found")
    }

    private suspend fun readTransaction(escrowId: String): EscrowTransaction? {
        val snapshot = escrowCollection.document(escrowId).get().await()
        val data = snapshot.data ?: return null
        return data.toTransaction(snapshot.id)
    }

    override fun observeTransactions(jobId: String): Flow<List<EscrowTransaction>> = callbackFlow {
        val registration = escrowCollection
            .whereEqualTo("jobId", jobId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc ->
                        doc.data?.toTransaction(doc.id)
                    })
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeUserEscrows(uid: String): Flow<List<EscrowTransaction>> = callbackFlow {
        val employerReg = escrowCollection
            .whereEqualTo("employerId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(it.documents.mapNotNull { d -> d.data?.toTransaction(d.id) }) }
            }
        val workerReg = escrowCollection
            .whereEqualTo("workerId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(it.documents.mapNotNull { d -> d.data?.toTransaction(d.id) }) }
            }
        awaitClose {
            employerReg.remove()
            workerReg.remove()
        }
    }

    private fun Map<String, Any>.toTransaction(id: String): EscrowTransaction = EscrowTransaction(
        id = id,
        jobId = this["jobId"] as? String ?: "",
        employerId = this["employerId"] as? String ?: "",
        workerId = this["workerId"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
        status = EscrowStatus.valueOf(this["status"] as? String ?: "PENDING_FUNDING"),
        createdAt = (this["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: (this["createdAt"] as? Long) ?: 0L,
        releasedAt = (this["releasedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
        refundedAt = (this["refundedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
        completedAt = (this["completedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
        startedAt = (this["startedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
    )
}
