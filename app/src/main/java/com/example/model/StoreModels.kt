package com.example.model

data class StoreProfile(
    val name: String = "",
    val email: String = "",
    val cuisine: String = "",
    val address: String = "",
    val phone: String = "",
    val upiId: String = "",
    val isOpen: Boolean = true,
    val hours: String = "7:00 AM - 9:00 PM",
    val imageUrl: String = "",
    val approved: Boolean = false,
    val suspended: Boolean = false,
    val adminNote: String = "",
    val lat: Double = 26.4524,
    val lng: Double = 89.9723,
    val locationLink: String = "",
    val storeDiscountPercent: Int = 0,
    val storeDiscountMinOrder: Double = 0.0
)

data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val qty: Int = 1,
    val price: Double = 0.0,
    val purchaseCost: Double = 0.0,
    val commissionPercentage: Double? = null,
    val commissionAmount: Double = 0.0,
    val storeEarning: Double = 0.0,
    val commissionRuleSource: String = ""
)

data class StoreOrder(
    val orderId: String = "",
    val batchGroupId: String = "",
    val restaurantId: String = "",
    val customerUid: String = "",
    val deliveryUid: String = "",
    val status: String = "PLACED",
    val paymentMode: String = "COD",
    val paymentStatus: String = "",
    val pickupOtp: String = "",
    val address: String = "",
    val phone: String = "",
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val riderLat: Double? = null,
    val riderLng: Double? = null,
    val locationLink: String = "",
    val subtotalAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val items: List<OrderItem> = emptyList(),
    val isSpamSuspect: Boolean = false,
    val emergencyAlert: Boolean = false,
    val emergencyDetails: String = "",
    val cancelReason: String = "",
    val cancelledBy: String = ""
) {
    val itemsValue: Double
        get() = if (subtotalAmount > 0.0) subtotalAmount else totalAmount

    val effectiveTripId: String
        get() = batchGroupId.ifBlank { orderId }
}

data class ProductItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val sellingPrice: Double = price,
    val purchaseCost: Double = 0.0,
    val commissionPercentage: Double? = null,
    val commissionAmount: Double = 0.0,
    val storeEarning: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val commissionRuleSource: String = "",
    val lastUpdated: Long = 0L,
    val mrp: Double = 0.0,
    val discountPercent: Int = 0,
    val isOnSale: Boolean = false,
    val saleBadgeText: String = "",
    val searchKeywords: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val available: Boolean = true,
    val adminApproved: Boolean = false,
    val approvalStatus: String = "PENDING",
    val rejectionReason: String = "",
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val updatedAt: Long = 0L,
    val stockQty: Int? = null,
    val inventoryFlagged: Boolean = false,
    val inventoryFlagReason: String = ""
)

enum class CommissionRuleSource(val label: String, val priority: Int) {
    PRODUCT("Product Rule", 1),
    STORE("Store Rule", 2),
    CATEGORY("Category Rule", 3),
    GLOBAL("Global Default", 4),
    CUSTOM_OVERRIDE("Custom Override", 0),
    DISABLED("Rule Disabled", 5);

    companion object {
        fun fromWire(value: String): CommissionRuleSource =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: GLOBAL
    }
}

data class CommissionRuleEntry(
    val targetId: String = "",
    val targetLabel: String = "",
    val storeId: String = "",
    val percent: Double = 0.0,
    val enabled: Boolean = true,
    val updatedAt: Long = 0L
)

data class CommissionRules(
    val globalCommissionPercent: Double = 5.0,
    val globalRuleEnabled: Boolean = true,
    val allowLossMakingProducts: Boolean = false,
    val storeCommissions: Map<String, CommissionRuleEntry> = emptyMap(),
    val categoryCommissions: Map<String, CommissionRuleEntry> = emptyMap(),
    val productCommissions: Map<String, CommissionRuleEntry> = emptyMap(),
    val updatedAt: Long = 0L
)

data class OrderEarningsLedgerEntry(
    val orderId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val orderStatus: String = "PLACED",
    val paymentStatus: String = "",
    val ledgerStatus: String = "ACTIVE", // ACTIVE or REVERSED
    val grossAmount: Double = 0.0,
    val commissionPercentage: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val storeEarning: Double = 0.0,
    val purchaseCostTotal: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val reversedCommissionAmount: Double = 0.0,
    val reversedStoreEarning: Double = 0.0,
    val commissionRuleSource: String = CommissionRuleSource.GLOBAL.name,
    val commissionDeducted: Boolean = false,
    val reversalReason: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val reversedAt: Long = 0L
)

data class StoreSummaryReport(
    val storeId: String = "",
    val storeName: String = "",
    val totalOrders: Int = 0,
    val activeOrders: Int = 0,
    val reversedOrders: Int = 0,
    val grossSales: Double = 0.0,
    val totalChutkiCommission: Double = 0.0,
    val totalStoreEarning: Double = 0.0,
    val totalEstimatedProfit: Double = 0.0
)

data class DeliveryRider(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val isOnline: Boolean? = null,
    val lastSeen: Long = 0L,
    val lat: Double? = null,
    val lng: Double? = null
) {
    /**
     * Presence is considered reliable only when the backend explicitly reports an `isOnline` flag,
     * and if `lastSeen > 0`, it was updated within the last 10 minutes (or `lastSeen == 0L`).
     */
    fun isReliablyOnline(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val onlineFlag = isOnline ?: return false
        if (!onlineFlag) return false
        if (lastSeen <= 0L) return true
        return (nowMillis - lastSeen) <= 10 * 60 * 1000L
    }

    val hasReliablePresence: Boolean
        get() = isOnline != null
}

data class CustomerRecord(
    val uid: String = "",
    val blocked: Boolean = false
)

data class StorePayout(
    val payoutId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storePhone: String = "",
    val storeUpiId: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING",
    val adminRef: String = "",
    val createdAt: Long = 0L
)

data class StoreFeedback(
    val feedbackId: String = "",
    val role: String = "STORE",
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val adminReply: String = "",
    val createdAt: Long = 0L
)

data class CustomCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val active: Boolean = true
)

data class AdminSettings(
    val commissionPercent: Double = 5.0,
    val allowLossMakingProducts: Boolean = false,
    val minStorePayout: Double = 10.0,
    val maxStorePayout: Double = 50000.0,
    val allowStoreProductEdit: Boolean = true,
    val allowStoreQuickSeed: Boolean = true,
    val storeAnnouncement: String = "",
    val supportHours: String = "",
    val supportPhone: String = "",
    val supportWhatsapp: String = "",
    val customCategories: List<CustomCategory> = emptyList()
)

data class ProductSalesStat(
    val qty: Int = 0,
    val revenue: Double = 0.0
)

enum class PartnerTab {
    ORDERS, MENU, INVENTORY, REPORTS, PROFILE
}

enum class ToastKind {
    SUCCESS, DANGER, INFO, WARNING
}

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val kind: ToastKind = ToastKind.SUCCESS,
    val isKotAlert: Boolean = false
)

enum class OrderRiskLevel(val label: String) {
    LOW("Low Risk"),
    MEDIUM("Caution"),
    HIGH("High Risk"),
    CRITICAL("Critical Scam/Spam")
}

data class SecurityShieldConfig(
    val scamShieldEnabled: Boolean = true,
    val codVerificationLimit: Double = 1500.0,
    val velocityGuardEnabled: Boolean = true,
    val duplicateOrderGuardEnabled: Boolean = true,
    val upiChangeLockEnabled: Boolean = false,
    val strictLinkSanitizer: Boolean = true,
    val panicSafeModeActive: Boolean = false
)

data class OrderRiskAssessment(
    val orderId: String = "",
    val score: Int = 0,
    val level: OrderRiskLevel = OrderRiskLevel.LOW,
    val reasons: List<String> = emptyList(),
    val isDuplicateSuspect: Boolean = false,
    val isPhishingTextSuspect: Boolean = false
)

data class PhishingScanResult(
    val isThreat: Boolean = false,
    val riskScore: Int = 0,
    val level: OrderRiskLevel = OrderRiskLevel.LOW,
    val findings: List<String> = emptyList()
)

data class SecurityPosture(
    val overallScore: Int = 100,
    val runtimeTamperDetected: Boolean = false,
    val runtimeFindings: List<String> = emptyList(),
    val flaggedOrderCount: Int = 0,
    val criticalOrderCount: Int = 0
)

data class AuthSession(
    val uid: String? = null,
    val email: String = "",
    val isEmailVerified: Boolean = false,
    val isGoogleProvider: Boolean = false
) {
    val requiresEmailVerification: Boolean
        get() = !uid.isNullOrBlank() && !isGoogleProvider && !isEmailVerified

    val isFullyAuthenticated: Boolean
        get() = !uid.isNullOrBlank() && (isGoogleProvider || isEmailVerified)
}

data class InAppCallRecord(
    val callId: String = "",
    val tripId: String = "",
    val orderId: String = "",
    val callerRole: String = "RESTAURANT",
    val restaurantUid: String = "",
    val restaurantName: String = "",
    val riderUid: String = "",
    val riderName: String = "",
    val riderPhotoUrl: String = "",
    val status: String = "RINGING",
    val restaurantAnswered: Boolean = false,
    val isMuted: Boolean = false,
    val isOnHold: Boolean = false,
    val startedAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val isActiveOrRinging: Boolean
        get() = status.equals("RINGING", ignoreCase = true) ||
            status.equals("CONNECTING", ignoreCase = true) ||
            status.equals("CONNECTED", ignoreCase = true)

    val isTerminal: Boolean
        get() = status.equals("DECLINED", ignoreCase = true) ||
            status.equals("ENDED", ignoreCase = true) ||
            status.equals("MISSED", ignoreCase = true) ||
            status.equals("BUSY", ignoreCase = true) ||
            status.equals("TIMEOUT", ignoreCase = true) ||
            status.equals("FAILED", ignoreCase = true)
}

data class WebRtcSessionDescriptionData(
    val type: String = "",
    val sdp: String = ""
)

data class WebRtcIceCandidateData(
    val candidateId: String = "",
    val candidate: String = "",
    val sdpMid: String = "0",
    val sdpMLineIndex: Int = 0
)

data class CallUiSessionState(
    val callId: String = "",
    val orderId: String = "",
    val riderUid: String = "",
    val riderName: String = "",
    val callerRole: String = "RESTAURANT",
    val status: String = "IDLE", // IDLE, RINGING, CONNECTING, CONNECTED, ENDED, DECLINED
    val restaurantAnswered: Boolean = false,
    val isIncoming: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    val startedAt: Long = 0L,
    val connectedAt: Long = 0L,
    val durationSeconds: Long = 0L,
    val errorBanner: String = ""
) {
    val isActiveOrRinging: Boolean
        get() = status.equals("RINGING", ignoreCase = true) ||
            status.equals("CONNECTING", ignoreCase = true) ||
            status.equals("CONNECTED", ignoreCase = true)

    fun toRecord(): InAppCallRecord = InAppCallRecord(
        callId = callId,
        orderId = orderId,
        riderUid = riderUid,
        riderName = riderName,
        callerRole = callerRole,
        status = status,
        restaurantAnswered = restaurantAnswered,
        isMuted = isMuted,
        isOnHold = isOnHold
    )
}

data class TripChatMessage(
    val messageId: String = "",
    val orderId: String = "",
    val senderUid: String = "",
    val senderRole: String = "RESTAURANT",
    val receiverUid: String = "",
    val messageType: String = "TEXT",
    val message: String = "",
    val timestamp: Long = 0L,
    val status: String = "SENT"
)


