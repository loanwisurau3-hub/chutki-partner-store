package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminSettings
import com.example.model.CommissionRuleSource
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.StorePayout
import com.example.model.StoreProfile
import com.example.ui.components.PartnerBadge
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgCardElevated
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.InfoSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningSoft
import com.example.util.EarningsCalculator
import com.example.util.EarningsDashboardSummary
import com.example.util.LedgerFilterOption
import com.example.util.StoreUtils
import kotlin.math.max

@Composable
fun EarningsDashboardScreen(
    storeProfile: StoreProfile,
    adminSettings: AdminSettings,
    orderLedgerEntries: Map<String, OrderEarningsLedgerEntry>,
    payouts: List<StorePayout>,
    onRequestPayout: () -> Unit,
    onOpenWalletPayouts: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    var selectedFilter by remember { mutableStateOf(LedgerFilterOption.ALL) }

    val summary = remember(orderLedgerEntries, payouts) {
        EarningsCalculator.summarizeStoreDashboard(
            ledgerEntries = orderLedgerEntries.values.toList(),
            payouts = payouts
        )
    }

    val filteredEntries = remember(summary.sortedLedgerEntries, selectedFilter) {
        EarningsCalculator.filterLedgerEntries(
            entries = summary.sortedLedgerEntries,
            filter = selectedFilter
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (onBack != null) {
            Surface(
                color = BgSurface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("earnings_dashboard_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextMain
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Earnings Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${storeProfile.name.ifBlank { "Store Partner" }} • Firebase Ledger",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PartnerBadge(
                            text = "${summary.totalOrderCount} LEDGER",
                            containerColor = EmeraldSoft,
                            contentColor = FreshGreen
                        )
                    }
                    HorizontalDivider(color = BorderSlate)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("earnings_dashboard_list")
            ) {
                item {
                    HeroPartnerIncomeCard(
                        summary = summary,
                        storeProfile = storeProfile,
                        adminSettings = adminSettings,
                        onRequestPayout = onRequestPayout,
                        onOpenWalletPayouts = onOpenWalletPayouts,
                        modifier = Modifier.widthIn(max = 880.dp)
                    )
                }

                item {
                    CoreSummaryMetricsSection(
                        summary = summary,
                        isWide = isWide,
                        modifier = Modifier.widthIn(max = 880.dp)
                    )
                }

                item {
                    RevenueSplitAndRulesCard(
                        summary = summary,
                        modifier = Modifier.widthIn(max = 880.dp)
                    )
                }

                item {
                    LedgerFilterHeader(
                        summary = summary,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it },
                        modifier = Modifier.widthIn(max = 880.dp)
                    )
                }

                if (filteredEntries.isEmpty()) {
                    item {
                        EmptyLedgerStateCard(
                            selectedFilter = selectedFilter,
                            modifier = Modifier.widthIn(max = 880.dp)
                        )
                    }
                } else {
                    items(
                        items = filteredEntries,
                        key = { "dash_ledger_${it.orderId}" }
                    ) { entry ->
                        LedgerTransactionCard(
                            entry = entry,
                            modifier = Modifier.widthIn(max = 880.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroPartnerIncomeCard(
    summary: EarningsDashboardSummary,
    storeProfile: StoreProfile,
    adminSettings: AdminSettings,
    onRequestPayout: () -> Unit,
    onOpenWalletPayouts: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("earnings_hero_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, FreshGreen.copy(alpha = 0.65f)),
        shadowElevation = 6.dp
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = FreshGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "STORE PARTNER INCOME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreen
                    )
                }
                Surface(
                    color = EmeraldSoft,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Text(
                            text = "RTDB Ledger Live",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshGreen
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = EarningsCalculator.formatRupees(summary.deliveredStoreIncome),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary,
                    modifier = Modifier.testTag("hero_partner_income_value")
                )
                Text(
                    text = "Net store share earned across ${summary.deliveredOrderCount} delivered order${if (summary.deliveredOrderCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMiniStat(
                    label = "Net Profit",
                    value = EarningsCalculator.formatRupees(summary.deliveredEstimatedProfit),
                    valueColor = FreshGreen,
                    modifier = Modifier.weight(1f)
                )
                HeroMiniStat(
                    label = "In-Flight Earn",
                    value = EarningsCalculator.formatRupees(summary.inProgressStoreIncome),
                    valueColor = InfoSoft,
                    modifier = Modifier.weight(1f)
                )
                HeroMiniStat(
                    label = "Chutki Fee",
                    value = EarningsCalculator.formatRupees(summary.deliveredCommissionDeducted),
                    valueColor = SunnyAmberInk,
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.requestedPendingPayoutCount > 0) {
                Surface(
                    color = SunnyYellow.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SunnyYellow.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SunnyAmberInk,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "${summary.requestedPendingPayoutCount} payout request pending Admin transfer (${EarningsCalculator.formatRupees(summary.requestedPendingPayoutTotal)})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningSoft
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestPayout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("dashboard_request_payout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Request Payout",
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onOpenWalletPayouts != null) {
                    OutlinedButton(
                        onClick = onOpenWalletPayouts,
                        border = BorderStroke(1.dp, BorderSlate),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dashboard_open_wallet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = TextMain,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Payouts & CSV",
                            color = TextMain,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "UPI: ${storeProfile.upiId.ifBlank { "Not configured" }} • Min Payout ${EarningsCalculator.formatRupees(adminSettings.minStorePayout)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun HeroMiniStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BgCardElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CoreSummaryMetricsSection(
    summary: EarningsDashboardSummary,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Key Financial Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextMain
        )

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TotalSalesKpiCard(summary, Modifier.weight(1f))
                PendingPayoutsKpiCard(summary, Modifier.weight(1f))
                PartnerIncomeKpiCard(summary, Modifier.weight(1f))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TotalSalesKpiCard(summary, Modifier.weight(1f))
                PendingPayoutsKpiCard(summary, Modifier.weight(1f))
            }
            PartnerIncomeKpiCard(summary, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TotalSalesKpiCard(
    summary: EarningsDashboardSummary,
    modifier: Modifier = Modifier
) {
    DashboardKpiCard(
        title = "TOTAL SALES",
        primaryValue = EarningsCalculator.formatRupees(summary.totalSalesDelivered),
        valueColor = TextMain,
        icon = Icons.Default.ShoppingBag,
        iconTint = InfoSky,
        iconBg = InfoSoft.copy(alpha = 0.12f),
        line1 = "All Active: ${EarningsCalculator.formatRupees(summary.totalSalesAllActive)}",
        line2 = "${summary.deliveredOrderCount} delivered • Avg ${EarningsCalculator.formatRupees(summary.averageOrderValue)}",
        cardTag = "kpi_total_sales_card",
        valueTag = "kpi_total_sales_value",
        modifier = modifier
    )
}

@Composable
private fun PendingPayoutsKpiCard(
    summary: EarningsDashboardSummary,
    modifier: Modifier = Modifier
) {
    val pendingLabel = if (summary.requestedPendingPayoutCount > 0) {
        "Requested: ${EarningsCalculator.formatRupees(summary.requestedPendingPayoutTotal)} (${summary.requestedPendingPayoutCount})"
    } else {
        "Paid Settled: ${EarningsCalculator.formatRupees(summary.paidPayoutTotal)} (${summary.paidPayoutCount})"
    }
    DashboardKpiCard(
        title = "PENDING PAYOUTS",
        primaryValue = EarningsCalculator.formatRupees(summary.withdrawableBalance),
        valueColor = EmeraldPrimary,
        icon = Icons.Default.AccountBalanceWallet,
        iconTint = FreshGreen,
        iconBg = EmeraldSoft,
        line1 = pendingLabel,
        line2 = if (summary.requestedPendingPayoutCount > 0) {
            "Paid Settled: ${EarningsCalculator.formatRupees(summary.paidPayoutTotal)}"
        } else {
            "Ready for UPI withdrawal"
        },
        cardTag = "kpi_pending_payouts_card",
        valueTag = "kpi_pending_payouts_value",
        modifier = modifier
    )
}

@Composable
private fun PartnerIncomeKpiCard(
    summary: EarningsDashboardSummary,
    modifier: Modifier = Modifier
) {
    val reversedInfo = if (summary.reversedOrderCount > 0) {
        "Reversed: -${EarningsCalculator.formatRupees(summary.reversedStoreEarning)} (${summary.reversedOrderCount} orders)"
    } else {
        "Zero reversals • 100% clean ledger"
    }
    DashboardKpiCard(
        title = "PARTNER INCOME (ACTIVE)",
        primaryValue = EarningsCalculator.formatRupees(summary.totalActiveStoreIncome),
        valueColor = FreshGreen,
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        iconTint = SuccessTeal,
        iconBg = SuccessTeal.copy(alpha = 0.14f),
        line1 = "Delivered ${EarningsCalculator.formatRupees(summary.deliveredStoreIncome)} + Active ${EarningsCalculator.formatRupees(summary.inProgressStoreIncome)}",
        line2 = reversedInfo,
        cardTag = "kpi_partner_income_card",
        valueTag = "kpi_partner_income_value",
        modifier = modifier
    )
}

@Composable
private fun DashboardKpiCard(
    title: String,
    primaryValue: String,
    valueColor: Color,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    line1: String,
    line2: String,
    cardTag: String,
    valueTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(cardTag),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = primaryValue,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                modifier = Modifier.testTag(valueTag)
            )

            Text(
                text = line1,
                style = MaterialTheme.typography.labelSmall,
                color = TextMain,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = line2,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RevenueSplitAndRulesCard(
    summary: EarningsDashboardSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("revenue_split_card"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Store Share vs Chutki Commission",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
                PartnerBadge(
                    text = "Avg Fee ${EarningsCalculator.formatPercent(summary.effectiveCommissionPercent)}",
                    containerColor = SunnyYellow.copy(alpha = 0.25f),
                    contentColor = SunnyAmberInk
                )
            }

            val hasActive = summary.totalSalesAllActive > 0.0
            val storeWeight = if (hasActive) {
                max(0.02f, (summary.storeSharePercent / 100.0).toFloat())
            } else 1f
            val commWeight = if (hasActive && summary.effectiveCommissionPercent > 0.0) {
                max(0.02f, (summary.effectiveCommissionPercent / 100.0).toFloat())
            } else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgInput)
            ) {
                if (hasActive) {
                    Box(
                        modifier = Modifier
                            .weight(storeWeight)
                            .fillMaxHeight()
                            .background(FreshGreen)
                    )
                    if (commWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(commWeight)
                                .fillMaxHeight()
                                .background(SunnyYellow)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(FreshGreen)
                    )
                    Text(
                        text = "Store (${EarningsCalculator.formatPercent(summary.storeSharePercent)}): ${EarningsCalculator.formatRupees(summary.totalActiveStoreIncome)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FreshGreen
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(SunnyYellow)
                    )
                    Text(
                        text = "Chutki: ${EarningsCalculator.formatRupees(summary.totalActiveCommission)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SunnyAmberInk
                    )
                }
            }

            if (summary.ruleSourceBreakdown.isNotEmpty()) {
                HorizontalDivider(color = BorderSlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Applied Rules:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    summary.ruleSourceBreakdown.entries.forEach { (source, count) ->
                        PartnerBadge(
                            text = "${source.label}: $count",
                            containerColor = BgInput,
                            contentColor = TextMain
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerFilterHeader(
    summary: EarningsDashboardSummary,
    selectedFilter: LedgerFilterOption,
    onSelectFilter: (LedgerFilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Firebase Ledger Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
            }
            Text(
                text = "${summary.totalOrderCount} entries",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("ledger_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LedgerFilterOption.entries.forEach { option ->
                val count = when (option) {
                    LedgerFilterOption.ALL -> summary.totalOrderCount
                    LedgerFilterOption.ACTIVE -> summary.activeOrderCount
                    LedgerFilterOption.DELIVERED -> summary.deliveredOrderCount
                    LedgerFilterOption.REVERSED -> summary.reversedOrderCount
                }
                val isSelected = selectedFilter == option
                Surface(
                    color = if (isSelected) FreshGreen else BgCard,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) FreshGreen else BorderSlate
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelectFilter(option) }
                        .testTag("ledger_filter_${option.name.lowercase()}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${option.label} ($count)",
                            color = if (isSelected) Color.White else TextMain,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTransactionCard(
    entry: OrderEarningsLedgerEntry,
    modifier: Modifier = Modifier
) {
    val isReversed = EarningsCalculator.isLedgerReversed(entry)
    val isDelivered = !isReversed && entry.orderStatus.equals("DELIVERED", ignoreCase = true)
    val (statusBg, statusFg, statusLabel) = when {
        isReversed -> Triple(DangerRed.copy(alpha = 0.14f), DangerSoft, "${entry.orderStatus} • REVERSED")
        isDelivered -> Triple(EmeraldSoft, FreshGreen, "DELIVERED • ACTIVE")
        else -> Triple(InfoSky.copy(alpha = 0.14f), InfoSoft, "${entry.orderStatus} • IN-FLIGHT")
    }
    val ruleLabel = CommissionRuleSource.fromWire(entry.commissionRuleSource).label
    val ts = if (entry.createdAt > 0L) entry.createdAt else entry.updatedAt

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ledger_entry_${entry.orderId}"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isReversed) DangerRed.copy(alpha = 0.45f) else BorderSlate
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "#${entry.orderId.takeLast(8).ifBlank { entry.orderId }}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    if (ts > 0L) {
                        Text(
                            text = StoreUtils.formatDate(ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                PartnerBadge(
                    text = statusLabel,
                    containerColor = statusBg,
                    contentColor = statusFg
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerMetricColumn(
                    label = "Gross Sale",
                    value = EarningsCalculator.formatRupees(entry.grossAmount),
                    valueColor = TextMain,
                    modifier = Modifier.weight(1f)
                )
                LedgerMetricColumn(
                    label = if (isReversed) "Reversed Fee" else "Chutki (${EarningsCalculator.formatPercent(entry.commissionPercentage)})",
                    value = if (isReversed) {
                        "-${EarningsCalculator.formatRupees(entry.reversedCommissionAmount)}"
                    } else {
                        "-${EarningsCalculator.formatRupees(entry.commissionAmount)}"
                    },
                    valueColor = if (isReversed) DangerSoft else SunnyAmberInk,
                    modifier = Modifier.weight(1f)
                )
                LedgerMetricColumn(
                    label = if (isReversed) "Reversed Share" else "Store Earning",
                    value = if (isReversed) {
                        EarningsCalculator.formatRupees(0.0)
                    } else {
                        EarningsCalculator.formatRupees(entry.storeEarning)
                    },
                    valueColor = if (isReversed) DangerSoft else FreshGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PartnerBadge(
                    text = "Rule: $ruleLabel",
                    containerColor = BgInput,
                    contentColor = TextMuted
                )
                if (isReversed) {
                    Text(
                        text = entry.reversalReason.ifBlank { "Cancelled / Refunded" },
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerSoft,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Est. Profit: ${EarningsCalculator.formatRupees(entry.estimatedProfit)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = InfoSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerMetricColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BgInput,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyLedgerStateCard(
    selectedFilter: LedgerFilterOption,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("earnings_empty_state"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = FreshGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "No ${selectedFilter.label.lowercase()} ledger entries yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = "Orders synced to Firebase Realtime Database earningsLedger will appear here automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
