package com.example

import com.example.model.AuthSession
import com.example.model.CommissionRuleEntry
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.CustomerRecord
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.OrderItem
import com.example.model.OrderRiskLevel
import com.example.model.ProductItem
import com.example.model.SecurityShieldConfig
import com.example.model.StoreOrder
import com.example.model.StorePayout
import com.example.util.EarningsCalculator
import com.example.util.LedgerFilterOption
import com.example.util.SecurityGuard
import com.example.util.StoreUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun mobileValidation_matchesIndianTenDigits() {
        assertTrue(StoreUtils.isValidIndianMobile("9876543210"))
        assertTrue(StoreUtils.isValidIndianMobile("+91 7002123456"))
        assertFalse(StoreUtils.isValidIndianMobile("1234567890"))
        assertFalse(StoreUtils.isValidIndianMobile("98765"))
    }

    @Test
    fun upiValidation_matchesStandardHandles() {
        assertTrue(StoreUtils.isValidUpiId("storeowner@okicici"))
        assertTrue(StoreUtils.isValidUpiId("gossaigaon.bazar@ybl"))
        assertFalse(StoreUtils.isValidUpiId("invalid-upi"))
        assertFalse(StoreUtils.isValidUpiId("@okicici"))
    }

    @Test
    fun pickupCode_usesExplicitOtpOrDeterministicFourDigitHash() {
        assertEquals("4321", StoreUtils.getStorePickupCode("4321", "-Order123"))
        val generated = StoreUtils.getStorePickupCode("", "-Order123456")
        assertEquals(4, generated.length)
        assertEquals(generated, StoreUtils.getStorePickupCode(null, "-Order123456"))
    }

    @Test
    fun imageUrlCleanup_resolvesDriveDropboxImgur() {
        assertEquals(
            "https://drive.google.com/uc?export=view&id=abc123XYZ",
            StoreUtils.cleanAndResolveImageUrl("https://drive.google.com/file/d/abc123XYZ/view")
        )
        assertEquals(
            "https://www.dropbox.com/s/test/item.jpg?raw=1",
            StoreUtils.cleanAndResolveImageUrl("https://www.dropbox.com/s/test/item.jpg?dl=0")
        )
        assertEquals(
            "https://i.imgur.com/XyZ99.jpg",
            StoreUtils.cleanAndResolveImageUrl("https://imgur.com/XyZ99")
        )
    }

    @Test
    fun distanceCalculation_returnsPositiveKmOrNullWhenMissing() {
        assertNull(StoreUtils.calculateDistanceKm(26.4524, 89.9723, null, 89.98))
        val km = StoreUtils.calculateDistanceKm(26.4524, 89.9723, 26.4624, 89.9823)
        assertNotNull(km)
        assertTrue(km!! > 0.5 && km < 3.0)
    }

    @Test
    fun csvExport_formatsHeaderAndRows() {
        val orders = listOf(
            StoreOrder(
                orderId = "-Ord998877",
                phone = "9876543210",
                paymentMode = "UPI",
                subtotalAmount = 240.0,
                status = "DELIVERED",
                createdAt = 1700000000000L,
                items = listOf(OrderItem(name = "Potato", qty = 2, price = 120.0))
            )
        )
        val csv = StoreUtils.buildSalesCsv(orders)
        assertTrue(csv.startsWith("Order ID,Date,Phone,Payment Mode,Items Subtotal,Status\n"))
        assertTrue(csv.contains("998877"))
        assertTrue(csv.contains("UPI,240.00,DELIVERED"))
    }

    @Test
    fun earningsCalculator_promptExampleAndPriceChanges() {
        // Prompt sample: Purchase Cost 100, Selling Price 150, Commission 10%
        val base = EarningsCalculator.calculate(
            purchaseCost = 100.0,
            sellingPrice = 150.0,
            commissionPercent = 10.0,
            ruleSource = CommissionRuleSource.GLOBAL
        )
        assertEquals(15.0, base.commissionAmount, 0.001)
        assertEquals(135.0, base.storeEarning, 0.001)
        assertEquals(35.0, base.estimatedProfit, 0.001)

        // Selling price changes to 200
        val updatedSell = EarningsCalculator.calculate(100.0, 200.0, 10.0)
        assertEquals(20.0, updatedSell.commissionAmount, 0.001)
        assertEquals(180.0, updatedSell.storeEarning, 0.001)
        assertEquals(80.0, updatedSell.estimatedProfit, 0.001)

        // Purchase cost changes to 120
        val updatedCost = EarningsCalculator.calculate(120.0, 200.0, 10.0)
        assertEquals(180.0, updatedCost.storeEarning, 0.001)
        assertEquals(60.0, updatedCost.estimatedProfit, 0.001)

        // Commission changes to 15%
        val updatedComm = EarningsCalculator.calculate(120.0, 200.0, 15.0)
        assertEquals(30.0, updatedComm.commissionAmount, 0.001)
        assertEquals(170.0, updatedComm.storeEarning, 0.001)
        assertEquals(50.0, updatedComm.estimatedProfit, 0.001)
    }

    @Test
    fun earningsCalculator_decimalPricesPrecisionAndRounding() {
        // 149.75 * 7.5% = 11.23125 -> 11.23; Store = 138.52; Profit = 138.52 - 99.45 = 39.07
        val decimalCalc = EarningsCalculator.calculate(
            purchaseCost = 99.45,
            sellingPrice = 149.75,
            commissionPercent = 7.5
        )
        assertEquals(11.23, decimalCalc.commissionAmount, 0.001)
        assertEquals(138.52, decimalCalc.storeEarning, 0.001)
        assertEquals(39.07, decimalCalc.estimatedProfit, 0.001)
        assertEquals("₹138.52", EarningsCalculator.formatRupees(decimalCalc.storeEarning))
        assertEquals("₹150", EarningsCalculator.formatRupees(150.0))
    }

    @Test
    fun commissionPriority_productOverStoreOverCategoryOverGlobal() {
        val rules = CommissionRules(
            globalCommissionPercent = 5.0,
            globalRuleEnabled = true,
            categoryCommissions = mapOf(
                "VEG" to CommissionRuleEntry("VEG", "Vegetables", percent = 7.0, enabled = true),
                "DAIRY" to CommissionRuleEntry("DAIRY", "Dairy", percent = 4.0, enabled = false)
            ),
            storeCommissions = mapOf(
                "store_A" to CommissionRuleEntry("store_A", "Store A", percent = 9.0, enabled = true),
                "store_B" to CommissionRuleEntry("store_B", "Store B", percent = 11.0, enabled = false)
            ),
            productCommissions = mapOf(
                "prod_1" to CommissionRuleEntry("prod_1", "Potato", "store_A", percent = 12.0, enabled = true),
                "prod_2" to CommissionRuleEntry("prod_2", "Onion", "store_A", percent = 15.0, enabled = false)
            )
        )

        // 1. Product override wins over Store, Category, Global
        val p1 = EarningsCalculator.resolveCommission("prod_1", "store_A", "VEG", rules)
        assertEquals(CommissionRuleSource.PRODUCT, p1.source)
        assertEquals(12.0, p1.percent, 0.001)

        // 2. Disabled product override falls back to enabled Store rule
        val p2 = EarningsCalculator.resolveCommission("prod_2", "store_A", "VEG", rules)
        assertEquals(CommissionRuleSource.STORE, p2.source)
        assertEquals(9.0, p2.percent, 0.001)

        // 3. Disabled store rule falls back to enabled Category rule
        val p3 = EarningsCalculator.resolveCommission("prod_99", "store_B", "VEG", rules)
        assertEquals(CommissionRuleSource.CATEGORY, p3.source)
        assertEquals(7.0, p3.percent, 0.001)

        // 4. Disabled category rule falls back to Global default
        val p4 = EarningsCalculator.resolveCommission("prod_99", "store_B", "DAIRY", rules)
        assertEquals(CommissionRuleSource.GLOBAL, p4.source)
        assertEquals(5.0, p4.percent, 0.001)

        // 5. Disabled Global rule returns 0%
        val p5 = EarningsCalculator.resolveCommission(
            "prod_99",
            "store_B",
            "DAIRY",
            rules.copy(globalRuleEnabled = false)
        )
        assertEquals(CommissionRuleSource.DISABLED, p5.source)
        assertEquals(0.0, p5.percent, 0.001)
    }

    @Test
    fun businessRulesValidation_rejectsZeroNegativeInvalidAndLossMakingUnlessAllowed() {
        // Zero selling price
        assertFalse(
            EarningsCalculator.validateAndCalculate("50", "0", "10", CommissionRuleSource.GLOBAL, false).isValid
        )
        // Negative selling price
        assertFalse(
            EarningsCalculator.validateAndCalculate("50", "-10", "10", CommissionRuleSource.GLOBAL, false).isValid
        )
        // Negative purchase cost
        assertFalse(
            EarningsCalculator.validateAndCalculate("-1", "100", "10", CommissionRuleSource.GLOBAL, false).isValid
        )
        // Commission > 100 or < 0
        assertFalse(
            EarningsCalculator.validateAndCalculate("50", "100", "101", CommissionRuleSource.GLOBAL, false).isValid
        )
        assertFalse(
            EarningsCalculator.validateAndCalculate("50", "100", "-5", CommissionRuleSource.GLOBAL, false).isValid
        )
        // Loss making blocked when allowLossMakingProducts = false (100 - 10% = 90 < 95 cost)
        val blockedLoss = EarningsCalculator.validateAndCalculate(
            "95", "100", "10", CommissionRuleSource.GLOBAL, allowLossMakingProducts = false
        )
        assertFalse(blockedLoss.isValid)
        assertTrue(blockedLoss.breakdown?.isLossMaking == true)

        // Allowed when Admin enables allowLossMakingProducts
        val allowedLoss = EarningsCalculator.validateAndCalculate(
            "95", "100", "10", CommissionRuleSource.GLOBAL, allowLossMakingProducts = true
        )
        assertTrue(allowedLoss.isValid)
        assertEquals(-5.0, allowedLoss.breakdown!!.estimatedProfit, 0.001)
    }

    @Test
    fun orderLedger_preservesHistoryPreventDoubleDeductionAndReversesRefunds() {
        val rulesV1 = CommissionRules(globalCommissionPercent = 10.0, globalRuleEnabled = true)
        val products = listOf(
            ProductItem(
                itemId = "p1",
                name = "Basmati Rice",
                price = 200.0,
                sellingPrice = 200.0,
                purchaseCost = 150.0,
                commissionPercentage = 10.0,
                commissionRuleSource = CommissionRuleSource.GLOBAL.name
            )
        )
        val order = StoreOrder(
            orderId = "ord_1001",
            restaurantId = "store_1",
            status = "DELIVERED",
            subtotalAmount = 200.0,
            items = listOf(OrderItem(itemId = "p1", name = "Basmati Rice", qty = 1, price = 200.0))
        )

        val initialLedger = EarningsCalculator.buildOrUpdateOrderLedger(
            order = order,
            productsByIdOrName = EarningsCalculator.buildProductIndex(products),
            rules = rulesV1,
            existingEntry = null,
            storeName = "Test Mart"
        )
        assertEquals("ACTIVE", initialLedger.ledgerStatus)
        assertTrue(initialLedger.commissionDeducted)
        assertEquals(20.0, initialLedger.commissionAmount, 0.001)
        assertEquals(180.0, initialLedger.storeEarning, 0.001)
        assertEquals(30.0, initialLedger.estimatedProfit, 0.001)

        // Admin later changes global commission to 25% -> historical order ledger MUST NOT change or deduct twice
        val rulesV2 = CommissionRules(globalCommissionPercent = 25.0, globalRuleEnabled = true)
        val reSynced = EarningsCalculator.buildOrUpdateOrderLedger(
            order = order,
            productsByIdOrName = EarningsCalculator.buildProductIndex(products),
            rules = rulesV2,
            existingEntry = initialLedger,
            storeName = "Test Mart"
        )
        assertEquals(10.0, reSynced.commissionPercentage, 0.001)
        assertEquals(20.0, reSynced.commissionAmount, 0.001)
        assertEquals(180.0, reSynced.storeEarning, 0.001)

        // Order is cancelled / refunded -> reverses earnings & commission cleanly once
        val cancelledOrder = order.copy(status = "CANCELLED", cancelReason = "Customer requested refund")
        val reversed = EarningsCalculator.buildOrUpdateOrderLedger(
            order = cancelledOrder,
            productsByIdOrName = EarningsCalculator.buildProductIndex(products),
            rules = rulesV2,
            existingEntry = reSynced,
            storeName = "Test Mart"
        )
        assertEquals("REVERSED", reversed.ledgerStatus)
        assertEquals(0.0, reversed.commissionAmount, 0.001)
        assertEquals(0.0, reversed.storeEarning, 0.001)
        assertEquals(20.0, reversed.reversedCommissionAmount, 0.001)
        assertEquals(180.0, reversed.reversedStoreEarning, 0.001)

        // Re-running after reversal does not double-subtract or corrupt reversed totals
        val refundedAgain = EarningsCalculator.buildOrUpdateOrderLedger(
            order = cancelledOrder.copy(status = "REFUNDED"),
            productsByIdOrName = EarningsCalculator.buildProductIndex(products),
            rules = rulesV2,
            existingEntry = reversed,
            storeName = "Test Mart"
        )
        assertEquals("REVERSED", refundedAgain.ledgerStatus)
        assertEquals(0.0, refundedAgain.storeEarning, 0.001)
        assertEquals(20.0, refundedAgain.reversedCommissionAmount, 0.001)
        assertEquals(180.0, refundedAgain.reversedStoreEarning, 0.001)
    }

    @Test
    fun incomingKotHelpers_filterActionableOrdersAndFormatAlertText() {
        val codKot = StoreOrder(
            orderId = "-Ord123456",
            status = "PLACED",
            paymentMode = "COD",
            subtotalAmount = 275.0,
            items = listOf(
                OrderItem(name = "Potato", qty = 2, price = 50.0),
                OrderItem(name = "Atta", qty = 1, price = 175.0)
            )
        )
        val unverifiedUpi = codKot.copy(orderId = "-Ord999111", paymentMode = "UPI", paymentStatus = "PENDING")
        val verifiedUpi = unverifiedUpi.copy(paymentStatus = "VERIFIED")
        val alreadyPacking = codKot.copy(status = "PREPARING")

        assertTrue(StoreUtils.isActionableKotOrder(codKot))
        assertFalse(StoreUtils.isActionableKotOrder(unverifiedUpi))
        assertTrue(StoreUtils.isActionableKotOrder(verifiedUpi))
        assertFalse(StoreUtils.isActionableKotOrder(alreadyPacking))

        val text = StoreUtils.formatIncomingKotAlertText(codKot)
        assertTrue(text.contains("#123456"))
        assertTrue(text.contains("3 items"))
        assertTrue(text.contains("COD"))
        assertTrue(text.contains("₹275"))
    }

    @Test
    fun dashboardSummary_aggregatesIncomePendingPayoutsAndTotalSalesFromLedger() {
        val ledger = listOf(
            OrderEarningsLedgerEntry(
                orderId = "ord_del_1",
                storeId = "s1",
                orderStatus = "DELIVERED",
                ledgerStatus = "ACTIVE",
                grossAmount = 500.0,
                commissionPercentage = 10.0,
                commissionAmount = 50.0,
                storeEarning = 450.0,
                estimatedProfit = 120.0,
                commissionRuleSource = CommissionRuleSource.STORE.name,
                createdAt = 3000L
            ),
            OrderEarningsLedgerEntry(
                orderId = "ord_del_2",
                storeId = "s1",
                orderStatus = "DELIVERED",
                ledgerStatus = "ACTIVE",
                grossAmount = 300.0,
                commissionPercentage = 5.0,
                commissionAmount = 15.0,
                storeEarning = 285.0,
                estimatedProfit = 65.0,
                commissionRuleSource = CommissionRuleSource.GLOBAL.name,
                createdAt = 2000L
            ),
            OrderEarningsLedgerEntry(
                orderId = "ord_prog_3",
                storeId = "s1",
                orderStatus = "PREPARING",
                ledgerStatus = "ACTIVE",
                grossAmount = 200.0,
                commissionPercentage = 10.0,
                commissionAmount = 20.0,
                storeEarning = 180.0,
                estimatedProfit = 40.0,
                commissionRuleSource = CommissionRuleSource.PRODUCT.name,
                createdAt = 4000L
            ),
            OrderEarningsLedgerEntry(
                orderId = "ord_rev_4",
                storeId = "s1",
                orderStatus = "CANCELLED",
                ledgerStatus = "REVERSED",
                grossAmount = 150.0,
                commissionAmount = 0.0,
                storeEarning = 0.0,
                reversedCommissionAmount = 15.0,
                reversedStoreEarning = 135.0,
                reversalReason = "Out of stock",
                createdAt = 1000L
            )
        )
        val payouts = listOf(
            StorePayout(payoutId = "p1", amount = 250.0, status = "PAID"),
            StorePayout(payoutId = "p2", amount = 200.0, status = "PENDING"),
            StorePayout(payoutId = "p3", amount = 100.0, status = "REJECTED")
        )

        val summary = EarningsCalculator.summarizeStoreDashboard(ledger, payouts)

        assertEquals(735.0, summary.deliveredStoreIncome, 0.001)
        assertEquals(185.0, summary.deliveredEstimatedProfit, 0.001)
        assertEquals(65.0, summary.deliveredCommissionDeducted, 0.001)
        assertEquals(180.0, summary.inProgressStoreIncome, 0.001)
        assertEquals(915.0, summary.totalActiveStoreIncome, 0.001)
        assertEquals(85.0, summary.totalActiveCommission, 0.001)
        assertEquals(135.0, summary.reversedStoreEarning, 0.001)

        assertEquals(250.0, summary.paidPayoutTotal, 0.001)
        assertEquals(1, summary.paidPayoutCount)
        assertEquals(200.0, summary.requestedPendingPayoutTotal, 0.001)
        assertEquals(1, summary.requestedPendingPayoutCount)
        assertEquals(1, summary.rejectedPayoutCount)
        // Withdrawable = 735 - 250 = 485
        assertEquals(485.0, summary.withdrawableBalance, 0.001)

        assertEquals(800.0, summary.totalSalesDelivered, 0.001)
        assertEquals(1000.0, summary.totalSalesAllActive, 0.001)
        assertEquals(2, summary.deliveredOrderCount)
        assertEquals(1, summary.inProgressOrderCount)
        assertEquals(3, summary.activeOrderCount)
        assertEquals(1, summary.reversedOrderCount)
        assertEquals(4, summary.totalOrderCount)
        assertEquals(333.33, summary.averageOrderValue, 0.001)
        assertEquals(8.5, summary.effectiveCommissionPercent, 0.001)
        assertEquals(91.5, summary.storeSharePercent, 0.001)

        assertEquals(4, EarningsCalculator.filterLedgerEntries(ledger, LedgerFilterOption.ALL).size)
        assertEquals(3, EarningsCalculator.filterLedgerEntries(ledger, LedgerFilterOption.ACTIVE).size)
        assertEquals(2, EarningsCalculator.filterLedgerEntries(ledger, LedgerFilterOption.DELIVERED).size)
        assertEquals(1, EarningsCalculator.filterLedgerEntries(ledger, LedgerFilterOption.REVERSED).size)
        assertEquals("ord_prog_3", summary.sortedLedgerEntries.first().orderId)

        val empty = EarningsCalculator.summarizeStoreDashboard(emptyList(), emptyList())
        assertEquals(0.0, empty.deliveredStoreIncome, 0.001)
        assertEquals(0.0, empty.withdrawableBalance, 0.001)
        assertEquals(0.0, empty.totalSalesDelivered, 0.001)
        assertEquals(0, empty.totalOrderCount)
    }

    @Test
    fun securityGuard_passwordStrength_blocksWeakAndAcceptsStrong() {
        assertFalse(SecurityGuard.evaluatePasswordStrength("123456").isAcceptable)
        assertFalse(SecurityGuard.evaluatePasswordStrength("password").isAcceptable)
        assertFalse(SecurityGuard.evaluatePasswordStrength("abcdefg").isAcceptable)
        assertFalse(SecurityGuard.evaluatePasswordStrength("12345678").isAcceptable)
        val strong = SecurityGuard.evaluatePasswordStrength("Kirana@2026")
        assertTrue(strong.isAcceptable)
        assertEquals(4, strong.score)
    }

    @Test
    fun securityGuard_urlAndInputSanitizer_blocksXssCleartextAndShorteners() {
        assertEquals("", StoreUtils.cleanAndResolveImageUrl("javascript:alert(1)"))
        assertEquals("", StoreUtils.cleanAndResolveImageUrl("data:text/html,<script>evil()</script>"))
        assertEquals("https://example.com/pic.jpg", StoreUtils.cleanAndResolveImageUrl("http://example.com/pic.jpg"))
        assertFalse(SecurityGuard.isSafeExternalUrl("https://bit.ly/3fake"))
        assertFalse(SecurityGuard.isSafeExternalUrl("https://192.168.1.50/phish"))
        assertTrue(SecurityGuard.isSafeExternalUrl("https://images.unsplash.com/photo-123"))
        assertEquals("safeKey123", SecurityGuard.sanitizeFirebaseKey("safe\$Key.[1]/2#3"))
    }

    @Test
    fun securityGuard_phishingScanner_detectsRemoteScamLinksAndSyntheticPhones() {
        val scam = SecurityGuard.scanPhishingOrScamMessage(
            "Urgent KYC update! Install AnyDesk and share OTP via http://10.0.0.1/kyc or bit.ly/pay"
        )
        assertTrue(scam.isThreat)
        assertTrue(scam.riskScore >= 70)
        assertEquals(OrderRiskLevel.CRITICAL, scam.level)
        assertTrue(scam.findings.any { it.contains("AnyDesk") })

        val fakePhone = SecurityGuard.scanPhishingOrScamMessage("9999999999")
        assertTrue(fakePhone.isThreat)

        val safeMsg = SecurityGuard.scanPhishingOrScamMessage("Deliver fresh potatoes at Main Bazar Gate 2")
        assertFalse(safeMsg.isThreat)
        assertEquals(OrderRiskLevel.LOW, safeMsg.level)
    }

    @Test
    fun securityGuard_orderRiskEngine_flagsScamSpamTamperingVelocityAndDuplicates() {
        val now = 1_700_000_000_000L
        val config = SecurityShieldConfig(codVerificationLimit = 1500.0)
        val customers = mapOf("cust_bad" to CustomerRecord(uid = "cust_bad", blocked = true))

        val priorCancelled1 = StoreOrder(
            orderId = "o_prev1",
            customerUid = "cust_burst",
            phone = "9876501234",
            status = "CANCELLED",
            createdAt = now - 10 * 60_000L
        )
        val priorCancelled2 = StoreOrder(
            orderId = "o_prev2",
            customerUid = "cust_burst",
            phone = "9876501234",
            status = "CANCELLED",
            createdAt = now - 5 * 60_000L
        )
        val recentActive = StoreOrder(
            orderId = "o_prev3",
            customerUid = "cust_burst",
            phone = "9876501234",
            status = "PLACED",
            subtotalAmount = 1800.0,
            items = listOf(OrderItem(name = "Atta", qty = 2, price = 900.0)),
            createdAt = now - 60_000L
        )
        val suspiciousOrder = StoreOrder(
            orderId = "o_new",
            customerUid = "cust_burst",
            phone = "9876501234",
            paymentMode = "COD",
            address = "Call for OTP refund link bit.ly/abc",
            subtotalAmount = 1800.0,
            items = listOf(OrderItem(name = "Atta", qty = 2, price = 900.0)),
            createdAt = now
        )
        val tamperedOrder = StoreOrder(
            orderId = "o_tamper",
            customerUid = "cust_bad",
            phone = "1234567890",
            paymentMode = "UPI",
            paymentStatus = "PENDING",
            subtotalAmount = 50.0,
            items = listOf(OrderItem(name = "Oil", qty = 5, price = 200.0)),
            createdAt = now
        )

        val all = listOf(priorCancelled1, priorCancelled2, recentActive, suspiciousOrder, tamperedOrder)
        val risk1 = SecurityGuard.evaluateOrderRisk(suspiciousOrder, all, customers, config)
        assertTrue(risk1.score >= 70)
        assertEquals(OrderRiskLevel.CRITICAL, risk1.level)
        assertTrue(risk1.isDuplicateSuspect)
        assertTrue(risk1.isPhishingTextSuspect)

        val risk2 = SecurityGuard.evaluateOrderRisk(tamperedOrder, all, customers, config)
        assertEquals(OrderRiskLevel.CRITICAL, risk2.level)
        assertTrue(risk2.reasons.any { it.contains("tampered", ignoreCase = true) })
        assertTrue(risk2.reasons.any { it.contains("Blocked", ignoreCase = true) })

        val safeOverride = SecurityGuard.evaluateOrderRisk(
            order = suspiciousOrder,
            allOrders = all,
            customers = customers,
            config = config,
            safeMarkedOrderIds = setOf("o_new")
        )
        assertEquals(OrderRiskLevel.LOW, safeOverride.level)
        assertEquals(0, safeOverride.score)
    }

    @Test
    fun authSession_emailVerificationAndGoogleProviderRules() {
        val unauth = AuthSession()
        assertFalse(unauth.requiresEmailVerification)
        assertFalse(unauth.isFullyAuthenticated)

        val unverifiedEmailUser = AuthSession(
            uid = "u_123",
            email = "owner@store.in",
            isEmailVerified = false,
            isGoogleProvider = false
        )
        assertTrue(unverifiedEmailUser.requiresEmailVerification)
        assertFalse(unverifiedEmailUser.isFullyAuthenticated)

        val verifiedEmailUser = unverifiedEmailUser.copy(isEmailVerified = true)
        assertFalse(verifiedEmailUser.requiresEmailVerification)
        assertTrue(verifiedEmailUser.isFullyAuthenticated)

        val googleUser = AuthSession(
            uid = "u_google",
            email = "partner@gmail.com",
            isEmailVerified = false,
            isGoogleProvider = true
        )
        assertFalse(googleUser.requiresEmailVerification)
        assertTrue(googleUser.isFullyAuthenticated)

        assertTrue(SecurityGuard.isValidEmail("partner.store+1@gmail.com"))
        assertFalse(SecurityGuard.isValidEmail("not-an-email"))
        assertFalse(SecurityGuard.isValidEmail("store@"))
    }
}
