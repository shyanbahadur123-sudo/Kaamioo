package com.kaamio.nepal.payment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.khalti.checkout.Khalti
import com.khalti.checkout.callbacks.OnMessage
import com.khalti.checkout.callbacks.OnPaymentResult
import com.khalti.checkout.data.Environment
import com.khalti.checkout.data.KhaltiPayConfig
import com.khalti.checkout.data.PaymentPayload
import com.khalti.checkout.data.PaymentResult as KhaltiPaymentResult

fun interface OnKhaltiPaymentResult {
    fun onResult(success: Boolean, transactionId: String?, error: String?)
}

@Composable
fun rememberKhaltiPay(
    publicKey: String,
    pidx: String,
    environment: Environment = Environment.TEST,
    onResult: OnKhaltiPaymentResult
): () -> Unit {
    val context = LocalContext.current
    val khalti = remember(pidx) {
        val config = KhaltiPayConfig(
            publicKey = publicKey,
            pidx = pidx,
            environment = environment
        )
        val paymentCallback = OnPaymentResult { result: KhaltiPaymentResult, _: Khalti ->
            val payload: PaymentPayload? = result.payload
            onResult.onResult(
                success = result.status == "Completed",
                transactionId = payload?.transactionId,
                error = result.message
            )
        }
        val messageCallback = OnMessage { payload, k ->
            if (payload.needsPaymentConfirmation) {
                k.verify()
            } else {
                onResult.onResult(
                    success = false,
                    transactionId = null,
                    error = payload.message
                )
            }
        }
        Khalti.init(
            context = context,
            config = config,
            onPaymentResult = paymentCallback,
            onMessage = messageCallback
        )
    }
    return { khalti.open() }
}
