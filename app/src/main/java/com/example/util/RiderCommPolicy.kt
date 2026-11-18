package com.example.util

import com.example.model.InAppCallRecord
import com.example.model.StoreOrder
import com.example.model.TripChatMessage
import java.util.Locale

object RiderCommPolicy {

    const val RINGING_WINDOW_MS = 45_000L
    const val MAX_CHAT_MESSAGE_LENGTH = 500

    val QUICK_CHAT_TEMPLATES = listOf(
        "Order Ready for Pickup",
        "Please contact restaurant staff.",
        "Your order is ready for pickup.",
        "Order is packed and waiting at the counter."
    )

    private val ACTIVE_COMMUNICATION_STAGES = setOf(
        "ACCEPTED_BY_RIDER",
        "ACCEPTED",
        "PREPARING",
        "PACKING",
        "READY_FOR_PICKUP",
        "PICKED_UP",
        "OUT_FOR_DELIVERY"
    )

    private val TERMINAL_ORDER_STAGES = setOf(
        "DELIVERED",
        "COMPLETED",
        "CANCELLED",
        "CANCELED",
        "REFUNDED",
        "REJECTED"
    )

    /**
     * Checks whether the order is in an active rider delivery stage where In-App Calling
     * and Real-Time Chat are permitted.
     */
    fun isCommunicationStageActive(status: String?): Boolean {
        val normalized = (status ?: "").trim().uppercase(Locale.US)
        if (normalized.isEmpty() || normalized in TERMINAL_ORDER_STAGES) return false
        return normalized in ACTIVE_COMMUNICATION_STAGES
    }

    fun isTerminalOrderStage(status: String?): Boolean {
        val normalized = (status ?: "").trim().uppercase(Locale.US)
        return normalized in TERMINAL_ORDER_STAGES
    }

    /**
     * Validates whether the given [order] is eligible for Restaurant <-> Rider communication:
     * - Order belongs to [authenticatedRestaurantUid]
     * - Rider UID ([StoreOrder.deliveryUid]) is assigned and non-blank
     * - Order status is in an active delivery stage
     */
    fun canCommunicateWithRider(
        order: StoreOrder?,
        authenticatedRestaurantUid: String?
    ): Boolean {
        if (order == null) return false
        val restUid = (authenticatedRestaurantUid ?: "").trim()
        if (restUid.isEmpty()) return false
        if (order.orderId.isBlank()) return false
        if (order.restaurantId != restUid) return false
        if (order.deliveryUid.isBlank()) return false
        return isCommunicationStageActive(order.status)
    }

    /**
     * Validates all preconditions before initiating an outgoing Restaurant -> Rider call.
     * Returns null if valid, or a user-facing error message if blocked.
     */
    fun validateOutgoingCall(
        isAuthenticated: Boolean,
        authenticatedRestaurantUid: String,
        order: StoreOrder?,
        existingLocalActiveCall: InAppCallRecord?,
        existingRemoteOrderCall: InAppCallRecord?,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (!isAuthenticated || authenticatedRestaurantUid.isBlank()) {
            return "Authentication required before placing an in-app call."
        }
        if (order == null || order.orderId.isBlank()) {
            return "Order not found."
        }
        if (order.restaurantId != authenticatedRestaurantUid) {
            return "Unauthorized: Order does not belong to this restaurant."
        }
        if (order.deliveryUid.isBlank()) {
            return "No rider is assigned to this order yet."
        }
        if (!isCommunicationStageActive(order.status)) {
            return "In-app calling is closed for ${order.status.lowercase(Locale.US)} orders."
        }
        if (existingLocalActiveCall != null && existingLocalActiveCall.isActiveOrRinging) {
            return "Another in-app voice call is already active."
        }
        if (existingRemoteOrderCall != null &&
            existingRemoteOrderCall.isActiveOrRinging &&
            (nowMillis - existingRemoteOrderCall.updatedAt.coerceAtLeast(existingRemoteOrderCall.startedAt)) <= RINGING_WINDOW_MS
        ) {
            return "A call is already active on this order."
        }
        return null
    }

    /**
     * Validates whether an incoming call snapshot from `restaurantCallInbox/{restaurantUid}`
     * should trigger the incoming call popup:
     * - callerRole == "RIDER"
     * - status == "RINGING"
     * - restaurantUid matches [authenticatedRestaurantUid]
     * - orderId belongs to [authorizedOrders] of that restaurant
     * - updatedAt is within [RINGING_WINDOW_MS]
     */
    fun isValidIncomingRiderCall(
        call: InAppCallRecord?,
        authenticatedRestaurantUid: String,
        authorizedOrders: List<StoreOrder>,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (call == null) return false
        if (authenticatedRestaurantUid.isBlank()) return false
        if (call.callId.isBlank() || call.orderId.isBlank() || call.riderUid.isBlank()) return false
        if (!call.callerRole.equals("RIDER", ignoreCase = true)) return false
        if (!call.status.equals("RINGING", ignoreCase = true)) return false
        if (call.restaurantUid != authenticatedRestaurantUid) return false

        val matchingOrder = authorizedOrders.firstOrNull { it.orderId == call.orderId } ?: return false
        if (matchingOrder.restaurantId != authenticatedRestaurantUid) return false
        if (matchingOrder.deliveryUid.isNotBlank() && matchingOrder.deliveryUid != call.riderUid) return false
        if (!isCommunicationStageActive(matchingOrder.status)) return false

        val referenceTime = if (call.updatedAt > 0L) call.updatedAt else call.startedAt
        if (referenceTime <= 0L) return false
        val ageMs = nowMillis - referenceTime
        return ageMs in -10_000L..RINGING_WINDOW_MS
    }

    /**
     * Builds the canonical RTDB JSON map for `inAppCalls/{orderId}`, `riderCallInbox/{riderUid}`,
     * and `restaurantCallInbox/{restaurantUid}` when Restaurant initiates a call.
     */
    fun buildOutgoingCallPayload(
        order: StoreOrder,
        restaurantUid: String,
        restaurantName: String,
        riderUid: String,
        riderName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): InAppCallRecord {
        val cleanCallId = "call_$nowMillis"
        val cleanTripId = order.effectiveTripId
        return InAppCallRecord(
            callId = cleanCallId,
            tripId = cleanTripId,
            orderId = order.orderId,
            callerRole = "RESTAURANT",
            restaurantUid = restaurantUid,
            restaurantName = StoreUtils.sanitizeText(restaurantName).ifBlank { "Restaurant" },
            riderUid = riderUid,
            riderName = StoreUtils.sanitizeText(riderName).ifBlank { "Delivery Rider" },
            status = "RINGING",
            restaurantAnswered = false,
            isMuted = false,
            isOnHold = false,
            startedAt = nowMillis,
            updatedAt = nowMillis
        )
    }

    fun toCallMap(record: InAppCallRecord): Map<String, Any> = mapOf(
        "callId" to record.callId,
        "tripId" to record.tripId.ifBlank { record.orderId },
        "orderId" to record.orderId,
        "callerRole" to record.callerRole,
        "restaurantUid" to record.restaurantUid,
        "restaurantName" to record.restaurantName,
        "riderUid" to record.riderUid,
        "riderName" to record.riderName,
        "status" to record.status,
        "restaurantAnswered" to record.restaurantAnswered,
        "isMuted" to record.isMuted,
        "isOnHold" to record.isOnHold,
        "startedAt" to record.startedAt,
        "updatedAt" to record.updatedAt
    )

    /**
     * Filters `tripChats/{orderId}/messages` so Restaurant Staff ONLY see messages exchanged
     * between RESTAURANT and RIDER for this order, and NEVER see private Customer <-> Rider messages.
     */
    fun isRestaurantRiderMessage(
        message: TripChatMessage,
        restaurantUid: String,
        riderUid: String,
        customerUid: String = ""
    ): Boolean {
        val role = message.senderRole.trim().uppercase(Locale.US)
        if (role != "RESTAURANT" && role != "RIDER") return false

        // Reject any message involving the customer UID
        if (customerUid.isNotBlank() &&
            (message.senderUid == customerUid || message.receiverUid == customerUid)
        ) {
            return false
        }

        return when (role) {
            "RESTAURANT" -> {
                (restaurantUid.isBlank() || message.senderUid == restaurantUid) &&
                    (riderUid.isBlank() || message.receiverUid.isBlank() || message.receiverUid == riderUid || message.receiverUid == "RIDER")
            }
            "RIDER" -> {
                (riderUid.isBlank() || message.senderUid == riderUid) &&
                    (restaurantUid.isBlank() || message.receiverUid.isBlank() || message.receiverUid == restaurantUid || message.receiverUid == "RESTAURANT")
            }
            else -> false
        }
    }

    /**
     * Formats connected call duration (in seconds) into MM:SS.
     */
    fun formatCallDuration(seconds: Long): String {
        val safeSec = seconds.coerceAtLeast(0L)
        val mins = safeSec / 60L
        val remSec = safeSec % 60L
        return String.format(Locale.US, "%02d:%02d", mins, remSec)
    }

    /**
     * Redacts raw phone numbers in any free-form string to preserve participant privacy.
     */
    fun redactPhoneNumbers(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.replace(Regex("(?<!\\d)(?:\\+?91[\\s-]?)?[6-9]\\d{9}(?!\\d)"), "[Hidden Phone]")
    }

    /**
     * Returns a privacy-preserving delivery locality summary without exposing full private house/flat details
     * when not required.
     */
    fun formatPrivacySafeAddress(rawAddress: String?): String {
        val clean = redactPhoneNumbers(rawAddress).trim()
        if (clean.isBlank()) return "Gossaigaon Delivery Zone"
        val parts = clean.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return if (parts.size >= 2) {
            parts.takeLast(2).joinToString(", ")
        } else {
            clean
        }
    }
}
