package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AdminSettings
import com.example.model.CommissionRuleSource
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.StoreOrder
import com.example.model.StorePayout
import com.example.model.StoreProfile
import com.example.model.ToastKind
import com.example.ui.components.PartnerBadge
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.util.EarningsCalculator
import com.example.util.StoreUtils
import java.util.Locale

@Composable
fun WalletTabScreen(
    storeProfile: StoreProfile,
    adminSettings: AdminSettings,
    deliveredCount: Int,
    grossRevenue: Double,
    totalChutkiCommission: Double,
    netStoreShare: Double,
    totalEstimatedProfit: Double,
    totalSettled: Double,
    pendingPayoutBalance: Double,
    payouts: List<StorePayout>,
    allOrders: List<StoreOrder>,
    orderLedgerEntries: Map<String, OrderEarningsLedgerEntry>,
    onRequestPayout: () -> Unit,
    onToast: (String, ToastKind) -> Unit
) {
    val context = LocalContext.current

    val csvCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csv = StoreUtils.buildSalesCsv(allOrders)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onToast("Sales CSV exported!", ToastKind.SUCCESS)
            }.onFailure {
                onToast("Failed to save CSV: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Wallet Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, EmeraldPrimary),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFECFDF5), BgSurface)
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "NET PAYABLE BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", pendingPayoutBalance)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )

                    Text(
                        text = "UPI: ${storeProfile.upiId.ifBlank { "Not Set" }}  •  Fee ${adminSettings.commissionPercent.toInt()}%  •  ₹${adminSettings.minStorePayout.toInt()}–₹${adminSettings.maxStorePayout.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRequestPayout,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("request_payout_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Request Payout", fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = {
                                if (allOrders.isEmpty()) {
                                    onToast("No sales data!", ToastKind.DANGER)
                                } else {
                                    val fileName = "Store_Sales_${StoreUtils.formatIsoDate(System.currentTimeMillis())}.csv"
                                    csvCreateLauncher.launch(fileName)
                                }
                            },
                            border = BorderStroke(1.dp, BorderSlate),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TextMain, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sales CSV", color = TextMain, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4 Stat Cards (2 rows of 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WalletStatCard(
                        title = "DELIVERED ORDERS",
                        value = deliveredCount.toString(),
                        valueColor = TextMain,
                        modifier = Modifier.weight(1f)
                    )
                    WalletStatCard(
                        title = "GROSS ITEMS SALES",
                        value = "₹${String.format(Locale.US, "%.2f", grossRevenue)}",
                        valueColor = TextMain,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WalletStatCard(
                        title = "NET STORE SHARE",
                        value = EarningsCalculator.formatRupees(netStoreShare),
                        valueColor = FreshGreen,
                        modifier = Modifier.weight(1f)
                    )
                    WalletStatCard(
                        title = "CHUTKI COMMISSION",
                        value = EarningsCalculator.formatRupees(totalChutkiCommission),
                        valueColor = SunnyAmberInk,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WalletStatCard(
                        title = "EST. STORE PROFIT",
                        value = EarningsCalculator.formatRupees(totalEstimatedProfit),
                        valueColor = InfoSky,
                        modifier = Modifier.weight(1f)
                    )
                    WalletStatCard(
                        title = "PAID BY ADMIN",
                        value = EarningsCalculator.formatRupees(totalSettled),
                        valueColor = SuccessTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Payout History Section
        item {
            Text(
                text = "Payout Requests & Payment Receipts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )
            Text(
                text = "Transferred by Admin to your Store UPI",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (payouts.isEmpty()) {
            item {
                Surface(
                    color = BgSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = "No payout requests yet.",
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }
            }
        } else {
            items(payouts, key = { it.payoutId }) { p ->
                val (badgeBg, badgeFg, statusTxt) = when (p.status) {
                    "PAID" -> Triple(SuccessTeal.copy(alpha = 0.16f), SuccessTeal, "Paid")
                    "REJECTED" -> Triple(DangerRed.copy(alpha = 0.16f), DangerSoft, "Rejected")
                    else -> Triple(SunnyYellow.copy(alpha = 0.28f), WarningSoft, "Pending")
                }
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", p.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "UPI: ${p.storeUpiId.ifBlank { "N/A" }} • ${StoreUtils.formatDate(p.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = "UTR/Ref: ${p.adminRef.ifBlank { "Awaiting" }}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = InfoSky
                            )
                        }
                        PartnerBadge(
                            text = statusTxt,
                            containerColor = badgeBg,
                            contentColor = badgeFg
                        )
                    }
                }
            }
        }

        // Order-by-Order Sales & Cancellation Ledger
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Order-by-Order Sales & Cancellation Ledger",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )
        }

        if (allOrders.isEmpty()) {
            item {
                Surface(
                    color = BgSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = "No sales records yet.",
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }
            }
        } else {
            items(allOrders, key = { "ledger_${it.orderId}" }) { o ->
                val ledger = orderLedgerEntries[o.orderId]
                val isReversed = ledger?.ledgerStatus == "REVERSED" || EarningsCalculator.isOrderReversed(o)
                val (sBg, sFg) = when {
                    isReversed -> DangerRed.copy(alpha = 0.16f) to DangerSoft
                    o.status == "DELIVERED" -> FreshGreen.copy(alpha = 0.16f) to FreshGreen
                    else -> Color(0xFF3B82F6).copy(alpha = 0.14f) to Color(0xFF2563EB)
                }
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isReversed) DangerRed.copy(alpha = 0.45f) else BorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${o.orderId.takeLast(6)} • ${StoreUtils.formatDate(o.createdAt)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            PartnerBadge(
                                text = if (isReversed) "${o.status} • REVERSED" else o.status,
                                containerColor = sBg,
                                contentColor = sFg
                            )
                        }
                        Text(
                            text = o.items.joinToString(", ") { "${it.qty}× ${it.name}" }.ifBlank { "Order items" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${o.paymentMode} • Customer #${o.customerUid.takeLast(5).ifBlank { "Protected" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "Gross: ${EarningsCalculator.formatRupees(ledger?.grossAmount ?: o.itemsValue)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }
                        if (ledger != null) {
                            val ruleLabel = CommissionRuleSource.fromWire(ledger.commissionRuleSource).label
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isReversed) {
                                        "Reversed Comm: ${EarningsCalculator.formatRupees(ledger.reversedCommissionAmount)}"
                                    } else {
                                        "Chutki (${EarningsCalculator.formatPercent(ledger.commissionPercentage)} • $ruleLabel): ${EarningsCalculator.formatRupees(ledger.commissionAmount)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isReversed) DangerSoft else SunnyAmberInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isReversed) {
                                        "Net: ₹0"
                                    } else {
                                        "Store Earn: ${EarningsCalculator.formatRupees(ledger.storeEarning)}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isReversed) DangerSoft else FreshGreen
                                )
                            }
                        }
                        if (isReversed && (o.cancelReason.isNotBlank() || ledger?.reversalReason?.isNotBlank() == true)) {
                            Text(
                                text = "Reversal reason: ${o.cancelReason.ifBlank { ledger?.reversalReason.orEmpty() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DangerSoft
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletStatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}
