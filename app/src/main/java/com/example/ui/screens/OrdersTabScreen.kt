package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomerRecord
import com.example.model.DeliveryRider
import com.example.model.OrderRiskAssessment
import com.example.model.OrderRiskLevel
import com.example.model.StoreOrder
import com.example.model.StoreProfile
import com.example.model.ToastKind
import com.example.ui.components.PartnerBadge
import com.example.ui.components.RejectOrderDialog
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrangeOut
import com.example.ui.theme.PurpleReady
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.util.RiderCommPolicy
import com.example.util.StoreUtils
import java.util.Locale

@Composable
fun OrdersTabScreen(
    activeOrders: List<StoreOrder>,
    storeProfile: StoreProfile,
    deliveryRiders: Map<String, DeliveryRider>,
    customers: Map<String, CustomerRecord>,
    orderRiskAssessments: Map<String, OrderRiskAssessment> = emptyMap(),
    unreadChatCountByOrder: Map<String, Int> = emptyMap(),
    onUpdateStatus: (StoreOrder, String) -> Unit,
    onRejectOrder: (String, String) -> Unit,
    onReportSpam: (StoreOrder, OrderRiskAssessment) -> Unit = { _, _ -> },
    onMarkOrderSafe: (String) -> Unit = {},
    onStartRiderCall: (StoreOrder, DeliveryRider?) -> Unit,
    onOpenRiderChat: (StoreOrder) -> Unit,
    onToast: (String, ToastKind) -> Unit
) {
    val context = LocalContext.current
    var rejectTargetOrder by remember { mutableStateOf<StoreOrder?>(null) }
    var highRiskConfirmTarget by remember { mutableStateOf<Pair<StoreOrder, OrderRiskAssessment>?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onToast("Microphone permission required for in-app voice calls.", ToastKind.WARNING)
        }
    }

    rejectTargetOrder?.let { target ->
        RejectOrderDialog(
            orderId = target.orderId,
            onDismiss = { rejectTargetOrder = null },
            onConfirm = { reason ->
                onRejectOrder(target.orderId, reason)
                rejectTargetOrder = null
            }
        )
    }

    highRiskConfirmTarget?.let { (order, risk) ->
        AlertDialog(
            onDismissRequest = { highRiskConfirmTarget = null },
            containerColor = BgSurface,
            title = {
                Text(
                    text = "Confirm Critical Risk Order #${order.orderId.takeLast(6)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = DangerSoft
                )
            },
            text = {
                Text(
                    text = "Scam & Spam Shield flagged this order (${risk.score}/100): ${risk.reasons.joinToString(", ")}. Call customer before packing or report as spam.",
                    color = TextMain
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(order, "ACCEPTED")
                        highRiskConfirmTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                    modifier = Modifier.testTag("confirm_high_risk_accept_button")
                ) {
                    Text("Accept Anyway", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onReportSpam(order, risk)
                        highRiskConfirmTarget = null
                    },
                    modifier = Modifier.testTag("dialog_report_spam_button")
                ) {
                    Text("Block & Report Spam", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 330.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Live Packing Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "15-minute express orders ring until accepted",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                if (activeOrders.isNotEmpty()) {
                    PartnerBadge(
                        text = "${activeOrders.size} Active",
                        containerColor = EmeraldPrimary.copy(alpha = 0.14f),
                        contentColor = EmeraldPrimary
                    )
                }
            }
        }

        if (activeOrders.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 52.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(BgInput),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBasket,
                                contentDescription = null,
                                tint = EmeraldPrimary.copy(alpha = 0.65f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No Active Orders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain
                        )
                        Text(
                            text = "New incoming grocery orders will ring and appear here automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeOrders, key = { it.orderId }) { order ->
                val risk = orderRiskAssessments[order.orderId] ?: OrderRiskAssessment(orderId = order.orderId)
                val unreadCount = unreadChatCountByOrder[order.orderId] ?: 0
                OrderPackingCard(
                    order = order,
                    storeProfile = storeProfile,
                    rider = deliveryRiders[order.deliveryUid],
                    isCustomerBanned = customers[order.customerUid]?.blocked == true,
                    riskAssessment = risk,
                    unreadChatCount = unreadCount,
                    onPrintKot = { StoreUtils.printKotSlip(context, order) },
                    onOpenMap = { url -> StoreUtils.openExternalUrl(context, url) },
                    onCallCustomer = { phone -> StoreUtils.openDialer(context, phone) },
                    onStartRiderCall = { ord, rdr ->
                        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        onStartRiderCall(ord, rdr)
                    },
                    onOpenRiderChat = { ord -> onOpenRiderChat(ord) },
                    onUpdateStatus = { status ->
                        if (status == "ACCEPTED" && risk.level == OrderRiskLevel.CRITICAL) {
                            highRiskConfirmTarget = order to risk
                        } else {
                            onUpdateStatus(order, status)
                        }
                    },
                    onRejectClick = { rejectTargetOrder = order },
                    onReportSpam = { onReportSpam(order, risk) },
                    onMarkSafe = { onMarkOrderSafe(order.orderId) },
                    onBlockedDispatchClick = {
                        onToast("Blocked until Admin verifies UPI!", ToastKind.DANGER)
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderPackingCard(
    order: StoreOrder,
    storeProfile: StoreProfile,
    rider: DeliveryRider?,
    isCustomerBanned: Boolean,
    riskAssessment: OrderRiskAssessment,
    unreadChatCount: Int,
    onPrintKot: () -> Unit,
    onOpenMap: (String) -> Unit,
    onCallCustomer: (String) -> Unit,
    onStartRiderCall: (StoreOrder, DeliveryRider?) -> Unit,
    onOpenRiderChat: (StoreOrder) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onRejectClick: () -> Unit,
    onReportSpam: () -> Unit,
    onMarkSafe: () -> Unit,
    onBlockedDispatchClick: () -> Unit
) {
    val isUpi = order.paymentMode == "UPI"
    val isWallet = order.paymentMode == "WALLET"
    val isUpiUnverified = isUpi && order.paymentStatus != "VERIFIED"
    val isUpiVerified = isUpi && order.paymentStatus == "VERIFIED"
    val pickupCode = StoreUtils.getStorePickupCode(order.pickupOtp, order.orderId)

    val accentColor = when {
        isUpiUnverified -> DangerRed
        order.status == "PREPARING" || order.status == "PACKING" -> WarningAmber
        order.status == "READY_FOR_PICKUP" -> InfoSky
        else -> EmeraldPrimary
    }

    val statusText = when {
        isUpiUnverified -> "PAYMENT UNVERIFIED"
        order.status == "ACCEPTED" -> "ACCEPTED"
        order.status == "PREPARING" || order.status == "PACKING" -> "PACKING"
        order.status == "READY_FOR_PICKUP" -> "READY"
        order.status == "PICKED_UP" || order.status == "OUT_FOR_DELIVERY" -> "OUT FOR DELIVERY"
        else -> "NEW PLACED"
    }

    val statusBg = when {
        isUpiUnverified -> DangerRed.copy(alpha = 0.14f)
        order.status == "PREPARING" || order.status == "PACKING" -> SunnyYellow.copy(alpha = 0.28f)
        order.status == "READY_FOR_PICKUP" -> PurpleReady.copy(alpha = 0.14f)
        order.status == "PICKED_UP" || order.status == "OUT_FOR_DELIVERY" -> OrangeOut.copy(alpha = 0.14f)
        else -> Color(0xFF3B82F6).copy(alpha = 0.14f)
    }
    val statusFg = when {
        isUpiUnverified -> DangerSoft
        order.status == "PREPARING" || order.status == "PACKING" -> WarningSoft
        order.status == "READY_FOR_PICKUP" -> PurpleReady
        order.status == "PICKED_UP" || order.status == "OUT_FOR_DELIVERY" -> OrangeOut
        else -> Color(0xFF2563EB)
    }

    val distToCustomer = StoreUtils.calculateDistanceKm(
        storeProfile.lat,
        storeProfile.lng,
        order.customerLat,
        order.customerLng
    )
    val riderLat = order.riderLat ?: rider?.lat
    val riderLng = order.riderLng ?: rider?.lng
    val distRiderToStore = StoreUtils.calculateDistanceKm(
        storeProfile.lat,
        storeProfile.lng,
        riderLat,
        riderLng
    )
    val customerMapLink = if (order.customerLat != null && order.customerLng != null) {
        "https://www.google.com/maps?q=${order.customerLat},${order.customerLng}"
    } else {
        order.locationLink
    }

    Surface(
        color = BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "#${order.orderId.takeLast(6)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain
                        )
                        Text(
                            text = StoreUtils.formatTime(order.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onPrintKot,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, BorderSlate),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.testTag("kot_print_${order.orderId.takeLast(6)}")
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "Print KOT",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("KOT", color = TextMain, style = MaterialTheme.typography.labelMedium)
                        }
                        PartnerBadge(
                            text = statusText,
                            containerColor = statusBg,
                            contentColor = statusFg,
                            icon = if (isUpiUnverified) Icons.Default.Lock else null
                        )
                    }
                }

                if (isCustomerBanned || order.isSpamSuspect) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isCustomerBanned) {
                            PartnerBadge(
                                text = "BANNED CUSTOMER",
                                containerColor = DangerRed.copy(alpha = 0.16f),
                                contentColor = DangerSoft,
                                icon = Icons.Default.Warning
                            )
                        }
                        if (order.isSpamSuspect) {
                            PartnerBadge(
                                text = "SPAM SUSPECT",
                                containerColor = DangerRed.copy(alpha = 0.16f),
                                contentColor = DangerSoft,
                                icon = Icons.Default.Warning
                            )
                        }
                    }
                }

                if (riskAssessment.level != OrderRiskLevel.LOW) {
                    val isSevere = riskAssessment.level == OrderRiskLevel.CRITICAL ||
                        riskAssessment.level == OrderRiskLevel.HIGH
                    val riskBg = if (isSevere) DangerRed.copy(alpha = 0.12f) else SunnyYellow.copy(alpha = 0.22f)
                    val riskBorder = if (isSevere) DangerRed else SunnyYellow
                    val riskFg = if (isSevere) DangerSoft else WarningSoft

                    Surface(
                        color = riskBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, riskBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_risk_banner_${order.orderId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = riskFg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${riskAssessment.level.label.uppercase()} (${riskAssessment.score}/100)",
                                        color = riskFg,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                if (riskAssessment.isDuplicateSuspect) {
                                    PartnerBadge(
                                        text = "DUPLICATE?",
                                        containerColor = DangerRed.copy(alpha = 0.18f),
                                        contentColor = DangerSoft
                                    )
                                }
                            }
                            if (riskAssessment.reasons.isNotEmpty()) {
                                Text(
                                    text = riskAssessment.reasons.joinToString(" • "),
                                    color = riskFg,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (order.phone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { onCallCustomer(order.phone) },
                                        border = BorderStroke(1.dp, riskFg.copy(alpha = 0.55f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Call Verify", color = riskFg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onMarkSafe,
                                    border = BorderStroke(1.dp, FreshGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("order_mark_safe_${order.orderId}")
                                ) {
                                    Text("Safe", color = FreshGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onReportSpam,
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("order_report_spam_${order.orderId}")
                                ) {
                                    Text("Report Spam", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                if (order.emergencyAlert) {
                    Surface(
                        color = DangerRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DangerRed)
                    ) {
                        Text(
                            text = order.emergencyDetails.ifBlank { "Rider delay alert" },
                            color = DangerSoft,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                // Handover Code & Payment Summary Row
                Surface(
                    color = InfoSky.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "HANDOVER CODE",
                                color = InfoSky,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Verify with rider on pickup",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            color = BgSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = pickupCode,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = InfoSky,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Destination Box
                Surface(
                    color = BgInput,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DESTINATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                if (distToCustomer != null) {
                                    Text(
                                        text = "~${String.format(Locale.US, "%.1f", distToCustomer)} km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InfoSky
                                    )
                                }
                            }
                            Text(
                                text = order.address.ifBlank { "Gossaigaon" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMain
                            )
                            if (customerMapLink.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onOpenMap(customerMapLink) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Map,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Open Google Maps",
                                        color = EmeraldPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Payment Mode Badge
                    when {
                        isWallet -> PartnerBadge(
                            text = "Wallet Paid (Verified)",
                            containerColor = SuccessTeal.copy(alpha = 0.16f),
                            contentColor = SuccessTeal,
                            icon = Icons.Default.Verified
                        )
                        isUpi && isUpiVerified -> PartnerBadge(
                            text = "UPI Paid (Verified)",
                            containerColor = SuccessTeal.copy(alpha = 0.16f),
                            contentColor = SuccessTeal,
                            icon = Icons.Default.Verified
                        )
                        isUpi -> PartnerBadge(
                            text = "UPI UNVERIFIED",
                            containerColor = DangerRed.copy(alpha = 0.16f),
                            contentColor = DangerSoft,
                            icon = Icons.Default.Lock
                        )
                        else -> PartnerBadge(
                            text = "COD CASH",
                            containerColor = Color(0xFF3B82F6).copy(alpha = 0.14f),
                            contentColor = Color(0xFF2563EB)
                        )
                    }

                    // Items List
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.qty}× ${item.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMain,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${(item.price * item.qty).toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderSlate)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Items Value:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", order.itemsValue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    }

                    // Assigned Rider Box
                    Surface(
                        color = BgInput,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ASSIGNED RIDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                if (distRiderToStore != null) {
                                    Text(
                                        text = "~${String.format(Locale.US, "%.1f", distRiderToStore)} km away",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessTeal
                                    )
                                }
                            }
                            val canCommunicate = RiderCommPolicy.canCommunicateWithRider(order, order.restaurantId)
                            if (rider != null || order.deliveryUid.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.DirectionsBike,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (!rider?.name.isNullOrBlank()) rider!!.name else "Assigned Rider",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMain
                                                )
                                                if (rider?.isReliablyOnline() == true) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(SuccessTeal)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (rider?.isReliablyOnline() == true) "Online" else "Rider Assigned",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    if (canCommunicate) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { onStartRiderCall(order, rider) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                border = BorderStroke(1.dp, EmeraldPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("call_rider_button_${order.orderId}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Call,
                                                    contentDescription = "Call Rider",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Call", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }

                                            val unreadCount = unreadChatCount
                                            androidx.compose.material3.BadgedBox(
                                                badge = {
                                                    if (unreadCount > 0) {
                                                        androidx.compose.material3.Badge(
                                                            containerColor = DangerRed,
                                                            contentColor = Color.White
                                                        ) {
                                                            Text(unreadCount.toString())
                                                        }
                                                    }
                                                }
                                            ) {
                                                Button(
                                                    onClick = { onOpenRiderChat(order) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessTeal),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.testTag("message_rider_button_${order.orderId}")
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Chat,
                                                        contentDescription = "Message Rider",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Message", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Awaiting Fleet Auto-Assignment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Action Buttons
                    when {
                        isUpiUnverified -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = DangerRed.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        text = "DISPATCH LOCKED: UPI PENDING ADMIN VERIFY",
                                        color = DangerSoft,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onRejectClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("Reject", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = onBlockedDispatchClick,
                                        border = BorderStroke(1.dp, DangerRed),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(48.dp)
                                    ) {
                                        Text("Dispatch Locked", color = DangerSoft, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        order.status == "PLACED" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onRejectClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Reject", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onUpdateStatus("ACCEPTED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessTeal),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(48.dp)
                                ) {
                                    Text("Accept Order", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        order.status == "ACCEPTED" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onRejectClick,
                                    border = BorderStroke(1.dp, DangerRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = DangerRed)
                                }
                                Button(
                                    onClick = { onUpdateStatus("PREPARING") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SunnyYellow,
                                        contentColor = TextMain
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Start Packing", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        order.status == "PREPARING" || order.status == "PACKING" -> {
                            Button(
                                onClick = { onUpdateStatus("READY_FOR_PICKUP") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mark Packed & Ready", fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        order.status == "READY_FOR_PICKUP" -> {
                            Surface(
                                color = InfoSky.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, InfoSky),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Rider Pickup Code: [ $pickupCode ]",
                                    color = InfoSky,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        else -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Picked Up by Rider",
                                    color = SuccessTeal,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                    }
                }
            }
        }
    }
}
