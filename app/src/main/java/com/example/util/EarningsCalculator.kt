package com.example.util

import com.example.model.AdminSettings
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.ProductItem
import com.example.model.StoreOrder
import com.example.model.StorePayout
import com.example.model.StoreProfile
import com.example.model.StoreSummaryReport
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max

data class ResolvedCommission(
    val percent: Double,
    val source: CommissionRuleSource
)

data class EarningsBreakdown(
    val purchaseCost: Double,
    val sellingPrice: Double,
    val commissionPercentage: Double,
    val commissionAmount: Double,
    val storeEarning: Double,
    val estimatedProfit: Double,
    val ruleSource: CommissionRuleSource,
    val isLossMaking: Boolean
)

data class PricingValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
    val breakdown: EarningsBreakdown? = null
)

enum class LedgerFilterOption(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    DELIVERED("Delivered"),
    REVERSED("Reversed")
}

data class EarningsDashboardSummary(
    val deliveredStoreIncome: Double = 0.0,
    val deliveredEstimatedProfit: Double = 0.0,
    val deliveredCommissionDeducted: Double = 0.0,
    val inProgressStoreIncome: Double = 0.0,
    val totalActiveStoreIncome: Double = 0.0,
    val totalActiveCommission: Double = 0.0,
    val reversedStoreEarning: Double = 0.0,
    val reversedCommission: Double = 0.0,
    val paidPayoutTotal: Double = 0.0,
    val paidPayoutCount: Int = 0,
    val requestedPendingPayoutTotal: Double = 0.0,
    val requestedPendingPayoutCount: Int = 0,
    val rejectedPayoutCount: Int = 0,
    val withdrawableBalance: Double = 0.0,
    val totalSalesDelivered: Double = 0.0,
    val totalSalesAllActive: Double = 0.0,
    val deliveredOrderCount: Int = 0,
    val inProgressOrderCount: Int = 0,
    val activeOrderCount: Int = 0,
    val reversedOrderCount: Int = 0,
    val totalOrderCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val effectiveCommissionPercent: Double = 0.0,
    val storeSharePercent: Double = 0.0,
    val ruleSourceBreakdown: Map<CommissionRuleSource, Int> = emptyMap(),
    val sortedLedgerEntries: List<OrderEarningsLedgerEntry> = emptyList()
)

/**
 * Single source of truth for Chutki Store Partner Commission Priority & Earnings Calculations.
 *
 * Commission Resolution Priority:
 * 1. Product-specific commission (when enabled)
 * 2. Store-specific commission (when enabled)
 * 3. Category-specific commission (when enabled)
 * 4. Global default commission (when enabled; otherwise 0%)
 *
 * Calculation Formulas (rounded to 2 decimal places using HALF_UP):
 * - Commission Amount = Selling Price × Commission Percentage ÷ 100
 * - Store Partner Receives = Selling Price − Commission Amount
 * - Store Partner Profit = Store Partner Receives − Purchase Cost
 */
object EarningsCalculator {

    private val HUNDRED = BigDecimal("100")

    fun round2(value: Double): Double {
        if (!value.isFinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun round2(value: BigDecimal): BigDecimal =
        value.setScale(2, RoundingMode.HALF_UP)

    fun effectiveRules(
        commissionRules: CommissionRules,
        adminSettings: AdminSettings
    ): CommissionRules {
        val mergedGlobal = if (commissionRules.updatedAt == 0L && adminSettings.commissionPercent >= 0.0) {
            adminSettings.commissionPercent
        } else {
            commissionRules.globalCommissionPercent
        }
        return commissionRules.copy(
            globalCommissionPercent = mergedGlobal.coerceIn(0.0, 100.0),
            allowLossMakingProducts = commissionRules.allowLossMakingProducts || adminSettings.allowLossMakingProducts
        )
    }

    fun resolveCommission(
        productId: String?,
        storeId: String?,
        category: String?,
        rules: CommissionRules
    ): ResolvedCommission {
        val cleanProductId = productId?.trim().orEmpty()
        if (cleanProductId.isNotEmpty()) {
            val productRule = rules.productCommissions[cleanProductId]
            if (productRule != null && productRule.enabled) {
                return ResolvedCommission(
                    percent = round2(productRule.percent.coerceIn(0.0, 100.0)),
                    source = CommissionRuleSource.PRODUCT
                )
            }
        }

        val cleanStoreId = storeId?.trim().orEmpty()
        if (cleanStoreId.isNotEmpty()) {
            val storeRule = rules.storeCommissions[cleanStoreId]
            if (storeRule != null && storeRule.enabled) {
                return ResolvedCommission(
                    percent = round2(storeRule.percent.coerceIn(0.0, 100.0)),
                    source = CommissionRuleSource.STORE
                )
            }
        }

        val cleanCategory = category?.trim()?.uppercase(Locale.US).orEmpty()
        if (cleanCategory.isNotEmpty()) {
            val catRule = rules.categoryCommissions[cleanCategory]
                ?: rules.categoryCommissions.entries.firstOrNull {
                    it.key.equals(cleanCategory, ignoreCase = true)
                }?.value
            if (catRule != null && catRule.enabled) {
                return ResolvedCommission(
                    percent = round2(catRule.percent.coerceIn(0.0, 100.0)),
                    source = CommissionRuleSource.CATEGORY
                )
            }
        }

        if (rules.globalRuleEnabled) {
            return ResolvedCommission(
                percent = round2(rules.globalCommissionPercent.coerceIn(0.0, 100.0)),
                source = CommissionRuleSource.GLOBAL
            )
        }

        return ResolvedCommission(percent = 0.0, source = CommissionRuleSource.DISABLED)
    }

    fun calculate(
        purchaseCost: Double,
        sellingPrice: Double,
        commissionPercent: Double,
        ruleSource: CommissionRuleSource = CommissionRuleSource.GLOBAL
    ): EarningsBreakdown {
        val costBd = round2(BigDecimal.valueOf(purchaseCost.takeIf { it.isFinite() } ?: 0.0))
        val sellBd = round2(BigDecimal.valueOf(sellingPrice.takeIf { it.isFinite() } ?: 0.0))
        val commPctBd = round2(BigDecimal.valueOf(commissionPercent.takeIf { it.isFinite() } ?: 0.0))

        val commissionAmountBd = round2(
            sellBd.multiply(commPctBd).divide(HUNDRED, 6, RoundingMode.HALF_UP)
        )
        val storeEarningBd = round2(sellBd.subtract(commissionAmountBd))
        val estimatedProfitBd = round2(storeEarningBd.subtract(costBd))

        return EarningsBreakdown(
            purchaseCost = costBd.toDouble(),
            sellingPrice = sellBd.toDouble(),
            commissionPercentage = commPctBd.toDouble(),
            commissionAmount = commissionAmountBd.toDouble(),
            storeEarning = storeEarningBd.toDouble(),
            estimatedProfit = estimatedProfitBd.toDouble(),
            ruleSource = ruleSource,
            isLossMaking = storeEarningBd < costBd
        )
    }

    fun validateAndCalculate(
        purchaseCostText: String,
        sellingPriceText: String,
        commissionPercentText: String,
        ruleSource: CommissionRuleSource,
        allowLossMakingProducts: Boolean
    ): PricingValidationResult {
        val trimmedCost = purchaseCostText.trim()
        val trimmedSell = sellingPriceText.trim()
        val trimmedComm = commissionPercentText.trim()

        val purchaseCost = if (trimmedCost.isEmpty()) 0.0 else trimmedCost.toDoubleOrNull()
        val sellingPrice = trimmedSell.toDoubleOrNull()
        val commissionPercent = trimmedComm.toDoubleOrNull()

        if (purchaseCost == null || !purchaseCost.isFinite() || purchaseCost < 0.0) {
            return PricingValidationResult(
                isValid = false,
                errorMessage = "Purchase Cost cannot be negative."
            )
        }
        if (sellingPrice == null || !sellingPrice.isFinite() || sellingPrice <= 0.0) {
            return PricingValidationResult(
                isValid = false,
                errorMessage = "Selling Price must be greater than ₹0."
            )
        }
        if (commissionPercent == null || !commissionPercent.isFinite() || commissionPercent < 0.0 || commissionPercent > 100.0) {
            return PricingValidationResult(
                isValid = false,
                errorMessage = "Commission must be between 0% and 100%."
            )
        }

        val breakdown = calculate(
            purchaseCost = purchaseCost,
            sellingPrice = sellingPrice,
            commissionPercent = commissionPercent,
            ruleSource = ruleSource
        )

        if (breakdown.isLossMaking && !allowLossMakingProducts) {
            return PricingValidationResult(
                isValid = false,
                errorMessage = "Store Partner receives ${formatRupees(breakdown.storeEarning)} which is less than Purchase Cost ${formatRupees(breakdown.purchaseCost)}. Loss-making products are blocked by Admin.",
                breakdown = breakdown
            )
        }

        val warning = if (breakdown.isLossMaking && allowLossMakingProducts) {
            "Loss-making margin (${formatRupees(breakdown.estimatedProfit)}) — permitted by Admin setting."
        } else null

        return PricingValidationResult(
            isValid = true,
            warningMessage = warning,
            breakdown = breakdown
        )
    }

    fun isOrderReversed(order: StoreOrder): Boolean {
        val statusUpper = order.status.trim().uppercase(Locale.US)
        val payUpper = order.paymentStatus.trim().uppercase(Locale.US)
        return statusUpper == "CANCELLED" || statusUpper == "REFUNDED" || payUpper == "REFUNDED"
    }

    /**
     * Builds or updates a per-order earnings ledger entry.
     *
     * Rules enforced:
     * 1. Historical order earnings NEVER change when future commission rules update (if existingEntry.commissionDeducted).
     * 2. Commission is NEVER deducted twice for the same orderId.
     * 3. Order cancellation or refund reverses the commission & store earning cleanly (and never double-reverses).
     */
    fun buildOrUpdateOrderLedger(
        order: StoreOrder,
        productsByIdOrName: Map<String, ProductItem>,
        rules: CommissionRules,
        existingEntry: OrderEarningsLedgerEntry?,
        storeName: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): OrderEarningsLedgerEntry {
        val reversed = isOrderReversed(order)

        if (existingEntry != null && existingEntry.commissionDeducted) {
            if (reversed) {
                if (existingEntry.ledgerStatus == "REVERSED") {
                    return existingEntry.copy(
                        orderStatus = order.status,
                        paymentStatus = order.paymentStatus
                    )
                }
                val revCommission = if (existingEntry.reversedCommissionAmount > 0.0) {
                    existingEntry.reversedCommissionAmount
                } else {
                    existingEntry.commissionAmount
                }
                val revStoreEarning = if (existingEntry.reversedStoreEarning > 0.0) {
                    existingEntry.reversedStoreEarning
                } else {
                    existingEntry.storeEarning
                }
                return existingEntry.copy(
                    orderStatus = order.status,
                    paymentStatus = order.paymentStatus,
                    ledgerStatus = "REVERSED",
                    commissionAmount = 0.0,
                    storeEarning = 0.0,
                    estimatedProfit = 0.0,
                    reversedCommissionAmount = round2(revCommission),
                    reversedStoreEarning = round2(revStoreEarning),
                    reversalReason = order.cancelReason.ifBlank {
                        if (order.paymentStatus.equals("REFUNDED", ignoreCase = true)) {
                            "Order Refunded"
                        } else {
                            "Order Cancelled"
                        }
                    },
                    reversedAt = nowMillis,
                    updatedAt = nowMillis
                )
            }
            return existingEntry.copy(
                orderStatus = order.status,
                paymentStatus = order.paymentStatus,
                storeName = existingEntry.storeName.ifBlank { storeName }
            )
        }

        // Fresh calculation at order time
        val grossBd: BigDecimal
        var totalCommBd = BigDecimal.ZERO
        var totalCostBd = BigDecimal.ZERO
        var dominantSource = CommissionRuleSource.GLOBAL

        if (order.items.isNotEmpty()) {
            var itemsGross = BigDecimal.ZERO
            order.items.forEachIndexed { idx, item ->
                val matchedProduct = productsByIdOrName[item.itemId]
                    ?: productsByIdOrName[item.name.trim().lowercase(Locale.US)]
                val resolved = when {
                    item.commissionPercentage != null -> ResolvedCommission(
                        percent = round2(item.commissionPercentage.coerceIn(0.0, 100.0)),
                        source = CommissionRuleSource.fromWire(item.commissionRuleSource)
                    )
                    matchedProduct?.commissionPercentage != null -> ResolvedCommission(
                        percent = round2(matchedProduct.commissionPercentage.coerceIn(0.0, 100.0)),
                        source = CommissionRuleSource.fromWire(matchedProduct.commissionRuleSource)
                    )
                    else -> resolveCommission(
                        productId = item.itemId.ifBlank { matchedProduct?.itemId },
                        storeId = order.restaurantId,
                        category = matchedProduct?.category,
                        rules = rules
                    )
                }
                if (idx == 0) dominantSource = resolved.source
                val qtyBd = BigDecimal.valueOf(item.qty.coerceAtLeast(1).toLong())
                val lineGross = round2(BigDecimal.valueOf(item.price).multiply(qtyBd))
                val lineCost = round2(
                    BigDecimal.valueOf(
                        if (item.purchaseCost > 0.0) item.purchaseCost else (matchedProduct?.purchaseCost ?: 0.0)
                    ).multiply(qtyBd)
                )
                val lineComm = round2(
                    lineGross.multiply(BigDecimal.valueOf(resolved.percent)).divide(HUNDRED, 6, RoundingMode.HALF_UP)
                )
                itemsGross = itemsGross.add(lineGross)
                totalCommBd = totalCommBd.add(lineComm)
                totalCostBd = totalCostBd.add(lineCost)
            }
            // Respect order.subtotalAmount if explicitly set and differs slightly from item sum
            val declaredGross = round2(BigDecimal.valueOf(order.itemsValue))
            grossBd = if (declaredGross > BigDecimal.ZERO && itemsGross > BigDecimal.ZERO &&
                declaredGross.subtract(itemsGross).abs() > BigDecimal("0.01")
            ) {
                val ratio = declaredGross.divide(itemsGross, 6, RoundingMode.HALF_UP)
                totalCommBd = round2(totalCommBd.multiply(ratio))
                declaredGross
            } else {
                if (itemsGross > BigDecimal.ZERO) round2(itemsGross) else round2(declaredGross)
            }
        } else {
            grossBd = round2(BigDecimal.valueOf(order.itemsValue))
            val resolved = resolveCommission(
                productId = null,
                storeId = order.restaurantId,
                category = null,
                rules = rules
            )
            dominantSource = resolved.source
            totalCommBd = round2(
                grossBd.multiply(BigDecimal.valueOf(resolved.percent)).divide(HUNDRED, 6, RoundingMode.HALF_UP)
            )
        }

        val effectiveCommPct = if (grossBd > BigDecimal.ZERO) {
            round2(totalCommBd.multiply(HUNDRED).divide(grossBd, 6, RoundingMode.HALF_UP)).toDouble()
        } else {
            resolveCommission(null, order.restaurantId, null, rules).percent
        }
        val storeEarningBd = round2(grossBd.subtract(totalCommBd))
        val estimatedProfitBd = round2(storeEarningBd.subtract(totalCostBd))
        val createdTs = if (order.createdAt > 0L) order.createdAt else nowMillis

        return if (reversed) {
            OrderEarningsLedgerEntry(
                orderId = order.orderId,
                storeId = order.restaurantId,
                storeName = storeName,
                orderStatus = order.status,
                paymentStatus = order.paymentStatus,
                ledgerStatus = "REVERSED",
                grossAmount = grossBd.toDouble(),
                commissionPercentage = effectiveCommPct,
                commissionAmount = 0.0,
                storeEarning = 0.0,
                purchaseCostTotal = totalCostBd.toDouble(),
                estimatedProfit = 0.0,
                reversedCommissionAmount = totalCommBd.toDouble(),
                reversedStoreEarning = storeEarningBd.toDouble(),
                commissionRuleSource = dominantSource.name,
                commissionDeducted = true,
                reversalReason = order.cancelReason.ifBlank { "Order Cancelled / Refunded" },
                createdAt = createdTs,
                updatedAt = nowMillis,
                reversedAt = nowMillis
            )
        } else {
            OrderEarningsLedgerEntry(
                orderId = order.orderId,
                storeId = order.restaurantId,
                storeName = storeName,
                orderStatus = order.status,
                paymentStatus = order.paymentStatus,
                ledgerStatus = "ACTIVE",
                grossAmount = grossBd.toDouble(),
                commissionPercentage = effectiveCommPct,
                commissionAmount = totalCommBd.toDouble(),
                storeEarning = storeEarningBd.toDouble(),
                purchaseCostTotal = totalCostBd.toDouble(),
                estimatedProfit = estimatedProfitBd.toDouble(),
                reversedCommissionAmount = 0.0,
                reversedStoreEarning = 0.0,
                commissionRuleSource = dominantSource.name,
                commissionDeducted = true,
                createdAt = createdTs,
                updatedAt = nowMillis
            )
        }
    }

    fun buildProductIndex(products: List<ProductItem>): Map<String, ProductItem> = buildMap {
        products.forEach { product ->
            if (product.itemId.isNotBlank()) put(product.itemId, product)
            if (product.name.isNotBlank()) put(product.name.trim().lowercase(Locale.US), product)
        }
    }

    fun buildStoreReports(
        ledgerEntries: List<OrderEarningsLedgerEntry>,
        storesById: Map<String, StoreProfile>
    ): List<StoreSummaryReport> {
        return ledgerEntries
            .groupBy { it.storeId.ifBlank { "unknown" } }
            .map { (storeId, entries) ->
                val resolvedName = storesById[storeId]?.name?.takeIf { it.isNotBlank() }
                    ?: entries.firstNotNullOfOrNull { it.storeName.takeIf(String::isNotBlank) }
                    ?: "Store ${storeId.takeLast(6)}"
                val activeEntries = entries.filter { it.ledgerStatus != "REVERSED" }
                val reversedCount = entries.count { it.ledgerStatus == "REVERSED" }
                StoreSummaryReport(
                    storeId = storeId,
                    storeName = resolvedName,
                    totalOrders = entries.size,
                    activeOrders = activeEntries.size,
                    reversedOrders = reversedCount,
                    grossSales = round2(activeEntries.sumOf { it.grossAmount }),
                    totalChutkiCommission = round2(activeEntries.sumOf { it.commissionAmount }),
                    totalStoreEarning = round2(activeEntries.sumOf { it.storeEarning }),
                    totalEstimatedProfit = round2(activeEntries.sumOf { it.estimatedProfit })
                )
            }
            .sortedByDescending { it.grossSales }
    }

    fun isLedgerReversed(entry: OrderEarningsLedgerEntry): Boolean {
        val statusUpper = entry.orderStatus.trim().uppercase(Locale.US)
        val payUpper = entry.paymentStatus.trim().uppercase(Locale.US)
        return entry.ledgerStatus.equals("REVERSED", ignoreCase = true) ||
            statusUpper == "CANCELLED" ||
            statusUpper == "REFUNDED" ||
            payUpper == "REFUNDED"
    }

    fun filterLedgerEntries(
        entries: List<OrderEarningsLedgerEntry>,
        filter: LedgerFilterOption
    ): List<OrderEarningsLedgerEntry> {
        val sorted = entries.sortedByDescending { max(it.createdAt, it.updatedAt) }
        return when (filter) {
            LedgerFilterOption.ALL -> sorted
            LedgerFilterOption.ACTIVE -> sorted.filter { !isLedgerReversed(it) }
            LedgerFilterOption.DELIVERED -> sorted.filter {
                !isLedgerReversed(it) && it.orderStatus.equals("DELIVERED", ignoreCase = true)
            }
            LedgerFilterOption.REVERSED -> sorted.filter { isLedgerReversed(it) }
        }
    }

    fun summarizeStoreDashboard(
        ledgerEntries: List<OrderEarningsLedgerEntry>,
        payouts: List<StorePayout>
    ): EarningsDashboardSummary {
        val sorted = ledgerEntries.sortedByDescending { max(it.createdAt, it.updatedAt) }

        var deliveredStoreIncome = BigDecimal.ZERO
        var deliveredProfit = BigDecimal.ZERO
        var deliveredComm = BigDecimal.ZERO
        var deliveredSales = BigDecimal.ZERO
        var deliveredCount = 0

        var inProgressStoreIncome = BigDecimal.ZERO
        var inProgressComm = BigDecimal.ZERO
        var inProgressSales = BigDecimal.ZERO
        var inProgressCount = 0

        var reversedStoreEarning = BigDecimal.ZERO
        var reversedComm = BigDecimal.ZERO
        var reversedCount = 0

        val sourceCounts = mutableMapOf<CommissionRuleSource, Int>()

        sorted.forEach { entry ->
            if (isLedgerReversed(entry)) {
                reversedCount++
                reversedStoreEarning = reversedStoreEarning.add(BigDecimal.valueOf(entry.reversedStoreEarning))
                reversedComm = reversedComm.add(BigDecimal.valueOf(entry.reversedCommissionAmount))
            } else {
                val src = CommissionRuleSource.fromWire(entry.commissionRuleSource)
                sourceCounts[src] = (sourceCounts[src] ?: 0) + 1

                if (entry.orderStatus.equals("DELIVERED", ignoreCase = true)) {
                    deliveredCount++
                    deliveredSales = deliveredSales.add(BigDecimal.valueOf(entry.grossAmount))
                    deliveredComm = deliveredComm.add(BigDecimal.valueOf(entry.commissionAmount))
                    deliveredStoreIncome = deliveredStoreIncome.add(BigDecimal.valueOf(entry.storeEarning))
                    deliveredProfit = deliveredProfit.add(BigDecimal.valueOf(entry.estimatedProfit))
                } else {
                    inProgressCount++
                    inProgressSales = inProgressSales.add(BigDecimal.valueOf(entry.grossAmount))
                    inProgressComm = inProgressComm.add(BigDecimal.valueOf(entry.commissionAmount))
                    inProgressStoreIncome = inProgressStoreIncome.add(BigDecimal.valueOf(entry.storeEarning))
                }
            }
        }

        val totalActiveSales = deliveredSales.add(inProgressSales)
        val totalActiveComm = deliveredComm.add(inProgressComm)
        val totalActiveIncome = deliveredStoreIncome.add(inProgressStoreIncome)
        val activeCount = deliveredCount + inProgressCount

        var paidTotal = BigDecimal.ZERO
        var paidCount = 0
        var requestedPendingTotal = BigDecimal.ZERO
        var requestedPendingCount = 0
        var rejectedCount = 0

        payouts.forEach { payout ->
            when (payout.status.trim().uppercase(Locale.US)) {
                "PAID" -> {
                    paidCount++
                    paidTotal = paidTotal.add(BigDecimal.valueOf(payout.amount))
                }
                "REJECTED" -> rejectedCount++
                else -> {
                    requestedPendingCount++
                    requestedPendingTotal = requestedPendingTotal.add(BigDecimal.valueOf(payout.amount))
                }
            }
        }

        val deliveredIncomeDouble = round2(deliveredStoreIncome.toDouble())
        val paidDouble = round2(paidTotal.toDouble())
        val withdrawable = round2(max(0.0, deliveredIncomeDouble - paidDouble))

        val totalSalesActiveDouble = round2(totalActiveSales.toDouble())
        val totalActiveCommDouble = round2(totalActiveComm.toDouble())
        val avgOrderValue = if (activeCount > 0) {
            round2(totalSalesActiveDouble / activeCount)
        } else 0.0

        val effectiveCommPct = if (totalSalesActiveDouble > 0.0) {
            round2((totalActiveCommDouble / totalSalesActiveDouble) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0
        val storeSharePct = if (totalSalesActiveDouble > 0.0) {
            round2(100.0 - effectiveCommPct).coerceIn(0.0, 100.0)
        } else 0.0

        return EarningsDashboardSummary(
            deliveredStoreIncome = deliveredIncomeDouble,
            deliveredEstimatedProfit = round2(deliveredProfit.toDouble()),
            deliveredCommissionDeducted = round2(deliveredComm.toDouble()),
            inProgressStoreIncome = round2(inProgressStoreIncome.toDouble()),
            totalActiveStoreIncome = round2(totalActiveIncome.toDouble()),
            totalActiveCommission = totalActiveCommDouble,
            reversedStoreEarning = round2(reversedStoreEarning.toDouble()),
            reversedCommission = round2(reversedComm.toDouble()),
            paidPayoutTotal = paidDouble,
            paidPayoutCount = paidCount,
            requestedPendingPayoutTotal = round2(requestedPendingTotal.toDouble()),
            requestedPendingPayoutCount = requestedPendingCount,
            rejectedPayoutCount = rejectedCount,
            withdrawableBalance = withdrawable,
            totalSalesDelivered = round2(deliveredSales.toDouble()),
            totalSalesAllActive = totalSalesActiveDouble,
            deliveredOrderCount = deliveredCount,
            inProgressOrderCount = inProgressCount,
            activeOrderCount = activeCount,
            reversedOrderCount = reversedCount,
            totalOrderCount = sorted.size,
            averageOrderValue = avgOrderValue,
            effectiveCommissionPercent = effectiveCommPct,
            storeSharePercent = storeSharePct,
            ruleSourceBreakdown = sourceCounts,
            sortedLedgerEntries = sorted
        )
    }

    fun formatRupees(amount: Double): String {
        val rounded = round2(amount)
        val isWhole = BigDecimal.valueOf(rounded).stripTrailingZeros().scale() <= 0
        val symbols = DecimalFormatSymbols(Locale.Builder().setLanguage("en").setRegion("IN").build())
        val pattern = if (isWhole) "#,##0" else "#,##0.00"
        val formatted = DecimalFormat(pattern, symbols).format(BigDecimal.valueOf(rounded).abs())
        return if (rounded < 0.0) "-₹$formatted" else "₹$formatted"
    }

    fun formatPercent(percent: Double): String {
        val rounded = round2(percent)
        val isWhole = BigDecimal.valueOf(rounded).stripTrailingZeros().scale() <= 0
        return if (isWhole) "${rounded.toInt()}%" else String.format(Locale.US, "%.2f%%", rounded)
    }

    fun formatEditableNumber(value: Double): String {
        if (value == 0.0) return ""
        val rounded = round2(value)
        val bd = BigDecimal.valueOf(rounded).stripTrailingZeros()
        return if (bd.scale() <= 0) bd.toPlainString() else String.format(Locale.US, "%.2f", rounded)
    }
}
