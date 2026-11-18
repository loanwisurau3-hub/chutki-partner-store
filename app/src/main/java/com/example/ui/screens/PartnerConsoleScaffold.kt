package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.model.PartnerTab
import com.example.model.StoreOrder
import com.example.model.ToastKind
import com.example.ui.StoreUiState
import com.example.ui.StoreViewModel
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.components.ActiveRiderCallBarAndModal
import com.example.ui.components.IncomingRiderCallPopup
import com.example.ui.components.RiderOrderChatSheet

@Composable
fun PartnerConsoleScaffold(
    state: StoreUiState,
    viewModel: StoreViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (state.selectedTab != PartnerTab.ORDERS) {
            viewModel.selectTab(PartnerTab.ORDERS)
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                viewModel.showToast("Press BACK again to exit", ToastKind.INFO)
            }
        }
    }

    val pendingInventoryCount = remember(state.products) {
        state.products.count { !it.adminApproved || it.inventoryFlagged }
    }

    val navItems = remember(state.activeOrders.size) {
        listOf(
            Triple(PartnerTab.ORDERS, "Orders", Icons.Default.Inventory2),
            Triple(PartnerTab.MENU, "Products", Icons.Default.ShoppingBasket),
            Triple(PartnerTab.INVENTORY, "Inventory", Icons.AutoMirrored.Filled.FactCheck),
            Triple(PartnerTab.REPORTS, "Wallet", Icons.Default.AccountBalanceWallet),
            Triple(PartnerTab.PROFILE, "Store", Icons.Default.Settings)
        )
    }

    if (state.showAdminCommissionHub) {
        AdminCommissionScreen(
            currentStoreId = state.uid,
            currentStoreName = state.storeProfile.name,
            commissionRules = state.commissionRules,
            products = state.products,
            availableCategories = state.adminSettings.customCategories,
            allStores = state.allStores,
            storeReports = state.storeReports,
            ledgerEntries = state.allLedgerEntries.values.sortedByDescending { it.createdAt },
            onBack = { viewModel.closeAdminCommissionHub() },
            onSaveGlobal = { pct, enabled, allowLoss -> viewModel.saveGlobalCommissionSettings(pct, enabled, allowLoss) },
            onSaveStoreRule = { storeId, storeName, pct, enabled -> viewModel.saveStoreCommissionRule(storeId, storeName, pct, enabled) },
            onDeleteStoreRule = { storeId -> viewModel.deleteStoreCommissionRule(storeId) },
            onSaveCategoryRule = { catId, catName, pct, enabled -> viewModel.saveCategoryCommissionRule(catId, catName, pct, enabled) },
            onDeleteCategoryRule = { catId -> viewModel.deleteCategoryCommissionRule(catId) },
            onSaveProductRule = { prodId, storeId, prodName, pct, enabled -> viewModel.saveProductCommissionRule(prodId, storeId, prodName, pct, enabled) },
            onDeleteProductRule = { prodId -> viewModel.deleteProductCommissionRule(prodId) }
        )
        return
    }

    var reportsSubTab by remember { mutableIntStateOf(0) }

    if (state.showEarningsDashboard) {
        EarningsDashboardScreen(
            storeProfile = state.storeProfile,
            adminSettings = state.adminSettings,
            orderLedgerEntries = state.orderLedgerEntries,
            payouts = state.payouts,
            onRequestPayout = { viewModel.requestPayout() },
            onOpenWalletPayouts = {
                viewModel.closeEarningsDashboard()
                reportsSubTab = 1
                viewModel.selectTab(PartnerTab.REPORTS)
            },
            onBack = { viewModel.closeEarningsDashboard() }
        )
        return
    }

    if (state.showSecurityShield) {
        SecurityShieldScreen(
            storeProfile = state.storeProfile,
            securityConfig = state.securityConfig,
            securityPosture = state.securityPosture,
            orders = state.allOrders,
            riskAssessments = state.orderRiskAssessments,
            onBack = { viewModel.closeSecurityShield() },
            onUpdateConfig = { transform -> viewModel.updateSecurityConfig(transform) },
            onTogglePanicSafeMode = { viewModel.togglePanicSafeMode() },
            onMarkOrderSafe = { orderId -> viewModel.markOrderVerifiedSafe(orderId) },
            onReportSpamOrder = { order, assessment -> viewModel.reportAndRejectSpamOrder(order, assessment) }
        )
        return
    }

    DisposableEffect(context) {
        val rtcEngine = com.example.util.AndroidWebRtcVoiceEngine(context)
        viewModel.attachWebRtcEngine(rtcEngine)
        onDispose {
            rtcEngine.releaseSession()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = BgDark,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                PartnerTopBarAndAlerts(
                    state = state,
                    onRingingClick = { viewModel.selectTab(PartnerTab.ORDERS) },
                    onToggleDuty = { viewModel.toggleStoreDuty() },
                    onToggleSound = { viewModel.toggleSound() },
                    onOpenEarningsDashboard = { viewModel.openEarningsDashboard() },
                    onOpenSecurityShield = { viewModel.openSecurityShield() },
                    onOpenAdminCommission = { viewModel.openAdminCommissionHub() },
                    onLogout = {
                        scope.launch {
                            withContext(NonCancellable) {
                                runCatching {
                                    CredentialManager.create(context)
                                        .clearCredentialState(ClearCredentialStateRequest())
                                }
                            }
                            viewModel.logout()
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(color = BorderSlate)
                        NavigationBar(
                            containerColor = BgSurface,
                            tonalElevation = 0.dp
                        ) {
                            navItems.forEach { (tab, label, icon) ->
                                val selected = state.selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        if (tab == PartnerTab.ORDERS && state.activeOrders.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = EmeraldPrimary, contentColor = Color.White) {
                                                        Text(state.activeOrders.size.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = label)
                                            }
                                        } else if (tab == PartnerTab.INVENTORY && pendingInventoryCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = SunnyYellow, contentColor = TextMain) {
                                                        Text(pendingInventoryCount.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = label)
                                            }
                                        } else {
                                            Icon(icon, contentDescription = label)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EmeraldPrimary,
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = EmeraldSoft,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = BgSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { (tab, label, icon) ->
                                val selected = state.selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        if (tab == PartnerTab.ORDERS && state.activeOrders.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = EmeraldPrimary, contentColor = Color.White) {
                                                        Text(state.activeOrders.size.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = label)
                                            }
                                        } else if (tab == PartnerTab.INVENTORY && pendingInventoryCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = SunnyYellow, contentColor = TextMain) {
                                                        Text(pendingInventoryCount.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = label)
                                            }
                                        } else {
                                            Icon(icon, contentDescription = label)
                                        }
                                    },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = EmeraldPrimary,
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = EmeraldSoft,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (state.selectedTab) {
                            PartnerTab.ORDERS -> OrdersTabScreen(
                                activeOrders = state.activeOrders,
                                storeProfile = state.storeProfile,
                                deliveryRiders = state.deliveryRiders,
                                customers = state.customers,
                                orderRiskAssessments = state.orderRiskAssessments,
                                unreadChatCountByOrder = state.unreadChatCountByOrder,
                                onUpdateStatus = { order: StoreOrder, status: String -> viewModel.setOrderStatus(order, status) },
                                onRejectOrder = { orderId, reason -> viewModel.rejectOrder(orderId, reason) },
                                onReportSpam = { order, assessment -> viewModel.reportAndRejectSpamOrder(order, assessment) },
                                onMarkOrderSafe = { orderId -> viewModel.markOrderVerifiedSafe(orderId) },
                                onStartRiderCall = { order, rider -> viewModel.startRiderVoiceCall(order, rider) },
                                onOpenRiderChat = { order -> viewModel.openOrderChat(order.orderId) },
                                onToast = { msg, kind -> viewModel.showToast(msg, kind) }
                            )

                            PartnerTab.MENU -> ProductsTabScreen(
                                storeId = state.uid,
                                products = state.products,
                                searchQuery = state.productSearchQuery,
                                adminSettings = state.adminSettings,
                                commissionRules = state.commissionRules,
                                salesStats = state.productSalesStats,
                                onSearchChange = { viewModel.updateProductSearch(it) },
                                onToggleStock = { viewModel.toggleProductStock(it) },
                                onSaveProduct = { id, name, cost, sell, commPct, ruleSource, mrp, cat, onSale, saleTag, kw, desc, img, avail, onDone ->
                                    viewModel.saveProduct(id, name, cost, sell, commPct, ruleSource, mrp, cat, onSale, saleTag, kw, desc, img, avail, onDone)
                                },
                                onDeleteProduct = { viewModel.deleteProduct(it) },
                                onQuickSeed = { viewModel.quickSeedProducts() },
                                onToast = { msg, kind -> viewModel.showToast(msg, kind) }
                            )

                            PartnerTab.INVENTORY -> InventoryScreen(
                                products = state.products,
                                onToggleApprove = { viewModel.toggleApproveInventory(it) },
                                onToggleFlag = { item, reason -> viewModel.toggleFlagInventory(item, reason) },
                                onUpdateStockQty = { item, newQty -> viewModel.updateStockQty(item, newQty) },
                                onToggleStock = { viewModel.toggleProductStock(it) }
                            )

                            PartnerTab.REPORTS -> WalletTabScreen(
                                storeProfile = state.storeProfile,
                                adminSettings = state.adminSettings,
                                deliveredCount = state.deliveredCount,
                                grossRevenue = state.grossRevenue,
                                totalChutkiCommission = state.totalChutkiCommission,
                                netStoreShare = state.netStoreShare,
                                totalEstimatedProfit = state.totalEstimatedProfit,
                                totalSettled = state.totalSettled,
                                pendingPayoutBalance = state.pendingPayoutBalance,
                                payouts = state.payouts,
                                allOrders = state.allOrders,
                                orderLedgerEntries = state.orderLedgerEntries,
                                onRequestPayout = { viewModel.requestPayout() },
                                onToast = { msg, kind -> viewModel.showToast(msg, kind) }
                            )

                            PartnerTab.PROFILE -> ProfileTabScreen(
                                storeProfile = state.storeProfile,
                                adminSettings = state.adminSettings,
                                commissionRules = state.commissionRules,
                                feedbacks = state.feedbacks,
                                securityPosture = state.securityPosture,
                                securityConfig = state.securityConfig,
                                onOpenSecurityShield = { viewModel.openSecurityShield() },
                                onOpenAdminCommission = { viewModel.openAdminCommissionHub() },
                                onSaveProfile = { name, upi, cuisine, phone, discPct, discMin, addr, hrs, img, lat, lng ->
                                    viewModel.saveStoreProfile(name, upi, cuisine, phone, discPct, discMin, addr, hrs, img, lat, lng)
                                },
                                onSubmitFeedback = { subj, msg, onDone -> viewModel.submitFeedback(subj, msg, onDone) },
                                onToast = { msg, kind -> viewModel.showToast(msg, kind) }
                            )
                        }
                    }
                }
            }
        }

        IncomingRiderCallPopup(
            callState = state.callUiState,
            onAnswer = { viewModel.answerIncomingRiderCall() },
            onDecline = { viewModel.declineIncomingRiderCall() }
        )

        if (state.callUiState.isActiveOrRinging && !state.callUiState.isIncoming) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                ActiveRiderCallBarAndModal(
                    callState = state.callUiState,
                    onMuteToggle = { viewModel.toggleCallMute() },
                    onHoldToggle = { viewModel.toggleCallHold() },
                    onSpeakerToggle = { enabled -> viewModel.toggleCallSpeakerphone(enabled) },
                    onEndCall = { viewModel.endActiveRiderCall() }
                )
            }
        }

        if (state.activeChatOrderId != null) {
            val activeOrder = state.allOrders.firstOrNull { it.orderId == state.activeChatOrderId }
            if (activeOrder != null) {
                RiderOrderChatSheet(
                    order = activeOrder,
                    rider = state.deliveryRiders[activeOrder.deliveryUid],
                    messages = state.chatMessagesByOrder[activeOrder.orderId] ?: emptyList(),
                    failedMessages = state.failedChatMessagesByOrder[activeOrder.orderId] ?: emptyList(),
                    onClose = { viewModel.closeOrderChat() },
                    onSendMessage = { text -> viewModel.sendOrderChatMessage(activeOrder.orderId, text) },
                    onRetryMessage = { msg -> viewModel.retryFailedChatMessage(activeOrder.orderId, msg) },
                    onCallRider = {
                        viewModel.closeOrderChat()
                        viewModel.startRiderVoiceCall(activeOrder, state.deliveryRiders[activeOrder.deliveryUid])
                    }
                )
            }
        }
    }
}
