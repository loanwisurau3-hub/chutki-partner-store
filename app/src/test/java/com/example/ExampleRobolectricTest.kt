package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.StoreDataSource
import com.example.model.AdminSettings
import com.example.model.CommissionRuleEntry
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.CustomerRecord
import com.example.model.DeliveryRider
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.OrderItem
import com.example.model.OrderRiskAssessment
import com.example.model.OrderRiskLevel
import com.example.model.PartnerTab
import com.example.model.ProductItem
import com.example.model.SecurityPosture
import com.example.model.SecurityShieldConfig
import com.example.model.StoreFeedback
import com.example.model.StoreOrder
import com.example.model.StorePayout
import com.example.model.StoreProfile
import com.example.model.StoreSummaryReport
import com.example.ui.StoreViewModel
import com.example.ui.components.PartnerToastBanner
import com.example.ui.components.ProductEditorFormContent
import com.example.ui.screens.AdminCommissionScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EarningsDashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrdersTabScreen
import com.example.ui.screens.SecurityShieldScreen
import com.example.ui.theme.GossaigaonStoreTheme
import com.example.util.StoreUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gossaigaon Store", appName)
    }

    @Test
    fun `inventory screen displays stock levels and triggers approve and flag toggles`() {
        var approvedClickedId: String? = null
        var flaggedReason: String? = null
        val products = listOf(
            ProductItem(
                itemId = "item_1",
                name = "Fresh Potatoes (1 kg)",
                price = 35.0,
                category = "VEG",
                available = true,
                stockQty = 4,
                adminApproved = false,
                inventoryFlagged = false
            ),
            ProductItem(
                itemId = "item_2",
                name = "Mustard Oil (1 L)",
                price = 165.0,
                category = "OIL_MASALA",
                available = false,
                stockQty = 0,
                adminApproved = true,
                inventoryFlagged = false
            )
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                InventoryScreen(
                    products = products,
                    onToggleApprove = { approvedClickedId = it.itemId },
                    onToggleFlag = { _, reason -> flaggedReason = reason },
                    onUpdateStockQty = { _, _ -> },
                    onToggleStock = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("inventory_grid")
            .performScrollToNode(hasText("Fresh Potatoes (1 kg)"))
        composeTestRule.onNodeWithText("Fresh Potatoes (1 kg)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Low Stock (4 units)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("inventory_approve_toggle_item_1").performClick()
        assertEquals("item_1", approvedClickedId)

        composeTestRule.onNodeWithTag("inventory_flag_toggle_item_1").performClick()
        composeTestRule.onNodeWithTag("confirm_flag_inventory_button").performClick()
        assertEquals("Stock Mismatch", flaggedReason)

        composeTestRule.onNodeWithTag("inventory_grid")
            .performScrollToNode(hasText("Mustard Oil (1 L)"))
        composeTestRule.onNodeWithText("Mustard Oil (1 L)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Out of Stock").assertIsDisplayed()
    }

    @Test
    fun `product form live calculator updates earnings on price cost and commission changes`() {
        var rulesState by mutableStateOf(
            CommissionRules(
                globalCommissionPercent = 10.0,
                globalRuleEnabled = true,
                allowLossMakingProducts = false
            )
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                ProductEditorFormContent(
                    editingItem = null,
                    storeId = "store_1",
                    commissionRules = rulesState,
                    availableCategories = StoreUtils.DEFAULT_CATEGORIES,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onToast = { _, _ -> }
                )
            }
        }

        // Enter Purchase Cost 100 and Selling Price 150 with default 10% commission
        composeTestRule.onNodeWithTag("product_purchase_cost_input").performTextInput("100")
        composeTestRule.onNodeWithTag("product_price_input").performTextInput("150")

        composeTestRule.onNodeWithTag("preview_selling_price").assertTextContains("₹150")
        composeTestRule.onNodeWithTag("preview_commission_amount").assertTextContains("₹15")
        composeTestRule.onNodeWithTag("preview_store_earning").assertTextContains("₹135")
        composeTestRule.onNodeWithTag("preview_estimated_profit").assertTextContains("₹35")
        composeTestRule.onNodeWithTag("submit_product_button").assertIsEnabled()

        // Admin changes global commission rule to 20% -> live form recalculates automatically
        rulesState = rulesState.copy(globalCommissionPercent = 20.0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("preview_commission_amount").assertTextContains("₹30")
        composeTestRule.onNodeWithTag("preview_store_earning").assertTextContains("₹120")
        composeTestRule.onNodeWithTag("preview_estimated_profit").assertTextContains("₹20")

        // Change Selling Price to decimal value 149.75 with 10% override
        composeTestRule.onNodeWithTag("product_commission_input").performTextClearance()
        composeTestRule.onNodeWithTag("product_commission_input").performTextInput("10")
        composeTestRule.onNodeWithTag("product_price_input").performTextClearance()
        composeTestRule.onNodeWithTag("product_price_input").performTextInput("149.75")
        composeTestRule.onNodeWithTag("preview_commission_amount").assertTextContains("₹14.98")
        composeTestRule.onNodeWithTag("preview_store_earning").assertTextContains("₹134.77")

        // Loss-making price (Selling 100, Cost 100, Comm 10% => Receives 90 < 100) blocks save
        composeTestRule.onNodeWithTag("product_price_input").performTextClearance()
        composeTestRule.onNodeWithTag("product_price_input").performTextInput("100")
        composeTestRule.onNodeWithTag("pricing_validation_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_product_button").assertIsNotEnabled()

        // Zero selling price shows validation error and disables save
        composeTestRule.onNodeWithTag("product_price_input").performTextClearance()
        composeTestRule.onNodeWithTag("product_price_input").performTextInput("0")
        composeTestRule.onNodeWithTag("pricing_validation_error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_product_button").assertIsNotEnabled()
    }

    @Test
    fun `editing existing product preloads fields and recalculates with store override`() {
        var savedCost = ""
        var savedSell = ""
        var savedComm = ""
        val existing = ProductItem(
            itemId = "prod_edit_1",
            name = "Mustard Oil 1L",
            price = 180.0,
            sellingPrice = 180.0,
            purchaseCost = 140.0,
            category = "OIL_MASALA",
            imageUrl = "https://example.com/oil.jpg"
        )
        val rules = CommissionRules(
            globalCommissionPercent = 5.0,
            storeCommissions = mapOf(
                "store_abc" to CommissionRuleEntry("store_abc", "Bazar Mart", percent = 8.0, enabled = true)
            )
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                ProductEditorFormContent(
                    editingItem = existing,
                    storeId = "store_abc",
                    commissionRules = rules,
                    availableCategories = StoreUtils.DEFAULT_CATEGORIES,
                    onDismiss = {},
                    onSave = { _, _, cost, sell, comm, _, _, _, _, _, _, _, _, _ ->
                        savedCost = cost
                        savedSell = sell
                        savedComm = comm
                    },
                    onToast = { _, _ -> }
                )
            }
        }

        // 180 * 8% = 14.40; Store = 165.60; Profit = 25.60
        composeTestRule.onNodeWithTag("preview_commission_amount").assertTextContains("₹14.40")
        composeTestRule.onNodeWithTag("preview_store_earning").assertTextContains("₹165.60")
        composeTestRule.onNodeWithTag("preview_estimated_profit").assertTextContains("₹25.60")

        composeTestRule.onNodeWithTag("product_price_input").performTextClearance()
        composeTestRule.onNodeWithTag("product_price_input").performTextInput("200")
        composeTestRule.onNodeWithTag("submit_product_button").performScrollTo().performClick()

        assertEquals("140", savedCost)
        assertEquals("200", savedSell)
        assertEquals("8", savedComm)
    }

    @Test
    fun `admin commission screen saves global store and category rules and renders reports`() {
        var savedGlobalPct = ""
        var savedStorePct = ""
        var savedCatPct = ""

        val sampleProducts = listOf(
            ProductItem(itemId = "p1", name = "Aloo 1kg", sellingPrice = 40.0, category = "VEG")
        )
        val sampleReports = listOf(
            StoreSummaryReport(
                storeId = "s1",
                storeName = "Gossaigaon Fresh",
                totalOrders = 5,
                activeOrders = 4,
                reversedOrders = 1,
                grossSales = 1000.0,
                totalChutkiCommission = 100.0,
                totalStoreEarning = 900.0
            )
        )
        val sampleLedger = listOf(
            OrderEarningsLedgerEntry(
                orderId = "o1",
                storeId = "s1",
                storeName = "Gossaigaon Fresh",
                grossAmount = 1000.0,
                commissionAmount = 100.0,
                storeEarning = 900.0,
                ledgerStatus = "ACTIVE"
            )
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                AdminCommissionScreen(
                    currentStoreId = "s1",
                    currentStoreName = "Gossaigaon Fresh",
                    commissionRules = CommissionRules(globalCommissionPercent = 5.0),
                    products = sampleProducts,
                    availableCategories = StoreUtils.DEFAULT_CATEGORIES,
                    allStores = emptyMap(),
                    storeReports = sampleReports,
                    ledgerEntries = sampleLedger,
                    onBack = {},
                    onSaveGlobal = { pct, _, _ -> savedGlobalPct = pct },
                    onSaveStoreRule = { _, _, pct, _ -> savedStorePct = pct },
                    onDeleteStoreRule = {},
                    onSaveCategoryRule = { _, _, pct, _ -> savedCatPct = pct },
                    onDeleteCategoryRule = {},
                    onSaveProductRule = { _, _, _, _, _ -> },
                    onDeleteProductRule = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("admin_global_commission_input").performTextClearance()
        composeTestRule.onNodeWithTag("admin_global_commission_input").performTextInput("7.5")
        composeTestRule.onNodeWithTag("save_global_commission_button").performClick()
        assertEquals("7.5", savedGlobalPct)

        composeTestRule.onNodeWithTag("admin_commission_list")
            .performScrollToNode(hasText("Save Store Commission Rule"))
        composeTestRule.onNodeWithTag("admin_store_commission_input").performTextClearance()
        composeTestRule.onNodeWithTag("admin_store_commission_input").performTextInput("9")
        composeTestRule.onNodeWithTag("save_store_commission_button").performClick()
        assertEquals("9", savedStorePct)

        composeTestRule.onNodeWithTag("admin_commission_list")
            .performScrollToNode(hasText("Save Category Commission Rule"))
        composeTestRule.onNodeWithTag("admin_category_commission_input").performTextClearance()
        composeTestRule.onNodeWithTag("admin_category_commission_input").performTextInput("12")
        composeTestRule.onNodeWithTag("save_category_commission_button").performClick()
        assertEquals("12", savedCatPct)

        composeTestRule.onNodeWithTag("admin_commission_list")
            .performScrollToNode(hasText("Gossaigaon Fresh"))
        composeTestRule.onNodeWithText("Gossaigaon Fresh").assertIsDisplayed()
        assertTrue(savedGlobalPct == "7.5" && savedStorePct == "9" && savedCatPct == "12")
    }

    @Test
    fun `incoming KOT order triggers visual toast notification and sound alert`() {
        val incomingKotFlow = MutableSharedFlow<StoreOrder>(extraBufferCapacity = 8)
        var chimeCount = 0

        val fakeRepo = object : StoreDataSource {
            override val authUidFlow: Flow<String?> = MutableStateFlow("store_uid_1")
            override suspend fun signIn(email: String, password: String) = Result.success("store_uid_1")
            override suspend fun registerStore(
                name: String, email: String, cuisine: String, address: String,
                phone: String, upiId: String, password: String, lat: Double, lng: Double
            ) = Result.success("store_uid_1")
            override fun signOut() {}
            override fun observeStoreProfile(uid: String) = flowOf(StoreProfile(name = "Test Kirana"))
            override fun observeAllStores() = flowOf(emptyMap<String, StoreProfile>())
            override fun observeStoreOrders(uid: String) = flowOf(emptyList<StoreOrder>())
            override fun observeIncomingKotOrders(uid: String): Flow<StoreOrder> = incomingKotFlow
            override fun observeAllOrders() = flowOf(emptyList<StoreOrder>())
            override fun observeStoreProducts(uid: String) = flowOf(emptyList<ProductItem>())
            override fun observeDeliveryRiders() = flowOf(emptyMap<String, DeliveryRider>())
            override fun observeCustomers() = flowOf(emptyMap<String, CustomerRecord>())
            override fun observeAdminSettings() = flowOf(AdminSettings())
            override fun observeCommissionRules() = flowOf(CommissionRules())
            override fun observeStoreEarningsLedger(uid: String) = flowOf(emptyMap<String, OrderEarningsLedgerEntry>())
            override fun observeAllEarningsLedger() = flowOf(emptyMap<String, OrderEarningsLedgerEntry>())
            override fun observeStorePayouts(uid: String) = flowOf(emptyList<StorePayout>())
            override fun observeStoreFeedbacks(uid: String) = flowOf(emptyList<StoreFeedback>())
            override suspend fun toggleStoreOpen(uid: String, isOpen: Boolean) = Result.success(Unit)
            override suspend fun updateStoreProfile(uid: String, updates: Map<String, Any>) = Result.success(Unit)
            override suspend fun updateOrderStatus(orderId: String, newStatus: String, pickupOtpIfNeeded: String?) = Result.success(Unit)
            override suspend fun rejectOrder(orderId: String, reason: String) = Result.success(Unit)
            override suspend fun reportOrderSpam(
                orderId: String, storeId: String, customerUid: String, phone: String, reason: String, riskScore: Int
            ) = Result.success(Unit)
            override suspend fun clearOrderSpamFlag(orderId: String) = Result.success(Unit)
            override suspend fun syncOrderEarningsLedger(entry: OrderEarningsLedgerEntry) = Result.success(Unit)
            override suspend fun saveGlobalCommissionSettings(globalPercent: Double, globalRuleEnabled: Boolean, allowLossMakingProducts: Boolean) = Result.success(Unit)
            override suspend fun saveStoreCommissionRule(storeId: String, storeName: String, percent: Double, enabled: Boolean) = Result.success(Unit)
            override suspend fun deleteStoreCommissionRule(storeId: String) = Result.success(Unit)
            override suspend fun saveCategoryCommissionRule(categoryId: String, categoryName: String, percent: Double, enabled: Boolean) = Result.success(Unit)
            override suspend fun deleteCategoryCommissionRule(categoryId: String) = Result.success(Unit)
            override suspend fun saveProductCommissionRule(productId: String, storeId: String, productName: String, percent: Double, enabled: Boolean) = Result.success(Unit)
            override suspend fun deleteProductCommissionRule(productId: String) = Result.success(Unit)
            override suspend fun saveProduct(uid: String, itemId: String?, itemData: Map<String, Any>) = Result.success(Unit)
            override suspend fun toggleProductStock(uid: String, itemId: String, available: Boolean) = Result.success(Unit)
            override suspend fun deleteProduct(uid: String, itemId: String) = Result.success(Unit)
            override suspend fun setInventoryApproval(uid: String, itemId: String, approved: Boolean) = Result.success(Unit)
            override suspend fun setInventoryFlag(uid: String, itemId: String, flagged: Boolean, reason: String) = Result.success(Unit)
            override suspend fun updateProductStockQty(uid: String, itemId: String, stockQty: Int) = Result.success(Unit)
            override suspend fun quickSeedProducts(uid: String, rules: CommissionRules) = Result.success(Unit)
            override suspend fun requestPayout(payout: Map<String, Any>) = Result.success(Unit)
            override suspend fun submitFeedback(feedback: Map<String, Any>) = Result.success(Unit)
        }

        val viewModel = StoreViewModel(
            repo = fakeRepo,
            playKotChime = { chimeCount++ },
            enablePeriodicRinging = false
        )

        composeTestRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            GossaigaonStoreTheme {
                PartnerToastBanner(
                    toast = uiState.activeToast,
                    onDismiss = { viewModel.onToastTapped() }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Navigate away to Products tab before new order arrives
        viewModel.selectTab(PartnerTab.MENU)

        val newOrder = StoreOrder(
            orderId = "-Ord778899",
            restaurantId = "store_uid_1",
            status = "PLACED",
            paymentMode = "COD",
            subtotalAmount = 320.0,
            items = listOf(OrderItem(name = "Atta 5kg", qty = 1, price = 320.0))
        )
        assertTrue(incomingKotFlow.tryEmit(newOrder))
        composeTestRule.waitForIdle()

        val activeToast = viewModel.uiState.value.activeToast
        assertEquals(1, chimeCount)
        assertNotNull(activeToast)
        assertTrue(activeToast!!.isKotAlert)
        assertTrue(activeToast.text.contains("#778899"))

        composeTestRule.onNodeWithTag("kot_toast_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("INCOMING KOT ORDER").assertIsDisplayed()

        // Duplicate emission of same orderId should NOT trigger a second chime
        assertTrue(incomingKotFlow.tryEmit(newOrder))
        composeTestRule.waitForIdle()
        assertEquals(1, chimeCount)

        // Tapping the KOT toast banner jumps back to Orders tab
        composeTestRule.onNodeWithTag("kot_toast_banner").performClick()
        assertEquals(PartnerTab.ORDERS, viewModel.uiState.value.selectedTab)

        // Muting sound suppresses chime on next KOT order while still showing visual toast
        viewModel.toggleSound()
        val secondOrder = newOrder.copy(orderId = "-Ord556677")
        assertTrue(incomingKotFlow.tryEmit(secondOrder))
        composeTestRule.waitForIdle()
        assertEquals(1, chimeCount)
        assertFalse(viewModel.uiState.value.soundEnabled)
        assertTrue(viewModel.uiState.value.activeToast?.text?.contains("#556677") == true)
    }

    @Test
    fun `earnings dashboard screen summarizes income pending payouts total sales and filters ledger`() {
        var payoutRequested = false
        var walletOpened = false
        var backClicked = false

        val ledgerEntries = mapOf(
            "ord_101" to OrderEarningsLedgerEntry(
                orderId = "ord_101",
                storeId = "store_1",
                storeName = "Gossaigaon Fresh",
                orderStatus = "DELIVERED",
                ledgerStatus = "ACTIVE",
                grossAmount = 600.0,
                commissionPercentage = 10.0,
                commissionAmount = 60.0,
                storeEarning = 540.0,
                estimatedProfit = 140.0,
                commissionRuleSource = CommissionRuleSource.STORE.name,
                createdAt = 3000L
            ),
            "ord_102" to OrderEarningsLedgerEntry(
                orderId = "ord_102",
                storeId = "store_1",
                storeName = "Gossaigaon Fresh",
                orderStatus = "CANCELLED",
                ledgerStatus = "REVERSED",
                grossAmount = 200.0,
                commissionPercentage = 10.0,
                commissionAmount = 0.0,
                storeEarning = 0.0,
                reversedCommissionAmount = 20.0,
                reversedStoreEarning = 180.0,
                commissionRuleSource = CommissionRuleSource.GLOBAL.name,
                reversalReason = "Customer cancelled",
                createdAt = 2000L
            )
        )
        val payouts = listOf(
            StorePayout(payoutId = "pay_1", amount = 140.0, status = "PAID"),
            StorePayout(payoutId = "pay_2", amount = 100.0, status = "PENDING")
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                EarningsDashboardScreen(
                    storeProfile = StoreProfile(name = "Gossaigaon Fresh", upiId = "fresh@okicici"),
                    adminSettings = AdminSettings(minStorePayout = 50.0),
                    orderLedgerEntries = ledgerEntries,
                    payouts = payouts,
                    onRequestPayout = { payoutRequested = true },
                    onOpenWalletPayouts = { walletOpened = true },
                    onBack = { backClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("hero_partner_income_value").assertTextContains("₹540")
        composeTestRule.onNodeWithTag("kpi_total_sales_value").assertTextContains("₹600")
        composeTestRule.onNodeWithTag("kpi_pending_payouts_value").assertTextContains("₹400")
        composeTestRule.onNodeWithTag("kpi_partner_income_value").assertTextContains("₹540")

        composeTestRule.onNodeWithTag("dashboard_request_payout_button").performClick()
        assertTrue(payoutRequested)

        composeTestRule.onNodeWithTag("dashboard_open_wallet_button").performClick()
        assertTrue(walletOpened)

        composeTestRule.onNodeWithTag("earnings_dashboard_list")
            .performScrollToNode(hasText("Firebase Ledger Transactions"))
        composeTestRule.onNodeWithTag("ledger_entry_ord_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ledger_entry_ord_102").assertIsDisplayed()

        composeTestRule.onNodeWithTag("ledger_filter_reversed").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ledger_entry_ord_101").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ledger_entry_ord_102").assertIsDisplayed()

        composeTestRule.onNodeWithTag("ledger_filter_delivered").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ledger_entry_ord_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ledger_entry_ord_102").assertDoesNotExist()

        composeTestRule.onNodeWithTag("earnings_dashboard_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun `security shield screen toggles rules runs phishing scanner and handles flagged orders`() {
        var configState by mutableStateOf(SecurityShieldConfig())
        var panicClicked = false
        var markedSafeId: String? = null
        var reportedSpamId: String? = null

        val flaggedOrder = StoreOrder(
            orderId = "ord_scam_9",
            customerUid = "cust_9",
            paymentMode = "COD",
            subtotalAmount = 2200.0,
            status = "PLACED"
        )
        val assessment = OrderRiskAssessment(
            orderId = "ord_scam_9",
            score = 80,
            level = OrderRiskLevel.CRITICAL,
            reasons = listOf("High-value COD (₹2,200)", "2 prior cancelled/refunded orders")
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                SecurityShieldScreen(
                    storeProfile = StoreProfile(name = "Bazar Store", approved = true),
                    securityConfig = configState,
                    securityPosture = SecurityPosture(
                        overallScore = 88,
                        runtimeTamperDetected = false,
                        flaggedOrderCount = 1,
                        criticalOrderCount = 1
                    ),
                    orders = listOf(flaggedOrder),
                    riskAssessments = mapOf("ord_scam_9" to assessment),
                    onBack = {},
                    onUpdateConfig = { transform -> configState = transform(configState) },
                    onTogglePanicSafeMode = { panicClicked = true },
                    onMarkOrderSafe = { markedSafeId = it },
                    onReportSpamOrder = { order, _ -> reportedSpamId = order.orderId }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("security_posture_score").assertTextContains("88")

        composeTestRule.onNodeWithTag("security_shield_list")
            .performScrollToNode(hasText("UPI Payout Change Lock"))
        composeTestRule.onNodeWithTag("toggle_upi_lock").performClick()
        assertTrue(configState.upiChangeLockEnabled)

        composeTestRule.onNodeWithTag("cod_limit_2500").performClick()
        assertEquals(2500.0, configState.codVerificationLimit, 0.001)

        composeTestRule.onNodeWithTag("security_shield_list")
            .performScrollToNode(hasText("Smart Phishing, Link & Scam Checker"))
        composeTestRule.onNodeWithTag("phishing_scanner_input")
            .performTextInput("Install AnyDesk and open bit.ly/kyc for refund")
        composeTestRule.onNodeWithTag("phishing_scan_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("phishing_scan_result")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("CRITICAL SCAM/SPAM", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("security_shield_list")
            .performScrollToNode(hasText("Flagged Spam & Scam Orders"))
        composeTestRule.onNodeWithTag("mark_safe_ord_scam_9").performClick()
        assertEquals("ord_scam_9", markedSafeId)

        composeTestRule.onNodeWithTag("report_spam_ord_scam_9").performClick()
        assertEquals("ord_scam_9", reportedSpamId)

        composeTestRule.onNodeWithTag("security_shield_list")
            .performScrollToNode(hasText("Emergency Panic Lock"))
        composeTestRule.onNodeWithTag("panic_lock_button").performClick()
        assertTrue(panicClicked)
    }

    @Test
    fun `orders tab displays risk shield banner and confirms critical order acceptance`() {
        var acceptedOrderId: String? = null
        var reportedOrderId: String? = null

        val criticalOrder = StoreOrder(
            orderId = "-OrdCrit77",
            status = "PLACED",
            paymentMode = "COD",
            phone = "9876501234",
            address = "Main Bazar",
            subtotalAmount = 2100.0,
            items = listOf(OrderItem(name = "Rice 25kg", qty = 1, price = 2100.0))
        )
        val riskMap = mapOf(
            "-OrdCrit77" to OrderRiskAssessment(
                orderId = "-OrdCrit77",
                score = 75,
                level = OrderRiskLevel.CRITICAL,
                reasons = listOf("High-value COD (₹2,100)", "Possible duplicate order within 5 mins"),
                isDuplicateSuspect = true
            )
        )

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                OrdersTabScreen(
                    activeOrders = listOf(criticalOrder),
                    storeProfile = StoreProfile(name = "Test Store"),
                    deliveryRiders = emptyMap(),
                    customers = emptyMap(),
                    orderRiskAssessments = riskMap,
                    onUpdateStatus = { order, _ -> acceptedOrderId = order.orderId },
                    onRejectOrder = { _, _ -> },
                    onReportSpam = { order, _ -> reportedOrderId = order.orderId },
                    onMarkOrderSafe = {},
                    onToast = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_risk_banner_-OrdCrit77").assertIsDisplayed()
        composeTestRule.onNodeWithText("DUPLICATE?").assertIsDisplayed()

        // Tapping Accept Order on CRITICAL risk triggers confirmation dialog first
        composeTestRule.onNodeWithText("Accept Order").performClick()
        composeTestRule.waitForIdle()
        assertEquals(null, acceptedOrderId)
        composeTestRule.onNodeWithTag("confirm_high_risk_accept_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_high_risk_accept_button").performClick()
        assertEquals("-OrdCrit77", acceptedOrderId)

        composeTestRule.onNodeWithTag("order_report_spam_-OrdCrit77").performClick()
        assertEquals("-OrdCrit77", reportedOrderId)
    }

    @Test
    fun `auth screen displays direct google login forgot password dialog and email verification gate`() {
        var pendingVerification by mutableStateOf(false)
        var resetEmailRequested: String? = null
        var checkVerifiedClicked = false
        var resendClicked = false
        var signOutClicked = false

        composeTestRule.setContent {
            GossaigaonStoreTheme {
                AuthScreen(
                    isLoading = false,
                    authError = "",
                    pendingEmailVerification = pendingVerification,
                    verificationEmail = "kirana.owner@gossaigaonbazar.com",
                    onClearError = {},
                    onLogin = { _, _ -> },
                    onRegister = { _, _, _, _, _, _, _, _, _ -> },
                    onSendPasswordReset = { email, onDone ->
                        resetEmailRequested = email
                        onDone()
                    },
                    onResendVerification = { resendClicked = true },
                    onCheckEmailVerified = { checkVerifiedClicked = true },
                    onSignOutVerification = { signOutClicked = true },
                    onToast = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Direct Google Login is visible
        composeTestRule.onNodeWithTag("google_signin_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()

        // Forgot Password opens dialog and dispatches reset email
        composeTestRule.onNodeWithTag("forgot_password_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("forgot_password_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("forgot_password_email_input")
            .performTextInput("kirana.owner@gossaigaonbazar.com")
        composeTestRule.onNodeWithTag("send_reset_link_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("kirana.owner@gossaigaonbazar.com", resetEmailRequested)

        // Switching to Email Verification Gate shows verification UI and actions
        composeTestRule.runOnUiThread {
            pendingVerification = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("email_verification_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("verification_email_text")
            .assertTextContains("kirana.owner@gossaigaonbazar.com")

        composeTestRule.onNodeWithTag("check_email_verified_button")
            .performScrollTo()
            .performClick()
        assertTrue(checkVerifiedClicked)

        composeTestRule.onNodeWithTag("resend_verification_button")
            .performScrollTo()
            .performClick()
        assertTrue(resendClicked)

        composeTestRule.onNodeWithTag("verification_sign_out_button")
            .performScrollTo()
            .performClick()
        assertTrue(signOutClicked)
    }
}
