package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.payment.EscrowService
import com.kaamio.nepal.payment.EscrowStatus
import com.kaamio.nepal.payment.EscrowTransaction
import com.kaamio.nepal.payment.PaymentRequest
import com.kaamio.nepal.payment.KhaltiPaymentGateway
import com.kaamio.nepal.payment.PaymentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val khaltiGateway: KhaltiPaymentGateway,
    private val escrowService: EscrowService
) : BaseViewModel() {

    private val _isInitiating = MutableStateFlow(false)
    val isInitiating = _isInitiating.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying = _isVerifying.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult = _paymentResult.asStateFlow()

    private val _activeEscrow = MutableStateFlow<EscrowTransaction?>(null)
    val activeEscrow = _activeEscrow.asStateFlow()

    fun initiatePayment(
        amount: Double,
        jobId: String,
        workerId: String,
        productName: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String
    ) {
        if (_isInitiating.value) return
        viewModelScope.launch {
            _isInitiating.value = true
            _paymentResult.value = null
            try {
                val orderId = "escrow_${jobId}_${workerId}"
                val result = khaltiGateway.initiatePayment(
                    PaymentRequest(
                        amount = amount,
                        orderId = orderId,
                        productName = productName,
                        customerName = customerName,
                        customerEmail = customerEmail,
                        customerPhone = customerPhone
                    )
                )
                result.onSuccess { paymentResult ->
                    _paymentResult.value = paymentResult
                    val escrow = EscrowTransaction(
                        id = orderId,
                        jobId = jobId,
                        employerId = "",
                        workerId = workerId,
                        amount = amount,
                        status = EscrowStatus.PENDING_FUNDING,
                        createdAt = System.currentTimeMillis()
                    )
                    escrowService.createEscrow(escrow)
                        .onSuccess { _activeEscrow.value = it }
                        .onFailure { showSnackbar(it.message ?: "Escrow creation failed") }
                }.onFailure {
                    _paymentResult.value = PaymentResult(success = false, errorMessage = it.message ?: "Payment initiation failed")
                    showSnackbar(it.message ?: "Payment initiation failed")
                }
            } catch (e: Exception) {
                _paymentResult.value = PaymentResult(success = false, errorMessage = e.message ?: "Payment initiation failed")
                showSnackbar(e.message ?: "Payment initiation failed")
            } finally {
                _isInitiating.value = false
            }
        }
    }

    fun confirmEscrowFunded(escrow: EscrowTransaction, transactionId: String?) {
        if (_isVerifying.value) return
        viewModelScope.launch {
            _isVerifying.value = true
            try {
                // 1. Verify the gateway payment server-side BEFORE touching escrow.
                // escrow-markFunded only accepts payments whose status is "completed"
                // on the server, which is set by khalti-verifyPayment / the webhook.
                val verified = transactionId != null &&
                    khaltiGateway.verifyPayment(transactionId)
                        .fold(onSuccess = { it.success }, onFailure = { false })
                if (!verified) {
                    showSnackbar("Payment could not be verified. Please retry.")
                    return@launch
                }
                // 2. Only a verified, completed payment may fund the escrow.
                escrowService.markFunded(escrow.id, transactionId)
                    .onSuccess {
                        _activeEscrow.value = it
                        _paymentResult.value = null
                        showSnackbar("Payment secured in escrow.")
                    }
                    .onFailure { showSnackbar(it.message ?: "Could not verify payment") }
            } catch (e: Exception) {
                showSnackbar(e.message ?: "Could not verify payment")
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun startWork(escrowId: String) {
        viewModelScope.launch {
            escrowService.startWork(escrowId)
                .onSuccess { _activeEscrow.value = it; showSnackbar("Work started.") }
                .onFailure { showSnackbar(it.message ?: "Could not start work") }
        }
    }

    fun markComplete(escrowId: String) {
        viewModelScope.launch {
            escrowService.markComplete(escrowId)
                .onSuccess { _activeEscrow.value = it; showSnackbar("Marked complete, awaiting employer release.") }
                .onFailure { showSnackbar(it.message ?: "Could not mark complete") }
        }
    }

    fun releaseEscrow(escrowId: String) {
        viewModelScope.launch {
            escrowService.releaseFunds(escrowId)
                .onSuccess { _activeEscrow.value = it; showSnackbar("Funds released to worker.") }
                .onFailure { showSnackbar(it.message ?: "Release failed") }
        }
    }

    fun refundEscrow(escrowId: String) {
        viewModelScope.launch {
            escrowService.refundEscrow(escrowId)
                .onSuccess { _activeEscrow.value = it; showSnackbar("Escrow refunded.") }
                .onFailure { showSnackbar(it.message ?: "Refund failed") }
        }
    }

    fun disputeEscrow(escrowId: String, reason: String) {
        viewModelScope.launch {
            escrowService.dispute(escrowId, reason)
                .onSuccess { _activeEscrow.value = it; showSnackbar("Escrow moved to dispute. Support has been notified.") }
                .onFailure { showSnackbar(it.message ?: "Could not open dispute") }
        }
    }

    private var escrowJobId: String? = null

    fun observeEscrow(jobId: String) {
        if (escrowJobId == jobId) return
        escrowJobId = jobId
        viewModelScope.launch {
            _activeEscrow.value = null
            try {
                escrowService.observeTransactions(jobId).collect { list ->
                    list.lastOrNull()?.let { _activeEscrow.value = it }
                }
            } catch (_: Exception) {
                showSnackbar("Could not load escrow updates.")
            }
        }
    }

    fun clearPaymentResult() { _paymentResult.value = null }
}
