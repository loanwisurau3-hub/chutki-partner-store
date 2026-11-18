package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StoreDataSource
import com.example.data.StoreRepository
import com.example.model.AdminSettings
import com.example.model.CallUiSessionState
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.CustomerRecord
import com.example.model.DeliveryRider
import com.example.model.InAppCallRecord
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.OrderRiskAssessment
import com.example.model.OrderRiskLevel
import com.example.model.PartnerTab
import com.example.model.ProductItem
import com.example.model.ProductSalesStat
import com.example.model.SecurityPosture
import com.example.model.SecurityShieldConfig
import com.example.model.StoreFeedback
import com.example.model.StoreOrder
import com.example.model.StorePayout
import com.example.model.StoreProfile
import com.example.model.StoreSummaryReport
import com.example.model.ToastKind
import com.example.model.ToastMessage
import com.example.model.TripChatMessage
import com.example.model.WebRtcIceCandidateData
import com.example.model.WebRtcSessionDescriptionData
import com.example.util.DefaultWebRtcPeerEngine
import com.example.util.EarningsCalculator
import com.example.util.RiderCommPolicy
import com.example.util.SecurityGuard
import com.example.util.StoreUtils
import com.example.util.WebRtcPeerCallbacks
import com.example.util.WebRtcPeerEngine
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class StoreUiState(
    val isAuthenticated: Boolean = false,
    val uid: String = "",
    val authEmail: String = "",
    val authLoading: Boolean = false,
    val authError: String = "",
    val authSuccessMessage: String = "",
    val pendingEmailVerification: Boolean = false,
    val verificationEmail: String = "",
    val resendVerificationCooldownSeconds: Int = 0,
    val selectedTab: PartnerTab = PartnerTab.ORDERS,
    val showAdminCommissionHub: Boolean = false,
    val showEarningsDashboard: Boolean = false,
    val showSecurityShield: Boolean = false,
    val authLockoutSeconds: Int = 0,
    val soundEnabled: Boolean = true,
    val productSearchQuery: String = "",
    val securityConfig: SecurityShieldConfig = SecurityShieldConfig(),
    val orderRiskAssessments: Map<String, OrderRiskAssessment> = emptyMap(),
    val securityPosture: SecurityPosture = SecurityPosture(),
    val safeMarkedOrderIds: Set<String> = emptySet(),
    val storeProfile: StoreProfile = StoreProfile(),
    val allOrders: List<StoreOrder> = emptyList(),
    val activeOrders: List<StoreOrder> = emptyList(),
    val ringingOrderCount: Int = 0,
    val products: List<ProductItem> = emptyList(),
    val deliveryRiders: Map<String, DeliveryRider> = emptyMap(),
    val customers: Map<String, CustomerRecord> = emptyMap(),
    val adminSettings: AdminSettings = AdminSettings(),
    val commissionRules: CommissionRules = CommissionRules(),
    val orderLedgerEntries: Map<String, OrderEarningsLedgerEntry> = emptyMap(),
    val allStores: Map<String, StoreProfile> = emptyMap(),
    val allAdminOrders: List<StoreOrder> = emptyList(),
    val allLedgerEntries: Map<String, OrderEarningsLedgerEntry> = emptyMap(),
    val storeReports: List<StoreSummaryReport> = emptyList(),
    val payouts: List<StorePayout> = emptyList(),
    val feedbacks: List<StoreFeedback> = emptyList(),
    val productSalesStats: Map<String, ProductSalesStat> = emptyMap(),
    val deliveredCount: Int = 0,
    val grossRevenue: Double = 0.0,
    val totalChutkiCommission: Double = 0.0,
    val netStoreShare: Double = 0.0,
    val totalEstimatedProfit: Double = 0.0,
    val totalSettled: Double = 0.0,
    val pendingPayoutBalance: Double = 0.0,
    val callUiState: CallUiSessionState = CallUiSessionState(),
    val activeChatOrderId: String? = null,
    val chatMessagesByOrder: Map<String, List<TripChatMessage>> = emptyMap(),
    val unreadChatCountByOrder: Map<String, Int> = emptyMap(),
    val failedChatMessagesByOrder: Map<String, List<TripChatMessage>> = emptyMap(),
    val selectedOrderDetailsId: String? = null,
    val activeToast: ToastMessage? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    private val repo: StoreDataSource,
    private val playKotChime: () -> Unit,
    private val enablePeriodicRinging: Boolean,
    initialWebRtcEngine: WebRtcPeerEngine = DefaultWebRtcPeerEngine()
) : ViewModel() {

    constructor(
        repo: StoreDataSource,
        playKotChime: () -> Unit,
        enablePeriodicRinging: Boolean
    ) : this(
        repo = repo,
        playKotChime = playKotChime,
        enablePeriodicRinging = enablePeriodicRinging,
        initialWebRtcEngine = DefaultWebRtcPeerEngine()
    )

    constructor() : this(
        repo = StoreRepository(),
        playKotChime = { StoreUtils.playOrderChimePulse() },
        enablePeriodicRinging = true,
        initialWebRtcEngine = DefaultWebRtcPeerEngine()
    )

    @Volatile
    private var webRtcEngine: WebRtcPeerEngine = initialWebRtcEngine

    private val _selectedTab = MutableStateFlow(PartnerTab.ORDERS)
    private val _showAdminCommissionHub = MutableStateFlow(false)
    private val _showEarningsDashboard = MutableStateFlow(false)
    private val _showSecurityShield = MutableStateFlow(false)
    private val _soundEnabled = MutableStateFlow(true)
    private val _productSearch = MutableStateFlow("")
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow("")
    private val _authSuccessMessage = MutableStateFlow("")
    private val _resendCooldownSeconds = MutableStateFlow(0)
    private val _authLockoutSeconds = MutableStateFlow(0)
    private val _securityConfig = MutableStateFlow(SecurityShieldConfig())
    private val _safeMarkedOrderIds = MutableStateFlow<Set<String>>(emptySet())
    private val _runtimeTamperState = MutableStateFlow(false to emptyList<String>())
    private val _callUiState = MutableStateFlow(CallUiSessionState())
    private val _activeChatOrderId = MutableStateFlow<String?>(null)
    private val _chatMessagesByOrder = MutableStateFlow<Map<String, List<TripChatMessage>>>(emptyMap())
    private val _failedChatMessagesByOrder = MutableStateFlow<Map<String, List<TripChatMessage>>>(emptyMap())
    private val _selectedOrderDetailsId = MutableStateFlow<String?>(null)
    private val _toast = MutableStateFlow<ToastMessage?>(null)

    private val alertedKotOrderIds = mutableSetOf<String>()
    private val handledIncomingCallIds = mutableSetOf<String>()
    private val handledRemoteCandidateKeys = mutableSetOf<String>()
    private var failedLoginAttempts = 0

    private var activeOrderCallListenerJob: Job? = null
    private var activeWebRtcOfferListenerJob: Job? = null
    private var activeWebRtcAnswerListenerJob: Job? = null
    private var activeWebRtcCandidatesListenerJob: Job? = null
    private var callTimeoutJob: Job? = null
    private var callDurationTickerJob: Job? = null
    private val orderChatListenerJobs = mutableMapOf<String, Job>()

    private val verifiedUidFlow = repo.authSessionFlow
        .map { session -> if (session.isFullyAuthenticated) session.uid else null }
        .distinctUntilChanged()

    private data class StoreRealtimeBundle(
        val uid: String,
        val profile: StoreProfile,
        val orders: List<StoreOrder>,
        val products: List<ProductItem>,
        val riders: Map<String, DeliveryRider>,
        val customers: Map<String, CustomerRecord>,
        val adminSettings: AdminSettings,
        val commissionRules: CommissionRules,
        val storeLedger: Map<String, OrderEarningsLedgerEntry>,
        val payouts: List<StorePayout>,
        val feedbacks: List<StoreFeedback>,
        val allStores: Map<String, StoreProfile>,
        val allOrders: List<StoreOrder>,
        val allLedger: Map<String, OrderEarningsLedgerEntry>
    )

    private val storeBundleFlow = verifiedUidFlow.flatMapLatest { uid ->
        if (uid == null) {
            alertedKotOrderIds.clear()
            flowOf(null)
        } else {
            val coreLeft = combine(
                repo.observeStoreProfile(uid),
                repo.observeStoreOrders(uid),
                repo.observeStoreProducts(uid),
                repo.observeDeliveryRiders(),
                repo.observeCustomers()
            ) { profile, orders, products, riders, customers ->
                listOf(profile, orders, products, riders, customers)
            }
            val coreRight = combine(
                repo.observeAdminSettings(),
                repo.observeCommissionRules(),
                repo.observeStoreEarningsLedger(uid),
                repo.observeStorePayouts(uid),
                repo.observeStoreFeedbacks(uid)
            ) { admin, commRules, storeLedger, payouts, feedbacks ->
                listOf(admin, commRules, storeLedger, payouts, feedbacks)
            }
            val adminOverlay = combine(
                repo.observeAllStores(),
                repo.observeAllOrders(),
                repo.observeAllEarningsLedger()
            ) { stores, allOrders, allLedger ->
                Triple(stores, allOrders, allLedger)
            }

            combine(coreLeft, coreRight, adminOverlay) { left, right, adminData ->
                @Suppress("UNCHECKED_CAST")
                StoreRealtimeBundle(
                    uid = uid,
                    profile = left[0] as StoreProfile,
                    orders = left[1] as List<StoreOrder>,
                    products = left[2] as List<ProductItem>,
                    riders = left[3] as Map<String, DeliveryRider>,
                    customers = left[4] as Map<String, CustomerRecord>,
                    adminSettings = right[0] as AdminSettings,
                    commissionRules = right[1] as CommissionRules,
                    storeLedger = right[2] as Map<String, OrderEarningsLedgerEntry>,
                    payouts = right[3] as List<StorePayout>,
                    feedbacks = right[4] as List<StoreFeedback>,
                    allStores = adminData.first,
                    allOrders = adminData.second,
                    allLedger = adminData.third
                )
            }
        }
    }

    private data class CommUiBundle(
        val callUiState: CallUiSessionState,
        val activeChatOrderId: String?,
        val chatMessagesByOrder: Map<String, List<TripChatMessage>>,
        val failedChatMessagesByOrder: Map<String, List<TripChatMessage>>,
        val selectedOrderDetailsId: String?
    )

    private val commUiFlow = combine(
        _callUiState,
        _activeChatOrderId,
        _chatMessagesByOrder,
        _failedChatMessagesByOrder,
        _selectedOrderDetailsId
    ) { callState, activeChatId, chats, failedChats, detailsId ->
        CommUiBundle(
            callUiState = callState,
            activeChatOrderId = activeChatId,
            chatMessagesByOrder = chats,
            failedChatMessagesByOrder = failedChats,
            selectedOrderDetailsId = detailsId
        )
    }

    private data class UiControlBundle(
        val tab: PartnerTab,
        val showAdminHub: Boolean,
        val showEarningsDashboard: Boolean,
        val showSecurityShield: Boolean,
        val sound: Boolean,
        val search: String,
        val loading: Boolean,
        val err: String,
        val successMsg: String,
        val resendCooldownSeconds: Int,
        val authLockoutSeconds: Int,
        val securityConfig: SecurityShieldConfig,
        val safeMarkedOrderIds: Set<String>,
        val runtimeTamper: Pair<Boolean, List<String>>,
        val comm: CommUiBundle
    )

    private val uiControlFlow = combine(
        combine(_selectedTab, _showAdminCommissionHub, _showEarningsDashboard, _showSecurityShield, _soundEnabled) { tab, showHub, showEarnings, showShield, sound ->
            listOf(tab, showHub, showEarnings, showShield, sound)
        },
        combine(_productSearch, _authLoading, _authError, _authSuccessMessage, _resendCooldownSeconds) { search, loading, err, successMsg, resendCd ->
            listOf(search, loading, err, successMsg, resendCd)
        },
        combine(_authLockoutSeconds, _securityConfig, _safeMarkedOrderIds, _runtimeTamperState) { lockout, cfg, safeIds, tamper ->
            listOf(lockout, cfg, safeIds, tamper)
        },
        commUiFlow
    ) { left, right, sec, comm ->
        @Suppress("UNCHECKED_CAST")
        UiControlBundle(
            tab = left[0] as PartnerTab,
            showAdminHub = left[1] as Boolean,
            showEarningsDashboard = left[2] as Boolean,
            showSecurityShield = left[3] as Boolean,
            sound = left[4] as Boolean,
            search = right[0] as String,
            loading = right[1] as Boolean,
            err = right[2] as String,
            successMsg = right[3] as String,
            resendCooldownSeconds = right[4] as Int,
            authLockoutSeconds = sec[0] as Int,
            securityConfig = sec[1] as SecurityShieldConfig,
            safeMarkedOrderIds = sec[2] as Set<String>,
            runtimeTamper = sec[3] as Pair<Boolean, List<String>>,
            comm = comm
        )
    }

    val uiState: StateFlow<StoreUiState> = combine(
        storeBundleFlow,
        repo.authSessionFlow,
        uiControlFlow,
        _toast
    ) { bundle, authSession, controls, toast ->
        val tab = controls.tab
        val showAdminHub = controls.showAdminHub
        val showEarningsDash = controls.showEarningsDashboard
        val showShield = controls.showSecurityShield
        val sound = controls.sound
        val search = controls.search
        val loading = controls.loading
        val err = controls.err

        if (bundle == null || !authSession.isFullyAuthenticated) {
            StoreUiState(
                isAuthenticated = false,
                authLoading = loading,
                authError = err,
                authSuccessMessage = controls.successMsg,
                pendingEmailVerification = authSession.requiresEmailVerification,
                verificationEmail = authSession.email,
                resendVerificationCooldownSeconds = controls.resendCooldownSeconds,
                authLockoutSeconds = controls.authLockoutSeconds,
                selectedTab = tab,
                showAdminCommissionHub = false,
                showEarningsDashboard = false,
                showSecurityShield = false,
                soundEnabled = sound,
                securityConfig = controls.securityConfig,
                activeToast = toast
            )
        } else {
            val effectiveRules = EarningsCalculator.effectiveRules(bundle.commissionRules, bundle.adminSettings)
            val productIndex = EarningsCalculator.buildProductIndex(bundle.products)

            val activeOrders = bundle.orders.filter {
                it.status != "DELIVERED" && it.status != "CANCELLED" && it.status != "REFUNDED"
            }
            val ringingCount = bundle.orders.count { o ->
                o.status == "PLACED" && !(o.paymentMode == "UPI" && o.paymentStatus != "VERIFIED")
            }
            val riskAssessments = bundle.orders.associate { order ->
                order.orderId to SecurityGuard.evaluateOrderRisk(
                    order = order,
                    allOrders = bundle.orders,
                    customers = bundle.customers,
                    config = controls.securityConfig,
                    safeMarkedOrderIds = controls.safeMarkedOrderIds
                )
            }
            val posture = SecurityGuard.evaluateMerchantPosture(
                profile = bundle.profile,
                config = controls.securityConfig,
                assessments = riskAssessments,
                activeOrders = activeOrders,
                cachedRuntimeTamper = controls.runtimeTamper
            )

            // Build deterministic store order ledger preserving historical entries and reversing cancelled/refunded
            val effectiveStoreLedger = buildMap<String, OrderEarningsLedgerEntry> {
                bundle.orders.forEach { order ->
                    val existing = bundle.storeLedger[order.orderId]
                    put(
                        order.orderId,
                        EarningsCalculator.buildOrUpdateOrderLedger(
                            order = order,
                            productsByIdOrName = productIndex,
                            rules = effectiveRules,
                            existingEntry = existing,
                            storeName = bundle.profile.name
                        )
                    )
                }
                bundle.storeLedger.forEach { (id, entry) ->
                    if (!containsKey(id)) put(id, entry)
                }
            }

            // Build global ledger view for Admin Commission & Earnings Reports
            val effectiveAllLedger = buildMap<String, OrderEarningsLedgerEntry> {
                bundle.allOrders.forEach { order ->
                    val existing = bundle.allLedger[order.orderId]
                    val storeNm = bundle.allStores[order.restaurantId]?.name.orEmpty()
                    put(
                        order.orderId,
                        EarningsCalculator.buildOrUpdateOrderLedger(
                            order = order,
                            productsByIdOrName = if (order.restaurantId == bundle.uid) productIndex else emptyMap(),
                            rules = effectiveRules,
                            existingEntry = existing,
                            storeName = storeNm
                        )
                    )
                }
                bundle.allLedger.forEach { (id, entry) ->
                    if (!containsKey(id)) put(id, entry)
                }
            }

            val salesStats = mutableMapOf<String, ProductSalesStat>()
            var deliveredCount = 0
            var grossRevenue = 0.0
            var totalChutkiCommission = 0.0
            var netShare = 0.0
            var totalProfit = 0.0

            bundle.orders.forEach { o ->
                if (o.status == "DELIVERED" && !EarningsCalculator.isOrderReversed(o)) {
                    deliveredCount++
                    val ledger = effectiveStoreLedger[o.orderId]
                    grossRevenue += ledger?.grossAmount ?: o.itemsValue
                    totalChutkiCommission += ledger?.commissionAmount ?: 0.0
                    netShare += ledger?.storeEarning ?: o.itemsValue
                    totalProfit += ledger?.estimatedProfit ?: 0.0
                    o.items.forEach { item ->
                        val key = item.itemId.ifBlank { item.name }
                        val prev = salesStats[key] ?: ProductSalesStat()
                        salesStats[key] = ProductSalesStat(
                            qty = prev.qty + item.qty,
                            revenue = prev.revenue + (item.price * item.qty)
                        )
                    }
                }
            }

            grossRevenue = EarningsCalculator.round2(grossRevenue)
            totalChutkiCommission = EarningsCalculator.round2(totalChutkiCommission)
            netShare = EarningsCalculator.round2(netShare)
            totalProfit = EarningsCalculator.round2(totalProfit)

            val settled = EarningsCalculator.round2(
                bundle.payouts.filter { it.status == "PAID" }.sumOf { it.amount }
            )
            val pendingBalance = EarningsCalculator.round2(max(0.0, netShare - settled))
            val storeReports = EarningsCalculator.buildStoreReports(
                ledgerEntries = effectiveAllLedger.values.toList(),
                storesById = bundle.allStores
            )

            val unreadByOrder = controls.comm.chatMessagesByOrder.mapValues { (orderId, msgs) ->
                if (controls.comm.activeChatOrderId == orderId) {
                    0
                } else {
                    msgs.count { msg ->
                        msg.senderRole.equals("RIDER", ignoreCase = true) &&
                            !msg.status.equals("READ", ignoreCase = true)
                    }
                }
            }

            StoreUiState(
                isAuthenticated = true,
                uid = bundle.uid,
                authEmail = authSession.email,
                authLoading = loading,
                authError = err,
                authSuccessMessage = controls.successMsg,
                pendingEmailVerification = false,
                verificationEmail = authSession.email,
                resendVerificationCooldownSeconds = controls.resendCooldownSeconds,
                selectedTab = tab,
                showAdminCommissionHub = showAdminHub,
                showEarningsDashboard = showEarningsDash,
                showSecurityShield = showShield,
                authLockoutSeconds = controls.authLockoutSeconds,
                soundEnabled = sound,
                productSearchQuery = search,
                securityConfig = controls.securityConfig,
                orderRiskAssessments = riskAssessments,
                securityPosture = posture,
                safeMarkedOrderIds = controls.safeMarkedOrderIds,
                storeProfile = bundle.profile,
                allOrders = bundle.orders,
                activeOrders = activeOrders,
                ringingOrderCount = ringingCount,
                products = bundle.products,
                deliveryRiders = bundle.riders,
                customers = bundle.customers,
                adminSettings = bundle.adminSettings,
                commissionRules = effectiveRules,
                orderLedgerEntries = effectiveStoreLedger,
                allStores = bundle.allStores,
                allAdminOrders = bundle.allOrders,
                allLedgerEntries = effectiveAllLedger,
                storeReports = storeReports,
                payouts = bundle.payouts,
                feedbacks = bundle.feedbacks,
                productSalesStats = salesStats,
                deliveredCount = deliveredCount,
                grossRevenue = grossRevenue,
                totalChutkiCommission = totalChutkiCommission,
                netStoreShare = netShare,
                totalEstimatedProfit = totalProfit,
                totalSettled = settled,
                pendingPayoutBalance = pendingBalance,
                callUiState = controls.comm.callUiState,
                activeChatOrderId = controls.comm.activeChatOrderId,
                chatMessagesByOrder = controls.comm.chatMessagesByOrder,
                unreadChatCountByOrder = unreadByOrder,
                failedChatMessagesByOrder = controls.comm.failedChatMessagesByOrder,
                selectedOrderDetailsId = controls.comm.selectedOrderDetailsId,
                activeToast = toast
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StoreUiState()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _runtimeTamperState.value = SecurityGuard.inspectRuntimeTamper()
        }

        // Real-time Firebase RTDB ChildEventListener for incoming KOT orders:
        // fires an immediate sound chime + visual KOT toast notification on arrival.
        viewModelScope.launch {
            verifiedUidFlow.flatMapLatest { uid ->
                if (uid == null) emptyFlow() else repo.observeIncomingKotOrders(uid)
            }.collect { incomingOrder ->
                onIncomingKotOrder(incomingOrder)
            }
        }

        // Real-time Rider -> Restaurant incoming call inbox listener (`restaurantCallInbox/{restaurantUid}`)
        viewModelScope.launch {
            verifiedUidFlow.flatMapLatest { uid ->
                if (uid == null) emptyFlow() else repo.observeRestaurantCallInbox(uid)
            }.collect { inboxCall ->
                handleIncomingCallInboxUpdate(inboxCall)
            }
        }

        // Automatically attach real-time chat listeners for rider-assigned restaurant orders
        viewModelScope.launch {
            storeBundleFlow.collect { bundle ->
                if (bundle == null) {
                    clearAllChatListeners()
                } else {
                    syncOrderChatListeners(bundle.uid, bundle.orders)
                }
            }
        }

        if (enablePeriodicRinging) {
            viewModelScope.launch {
                while (true) {
                    val state = uiState.value
                    if (state.isAuthenticated && state.soundEnabled && state.ringingOrderCount > 0) {
                        playKotChime()
                    }
                    delay(3500L)
                }
            }
        }

        // Persist missing order ledger snapshots & reversal updates cleanly to Firebase RTDB
        viewModelScope.launch {
            storeBundleFlow.collect { bundle ->
                if (bundle != null && bundle.orders.isNotEmpty()) {
                    val effectiveRules = EarningsCalculator.effectiveRules(
                        bundle.commissionRules,
                        bundle.adminSettings
                    )
                    val productIndex = EarningsCalculator.buildProductIndex(bundle.products)
                    bundle.orders.forEach { order ->
                        val existing = bundle.storeLedger[order.orderId]
                        val reversed = EarningsCalculator.isOrderReversed(order)
                        val needsCreate = existing == null || !existing.commissionDeducted
                        val needsReversal = existing != null && existing.commissionDeducted &&
                            reversed && existing.ledgerStatus != "REVERSED"
                        if (needsCreate || needsReversal) {
                            val entry = EarningsCalculator.buildOrUpdateOrderLedger(
                                order = order,
                                productsByIdOrName = productIndex,
                                rules = effectiveRules,
                                existingEntry = existing,
                                storeName = bundle.profile.name
                            )
                            repo.syncOrderEarningsLedger(entry)
                        }
                    }
                }
            }
        }
    }

    private fun onIncomingKotOrder(order: StoreOrder) {
        if (!StoreUtils.isActionableKotOrder(order)) return
        if (!alertedKotOrderIds.add(order.orderId)) return
        val current = uiState.value
        val risk = SecurityGuard.evaluateOrderRisk(
            order = order,
            allOrders = current.allOrders,
            customers = current.customers,
            config = _securityConfig.value,
            safeMarkedOrderIds = _safeMarkedOrderIds.value
        )
        if (_soundEnabled.value) {
            playKotChime()
        }
        val alertText = if (risk.level == OrderRiskLevel.HIGH || risk.level == OrderRiskLevel.CRITICAL) {
            val topFlag = risk.reasons.firstOrNull().orEmpty()
            "SCAM/SPAM RISK #${order.orderId.takeLast(6)}: ${topFlag.ifBlank { risk.level.label }}"
        } else {
            StoreUtils.formatIncomingKotAlertText(order)
        }
        showToast(
            text = alertText,
            kind = if (risk.level == OrderRiskLevel.HIGH || risk.level == OrderRiskLevel.CRITICAL) {
                ToastKind.DANGER
            } else {
                ToastKind.WARNING
            },
            isKotAlert = true
        )
    }

    fun showToast(
        text: String,
        kind: ToastKind = ToastKind.SUCCESS,
        isKotAlert: Boolean = false
    ) {
        val msg = ToastMessage(
            id = System.currentTimeMillis(),
            text = text,
            kind = kind,
            isKotAlert = isKotAlert
        )
        _toast.value = msg
        val durationMs = if (isKotAlert) 5500L else 3600L
        viewModelScope.launch {
            delay(durationMs)
            if (_toast.value?.id == msg.id) {
                _toast.value = null
            }
        }
    }

    fun onToastTapped() {
        val current = _toast.value
        if (current?.isKotAlert == true) {
            _showAdminCommissionHub.value = false
            _showEarningsDashboard.value = false
            _showSecurityShield.value = false
            _selectedTab.value = PartnerTab.ORDERS
        }
        _toast.value = null
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun clearAuthError() {
        _authError.value = ""
        _authSuccessMessage.value = ""
    }

    fun selectTab(tab: PartnerTab) {
        _selectedTab.value = tab
    }

    fun openAdminCommissionHub() {
        _showAdminCommissionHub.value = true
    }

    fun closeAdminCommissionHub() {
        _showAdminCommissionHub.value = false
    }

    fun openEarningsDashboard() {
        _showEarningsDashboard.value = true
    }

    fun closeEarningsDashboard() {
        _showEarningsDashboard.value = false
    }

    fun openSecurityShield() {
        _showSecurityShield.value = true
    }

    fun closeSecurityShield() {
        _showSecurityShield.value = false
    }

    fun updateSecurityConfig(transform: (SecurityShieldConfig) -> SecurityShieldConfig) {
        _securityConfig.value = transform(_securityConfig.value)
    }

    fun togglePanicSafeMode() {
        val next = !_securityConfig.value.panicSafeModeActive
        _securityConfig.value = _securityConfig.value.copy(panicSafeModeActive = next)
        val state = uiState.value
        if (next && state.isAuthenticated && state.storeProfile.isOpen) {
            viewModelScope.launch {
                repo.toggleStoreOpen(state.uid, false)
            }
        }
        showToast(
            text = if (next) {
                "Panic Safe-Mode ACTIVE: Store closed & payouts locked!"
            } else {
                "Panic Safe-Mode deactivated."
            },
            kind = if (next) ToastKind.DANGER else ToastKind.SUCCESS
        )
    }

    fun markOrderVerifiedSafe(orderId: String) {
        if (orderId.isBlank()) return
        _safeMarkedOrderIds.value = _safeMarkedOrderIds.value + orderId
        viewModelScope.launch {
            repo.clearOrderSpamFlag(orderId)
            showToast("Order #${orderId.takeLast(6)} marked verified safe.", ToastKind.SUCCESS)
        }
    }

    fun reportAndRejectSpamOrder(order: StoreOrder, assessment: OrderRiskAssessment) {
        val state = uiState.value
        val reason = assessment.reasons.joinToString(" • ").ifBlank { "Suspected Scam / Spam Order" }
        viewModelScope.launch {
            repo.reportOrderSpam(
                orderId = order.orderId,
                storeId = state.uid,
                customerUid = order.customerUid,
                phone = order.phone,
                reason = reason,
                riskScore = assessment.score
            ).onSuccess {
                val cancelledOrder = order.copy(status = "CANCELLED", cancelReason = reason, isSpamSuspect = true)
                val reversedEntry = EarningsCalculator.buildOrUpdateOrderLedger(
                    order = cancelledOrder,
                    productsByIdOrName = EarningsCalculator.buildProductIndex(state.products),
                    rules = state.commissionRules,
                    existingEntry = state.orderLedgerEntries[order.orderId],
                    storeName = state.storeProfile.name
                )
                repo.syncOrderEarningsLedger(reversedEntry)
                showToast("Spam reported & order #${order.orderId.takeLast(6)} blocked!", ToastKind.DANGER)
            }.onFailure {
                showToast("Failed to report spam order.", ToastKind.DANGER)
            }
        }
    }

    fun toggleSound() {
        val next = !_soundEnabled.value
        _soundEnabled.value = next
        showToast(
            if (next) "Order sound alert active." else "Order sound muted.",
            ToastKind.INFO
        )
    }

    fun updateProductSearch(query: String) {
        _productSearch.value = query
    }

    fun login(email: String, password: String) {
        if (_authLockoutSeconds.value > 0) {
            _authError.value = "Too many failed attempts. Wait ${_authLockoutSeconds.value}s."
            return
        }
        val cleanEmail = email.trim()
        if (!SecurityGuard.isValidEmail(cleanEmail) || password.isBlank()) {
            _authError.value = "Please enter a valid email and password."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = ""
            _authSuccessMessage.value = ""
            repo.signIn(cleanEmail, password)
                .onSuccess {
                    failedLoginAttempts = 0
                    _authLockoutSeconds.value = 0
                }
                .onFailure {
                    failedLoginAttempts++
                    if (failedLoginAttempts >= 5) {
                        failedLoginAttempts = 0
                        startAuthLockout(30)
                        _authError.value = "Too many failed attempts. Login locked for 30s."
                    } else {
                        _authError.value = it.localizedMessage ?: "Login failed"
                    }
                }
            _authLoading.value = false
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (idToken.isBlank()) {
            _authError.value = "Google ID token was empty. Try again."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = ""
            _authSuccessMessage.value = ""
            repo.signInWithGoogleIdToken(idToken)
                .onSuccess {
                    failedLoginAttempts = 0
                    _authLockoutSeconds.value = 0
                    showToast("Signed in with Google!", ToastKind.SUCCESS)
                }
                .onFailure { err ->
                    _authError.value = err.localizedMessage ?: "Google Sign-In failed"
                }
            _authLoading.value = false
        }
    }

    fun reportGoogleSignInError(message: String) {
        _authLoading.value = false
        _authError.value = message
    }

    fun sendPasswordReset(email: String, onDone: () -> Unit = {}) {
        val cleanEmail = email.trim()
        if (!SecurityGuard.isValidEmail(cleanEmail)) {
            _authError.value = "Enter a valid store email address for password reset."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = ""
            _authSuccessMessage.value = ""
            repo.sendPasswordResetEmail(cleanEmail)
                .onSuccess {
                    val notice = "Password reset email sent to $cleanEmail. Check inbox & spam folder."
                    _authSuccessMessage.value = notice
                    showToast("Reset link sent to $cleanEmail", ToastKind.SUCCESS)
                    onDone()
                }
                .onFailure { err ->
                    _authError.value = err.localizedMessage ?: "Could not send password reset email."
                }
            _authLoading.value = false
        }
    }

    fun resendVerificationEmail() {
        if (_resendCooldownSeconds.value > 0) return
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = ""
            repo.sendEmailVerification()
                .onSuccess {
                    _authSuccessMessage.value = "Verification link sent! Open your email, click the link, then tap 'I've Verified — Continue'."
                    startResendCooldown(30)
                    showToast("Verification email sent!", ToastKind.SUCCESS)
                }
                .onFailure { err ->
                    _authError.value = err.localizedMessage ?: "Failed to send verification email."
                }
            _authLoading.value = false
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = ""
            repo.reloadEmailVerification()
                .onSuccess { session ->
                    if (session.isFullyAuthenticated) {
                        _authSuccessMessage.value = ""
                        showToast("Email verified! Welcome back.", ToastKind.SUCCESS)
                    } else {
                        _authError.value = "Email is still unverified. Click the link in your inbox first, then tap Continue."
                    }
                }
                .onFailure { err ->
                    _authError.value = err.localizedMessage ?: "Unable to refresh verification status."
                }
            _authLoading.value = false
        }
    }

    private fun startAuthLockout(seconds: Int) {
        viewModelScope.launch {
            for (rem in seconds downTo 1) {
                _authLockoutSeconds.value = rem
                delay(1000L)
            }
            _authLockoutSeconds.value = 0
        }
    }

    private fun startResendCooldown(seconds: Int) {
        viewModelScope.launch {
            for (rem in seconds downTo 1) {
                _resendCooldownSeconds.value = rem
                delay(1000L)
            }
            _resendCooldownSeconds.value = 0
        }
    }

    fun registerStore(
        name: String,
        email: String,
        cuisine: String,
        address: String,
        phone: String,
        upiId: String,
        password: String,
        lat: Double,
        lng: Double
    ) {
        val cleanName = StoreUtils.sanitizeText(name)
        val cleanCuisine = StoreUtils.sanitizeText(cuisine)
        val cleanAddress = StoreUtils.sanitizeText(address)
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanUpi = upiId.trim()

        if (cleanName.isBlank() || !SecurityGuard.isValidEmail(cleanEmail) || cleanCuisine.isBlank() || cleanAddress.isBlank()) {
            _authError.value = "Please complete all required store fields with a valid email."
            return
        }
        if (!StoreUtils.isValidIndianMobile(cleanPhone)) {
            _authError.value = "Enter valid 10-digit mobile number!"
            return
        }
        if (!StoreUtils.isValidUpiId(cleanUpi)) {
            _authError.value = "Enter valid UPI ID (e.g. store@okicici)!"
            return
        }
        val pwCheck = SecurityGuard.evaluatePasswordStrength(password)
        if (!pwCheck.isAcceptable) {
            _authError.value = pwCheck.message
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = ""
            _authSuccessMessage.value = ""
            repo.registerStore(
                name = cleanName,
                email = cleanEmail,
                cuisine = cleanCuisine,
                address = cleanAddress,
                phone = cleanPhone,
                upiId = cleanUpi,
                password = password,
                lat = lat,
                lng = lng
            ).onSuccess {
                _authSuccessMessage.value = "Verification link sent to $cleanEmail. Verify your inbox to launch the dashboard."
                startResendCooldown(30)
            }.onFailure {
                _authError.value = it.localizedMessage ?: "Registration failed"
            }
            _authLoading.value = false
        }
    }

    fun logout() {
        repo.signOut()
        _authError.value = ""
        _authSuccessMessage.value = ""
        _resendCooldownSeconds.value = 0
        _selectedTab.value = PartnerTab.ORDERS
        _showAdminCommissionHub.value = false
        _showEarningsDashboard.value = false
        _showSecurityShield.value = false
    }

    fun toggleStoreDuty() {
        val state = uiState.value
        if (!state.isAuthenticated) return
        if (state.storeProfile.suspended) {
            showToast("Store suspended by Admin!", ToastKind.DANGER)
            return
        }
        val newStatus = !state.storeProfile.isOpen
        if (newStatus && state.securityConfig.panicSafeModeActive) {
            showToast("Deactivate Panic Safe-Mode in Security Shield before opening store!", ToastKind.DANGER)
            return
        }
        viewModelScope.launch {
            repo.toggleStoreOpen(state.uid, newStatus).onSuccess {
                showToast(
                    if (newStatus) "Dukaan OPEN ho gayi!" else "Dukaan CLOSED mark ho gayi.",
                    if (newStatus) ToastKind.SUCCESS else ToastKind.INFO
                )
            }
        }
    }

    fun setOrderStatus(order: StoreOrder, newStatus: String) {
        val state = uiState.value
        if (state.storeProfile.suspended) {
            showToast("Store suspended by Admin!", ToastKind.DANGER)
            return
        }
        if (order.paymentMode == "UPI" && order.paymentStatus != "VERIFIED" &&
            newStatus != "CANCELLED" && newStatus != "REFUNDED"
        ) {
            showToast("UPI unverified! Dispatch blocked.", ToastKind.DANGER)
            return
        }
        val otpIfNeeded = if (newStatus == "READY_FOR_PICKUP" && order.pickupOtp.isBlank()) {
            StoreUtils.getStorePickupCode(order.pickupOtp, order.orderId)
        } else null

        viewModelScope.launch {
            repo.updateOrderStatus(order.orderId, newStatus, otpIfNeeded).onSuccess {
                val updatedOrder = order.copy(status = newStatus)
                val existingLedger = state.orderLedgerEntries[order.orderId]
                val syncedEntry = EarningsCalculator.buildOrUpdateOrderLedger(
                    order = updatedOrder,
                    productsByIdOrName = EarningsCalculator.buildProductIndex(state.products),
                    rules = state.commissionRules,
                    existingEntry = existingLedger,
                    storeName = state.storeProfile.name
                )
                repo.syncOrderEarningsLedger(syncedEntry)
                showToast("Order status: $newStatus!", ToastKind.INFO)
            }.onFailure {
                showToast("Update failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        val state = uiState.value
        val cleanReason = StoreUtils.sanitizeText(reason).ifBlank { "Item Out of Stock / Unverified UPI" }
        viewModelScope.launch {
            repo.rejectOrder(orderId, cleanReason).onSuccess {
                val matched = state.allOrders.firstOrNull { it.orderId == orderId }
                if (matched != null) {
                    val cancelledOrder = matched.copy(status = "CANCELLED", cancelReason = cleanReason)
                    val reversedEntry = EarningsCalculator.buildOrUpdateOrderLedger(
                        order = cancelledOrder,
                        productsByIdOrName = EarningsCalculator.buildProductIndex(state.products),
                        rules = state.commissionRules,
                        existingEntry = state.orderLedgerEntries[orderId],
                        storeName = state.storeProfile.name
                    )
                    repo.syncOrderEarningsLedger(reversedEntry)
                }
                showToast("Order rejected & earnings reversed.", ToastKind.DANGER)
            }.onFailure {
                showToast("Failed to reject order.", ToastKind.DANGER)
            }
        }
    }

    fun saveProduct(
        itemId: String?,
        name: String,
        purchaseCostText: String,
        sellingPriceText: String,
        commissionPercentText: String,
        commissionRuleSource: CommissionRuleSource,
        mrpText: String,
        category: String,
        isOnSale: Boolean,
        saleBadgeText: String,
        searchKeywords: String,
        description: String,
        imageUrlRaw: String,
        available: Boolean,
        onDone: () -> Unit
    ) {
        val state = uiState.value
        if (!state.adminSettings.allowStoreProductEdit) {
            showToast("Catalog editing locked by Admin!", ToastKind.DANGER)
            return
        }
        val cleanName = StoreUtils.sanitizeText(name)
        val cleanImg = StoreUtils.cleanAndResolveImageUrl(imageUrlRaw)

        if (cleanName.isBlank()) {
            showToast("Product name enter karein!", ToastKind.DANGER)
            return
        }

        val validation = EarningsCalculator.validateAndCalculate(
            purchaseCostText = purchaseCostText,
            sellingPriceText = sellingPriceText,
            commissionPercentText = commissionPercentText,
            ruleSource = commissionRuleSource,
            allowLossMakingProducts = state.commissionRules.allowLossMakingProducts
        )
        if (!validation.isValid || validation.breakdown == null) {
            showToast(validation.errorMessage ?: "Invalid pricing values!", ToastKind.DANGER)
            return
        }
        if (cleanImg.isBlank()) {
            showToast("Photo upload karein ya valid HTTPS image URL dalein!", ToastKind.DANGER)
            return
        }
        if (state.securityConfig.strictLinkSanitizer &&
            !cleanImg.startsWith("data:image/") &&
            !SecurityGuard.isSafeExternalUrl(cleanImg)
        ) {
            showToast("Blocked suspicious/shortened image URL! Use direct HTTPS image.", ToastKind.DANGER)
            return
        }

        val calc = validation.breakdown
        val mrp = mrpText.toDoubleOrNull() ?: 0.0
        val effectiveMrp = if (mrp > calc.sellingPrice) EarningsCalculator.round2(mrp) else 0.0
        val discountPct = if (effectiveMrp > calc.sellingPrice) {
            (((effectiveMrp - calc.sellingPrice) / effectiveMrp) * 100.0).roundToInt()
        } else 0
        val now = System.currentTimeMillis()

        val payload = mapOf<String, Any>(
            "name" to cleanName,
            "price" to calc.sellingPrice,
            "sellingPrice" to calc.sellingPrice,
            "purchaseCost" to calc.purchaseCost,
            "commissionPercentage" to calc.commissionPercentage,
            "commissionAmount" to calc.commissionAmount,
            "storeEarning" to calc.storeEarning,
            "estimatedProfit" to calc.estimatedProfit,
            "commissionRuleSource" to calc.ruleSource.name,
            "lastUpdated" to now,
            "mrp" to effectiveMrp,
            "discountPercent" to discountPct,
            "isOnSale" to isOnSale,
            "saleBadgeText" to StoreUtils.sanitizeText(saleBadgeText),
            "searchKeywords" to StoreUtils.sanitizeText(searchKeywords),
            "category" to category.trim().uppercase(),
            "description" to StoreUtils.sanitizeText(description),
            "imageUrl" to cleanImg,
            "available" to available,
            "adminApproved" to false,
            "approvalStatus" to "PENDING",
            "updatedAt" to now
        )

        viewModelScope.launch {
            repo.saveProduct(state.uid, itemId, payload).onSuccess {
                onDone()
                showToast("Product & earnings saved for Admin verification!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Save failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun toggleProductStock(item: ProductItem) {
        val state = uiState.value
        val next = !item.available
        viewModelScope.launch {
            repo.toggleProductStock(state.uid, item.itemId, next).onSuccess {
                showToast(if (next) "In Stock." else "Out of Stock.", ToastKind.INFO)
            }
        }
    }

    fun deleteProduct(item: ProductItem) {
        val state = uiState.value
        if (!state.adminSettings.allowStoreProductEdit) return
        viewModelScope.launch {
            repo.deleteProduct(state.uid, item.itemId).onSuccess {
                showToast("Product removed.", ToastKind.DANGER)
            }
        }
    }

    fun toggleApproveInventory(item: ProductItem) {
        val state = uiState.value
        val nextApproved = !item.adminApproved
        viewModelScope.launch {
            repo.setInventoryApproval(state.uid, item.itemId, nextApproved).onSuccess {
                showToast(
                    if (nextApproved) "${item.name} inventory update approved."
                    else "${item.name} moved to pending review.",
                    if (nextApproved) ToastKind.SUCCESS else ToastKind.INFO
                )
            }.onFailure {
                showToast("Approval update failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun toggleFlagInventory(item: ProductItem, reason: String = "") {
        val state = uiState.value
        val nextFlagged = !item.inventoryFlagged
        val cleanReason = StoreUtils.sanitizeText(reason).ifBlank { "Needs stock/price verification" }
        viewModelScope.launch {
            repo.setInventoryFlag(state.uid, item.itemId, nextFlagged, cleanReason).onSuccess {
                showToast(
                    if (nextFlagged) "${item.name} flagged for review."
                    else "${item.name} flag cleared.",
                    if (nextFlagged) ToastKind.WARNING else ToastKind.INFO
                )
            }.onFailure {
                showToast("Flag update failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun updateStockQty(item: ProductItem, newQty: Int) {
        val state = uiState.value
        viewModelScope.launch {
            repo.updateProductStockQty(state.uid, item.itemId, newQty).onSuccess {
                showToast("Stock level updated to ${newQty.coerceAtLeast(0)} units.", ToastKind.INFO)
            }.onFailure {
                showToast("Stock update failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun quickSeedProducts() {
        val state = uiState.value
        if (!state.adminSettings.allowStoreProductEdit) {
            showToast("Catalog editing locked by Admin!", ToastKind.DANGER)
            return
        }
        viewModelScope.launch {
            repo.quickSeedProducts(state.uid, state.commissionRules).onSuccess {
                showToast("6 items added! Sent to Admin for verification.", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Quick seed failed.", ToastKind.DANGER)
            }
        }
    }

    fun saveGlobalCommissionSettings(
        percentText: String,
        globalRuleEnabled: Boolean,
        allowLossMakingProducts: Boolean
    ) {
        val pct = percentText.toDoubleOrNull()
        if (pct == null || !pct.isFinite() || pct < 0.0 || pct > 100.0) {
            showToast("Commission % must be between 0 and 100!", ToastKind.DANGER)
            return
        }
        viewModelScope.launch {
            repo.saveGlobalCommissionSettings(pct, globalRuleEnabled, allowLossMakingProducts).onSuccess {
                val updatedRules = uiState.value.commissionRules.copy(
                    globalCommissionPercent = EarningsCalculator.round2(pct),
                    globalRuleEnabled = globalRuleEnabled,
                    allowLossMakingProducts = allowLossMakingProducts
                )
                recalculateStoreCatalogProducts(updatedRules)
                showToast("Global commission settings updated & products recalculated!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Failed to save global settings: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun saveStoreCommissionRule(storeId: String, storeName: String, percentText: String, enabled: Boolean) {
        val pct = percentText.toDoubleOrNull()
        if (storeId.isBlank()) {
            showToast("Select or enter a Store ID!", ToastKind.DANGER)
            return
        }
        if (pct == null || !pct.isFinite() || pct < 0.0 || pct > 100.0) {
            showToast("Store commission % must be between 0 and 100!", ToastKind.DANGER)
            return
        }
        viewModelScope.launch {
            repo.saveStoreCommissionRule(storeId, storeName, pct, enabled).onSuccess {
                val current = uiState.value.commissionRules
                val updated = current.copy(
                    storeCommissions = current.storeCommissions + (storeId.trim() to com.example.model.CommissionRuleEntry(
                        targetId = storeId.trim(),
                        targetLabel = storeName,
                        storeId = storeId.trim(),
                        percent = EarningsCalculator.round2(pct),
                        enabled = enabled
                    ))
                )
                if (storeId.trim() == uiState.value.uid) {
                    recalculateStoreCatalogProducts(updated)
                }
                showToast("Store commission rule saved!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun deleteStoreCommissionRule(storeId: String) {
        viewModelScope.launch {
            repo.deleteStoreCommissionRule(storeId).onSuccess {
                val current = uiState.value.commissionRules
                recalculateStoreCatalogProducts(
                    current.copy(storeCommissions = current.storeCommissions - storeId.trim())
                )
                showToast("Store commission rule removed.", ToastKind.INFO)
            }
        }
    }

    fun saveCategoryCommissionRule(categoryId: String, categoryName: String, percentText: String, enabled: Boolean) {
        val pct = percentText.toDoubleOrNull()
        if (categoryId.isBlank()) {
            showToast("Select or enter a Category!", ToastKind.DANGER)
            return
        }
        if (pct == null || !pct.isFinite() || pct < 0.0 || pct > 100.0) {
            showToast("Category commission % must be between 0 and 100!", ToastKind.DANGER)
            return
        }
        val cleanCat = categoryId.trim().uppercase()
        viewModelScope.launch {
            repo.saveCategoryCommissionRule(cleanCat, categoryName, pct, enabled).onSuccess {
                val current = uiState.value.commissionRules
                recalculateStoreCatalogProducts(
                    current.copy(
                        categoryCommissions = current.categoryCommissions + (cleanCat to com.example.model.CommissionRuleEntry(
                            targetId = cleanCat,
                            targetLabel = categoryName,
                            percent = EarningsCalculator.round2(pct),
                            enabled = enabled
                        ))
                    )
                )
                showToast("Category commission saved & recalculated!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun deleteCategoryCommissionRule(categoryId: String) {
        val cleanCat = categoryId.trim().uppercase()
        viewModelScope.launch {
            repo.deleteCategoryCommissionRule(cleanCat).onSuccess {
                val current = uiState.value.commissionRules
                recalculateStoreCatalogProducts(
                    current.copy(categoryCommissions = current.categoryCommissions - cleanCat)
                )
                showToast("Category commission rule removed.", ToastKind.INFO)
            }
        }
    }

    fun saveProductCommissionRule(
        productId: String,
        storeId: String,
        productName: String,
        percentText: String,
        enabled: Boolean
    ) {
        val pct = percentText.toDoubleOrNull()
        if (productId.isBlank()) {
            showToast("Select a Product!", ToastKind.DANGER)
            return
        }
        if (pct == null || !pct.isFinite() || pct < 0.0 || pct > 100.0) {
            showToast("Product commission % must be between 0 and 100!", ToastKind.DANGER)
            return
        }
        viewModelScope.launch {
            repo.saveProductCommissionRule(productId, storeId, productName, pct, enabled).onSuccess {
                val current = uiState.value.commissionRules
                recalculateStoreCatalogProducts(
                    current.copy(
                        productCommissions = current.productCommissions + (productId.trim() to com.example.model.CommissionRuleEntry(
                            targetId = productId.trim(),
                            targetLabel = productName,
                            storeId = storeId,
                            percent = EarningsCalculator.round2(pct),
                            enabled = enabled
                        ))
                    )
                )
                showToast("Product commission override saved!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun deleteProductCommissionRule(productId: String) {
        viewModelScope.launch {
            repo.deleteProductCommissionRule(productId).onSuccess {
                val current = uiState.value.commissionRules
                recalculateStoreCatalogProducts(
                    current.copy(productCommissions = current.productCommissions - productId.trim())
                )
                showToast("Product override removed.", ToastKind.INFO)
            }
        }
    }

    private suspend fun recalculateStoreCatalogProducts(updatedRules: CommissionRules) {
        val state = uiState.value
        if (!state.isAuthenticated || state.uid.isBlank()) return
        val now = System.currentTimeMillis()
        state.products.forEach { item ->
            if (item.commissionRuleSource == CommissionRuleSource.CUSTOM_OVERRIDE.name) return@forEach
            val resolved = EarningsCalculator.resolveCommission(
                productId = item.itemId,
                storeId = state.uid,
                category = item.category,
                rules = updatedRules
            )
            val sell = if (item.sellingPrice > 0.0) item.sellingPrice else item.price
            if (sell > 0.0) {
                val calc = EarningsCalculator.calculate(
                    purchaseCost = item.purchaseCost,
                    sellingPrice = sell,
                    commissionPercent = resolved.percent,
                    ruleSource = resolved.source
                )
                repo.saveProduct(
                    state.uid,
                    item.itemId,
                    mapOf(
                        "sellingPrice" to calc.sellingPrice,
                        "price" to calc.sellingPrice,
                        "commissionPercentage" to calc.commissionPercentage,
                        "commissionAmount" to calc.commissionAmount,
                        "storeEarning" to calc.storeEarning,
                        "estimatedProfit" to calc.estimatedProfit,
                        "commissionRuleSource" to calc.ruleSource.name,
                        "lastUpdated" to now
                    )
                )
            }
        }
    }

    fun requestPayout() {
        val state = uiState.value
        if (state.securityConfig.panicSafeModeActive) {
            showToast("Payouts locked while Panic Safe-Mode is active!", ToastKind.DANGER)
            return
        }
        val pending = state.pendingPayoutBalance
        val minPayout = state.adminSettings.minStorePayout
        val maxPayout = state.adminSettings.maxStorePayout

        if (pending <= 0.0) {
            showToast("No pending payout balance!", ToastKind.INFO)
            return
        }
        if (pending < minPayout) {
            showToast("Minimum ₹${minPayout.toInt()} required for payout!", ToastKind.WARNING)
            return
        }
        if (!StoreUtils.isValidUpiId(state.storeProfile.upiId)) {
            showToast("Save valid UPI ID in Store Settings first!", ToastKind.DANGER)
            selectTab(PartnerTab.PROFILE)
            return
        }
        if (state.payouts.any { it.status == "PENDING" }) {
            showToast("A payout request is already pending!", ToastKind.INFO)
            return
        }

        val reqAmount = ((min(pending, maxPayout)) * 100.0).roundToInt() / 100.0
        val payload = mapOf<String, Any>(
            "storeId" to state.uid,
            "storeName" to state.storeProfile.name.ifBlank { "Store" },
            "storePhone" to state.storeProfile.phone,
            "storeUpiId" to state.storeProfile.upiId,
            "amount" to reqAmount,
            "status" to "PENDING",
            "adminRef" to "",
            "createdAt" to System.currentTimeMillis()
        )
        viewModelScope.launch {
            repo.requestPayout(payload).onSuccess {
                showToast("₹${String.format("%.2f", reqAmount)} Payout Request sent to Admin!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Payout request failed.", ToastKind.DANGER)
            }
        }
    }

    fun saveStoreProfile(
        name: String,
        upiId: String,
        cuisine: String,
        phone: String,
        discountPercentText: String,
        discountMinText: String,
        address: String,
        hours: String,
        imageUrl: String,
        latText: String,
        lngText: String
    ) {
        val state = uiState.value
        val cleanPhone = phone.trim()
        val cleanUpi = upiId.trim()
        if (!StoreUtils.isValidIndianMobile(cleanPhone)) {
            showToast("Valid 10-digit mobile number enter karein!", ToastKind.DANGER)
            return
        }
        if (!StoreUtils.isValidUpiId(cleanUpi)) {
            showToast("Valid UPI ID enter karein!", ToastKind.DANGER)
            return
        }
        if (state.securityConfig.upiChangeLockEnabled &&
            state.storeProfile.upiId.isNotBlank() &&
            !cleanUpi.equals(state.storeProfile.upiId, ignoreCase = true)
        ) {
            showToast("UPI Payout Lock active! Unlock in Security Shield before changing UPI.", ToastKind.DANGER)
            return
        }
        val lat = latText.toDoubleOrNull() ?: 26.4524
        val lng = lngText.toDoubleOrNull() ?: 89.9723
        val discountPct = (discountPercentText.toIntOrNull() ?: 0).coerceIn(0, 80)
        val discountMin = (discountMinText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10000.0)
        val cleanImg = StoreUtils.cleanAndResolveImageUrl(imageUrl)
        if (state.securityConfig.strictLinkSanitizer && imageUrl.isNotBlank() && cleanImg.isBlank()) {
            showToast("Unsafe or non-HTTPS banner image URL blocked!", ToastKind.DANGER)
            return
        }

        val updates = mapOf<String, Any>(
            "name" to StoreUtils.sanitizeText(name),
            "upiId" to cleanUpi,
            "cuisine" to StoreUtils.sanitizeText(cuisine),
            "phone" to cleanPhone,
            "storeDiscountPercent" to discountPct,
            "storeDiscountMinOrder" to discountMin,
            "address" to StoreUtils.sanitizeText(address),
            "hours" to StoreUtils.sanitizeText(hours),
            "imageUrl" to cleanImg,
            "lat" to lat,
            "lng" to lng,
            "locationLink" to "https://www.google.com/maps?q=$lat,$lng"
        )
        viewModelScope.launch {
            repo.updateStoreProfile(state.uid, updates).onSuccess {
                showToast("Store profile, discounts & GPS saved!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Save failed: ${it.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun submitFeedback(subject: String, message: String, onDone: () -> Unit) {
        val state = uiState.value
        val cleanSubject = StoreUtils.sanitizeText(subject)
        val cleanMsg = StoreUtils.sanitizeText(message)
        if (cleanSubject.isBlank() || cleanMsg.isBlank()) {
            showToast("Please enter subject and message.", ToastKind.DANGER)
            return
        }
        val payload = mapOf<String, Any>(
            "role" to "STORE",
            "uid" to state.uid,
            "name" to state.storeProfile.name.ifBlank { "Store" },
            "phone" to state.storeProfile.phone,
            "subject" to cleanSubject,
            "message" to cleanMsg,
            "status" to "OPEN",
            "createdAt" to System.currentTimeMillis()
        )
        viewModelScope.launch {
            repo.submitFeedback(payload).onSuccess {
                onDone()
                showToast("Feedback sent to Admin Hub!", ToastKind.SUCCESS)
            }.onFailure {
                showToast("Failed to send feedback.", ToastKind.DANGER)
            }
        }
    }

    // =========================================================================
    // RIDER IN-APP VOICE CALL (VoIP via WebRTC) & REAL-TIME CHAT MANAGEMENT
    // =========================================================================

    fun attachWebRtcEngine(engine: WebRtcPeerEngine) {
        this.webRtcEngine = engine
    }

    fun openOrderDetails(orderId: String) {
        _selectedOrderDetailsId.value = orderId.trim().ifBlank { null }
    }

    fun closeOrderDetails() {
        _selectedOrderDetailsId.value = null
    }

    fun openOrderChat(orderId: String) {
        val cleanId = orderId.trim()
        if (cleanId.isBlank()) return
        _activeChatOrderId.value = cleanId
        // Mark pending unread incoming messages as READ
        val currentChats = _chatMessagesByOrder.value[cleanId] ?: emptyList()
        currentChats.forEach { msg ->
            if (msg.senderRole.equals("RIDER", ignoreCase = true) && !msg.status.equals("READ", ignoreCase = true)) {
                viewModelScope.launch {
                    repo.updateChatMessageStatus(cleanId, msg.messageId, "READ")
                }
            }
        }
    }

    fun closeOrderChat() {
        _activeChatOrderId.value = null
    }

    private fun handleIncomingCallInboxUpdate(call: InAppCallRecord?) {
        val state = uiState.value
        if (!state.isAuthenticated || state.uid.isBlank()) return
        if (call == null || call.callId.isBlank() || call.orderId.isBlank()) return

        val isValid = RiderCommPolicy.isValidIncomingRiderCall(
            call = call,
            authenticatedRestaurantUid = state.uid,
            authorizedOrders = state.allOrders
        )
        if (!isValid) return
        if (!handledIncomingCallIds.add(call.callId)) return

        // If restaurant is already in an active/ringing call, automatically notify busy status
        val currentCallState = _callUiState.value
        if (currentCallState.isActiveOrRinging) {
            viewModelScope.launch {
                repo.updateCallStatus(
                    orderId = call.orderId,
                    restaurantUid = state.uid,
                    riderUid = call.riderUid,
                    callId = call.callId,
                    status = "BUSY",
                    restaurantAnswered = false
                )
            }
            return
        }

        // Trigger incoming call alert ring & vibration
        runCatching { webRtcEngine.startIncomingRingAlert() }

        _callUiState.value = CallUiSessionState(
            callId = call.callId,
            orderId = call.orderId,
            riderUid = call.riderUid,
            riderName = call.riderName.ifBlank { "Delivery Rider" },
            callerRole = "RIDER",
            status = "RINGING",
            restaurantAnswered = false,
            isIncoming = true
        )

        // Listen to order call lifecycle
        observeActiveOrderCall(call.orderId)
    }

    fun startRiderVoiceCall(order: StoreOrder, rider: DeliveryRider?) {
        val state = uiState.value
        val riderUid = order.deliveryUid.ifBlank { rider?.uid.orEmpty() }
        val rName = rider?.name
        val riderName = if (!rName.isNullOrBlank()) rName else "Assigned Rider"

        val validationErr = RiderCommPolicy.validateOutgoingCall(
            isAuthenticated = state.isAuthenticated,
            authenticatedRestaurantUid = state.uid,
            order = order,
            existingLocalActiveCall = if (_callUiState.value.isActiveOrRinging) _callUiState.value.toRecord() else null,
            existingRemoteOrderCall = null
        )
        if (validationErr != null) {
            showToast(validationErr, ToastKind.DANGER)
            return
        }
        if (riderUid.isBlank()) {
            showToast("No rider UID available for this order.", ToastKind.DANGER)
            return
        }

        val now = System.currentTimeMillis()
        val callRecord = RiderCommPolicy.buildOutgoingCallPayload(
            order = order,
            restaurantUid = state.uid,
            restaurantName = state.storeProfile.name.ifBlank { "Restaurant" },
            riderUid = riderUid,
            riderName = riderName,
            nowMillis = now
        )

        _callUiState.value = CallUiSessionState(
            callId = callRecord.callId,
            orderId = order.orderId,
            riderUid = riderUid,
            riderName = riderName,
            callerRole = "RESTAURANT",
            status = "RINGING",
            restaurantAnswered = false,
            isIncoming = false,
            startedAt = now
        )

        viewModelScope.launch {
            repo.startOutgoingCall(callRecord).onSuccess {
                observeActiveOrderCall(order.orderId)
                startWebRtcCallerSignaling(order.orderId)
                startCallTimeoutWatcher(order.orderId, state.uid, riderUid, callRecord.callId)
            }.onFailure { err ->
                _callUiState.value = CallUiSessionState()
                showToast("Failed to initiate voice call: ${err.localizedMessage}", ToastKind.DANGER)
            }
        }
    }

    fun answerIncomingRiderCall() {
        val current = _callUiState.value
        if (!current.isIncoming || current.callId.isBlank() || current.orderId.isBlank()) return

        val state = uiState.value
        runCatching { webRtcEngine.stopIncomingRingAlert() }

        _callUiState.value = current.copy(status = "CONNECTING")

        viewModelScope.launch {
            // Read remote offer from inAppCalls/{orderId}/webrtc/offer
            repo.readWebRtcOffer(current.orderId).onSuccess { offerDesc ->
                val offerSdp = offerDesc?.sdp.orEmpty()
                if (offerSdp.isBlank()) {
                    showToast("Call connection failed: WebRTC offer missing.", ToastKind.DANGER)
                    endActiveRiderCall()
                    return@onSuccess
                }

                val callbacks = object : WebRtcPeerCallbacks {
                    override fun onLocalDescription(description: com.example.model.WebRtcSessionDescriptionData) {
                        viewModelScope.launch {
                            repo.writeWebRtcAnswer(current.orderId, description)
                        }
                    }
                    override fun onLocalIceCandidate(candidate: WebRtcIceCandidateData) {
                        viewModelScope.launch {
                            repo.pushWebRtcIceCandidate(current.orderId, isCaller = false, candidate = candidate)
                        }
                    }
                    override fun onConnectionStateChanged(connState: String) {
                        if (connState.lowercase(Locale.US) == "connected") {
                            markCallConnectedAndSync(current.orderId, state.uid, current.riderUid, current.callId)
                        }
                    }
                    override fun onPeerConnected() {
                        markCallConnectedAndSync(current.orderId, state.uid, current.riderUid, current.callId)
                    }
                    override fun onPeerFailure(reason: String) {
                        showToast("Call error: $reason", ToastKind.DANGER)
                        endActiveRiderCall()
                    }
                }

                webRtcEngine.startIncomingAnswer(offerSdp, callbacks)
                observeRemoteIceCandidates(current.orderId, isCaller = false)
            }.onFailure {
                showToast("Failed to establish peer connection.", ToastKind.DANGER)
                endActiveRiderCall()
            }
        }
    }

    fun declineIncomingRiderCall() {
        val current = _callUiState.value
        if (!current.isIncoming || current.callId.isBlank()) {
            endActiveRiderCall()
            return
        }
        val state = uiState.value
        runCatching { webRtcEngine.stopIncomingRingAlert() }

        viewModelScope.launch {
            repo.updateCallStatus(
                orderId = current.orderId,
                restaurantUid = state.uid,
                riderUid = current.riderUid,
                callId = current.callId,
                status = "DECLINED",
                restaurantAnswered = false
            )
        }
        cleanupCallSession()
    }

    fun endActiveRiderCall() {
        val current = _callUiState.value
        val state = uiState.value
        runCatching { webRtcEngine.stopIncomingRingAlert() }

        if (current.orderId.isNotBlank() && current.callId.isNotBlank() && state.isAuthenticated) {
            viewModelScope.launch {
                repo.updateCallStatus(
                    orderId = current.orderId,
                    restaurantUid = state.uid,
                    riderUid = current.riderUid,
                    callId = current.callId,
                    status = "ENDED",
                    restaurantAnswered = current.restaurantAnswered
                )
            }
        }
        cleanupCallSession()
    }

    fun toggleCallMute() {
        val current = _callUiState.value
        val next = !current.isMuted
        _callUiState.value = current.copy(isMuted = next)
        runCatching { webRtcEngine.setMuted(next) }
    }

    fun toggleCallHold() {
        val current = _callUiState.value
        val next = !current.isOnHold
        _callUiState.value = current.copy(isOnHold = next)
        runCatching { webRtcEngine.setOnHold(next) }
    }

    fun toggleCallSpeakerphone(enabled: Boolean) {
        val current = _callUiState.value
        _callUiState.value = current.copy(isSpeakerOn = enabled)
        runCatching { webRtcEngine.setSpeakerphone(enabled) }
    }

    private fun markCallConnectedAndSync(orderId: String, restaurantUid: String, riderUid: String, callId: String) {
        if (_callUiState.value.status == "CONNECTED") return
        _callUiState.value = _callUiState.value.copy(
            status = "CONNECTED",
            restaurantAnswered = true,
            connectedAt = System.currentTimeMillis()
        )
        startCallDurationTicker()

        viewModelScope.launch {
            repo.updateCallStatus(
                orderId = orderId,
                restaurantUid = restaurantUid,
                riderUid = riderUid,
                callId = callId,
                status = "CONNECTED",
                restaurantAnswered = true
            )
        }
    }

    private fun startWebRtcCallerSignaling(orderId: String) {
        val callbacks = object : WebRtcPeerCallbacks {
            override fun onLocalDescription(description: com.example.model.WebRtcSessionDescriptionData) {
                viewModelScope.launch {
                    repo.writeWebRtcOffer(orderId, description)
                }
            }
            override fun onLocalIceCandidate(candidate: WebRtcIceCandidateData) {
                viewModelScope.launch {
                    repo.pushWebRtcIceCandidate(orderId, isCaller = true, candidate = candidate)
                }
            }
            override fun onConnectionStateChanged(connState: String) {
                if (connState.lowercase(Locale.US) == "connected") {
                    val st = uiState.value
                    markCallConnectedAndSync(orderId, st.uid, _callUiState.value.riderUid, _callUiState.value.callId)
                }
            }
            override fun onPeerConnected() {
                val st = uiState.value
                markCallConnectedAndSync(orderId, st.uid, _callUiState.value.riderUid, _callUiState.value.callId)
            }
            override fun onPeerFailure(reason: String) {
                showToast("Call connection error: $reason", ToastKind.DANGER)
                endActiveRiderCall()
            }
        }

        webRtcEngine.startOutgoingOffer(callbacks)
        observeWebRtcAnswer(orderId)
        observeRemoteIceCandidates(orderId, isCaller = true)
    }

    private fun observeActiveOrderCall(orderId: String) {
        activeOrderCallListenerJob?.cancel()
        activeOrderCallListenerJob = viewModelScope.launch {
            repo.observeOrderCall(orderId).collect { call ->
                if (call == null || call.status.equals("ENDED", ignoreCase = true) || call.status.equals("DECLINED", ignoreCase = true)) {
                    if (_callUiState.value.orderId == orderId) {
                        if (call?.status == "DECLINED") showToast("Call declined by rider.", ToastKind.INFO)
                        cleanupCallSession()
                    }
                } else if (call.status.equals("CONNECTED", ignoreCase = true)) {
                    val cur = _callUiState.value
                    if (cur.orderId == orderId && cur.status != "CONNECTED") {
                        _callUiState.value = cur.copy(status = "CONNECTED", restaurantAnswered = true)
                        startCallDurationTicker()
                    }
                }
            }
        }
    }

    private fun observeWebRtcAnswer(orderId: String) {
        activeWebRtcAnswerListenerJob?.cancel()
        activeWebRtcAnswerListenerJob = viewModelScope.launch {
            repo.observeWebRtcAnswer(orderId).collect { answer ->
                if (answer != null && answer.sdp.isNotBlank()) {
                    runCatching { webRtcEngine.applyRemoteAnswer(answer.sdp) }
                }
            }
        }
    }

    private fun observeRemoteIceCandidates(orderId: String, isCaller: Boolean) {
        activeWebRtcCandidatesListenerJob?.cancel()
        activeWebRtcCandidatesListenerJob = viewModelScope.launch {
            repo.observeWebRtcIceCandidates(orderId, observeCallerCandidates = !isCaller).collect { candidate ->
                val key = "${candidate.sdpMLineIndex}_${candidate.candidate}"
                if (handledRemoteCandidateKeys.add(key)) {
                    runCatching { webRtcEngine.addRemoteIceCandidate(candidate) }
                }
            }
        }
    }

    private fun startCallTimeoutWatcher(orderId: String, restaurantUid: String, riderUid: String, callId: String) {
        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(45_000L) // 45s ringing timeout
            val cur = _callUiState.value
            if (cur.callId == callId && cur.status == "RINGING") {
                showToast("Call missed / no answer.", ToastKind.INFO)
                repo.updateCallStatus(orderId, restaurantUid, riderUid, callId, "MISSED", false)
                cleanupCallSession()
            }
        }
    }

    private fun startCallDurationTicker() {
        callDurationTickerJob?.cancel()
        callDurationTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val cur = _callUiState.value
                if (cur.status == "CONNECTED" && cur.connectedAt > 0L) {
                    val durSec = (System.currentTimeMillis() - cur.connectedAt) / 1000L
                    _callUiState.value = cur.copy(durationSeconds = durSec)
                } else {
                    break
                }
            }
        }
    }

    private fun cleanupCallSession() {
        activeOrderCallListenerJob?.cancel()
        activeWebRtcOfferListenerJob?.cancel()
        activeWebRtcAnswerListenerJob?.cancel()
        activeWebRtcCandidatesListenerJob?.cancel()
        callTimeoutJob?.cancel()
        callDurationTickerJob?.cancel()
        handledRemoteCandidateKeys.clear()
        runCatching { webRtcEngine.releaseSession() }
        _callUiState.value = CallUiSessionState()
    }

    // =========================================================================
    // RESTAURANT <-> RIDER REAL-TIME CHAT MANAGEMENT
    // =========================================================================

    private fun syncOrderChatListeners(restaurantUid: String, orders: List<StoreOrder>) {
        val activeRiderOrders = orders.filter { o ->
            o.deliveryUid.isNotBlank() && RiderCommPolicy.isCommunicationStageActive(o.status)
        }
        val currentObservedIds = activeRiderOrders.map { it.orderId }.toSet()

        // Cancel obsolete chat listeners
        orderChatListenerJobs.entries.removeAll { (orderId, job) ->
            if (orderId !in currentObservedIds) {
                job.cancel()
                true
            } else {
                false
            }
        }

        // Add new chat listeners
        activeRiderOrders.forEach { order ->
            if (!orderChatListenerJobs.containsKey(order.orderId)) {
                val job = viewModelScope.launch {
                    repo.observeOrderChatMessages(
                        orderId = order.orderId,
                        restaurantUid = restaurantUid,
                        riderUid = order.deliveryUid,
                        customerUid = order.customerUid
                    ).collect { msgs ->
                        val filtered = msgs.filter {
                            RiderCommPolicy.isRestaurantRiderMessage(it, restaurantUid, order.deliveryUid, order.customerUid)
                        }.sortedBy { it.timestamp }

                        val currentMap = _chatMessagesByOrder.value.toMutableMap()
                        currentMap[order.orderId] = filtered
                        _chatMessagesByOrder.value = currentMap

                        // If chat is currently open for this order, mark incoming Rider messages as READ & DELIVERED
                        if (_activeChatOrderId.value == order.orderId) {
                            filtered.forEach { msg ->
                                if (msg.senderRole.equals("RIDER", ignoreCase = true)) {
                                    if (!msg.status.equals("READ", ignoreCase = true)) {
                                        repo.updateChatMessageStatus(order.orderId, msg.messageId, "READ")
                                    }
                                } else if (msg.senderRole.equals("RESTAURANT", ignoreCase = true) && msg.status.equals("SENT", ignoreCase = true)) {
                                    repo.updateChatMessageStatus(order.orderId, msg.messageId, "DELIVERED")
                                }
                            }
                        }
                    }
                }
                orderChatListenerJobs[order.orderId] = job
            }
        }
    }

    private fun clearAllChatListeners() {
        orderChatListenerJobs.values.forEach { it.cancel() }
        orderChatListenerJobs.clear()
        _chatMessagesByOrder.value = emptyMap()
    }

    fun sendOrderChatMessage(orderId: String, text: String) {
        val cleanText = StoreUtils.sanitizeText(text, RiderCommPolicy.MAX_CHAT_MESSAGE_LENGTH)
        if (cleanText.isBlank()) return
        val state = uiState.value
        val order = state.allOrders.firstOrNull { it.orderId == orderId } ?: return
        val riderUid = order.deliveryUid
        if (riderUid.isBlank()) {
            showToast("No rider assigned for messaging.", ToastKind.DANGER)
            return
        }

        val tempMsgId = "msg_${System.currentTimeMillis()}_${(0..999).random()}"
        val tempMsg = TripChatMessage(
            messageId = tempMsgId,
            orderId = orderId,
            senderUid = state.uid,
            senderRole = "RESTAURANT",
            receiverUid = riderUid,
            messageType = "TEXT",
            message = cleanText,
            timestamp = System.currentTimeMillis(),
            status = "SENT"
        )

        // Optimistic append
        val currentMap = _chatMessagesByOrder.value.toMutableMap()
        val existingList = currentMap[orderId] ?: emptyList()
        currentMap[orderId] = existingList + tempMsg
        _chatMessagesByOrder.value = currentMap

        viewModelScope.launch {
            repo.sendOrderChatMessage(
                orderId = orderId,
                restaurantUid = state.uid,
                riderUid = riderUid,
                messageText = cleanText,
                existingMessageId = tempMsgId
            ).onFailure {
                // Track failure for retry
                val failedMap = _failedChatMessagesByOrder.value.toMutableMap()
                val failedList = failedMap[orderId] ?: emptyList()
                failedMap[orderId] = failedList + tempMsg
                _failedChatMessagesByOrder.value = failedMap
                showToast("Failed to send message. Tap to retry.", ToastKind.DANGER)
            }
        }
    }

    fun retryFailedChatMessage(orderId: String, failedMessage: TripChatMessage) {
        val state = uiState.value
        val order = state.allOrders.firstOrNull { it.orderId == orderId } ?: return
        val riderUid = order.deliveryUid

        viewModelScope.launch {
            repo.sendOrderChatMessage(
                orderId = orderId,
                restaurantUid = state.uid,
                riderUid = riderUid,
                messageText = failedMessage.message,
                existingMessageId = failedMessage.messageId
            ).onSuccess {
                val failedMap = _failedChatMessagesByOrder.value.toMutableMap()
                val failedList = failedMap[orderId] ?: emptyList()
                failedMap[orderId] = failedList.filterNot { m -> m.messageId == failedMessage.messageId }
                _failedChatMessagesByOrder.value = failedMap
            }.onFailure {
                showToast("Retry failed.", ToastKind.DANGER)
            }
        }
    }

    fun notifyRiderOrderReadyWithComm(order: StoreOrder) {
        val state = uiState.value
        setOrderStatus(order, "READY_FOR_PICKUP")
        if (order.deliveryUid.isNotBlank()) {
            val storeName = state.storeProfile.name.ifBlank { "Restaurant" }
            val otp = StoreUtils.getStorePickupCode(order.pickupOtp, order.orderId)
            viewModelScope.launch {
                repo.notifyRiderOrderReady(order, storeName, otp)
            }
            // Also send a quick message in order chat
            sendOrderChatMessage(order.orderId, "Order is ready for pickup! Handover code: $otp")
        }
    }
}

