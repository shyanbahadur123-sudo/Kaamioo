package com.kaamio.nepal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaamio.nepal.BuildConfig
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.payment.EscrowStatus
import com.kaamio.nepal.payment.rememberKhaltiPay
import com.kaamio.nepal.ui.theme.*

@Composable
fun PaymentScreen(
    homeViewModel: HomeViewModel,
    paymentViewModel: PaymentViewModel,
    profile: UserProfile,
    jobId: String,
    workerId: String,
    workerName: String,
    defaultAmount: Double
) {
    val theme = LocalKaamioTheme.current
    val activeEscrow by paymentViewModel.activeEscrow.collectAsState()
    val paymentResult by paymentViewModel.paymentResult.collectAsState()
    val isInitiating by paymentViewModel.isInitiating.collectAsState()
    val isVerifying by paymentViewModel.isVerifying.collectAsState()

    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }

    val openKhalti = paymentResult?.let { result ->
        val escrow = activeEscrow
        if (result.success && result.transactionId != null && escrow != null) {
            rememberKhaltiPay(
                publicKey = BuildConfig.KHALTI_PUBLIC_KEY,
                pidx = result.transactionId,
                environment = if (BuildConfig.KHALTI_ENV == "PROD") com.khalti.checkout.data.Environment.PROD else com.khalti.checkout.data.Environment.TEST,
                onResult = { success, transactionId, error ->
                    if (success && transactionId != null && escrow != null) {
                        paymentViewModel.confirmEscrowFunded(escrow, transactionId)
                    } else {
                        paymentViewModel.clearPaymentResult()
                    }
                }
            )
        } else null
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Market) }
                Spacer(modifier = Modifier.width(20.dp))
                Text("Escrow Payment", style = Typography.displaySmall, color = theme.textPrimary, fontWeight = FontWeight.Bold)
            }

            KaamioCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), elevation = 6.dp) {
                Text("SECURE PAYMENT TO", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
                Text(workerName, style = Typography.headlineMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Amount to Secure", style = Typography.labelMedium, color = theme.textSecondary)
                Text("NPR ${String.format("%.2f", defaultAmount)}", style = Typography.displaySmall, color = theme.textPrimary, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val status = activeEscrow?.status
                val escrowId = activeEscrow?.id ?: ""
                when (status) {
                    EscrowStatus.PENDING_FUNDING -> {
                        KaamioButton(
                            "Fund Escrow Now",
                            {
                                paymentViewModel.initiatePayment(
                                    defaultAmount, jobId, workerId, "Service",
                                    profile.name, profile.email, profile.phoneNumber
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isInitiating
                        )
                        if (isInitiating) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = theme.accent
                            )
                        }
                    }
                    EscrowStatus.FUNDED -> {
                        KaamioButton("Start Work", { paymentViewModel.startWork(escrowId) }, modifier = Modifier.fillMaxWidth())
                    }
                    EscrowStatus.IN_PROGRESS -> {
                        KaamioButton("Mark Work Complete", { paymentViewModel.markComplete(escrowId) }, modifier = Modifier.fillMaxWidth())
                        KaamioButton("Dispute Escrow", { showDisputeDialog = true }, modifier = Modifier.fillMaxWidth(), containerColor = theme.error)
                    }
                    EscrowStatus.COMPLETED -> {
                        KaamioButton("Release Funds to Worker", { paymentViewModel.releaseEscrow(escrowId) }, modifier = Modifier.fillMaxWidth())
                    }
                    EscrowStatus.RELEASED -> {
                        Text("Funds released. Transaction complete.", style = Typography.bodyLarge, color = theme.success)
                    }
                    EscrowStatus.REFUNDED -> {
                        Text("Escrow refunded.", style = Typography.bodyLarge, color = theme.textSecondary)
                    }
                    EscrowStatus.DISPUTED -> {
                        Text("Escrow under dispute. Support has been notified.", style = Typography.bodyLarge, color = theme.error)
                    }
                    null -> {
                        KaamioButton(
                            "Fund Escrow Now",
                            {
                                paymentViewModel.initiatePayment(
                                    defaultAmount, jobId, workerId, "Service",
                                    profile.name, profile.email, profile.phoneNumber
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isInitiating
                        )
                    }
                }

                Text(
                    if (status != null) "Escrow status: ${status.name.replace('_', ' ')}" else
                        "Funds will be held securely by Kaamio until you authorize release upon work completion.",
                    style = Typography.bodySmall,
                    color = theme.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (isVerifying) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = theme.accent
                    )
                }
            }
        }
    }

    if (showDisputeDialog) {
        AlertDialog(
            onDismissRequest = {
                showDisputeDialog = false
                disputeReason = ""
            },
            containerColor = theme.card,
            title = { Text("Open Dispute", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = disputeReason,
                    onValueChange = { disputeReason = it },
                    placeholder = { Text("Describe the issue...", color = theme.textTertiary) },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = theme.background,
                        unfocusedContainerColor = theme.background,
                        focusedTextColor = theme.textPrimary,
                        unfocusedTextColor = theme.textPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val escrowId = activeEscrow?.id ?: ""
                    paymentViewModel.disputeEscrow(escrowId, disputeReason.trim())
                    showDisputeDialog = false
                    disputeReason = ""
                }) { Text("Submit", color = theme.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDisputeDialog = false
                    disputeReason = ""
                }) { Text("Cancel", color = theme.textSecondary) }
            }
        )
    }

    LaunchedEffect(openKhalti) {
        openKhalti?.invoke()
    }
}