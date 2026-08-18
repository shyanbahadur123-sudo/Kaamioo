package com.kaamio.nepal.payment

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow

data class PaymentRequest(
    val amount: Double,
    val currency: String = "NPR",
    val orderId: String,
    val productName: String,
    val customerName: String = "Kaamio User",
    val customerEmail: String = "",
    val customerPhone: String = ""
)

data class PaymentResult(
    val success: Boolean,
    val transactionId: String? = null,
    val gatewayReference: String? = null,
    val errorMessage: String? = null
)

data class EscrowTransaction(
    val id: String,
    val jobId: String,
    val employerId: String,
    val workerId: String,
    val amount: Double,
    val status: EscrowStatus,
    val createdAt: Long,
    val releasedAt: Long? = null,
    val refundedAt: Long? = null,
    val completedAt: Long? = null,
    val startedAt: Long? = null
)

enum class EscrowStatus {
    PENDING_FUNDING,
    FUNDED,
    IN_PROGRESS,
    COMPLETED,
    RELEASED,
    REFUNDED,
    DISPUTED
}

interface PaymentGateway {
    suspend fun initiatePayment(request: PaymentRequest): Result<PaymentResult>
    suspend fun verifyPayment(transactionId: String): Result<PaymentResult>
    suspend fun processRefund(transactionId: String, amount: Double): Result<PaymentResult>
}

interface EscrowService {
    suspend fun createEscrow(transaction: EscrowTransaction): Result<EscrowTransaction>
    suspend fun markFunded(escrowId: String, transactionId: String?): Result<EscrowTransaction>
    suspend fun startWork(escrowId: String): Result<EscrowTransaction>
    suspend fun markComplete(escrowId: String): Result<EscrowTransaction>
    suspend fun releaseFunds(escrowId: String): Result<EscrowTransaction>
    suspend fun refundEscrow(escrowId: String): Result<EscrowTransaction>
    suspend fun dispute(escrowId: String, reason: String): Result<EscrowTransaction>
    fun observeTransactions(jobId: String): Flow<List<EscrowTransaction>>
    fun observeUserEscrows(uid: String): Flow<List<EscrowTransaction>>
}

class KhaltiPaymentGateway @javax.inject.Inject constructor(
    private val firebaseFunctions: FirebaseFunctions
) : PaymentGateway {

    override suspend fun initiatePayment(request: PaymentRequest): Result<PaymentResult> = runCatching {
        val data = hashMapOf(
            "amount" to request.amount.toLong(),
            "orderId" to request.orderId,
            "productName" to request.productName,
            "customerName" to request.customerName,
            "customerEmail" to request.customerEmail,
            "customerPhone" to request.customerPhone
        )
        val result = firebaseFunctions
            .getHttpsCallable("khalti-initiatePayment")
            .call(data)
            .await()
        val responseData = result.data as? Map<*, *>
        PaymentResult(
            success = true,
            transactionId = responseData?.get("pidx") as? String,
            gatewayReference = responseData?.get("paymentUrl") as? String
        )
    }

    override suspend fun verifyPayment(transactionId: String): Result<PaymentResult> = runCatching {
        val data = hashMapOf("pidx" to transactionId)
        val result = firebaseFunctions
            .getHttpsCallable("khalti-verifyPayment")
            .call(data)
            .await()
        val responseData = result.data as? Map<*, *>
        val status = responseData?.get("status") as? String
        PaymentResult(
            success = status == "Completed",
            transactionId = responseData?.get("transactionId") as? String ?: transactionId,
            gatewayReference = responseData?.get("pidx") as? String
        )
    }

    override suspend fun processRefund(transactionId: String, amount: Double): Result<PaymentResult> = runCatching {
        val data = hashMapOf(
            "transactionId" to transactionId,
            "amount" to amount.toLong()
        )
        val result = firebaseFunctions
            .getHttpsCallable("khalti-processRefund")
            .call(data)
            .await()
        val responseData = result.data as? Map<*, *>
        PaymentResult(
            success = responseData?.get("status") as? String == "refunded",
            transactionId = responseData?.get("refundId") as? String
        )
    }
}

class ESewaPaymentGateway @javax.inject.Inject constructor(
    private val firebaseFunctions: FirebaseFunctions
) : PaymentGateway {

    override suspend fun initiatePayment(request: PaymentRequest): Result<PaymentResult> = runCatching {
        val data = hashMapOf(
            "amount" to request.amount.toLong(),
            "orderId" to request.orderId,
            "productName" to request.productName,
            "customerName" to request.customerName,
            "customerEmail" to request.customerEmail,
            "customerPhone" to request.customerPhone
        )
        val result = firebaseFunctions
            .getHttpsCallable("esewa-initiatePayment")
            .call(data)
            .await()
        val responseData = result.data as? Map<*, *>
        PaymentResult(
            success = true,
            transactionId = responseData?.get("transactionId") as? String,
            gatewayReference = responseData?.get("paymentUrl") as? String
        )
    }

    override suspend fun verifyPayment(transactionId: String): Result<PaymentResult> = runCatching {
        val data = hashMapOf("transactionId" to transactionId)
        val result = firebaseFunctions
            .getHttpsCallable("esewa-verifyPayment")
            .call(data)
            .await()
        val responseData = result.data as? Map<*, *>
        val status = responseData?.get("status") as? String
        PaymentResult(
            success = status == "success",
            transactionId = transactionId,
            gatewayReference = responseData?.get("refId") as? String
        )
    }

    override suspend fun processRefund(transactionId: String, amount: Double): Result<PaymentResult> = runCatching {
        val data = hashMapOf(
            "transactionId" to transactionId,
            "amount" to amount.toLong()
        )
        val result = firebaseFunctions
            .getHttpsCallable("esewa-processRefund")
            .call(data)
            .await()
        val responseData = result.data as? Map<*, *>
        PaymentResult(
            success = responseData?.get("status") as? String == "refunded",
            transactionId = transactionId
        )
    }
}
