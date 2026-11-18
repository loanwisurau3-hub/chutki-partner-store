package com.example.data

import com.example.model.AdminSettings
import com.example.model.AuthSession
import com.example.model.CommissionRuleEntry
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.CustomCategory
import com.example.model.CustomerRecord
import com.example.model.DeliveryRider
import com.example.model.InAppCallRecord
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.OrderItem
import com.example.model.ProductItem
import com.example.model.StoreFeedback
import com.example.model.StoreOrder
import com.example.model.StorePayout
import com.example.model.StoreProfile
import com.example.model.TripChatMessage
import com.example.model.WebRtcIceCandidateData
import com.example.model.WebRtcSessionDescriptionData
import com.example.util.EarningsCalculator
import com.example.util.RiderCommPolicy
import com.example.util.SecurityGuard
import com.example.util.StoreUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await

interface StoreDataSource {
    val authUidFlow: Flow<String?>
    val authSessionFlow: Flow<AuthSession>
        get() = authUidFlow.map { uid ->
            AuthSession(uid = uid, isEmailVerified = !uid.isNullOrBlank())
        }
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthSession> =
        Result.failure(UnsupportedOperationException("Google Sign-In is unavailable"))
    suspend fun sendEmailVerification(): Result<Unit> = Result.success(Unit)
    suspend fun reloadEmailVerification(): Result<AuthSession> =
        Result.failure(UnsupportedOperationException("Email verification refresh is unavailable"))
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    suspend fun registerStore(
        name: String,
        email: String,
        cuisine: String,
        address: String,
        phone: String,
        upiId: String,
        password: String,
        lat: Double,
        lng: Double
    ): Result<String>
    fun signOut()
    fun observeStoreProfile(uid: String): Flow<StoreProfile>
    fun observeAllStores(): Flow<Map<String, StoreProfile>>
    fun observeStoreOrders(uid: String): Flow<List<StoreOrder>>
    fun observeIncomingKotOrders(uid: String): Flow<StoreOrder>
    fun observeAllOrders(): Flow<List<StoreOrder>>
    fun observeStoreProducts(uid: String): Flow<List<ProductItem>>
    fun observeDeliveryRiders(): Flow<Map<String, DeliveryRider>>
    fun observeCustomers(): Flow<Map<String, CustomerRecord>>
    fun observeAdminSettings(): Flow<AdminSettings>
    fun observeCommissionRules(): Flow<CommissionRules>
    fun observeStoreEarningsLedger(uid: String): Flow<Map<String, OrderEarningsLedgerEntry>>
    fun observeAllEarningsLedger(): Flow<Map<String, OrderEarningsLedgerEntry>>
    fun observeStorePayouts(uid: String): Flow<List<StorePayout>>
    fun observeStoreFeedbacks(uid: String): Flow<List<StoreFeedback>>
    suspend fun toggleStoreOpen(uid: String, isOpen: Boolean): Result<Unit>
    suspend fun updateStoreProfile(uid: String, updates: Map<String, Any>): Result<Unit>
    suspend fun updateOrderStatus(orderId: String, newStatus: String, pickupOtpIfNeeded: String? = null): Result<Unit>
    suspend fun notifyRiderOrderReady(
        order: StoreOrder,
        restaurantName: String,
        pickupOtp: String
    ): Result<Unit> = Result.success(Unit)
    suspend fun rejectOrder(orderId: String, reason: String): Result<Unit>
    suspend fun reportOrderSpam(
        orderId: String,
        storeId: String,
        customerUid: String,
        phone: String,
        reason: String,
        riskScore: Int
    ): Result<Unit>
    suspend fun clearOrderSpamFlag(orderId: String): Result<Unit>
    suspend fun syncOrderEarningsLedger(entry: OrderEarningsLedgerEntry): Result<Unit>
    suspend fun saveGlobalCommissionSettings(
        globalPercent: Double,
        globalRuleEnabled: Boolean,
        allowLossMakingProducts: Boolean
    ): Result<Unit>
    suspend fun saveStoreCommissionRule(storeId: String, storeName: String, percent: Double, enabled: Boolean): Result<Unit>
    suspend fun deleteStoreCommissionRule(storeId: String): Result<Unit>
    suspend fun saveCategoryCommissionRule(categoryId: String, categoryName: String, percent: Double, enabled: Boolean): Result<Unit>
    suspend fun deleteCategoryCommissionRule(categoryId: String): Result<Unit>
    suspend fun saveProductCommissionRule(
        productId: String,
        storeId: String,
        productName: String,
        percent: Double,
        enabled: Boolean
    ): Result<Unit>
    suspend fun deleteProductCommissionRule(productId: String): Result<Unit>
    suspend fun saveProduct(uid: String, itemId: String?, itemData: Map<String, Any>): Result<Unit>
    suspend fun toggleProductStock(uid: String, itemId: String, available: Boolean): Result<Unit>
    suspend fun deleteProduct(uid: String, itemId: String): Result<Unit>
    suspend fun setInventoryApproval(uid: String, itemId: String, approved: Boolean): Result<Unit>
    suspend fun setInventoryFlag(uid: String, itemId: String, flagged: Boolean, reason: String): Result<Unit>
    suspend fun updateProductStockQty(uid: String, itemId: String, stockQty: Int): Result<Unit>
    suspend fun quickSeedProducts(uid: String, rules: CommissionRules): Result<Unit>
    suspend fun requestPayout(payout: Map<String, Any>): Result<Unit>
    suspend fun submitFeedback(feedback: Map<String, Any>): Result<Unit>

    // In-App Voice Call & WebRTC Signaling
    fun observeRestaurantCallInbox(restaurantUid: String): Flow<InAppCallRecord?> = flowOf(null)
    fun observeOrderCall(orderId: String): Flow<InAppCallRecord?> = flowOf(null)
    suspend fun getOrderCallSnapshot(orderId: String): Result<InAppCallRecord?> = Result.success(null)
    suspend fun startOutgoingCall(call: InAppCallRecord): Result<Unit> = Result.success(Unit)
    suspend fun updateCallStatus(
        orderId: String,
        restaurantUid: String,
        riderUid: String,
        callId: String,
        status: String,
        restaurantAnswered: Boolean,
        isMuted: Boolean? = null,
        isOnHold: Boolean? = null
    ): Result<Unit> = Result.success(Unit)
    suspend fun writeWebRtcOffer(orderId: String, offer: WebRtcSessionDescriptionData): Result<Unit> = Result.success(Unit)
    suspend fun writeWebRtcAnswer(orderId: String, answer: WebRtcSessionDescriptionData): Result<Unit> = Result.success(Unit)
    suspend fun readWebRtcOffer(orderId: String): Result<WebRtcSessionDescriptionData?> = Result.success(null)
    fun observeWebRtcOffer(orderId: String): Flow<WebRtcSessionDescriptionData?> = flowOf(null)
    fun observeWebRtcAnswer(orderId: String): Flow<WebRtcSessionDescriptionData?> = flowOf(null)
    suspend fun pushWebRtcIceCandidate(
        orderId: String,
        isCaller: Boolean,
        candidate: WebRtcIceCandidateData
    ): Result<Unit> = Result.success(Unit)
    fun observeWebRtcIceCandidates(
        orderId: String,
        observeCallerCandidates: Boolean
    ): Flow<WebRtcIceCandidateData> = emptyFlow()

    // Restaurant <-> Rider Order Chat
    fun observeOrderChatMessages(
        orderId: String,
        restaurantUid: String,
        riderUid: String,
        customerUid: String = ""
    ): Flow<List<TripChatMessage>> = flowOf(emptyList())
    suspend fun sendOrderChatMessage(
        orderId: String,
        restaurantUid: String,
        riderUid: String,
        messageText: String,
        existingMessageId: String? = null
    ): Result<TripChatMessage> = Result.success(
        TripChatMessage(
            messageId = existingMessageId ?: "msg_${System.currentTimeMillis()}",
            orderId = orderId,
            senderUid = restaurantUid,
            senderRole = "RESTAURANT",
            receiverUid = riderUid,
            messageType = "TEXT",
            message = messageText,
            timestamp = System.currentTimeMillis(),
            status = "SENT"
        )
    )
    suspend fun updateChatMessageStatus(
        orderId: String,
        messageId: String,
        status: String
    ): Result<Unit> = Result.success(Unit)
}

class StoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
) : StoreDataSource {
    private val sessionUpdates = MutableSharedFlow<AuthSession>(extraBufferCapacity = 4)

    private fun currentAuthSession(): AuthSession {
        val user = auth.currentUser ?: return AuthSession()
        val isGoogle = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        return AuthSession(
            uid = user.uid,
            email = user.email.orEmpty(),
            isEmailVerified = user.isEmailVerified || isGoogle,
            isGoogleProvider = isGoogle
        )
    }

    override val authUidFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private val firebaseAuthSessionFlow: Flow<AuthSession> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(currentAuthSession())
        }
        auth.addAuthStateListener(listener)
        trySend(currentAuthSession())
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val authSessionFlow: Flow<AuthSession> =
        merge(firebaseAuthSessionFlow, sessionUpdates).distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val res = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("Authentication failed")
        runCatching { user.reload().await() }
        sessionUpdates.tryEmit(currentAuthSession())
        user.uid
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthSession> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val res = auth.signInWithCredential(credential).await()
        val user = res.user ?: error("Google Sign-In failed")
        val restaurantRef = db.getReference("restaurants/${user.uid}")
        val existingResult = runCatching { restaurantRef.get().await() }
        val existing = existingResult.getOrNull()
        val hasProfile = existing?.exists() == true &&
            existing.child("name").getValue(String::class.java).orEmpty().isNotBlank()
        if (existingResult.isSuccess && !hasProfile) {
            val defaultName = StoreUtils.sanitizeText(user.displayName.orEmpty())
                .ifBlank {
                    user.email.orEmpty().substringBefore("@").ifBlank { "Partner Store" }
                }
            val starterProfile = mapOf(
                "name" to defaultName,
                "email" to user.email.orEmpty().trim(),
                "cuisine" to "Grocery & Daily Needs",
                "address" to "Gossaigaon",
                "phone" to user.phoneNumber.orEmpty().filter(Char::isDigit).takeLast(10),
                "upiId" to "",
                "isOpen" to true,
                "hours" to "7:00 AM - 9:00 PM",
                "imageUrl" to StoreUtils.DEFAULT_STORE_BANNER,
                "approved" to false,
                "lat" to 26.4524,
                "lng" to 89.9723,
                "locationLink" to "https://www.google.com/maps?q=26.4524,89.9723"
            )
            runCatching { restaurantRef.setValue(starterProfile).await() }
        }
        val session = currentAuthSession().copy(isEmailVerified = true, isGoogleProvider = true)
        sessionUpdates.tryEmit(session)
        session
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Please sign in first.")
        user.sendEmailVerification().await()
    }

    override suspend fun reloadEmailVerification(): Result<AuthSession> = runCatching {
        val user = auth.currentUser ?: error("No active login session.")
        user.reload().await()
        val session = currentAuthSession()
        sessionUpdates.tryEmit(session)
        session
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun registerStore(
        name: String,
        email: String,
        cuisine: String,
        address: String,
        phone: String,
        upiId: String,
        password: String,
        lat: Double,
        lng: Double
    ): Result<String> = runCatching {
        val res = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("Registration failed")
        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name).build()
            ).await()
        }
        val payload = mapOf(
            "name" to name,
            "email" to email.trim(),
            "cuisine" to cuisine,
            "address" to address,
            "phone" to phone,
            "upiId" to upiId,
            "isOpen" to true,
            "hours" to "7:00 AM - 9:00 PM",
            "imageUrl" to StoreUtils.DEFAULT_STORE_BANNER,
            "approved" to false,
            "lat" to lat,
            "lng" to lng,
            "locationLink" to "https://www.google.com/maps?q=$lat,$lng"
        )
        db.getReference("restaurants/${user.uid}").setValue(payload).await()
        runCatching { user.sendEmailVerification().await() }
        sessionUpdates.tryEmit(currentAuthSession())
        user.uid
    }

    override fun signOut() {
        auth.signOut()
        sessionUpdates.tryEmit(AuthSession())
    }

    override fun observeStoreProfile(uid: String): Flow<StoreProfile> = callbackFlow {
        val ref = db.getReference("restaurants/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toStoreProfile())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeAllStores(): Flow<Map<String, StoreProfile>> = callbackFlow {
        val ref = db.getReference("restaurants")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = buildMap {
                    snapshot.children.forEach { child ->
                        val key = child.key ?: return@forEach
                        put(key, child.toStoreProfile())
                    }
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeStoreOrders(uid: String): Flow<List<StoreOrder>> = callbackFlow {
        val ref = db.getReference("orders")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    val restId = child.str("restaurantId")
                    if (restId == uid) child.toStoreOrder() else null
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Real-time Firebase RTDB ChildEventListener for newly arriving KOT orders for [uid].
     * Ignores initial historical snapshot children so alerts only trigger for orders added
     * (or UPI orders newly verified) after the listener connects.
     */
    override fun observeIncomingKotOrders(uid: String): Flow<StoreOrder> = callbackFlow {
        val query = db.getReference("orders")
            .orderByChild("restaurantId")
            .equalTo(uid)
        var initialSyncDone = false
        val notifiedOrderIds = mutableSetOf<String>()

        val childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val order = snapshot.toStoreOrder() ?: return
                if (!initialSyncDone) {
                    if (StoreUtils.isActionableKotOrder(order)) {
                        notifiedOrderIds.add(order.orderId)
                    }
                    return
                }
                if (StoreUtils.isActionableKotOrder(order) && notifiedOrderIds.add(order.orderId)) {
                    trySend(order)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val order = snapshot.toStoreOrder() ?: return
                if (initialSyncDone &&
                    StoreUtils.isActionableKotOrder(order) &&
                    notifiedOrderIds.add(order.orderId)
                ) {
                    trySend(order)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.key?.let { notifiedOrderIds.remove(it) }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
        }

        val initialLoadListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.mapNotNull { it.toStoreOrder() }.forEach { order ->
                    if (StoreUtils.isActionableKotOrder(order)) {
                        notifiedOrderIds.add(order.orderId)
                    }
                }
                initialSyncDone = true
            }

            override fun onCancelled(error: DatabaseError) {
                initialSyncDone = true
            }
        }

        query.addChildEventListener(childListener)
        query.addListenerForSingleValueEvent(initialLoadListener)
        awaitClose {
            query.removeEventListener(childListener)
            query.removeEventListener(initialLoadListener)
        }
    }

    override fun observeAllOrders(): Flow<List<StoreOrder>> = callbackFlow {
        val ref = db.getReference("orders")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.toStoreOrder() }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeStoreProducts(uid: String): Flow<List<ProductItem>> = callbackFlow {
        val ref = db.getReference("menus/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.toProductItem() }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeDeliveryRiders(): Flow<Map<String, DeliveryRider>> = callbackFlow {
        val ref = db.getReference("deliveryPartners")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = buildMap {
                    snapshot.children.forEach { child ->
                        val key = child.key ?: return@forEach
                        val rawPhoto = child.str("photoUrl")
                            .ifBlank { child.str("avatarUrl") }
                            .ifBlank { child.str("imageUrl") }
                            .ifBlank { child.str("profilePic") }
                        val onlineFlag = child.boolOrNull("isOnline")
                            ?: child.boolOrNull("online")
                            ?: child.child("presence").boolOrNull("isOnline")
                        val lastSeenTs = child.lng("lastSeen", 0L).takeIf { it > 0L }
                            ?: child.child("presence").lng("lastSeen", 0L)
                        put(
                            key,
                            DeliveryRider(
                                uid = key,
                                name = child.str("name"),
                                phone = child.str("phone"),
                                photoUrl = StoreUtils.cleanAndResolveImageUrl(rawPhoto),
                                isOnline = onlineFlag,
                                lastSeen = lastSeenTs,
                                lat = child.dblOrNull("lat"),
                                lng = child.dblOrNull("lng")
                            )
                        )
                    }
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeCustomers(): Flow<Map<String, CustomerRecord>> = callbackFlow {
        val ref = db.getReference("customers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = buildMap {
                    snapshot.children.forEach { child ->
                        val key = child.key ?: return@forEach
                        put(key, CustomerRecord(uid = key, blocked = child.bool("blocked", false)))
                    }
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeAdminSettings(): Flow<AdminSettings> = callbackFlow {
        val ref = db.getReference("admin/settings")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toAdminSettings())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeCommissionRules(): Flow<CommissionRules> = callbackFlow {
        val ref = db.getReference("admin/commissionRules")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toCommissionRules())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeStoreEarningsLedger(uid: String): Flow<Map<String, OrderEarningsLedgerEntry>> = callbackFlow {
        val ref = db.getReference("earningsLedger")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = buildMap {
                    snapshot.children.forEach { child ->
                        val entry = child.toLedgerEntry() ?: return@forEach
                        if (entry.storeId == uid) {
                            put(entry.orderId, entry)
                        }
                    }
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeAllEarningsLedger(): Flow<Map<String, OrderEarningsLedgerEntry>> = callbackFlow {
        val ref = db.getReference("earningsLedger")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = buildMap {
                    snapshot.children.forEach { child ->
                        val entry = child.toLedgerEntry() ?: return@forEach
                        put(entry.orderId, entry)
                    }
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeStorePayouts(uid: String): Flow<List<StorePayout>> = callbackFlow {
        val ref = db.getReference("storePayouts")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    if (child.str("storeId") == uid) {
                        StorePayout(
                            payoutId = child.key.orEmpty(),
                            storeId = child.str("storeId"),
                            storeName = child.str("storeName"),
                            storePhone = child.str("storePhone"),
                            storeUpiId = child.str("storeUpiId"),
                            amount = child.dbl("amount"),
                            status = child.str("status").ifBlank { "PENDING" },
                            adminRef = child.str("adminRef"),
                            createdAt = child.lng("createdAt")
                        )
                    } else null
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeStoreFeedbacks(uid: String): Flow<List<StoreFeedback>> = callbackFlow {
        val ref = db.getReference("feedbacks")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    if (child.str("uid") == uid) {
                        StoreFeedback(
                            feedbackId = child.key.orEmpty(),
                            role = child.str("role").ifBlank { "STORE" },
                            uid = child.str("uid"),
                            name = child.str("name"),
                            phone = child.str("phone"),
                            subject = child.str("subject"),
                            message = child.str("message"),
                            status = child.str("status").ifBlank { "OPEN" },
                            adminReply = child.str("adminReply"),
                            createdAt = child.lng("createdAt")
                        )
                    } else null
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun toggleStoreOpen(uid: String, isOpen: Boolean): Result<Unit> = runCatching {
        db.getReference("restaurants/$uid").updateChildren(mapOf("isOpen" to isOpen)).await()
    }

    override suspend fun updateStoreProfile(uid: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        db.getReference("restaurants/$uid").updateChildren(updates).await()
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String, pickupOtpIfNeeded: String?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis()
        )
        if (!pickupOtpIfNeeded.isNullOrBlank()) {
            updates["pickupOtp"] = pickupOtpIfNeeded
        }
        db.getReference("orders/$orderId").updateChildren(updates).await()
    }

    override suspend fun notifyRiderOrderReady(
        order: StoreOrder,
        restaurantName: String,
        pickupOtp: String
    ): Result<Unit> = runCatching {
        val safeRiderUid = SecurityGuard.sanitizeFirebaseKey(order.deliveryUid)
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(order.orderId)
        if (safeRiderUid.isBlank() || safeOrderId.isBlank()) return@runCatching
        val now = System.currentTimeMillis()
        val payload = mapOf<String, Any>(
            "orderId" to safeOrderId,
            "tripId" to order.effectiveTripId,
            "restaurantId" to order.restaurantId,
            "restaurantName" to StoreUtils.sanitizeText(restaurantName).ifBlank { "Restaurant" },
            "riderUid" to safeRiderUid,
            "status" to "READY_FOR_PICKUP",
            "requiresPickupCodeVerification" to true,
            "updatedAt" to now
        )
        db.getReference("riderPickupAlerts/$safeRiderUid/$safeOrderId").updateChildren(payload).await()
    }

    override suspend fun rejectOrder(orderId: String, reason: String): Result<Unit> = runCatching {
        val updates = mapOf<String, Any>(
            "status" to "CANCELLED",
            "cancelledBy" to "STORE",
            "cancelReason" to reason,
            "updatedAt" to System.currentTimeMillis()
        )
        db.getReference("orders/$orderId").updateChildren(updates).await()
    }

    override suspend fun reportOrderSpam(
        orderId: String,
        storeId: String,
        customerUid: String,
        phone: String,
        reason: String,
        riskScore: Int
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        val cleanReason = StoreUtils.sanitizeText(reason).ifBlank { "Suspected Scam / Spam Order" }
        if (safeOrderId.isNotBlank()) {
            db.getReference("orders/$safeOrderId").updateChildren(
                mapOf<String, Any>(
                    "isSpamSuspect" to true,
                    "status" to "CANCELLED",
                    "cancelledBy" to "STORE_SECURITY",
                    "cancelReason" to cleanReason,
                    "fraudRiskScore" to riskScore,
                    "updatedAt" to now
                )
            ).await()
        }
        val alertPayload = mapOf<String, Any>(
            "orderId" to safeOrderId,
            "storeId" to SecurityGuard.sanitizeFirebaseKey(storeId),
            "customerUid" to SecurityGuard.sanitizeFirebaseKey(customerUid),
            "phone" to phone.filter(Char::isDigit).takeLast(10),
            "reason" to cleanReason,
            "riskScore" to riskScore.coerceIn(0, 100),
            "reportedAt" to now
        )
        db.getReference("securityAlerts").push().setValue(alertPayload).await()
    }

    override suspend fun clearOrderSpamFlag(orderId: String): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isNotBlank()) {
            db.getReference("orders/$safeOrderId").updateChildren(
                mapOf<String, Any>(
                    "isSpamSuspect" to false,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    override suspend fun syncOrderEarningsLedger(entry: OrderEarningsLedgerEntry): Result<Unit> = runCatching {
        if (entry.orderId.isBlank()) return@runCatching
        val payload = mapOf<String, Any>(
            "orderId" to entry.orderId,
            "storeId" to entry.storeId,
            "storeName" to entry.storeName,
            "orderStatus" to entry.orderStatus,
            "paymentStatus" to entry.paymentStatus,
            "ledgerStatus" to entry.ledgerStatus,
            "grossAmount" to entry.grossAmount,
            "commissionPercentage" to entry.commissionPercentage,
            "commissionAmount" to entry.commissionAmount,
            "storeEarning" to entry.storeEarning,
            "purchaseCostTotal" to entry.purchaseCostTotal,
            "estimatedProfit" to entry.estimatedProfit,
            "reversedCommissionAmount" to entry.reversedCommissionAmount,
            "reversedStoreEarning" to entry.reversedStoreEarning,
            "commissionRuleSource" to entry.commissionRuleSource,
            "commissionDeducted" to entry.commissionDeducted,
            "reversalReason" to entry.reversalReason,
            "createdAt" to entry.createdAt,
            "updatedAt" to entry.updatedAt,
            "reversedAt" to entry.reversedAt
        )
        db.getReference("earningsLedger/${entry.orderId}").updateChildren(payload).await()
    }

    override suspend fun saveGlobalCommissionSettings(
        globalPercent: Double,
        globalRuleEnabled: Boolean,
        allowLossMakingProducts: Boolean
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val roundedPct = EarningsCalculator.round2(globalPercent.coerceIn(0.0, 100.0))
        val ruleUpdates = mapOf<String, Any>(
            "globalCommissionPercent" to roundedPct,
            "globalRuleEnabled" to globalRuleEnabled,
            "allowLossMakingProducts" to allowLossMakingProducts,
            "updatedAt" to now
        )
        db.getReference("admin/commissionRules").updateChildren(ruleUpdates).await()
        db.getReference("admin/settings").updateChildren(
            mapOf<String, Any>(
                "commissionPercent" to roundedPct,
                "allowLossMakingProducts" to allowLossMakingProducts
            )
        ).await()
    }

    override suspend fun saveStoreCommissionRule(
        storeId: String,
        storeName: String,
        percent: Double,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val cleanId = storeId.trim()
        require(cleanId.isNotEmpty()) { "Store ID is required" }
        val payload = mapOf<String, Any>(
            "targetId" to cleanId,
            "targetLabel" to storeName.ifBlank { cleanId },
            "storeId" to cleanId,
            "percent" to EarningsCalculator.round2(percent.coerceIn(0.0, 100.0)),
            "enabled" to enabled,
            "updatedAt" to now
        )
        db.getReference("admin/commissionRules/storeCommissions/$cleanId").setValue(payload).await()
        db.getReference("admin/commissionRules/updatedAt").setValue(now).await()
    }

    override suspend fun deleteStoreCommissionRule(storeId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        db.getReference("admin/commissionRules/storeCommissions/${storeId.trim()}").removeValue().await()
        db.getReference("admin/commissionRules/updatedAt").setValue(now).await()
    }

    override suspend fun saveCategoryCommissionRule(
        categoryId: String,
        categoryName: String,
        percent: Double,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val cleanId = categoryId.trim().uppercase()
        require(cleanId.isNotEmpty()) { "Category ID is required" }
        val payload = mapOf<String, Any>(
            "targetId" to cleanId,
            "targetLabel" to categoryName.ifBlank { cleanId },
            "percent" to EarningsCalculator.round2(percent.coerceIn(0.0, 100.0)),
            "enabled" to enabled,
            "updatedAt" to now
        )
        db.getReference("admin/commissionRules/categoryCommissions/$cleanId").setValue(payload).await()
        db.getReference("admin/commissionRules/updatedAt").setValue(now).await()
    }

    override suspend fun deleteCategoryCommissionRule(categoryId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        db.getReference("admin/commissionRules/categoryCommissions/${categoryId.trim().uppercase()}").removeValue().await()
        db.getReference("admin/commissionRules/updatedAt").setValue(now).await()
    }

    override suspend fun saveProductCommissionRule(
        productId: String,
        storeId: String,
        productName: String,
        percent: Double,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val cleanId = productId.trim()
        require(cleanId.isNotEmpty()) { "Product ID is required" }
        val payload = mapOf<String, Any>(
            "targetId" to cleanId,
            "targetLabel" to productName.ifBlank { cleanId },
            "storeId" to storeId.trim(),
            "percent" to EarningsCalculator.round2(percent.coerceIn(0.0, 100.0)),
            "enabled" to enabled,
            "updatedAt" to now
        )
        db.getReference("admin/commissionRules/productCommissions/$cleanId").setValue(payload).await()
        db.getReference("admin/commissionRules/updatedAt").setValue(now).await()
    }

    override suspend fun deleteProductCommissionRule(productId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        db.getReference("admin/commissionRules/productCommissions/${productId.trim()}").removeValue().await()
        db.getReference("admin/commissionRules/updatedAt").setValue(now).await()
    }

    override suspend fun saveProduct(uid: String, itemId: String?, itemData: Map<String, Any>): Result<Unit> = runCatching {
        val baseRef = db.getReference("menus/$uid")
        if (itemId.isNullOrBlank()) {
            baseRef.push().setValue(itemData).await()
        } else {
            baseRef.child(itemId).updateChildren(itemData).await()
        }
    }

    override suspend fun toggleProductStock(uid: String, itemId: String, available: Boolean): Result<Unit> = runCatching {
        db.getReference("menus/$uid/$itemId").updateChildren(mapOf("available" to available)).await()
    }

    override suspend fun deleteProduct(uid: String, itemId: String): Result<Unit> = runCatching {
        db.getReference("menus/$uid/$itemId").removeValue().await()
    }

    override suspend fun setInventoryApproval(
        uid: String,
        itemId: String,
        approved: Boolean
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "adminApproved" to approved,
            "approvalStatus" to if (approved) "APPROVED" else "PENDING",
            "updatedAt" to System.currentTimeMillis()
        )
        if (approved) {
            updates["inventoryFlagged"] = false
            updates["inventoryFlagReason"] = ""
        }
        db.getReference("menus/$uid/$itemId").updateChildren(updates).await()
    }

    override suspend fun setInventoryFlag(
        uid: String,
        itemId: String,
        flagged: Boolean,
        reason: String
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "inventoryFlagged" to flagged,
            "inventoryFlagReason" to if (flagged) reason else "",
            "updatedAt" to System.currentTimeMillis()
        )
        if (flagged) {
            updates["adminApproved"] = false
            updates["approvalStatus"] = "FLAGGED"
        } else {
            updates["approvalStatus"] = "PENDING"
        }
        db.getReference("menus/$uid/$itemId").updateChildren(updates).await()
    }

    override suspend fun updateProductStockQty(
        uid: String,
        itemId: String,
        stockQty: Int
    ): Result<Unit> = runCatching {
        val clamped = stockQty.coerceAtLeast(0)
        val updates = mapOf<String, Any>(
            "stockQty" to clamped,
            "available" to (clamped > 0),
            "updatedAt" to System.currentTimeMillis()
        )
        db.getReference("menus/$uid/$itemId").updateChildren(updates).await()
    }

    override suspend fun quickSeedProducts(
        uid: String,
        rules: CommissionRules
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        data class SeedSpec(
            val name: String,
            val cost: Double,
            val sell: Double,
            val mrp: Double,
            val disc: Int,
            val sale: Boolean,
            val kw: String,
            val cat: String,
            val desc: String,
            val img: String
        )
        val specs = listOf(
            SeedSpec(
                "Fresh Local Potatoes (Aloo - 1 kg)", 25.0, 35.0, 45.0, 22, true,
                "aloo, potato, sabzi", "VEG", "Farm fresh Assam potatoes",
                "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=300&auto=format&fit=crop&q=60"
            ),
            SeedSpec(
                "Fresh Red Onions (Pyaz - 1 kg)", 36.0, 48.0, 60.0, 20, false,
                "pyaz, onion, kanda", "VEG", "Premium Nashik red onions",
                "https://images.unsplash.com/photo-1618512496248-a07fe83aa8cb?w=300&auto=format&fit=crop&q=60"
            ),
            SeedSpec(
                "Ripe Country Tomatoes (1 kg)", 28.0, 40.0, 50.0, 20, true,
                "tamatar, tomato", "VEG", "Fresh red juicy tomatoes",
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300&auto=format&fit=crop&q=60"
            ),
            SeedSpec(
                "Aashirvaad Shudh Chakki Atta (5 kg)", 210.0, 245.0, 285.0, 14, false,
                "atta, wheat, flour", "STAPLES", "100% whole wheat flour",
                "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300&auto=format&fit=crop&q=60"
            ),
            SeedSpec(
                "Fortune Kachi Ghani Mustard Oil (1 L)", 142.0, 165.0, 190.0, 13, false,
                "oil, tel, mustard", "OIL_MASALA", "Pure pungent mustard oil",
                "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300&auto=format&fit=crop&q=60"
            ),
            SeedSpec(
                "Amul Taaza Toned Milk (500 ml)", 23.0, 28.0, 30.0, 7, false,
                "milk, doodh, amul", "DAIRY", "Fresh pasteurized milk pouch",
                "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=300&auto=format&fit=crop&q=60"
            )
        )
        val ref = db.getReference("menus/$uid")
        specs.forEach { s ->
            val resolved = EarningsCalculator.resolveCommission(null, uid, s.cat, rules)
            val calc = EarningsCalculator.calculate(s.cost, s.sell, resolved.percent, resolved.source)
            val payload = mapOf(
                "name" to s.name,
                "price" to calc.sellingPrice,
                "sellingPrice" to calc.sellingPrice,
                "purchaseCost" to calc.purchaseCost,
                "commissionPercentage" to calc.commissionPercentage,
                "commissionAmount" to calc.commissionAmount,
                "storeEarning" to calc.storeEarning,
                "estimatedProfit" to calc.estimatedProfit,
                "commissionRuleSource" to calc.ruleSource.name,
                "lastUpdated" to now,
                "updatedAt" to now,
                "mrp" to s.mrp,
                "discountPercent" to s.disc,
                "isOnSale" to s.sale,
                "searchKeywords" to s.kw,
                "category" to s.cat,
                "description" to s.desc,
                "imageUrl" to s.img,
                "available" to true,
                "adminApproved" to false,
                "approvalStatus" to "PENDING"
            )
            ref.push().setValue(payload).await()
        }
    }

    override suspend fun requestPayout(payout: Map<String, Any>): Result<Unit> = runCatching {
        db.getReference("storePayouts").push().setValue(payout).await()
    }

    override suspend fun submitFeedback(feedback: Map<String, Any>): Result<Unit> = runCatching {
        db.getReference("feedbacks").push().setValue(feedback).await()
    }

    override fun observeRestaurantCallInbox(restaurantUid: String): Flow<InAppCallRecord?> = callbackFlow {
        val safeUid = SecurityGuard.sanitizeFirebaseKey(restaurantUid)
        if (safeUid.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("restaurantCallInbox/$safeUid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toInAppCallRecord())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeOrderCall(orderId: String): Flow<InAppCallRecord?> = callbackFlow {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("inAppCalls/$safeOrderId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toInAppCallRecord(fallbackOrderId = safeOrderId))
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun getOrderCallSnapshot(orderId: String): Result<InAppCallRecord?> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank()) return@runCatching null
        val snap = db.getReference("inAppCalls/$safeOrderId").get().await()
        snap.toInAppCallRecord(fallbackOrderId = safeOrderId)
    }

    override suspend fun startOutgoingCall(call: InAppCallRecord): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(call.orderId)
        val safeRiderUid = SecurityGuard.sanitizeFirebaseKey(call.riderUid)
        val safeRestUid = SecurityGuard.sanitizeFirebaseKey(call.restaurantUid)
        require(safeOrderId.isNotBlank() && safeRiderUid.isNotBlank() && safeRestUid.isNotBlank()) {
            "Invalid call participant identifiers"
        }

        val now = System.currentTimeMillis()
        // Check if rider already has an active call belonging to another order/participant
        val riderInboxSnap = runCatching {
            db.getReference("riderCallInbox/$safeRiderUid").get().await()
        }.getOrNull()
        val existingRiderCall = riderInboxSnap?.toInAppCallRecord()
        if (existingRiderCall != null &&
            existingRiderCall.isActiveOrRinging &&
            existingRiderCall.orderId.isNotBlank() &&
            existingRiderCall.orderId != safeOrderId &&
            (now - existingRiderCall.updatedAt.coerceAtLeast(existingRiderCall.startedAt)) <= RiderCommPolicy.RINGING_WINDOW_MS
        ) {
            error("Rider is currently busy on another call.")
        }

        val callMap = RiderCommPolicy.toCallMap(call.copy(startedAt = call.startedAt.takeIf { it > 0L } ?: now, updatedAt = now))
        val multiUpdates = mapOf<String, Any?>(
            "inAppCalls/$safeOrderId" to callMap,
            "riderCallInbox/$safeRiderUid" to callMap,
            "restaurantCallInbox/$safeRestUid" to callMap
        )
        db.reference.updateChildren(multiUpdates).await()
    }

    override suspend fun updateCallStatus(
        orderId: String,
        restaurantUid: String,
        riderUid: String,
        callId: String,
        status: String,
        restaurantAnswered: Boolean,
        isMuted: Boolean?,
        isOnHold: Boolean?
    ): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        val safeRestUid = SecurityGuard.sanitizeFirebaseKey(restaurantUid)
        val safeRiderUid = SecurityGuard.sanitizeFirebaseKey(riderUid)
        if (safeOrderId.isBlank()) return@runCatching
        val now = System.currentTimeMillis()

        val multiUpdates = mutableMapOf<String, Any?>(
            "inAppCalls/$safeOrderId/status" to status,
            "inAppCalls/$safeOrderId/restaurantAnswered" to restaurantAnswered,
            "inAppCalls/$safeOrderId/updatedAt" to now
        )
        if (isMuted != null) {
            multiUpdates["inAppCalls/$safeOrderId/isMuted"] = isMuted
        }
        if (isOnHold != null) {
            multiUpdates["inAppCalls/$safeOrderId/isOnHold"] = isOnHold
        }

        if (safeRestUid.isNotBlank()) {
            multiUpdates["restaurantCallInbox/$safeRestUid/status"] = status
            multiUpdates["restaurantCallInbox/$safeRestUid/restaurantAnswered"] = restaurantAnswered
            multiUpdates["restaurantCallInbox/$safeRestUid/updatedAt"] = now
            if (isMuted != null) multiUpdates["restaurantCallInbox/$safeRestUid/isMuted"] = isMuted
            if (isOnHold != null) multiUpdates["restaurantCallInbox/$safeRestUid/isOnHold"] = isOnHold
        }
        if (safeRiderUid.isNotBlank()) {
            multiUpdates["riderCallInbox/$safeRiderUid/status"] = status
            multiUpdates["riderCallInbox/$safeRiderUid/restaurantAnswered"] = restaurantAnswered
            multiUpdates["riderCallInbox/$safeRiderUid/updatedAt"] = now
            if (isMuted != null) multiUpdates["riderCallInbox/$safeRiderUid/isMuted"] = isMuted
            if (isOnHold != null) multiUpdates["riderCallInbox/$safeRiderUid/isOnHold"] = isOnHold
        }

        db.reference.updateChildren(multiUpdates).await()
    }

    override suspend fun writeWebRtcOffer(
        orderId: String,
        offer: WebRtcSessionDescriptionData
    ): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        require(safeOrderId.isNotBlank() && offer.sdp.isNotBlank()) { "Invalid WebRTC offer" }
        val payload = mapOf<String, Any>(
            "type" to offer.type.ifBlank { "offer" },
            "sdp" to offer.sdp,
            "updatedAt" to System.currentTimeMillis()
        )
        db.getReference("inAppCalls/$safeOrderId/webrtc/offer").setValue(payload).await()
    }

    override suspend fun writeWebRtcAnswer(
        orderId: String,
        answer: WebRtcSessionDescriptionData
    ): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        require(safeOrderId.isNotBlank() && answer.sdp.isNotBlank()) { "Invalid WebRTC answer" }
        val payload = mapOf<String, Any>(
            "type" to answer.type.ifBlank { "answer" },
            "sdp" to answer.sdp,
            "updatedAt" to System.currentTimeMillis()
        )
        db.getReference("inAppCalls/$safeOrderId/webrtc/answer").setValue(payload).await()
    }

    override suspend fun readWebRtcOffer(orderId: String): Result<WebRtcSessionDescriptionData?> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank()) return@runCatching null
        val snap = db.getReference("inAppCalls/$safeOrderId/webrtc/offer").get().await()
        val sdp = snap.str("sdp")
        if (sdp.isBlank()) null
        else WebRtcSessionDescriptionData(
            type = snap.str("type").ifBlank { "offer" },
            sdp = sdp
        )
    }

    override fun observeWebRtcOffer(orderId: String): Flow<WebRtcSessionDescriptionData?> = callbackFlow {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("inAppCalls/$safeOrderId/webrtc/offer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.str("sdp")
                if (sdp.isBlank()) {
                    trySend(null)
                } else {
                    trySend(
                        WebRtcSessionDescriptionData(
                            type = snapshot.str("type").ifBlank { "offer" },
                            sdp = sdp
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeWebRtcAnswer(orderId: String): Flow<WebRtcSessionDescriptionData?> = callbackFlow {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("inAppCalls/$safeOrderId/webrtc/answer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.str("sdp")
                if (sdp.isBlank()) {
                    trySend(null)
                } else {
                    trySend(
                        WebRtcSessionDescriptionData(
                            type = snapshot.str("type").ifBlank { "answer" },
                            sdp = sdp
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun pushWebRtcIceCandidate(
        orderId: String,
        isCaller: Boolean,
        candidate: WebRtcIceCandidateData
    ): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank() || candidate.candidate.isBlank()) return@runCatching
        val branch = if (isCaller) "callerCandidates" else "receiverCandidates"
        val ref = db.getReference("inAppCalls/$safeOrderId/webrtc/$branch").push()
        val payload = mapOf<String, Any>(
            "candidate" to candidate.candidate,
            "sdpMid" to candidate.sdpMid.ifBlank { "0" },
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "createdAt" to System.currentTimeMillis()
        )
        ref.setValue(payload).await()
    }

    override fun observeWebRtcIceCandidates(
        orderId: String,
        observeCallerCandidates: Boolean
    ): Flow<WebRtcIceCandidateData> = callbackFlow {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank()) {
            awaitClose {}
            return@callbackFlow
        }
        val branch = if (observeCallerCandidates) "callerCandidates" else "receiverCandidates"
        val ref = db.getReference("inAppCalls/$safeOrderId/webrtc/$branch")
        val seenIds = mutableSetOf<String>()

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key.orEmpty()
                val candStr = snapshot.str("candidate")
                if (candStr.isNotBlank() && seenIds.add(id.ifBlank { candStr })) {
                    trySend(
                        WebRtcIceCandidateData(
                            candidateId = id,
                            candidate = candStr,
                            sdpMid = snapshot.str("sdpMid").ifBlank { "0" },
                            sdpMLineIndex = snapshot.intVal("sdpMLineIndex", 0)
                        )
                    )
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeOrderChatMessages(
        orderId: String,
        restaurantUid: String,
        riderUid: String,
        customerUid: String
    ): Flow<List<TripChatMessage>> = callbackFlow {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        if (safeOrderId.isBlank() || restaurantUid.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val ref = db.getReference("tripChats/$safeOrderId/messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.toTripChatMessage(orderId = safeOrderId)
                }.filter { msg ->
                    RiderCommPolicy.isRestaurantRiderMessage(
                        message = msg,
                        restaurantUid = restaurantUid,
                        riderUid = riderUid,
                        customerUid = customerUid
                    )
                }.sortedWith(compareBy<TripChatMessage> { it.timestamp }.thenBy { it.messageId })
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun sendOrderChatMessage(
        orderId: String,
        restaurantUid: String,
        riderUid: String,
        messageText: String,
        existingMessageId: String?
    ): Result<TripChatMessage> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        val safeRestUid = SecurityGuard.sanitizeFirebaseKey(restaurantUid)
        val safeRiderUid = SecurityGuard.sanitizeFirebaseKey(riderUid)
        val cleanText = StoreUtils.sanitizeText(messageText, RiderCommPolicy.MAX_CHAT_MESSAGE_LENGTH)
        require(safeOrderId.isNotBlank()) { "Order ID is required" }
        require(safeRestUid.isNotBlank()) { "Restaurant authentication is required" }
        require(safeRiderUid.isNotBlank()) { "Rider UID is required" }
        require(cleanText.isNotBlank()) { "Message cannot be empty" }

        val messagesRef = db.getReference("tripChats/$safeOrderId/messages")
        val targetRef = if (!existingMessageId.isNullOrBlank()) {
            messagesRef.child(SecurityGuard.sanitizeFirebaseKey(existingMessageId))
        } else {
            messagesRef.push()
        }
        val msgId = targetRef.key ?: "msg_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()
        val payload = mapOf<String, Any>(
            "messageId" to msgId,
            "senderUid" to safeRestUid,
            "senderRole" to "RESTAURANT",
            "receiverUid" to safeRiderUid,
            "messageType" to "TEXT",
            "message" to cleanText,
            "timestamp" to now,
            "status" to "SENT"
        )
        targetRef.setValue(payload).await()
        TripChatMessage(
            messageId = msgId,
            orderId = safeOrderId,
            senderUid = safeRestUid,
            senderRole = "RESTAURANT",
            receiverUid = safeRiderUid,
            messageType = "TEXT",
            message = cleanText,
            timestamp = now,
            status = "SENT"
        )
    }

    override suspend fun updateChatMessageStatus(
        orderId: String,
        messageId: String,
        status: String
    ): Result<Unit> = runCatching {
        val safeOrderId = SecurityGuard.sanitizeFirebaseKey(orderId)
        val safeMsgId = SecurityGuard.sanitizeFirebaseKey(messageId)
        if (safeOrderId.isBlank() || safeMsgId.isBlank()) return@runCatching
        db.getReference("tripChats/$safeOrderId/messages/$safeMsgId")
            .updateChildren(mapOf("status" to status))
            .await()
    }

    // Defensive RTDB Snapshot mappers
    private fun DataSnapshot.str(key: String): String =
        child(key).value?.toString()?.trim().orEmpty()

    private fun DataSnapshot.bool(key: String, default: Boolean): Boolean {
        val v = child(key).value ?: return default
        return when (v) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            is Number -> v.toInt() != 0
            else -> default
        }
    }

    private fun DataSnapshot.boolOrNull(key: String): Boolean? {
        val v = child(key).value ?: return null
        return when (v) {
            is Boolean -> v
            is String -> if (v.equals("true", ignoreCase = true)) true else if (v.equals("false", ignoreCase = true)) false else null
            is Number -> v.toInt() != 0
            else -> null
        }
    }

    private fun DataSnapshot.intVal(key: String, default: Int = 0): Int {
        val v = child(key).value ?: return default
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toDoubleOrNull()?.toInt() ?: default
            else -> default
        }
    }

    private fun DataSnapshot.intOrNull(key: String): Int? {
        val v = child(key).value ?: return null
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toDoubleOrNull()?.toInt()
            else -> null
        }
    }

    private fun DataSnapshot.lng(key: String, default: Long = 0L): Long {
        val v = child(key).value ?: return default
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toDoubleOrNull()?.toLong() ?: default
            else -> default
        }
    }

    private fun DataSnapshot.dbl(key: String, default: Double = 0.0): Double {
        val v = child(key).value ?: return default
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun DataSnapshot.dblOrNull(key: String): Double? {
        val v = child(key).value ?: return null
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    private fun DataSnapshot.toStoreProfile(): StoreProfile = StoreProfile(
        name = str("name"),
        email = str("email"),
        cuisine = str("cuisine"),
        address = str("address"),
        phone = str("phone"),
        upiId = str("upiId"),
        isOpen = bool("isOpen", true),
        hours = str("hours").ifBlank { "7:00 AM - 9:00 PM" },
        imageUrl = str("imageUrl"),
        approved = bool("approved", false),
        suspended = bool("suspended", false),
        adminNote = str("adminNote"),
        lat = dbl("lat", 26.4524),
        lng = dbl("lng", 89.9723),
        locationLink = str("locationLink"),
        storeDiscountPercent = intVal("storeDiscountPercent", 0),
        storeDiscountMinOrder = dbl("storeDiscountMinOrder", 0.0)
    )

    private fun DataSnapshot.toStoreOrder(): StoreOrder? {
        val id = key ?: return null
        val itemsSnap = child("items")
        val itemsList = itemsSnap.children.mapNotNull { itemSnap ->
            val itemName = itemSnap.str("name")
            if (itemName.isBlank()) null
            else OrderItem(
                itemId = itemSnap.str("itemId"),
                name = itemName,
                qty = itemSnap.intVal("qty", 1).coerceAtLeast(1),
                price = itemSnap.dbl("price", 0.0),
                purchaseCost = itemSnap.dbl("purchaseCost", 0.0),
                commissionPercentage = itemSnap.dblOrNull("commissionPercentage"),
                commissionAmount = itemSnap.dbl("commissionAmount", 0.0),
                storeEarning = itemSnap.dbl("storeEarning", 0.0),
                commissionRuleSource = itemSnap.str("commissionRuleSource")
            )
        }
        return StoreOrder(
            orderId = id,
            batchGroupId = str("batchGroupId").ifBlank { str("tripId") },
            restaurantId = str("restaurantId"),
            customerUid = str("customerUid"),
            deliveryUid = str("deliveryUid")
                .ifBlank { str("riderUid") }
                .ifBlank { str("assignedRiderUid") },
            status = str("status").ifBlank { "PLACED" },
            paymentMode = str("paymentMode").ifBlank { "COD" },
            paymentStatus = str("paymentStatus"),
            pickupOtp = str("pickupOtp"),
            address = str("address"),
            phone = str("phone"),
            customerLat = dblOrNull("customerLat"),
            customerLng = dblOrNull("customerLng"),
            riderLat = dblOrNull("riderLat"),
            riderLng = dblOrNull("riderLng"),
            locationLink = str("locationLink"),
            subtotalAmount = dbl("subtotalAmount", 0.0),
            totalAmount = dbl("totalAmount", 0.0),
            createdAt = lng("createdAt", 0L),
            updatedAt = lng("updatedAt", 0L),
            items = itemsList,
            isSpamSuspect = bool("isSpamSuspect", false),
            emergencyAlert = bool("emergencyAlert", false),
            emergencyDetails = str("emergencyDetails"),
            cancelReason = str("cancelReason"),
            cancelledBy = str("cancelledBy")
        )
    }

    private fun DataSnapshot.toInAppCallRecord(fallbackOrderId: String = ""): InAppCallRecord? {
        if (!exists()) return null
        val callId = str("callId")
        val orderId = str("orderId").ifBlank { fallbackOrderId }
        if (callId.isBlank() && orderId.isBlank()) return null
        return InAppCallRecord(
            callId = callId,
            tripId = str("tripId").ifBlank { orderId },
            orderId = orderId,
            callerRole = str("callerRole").ifBlank { "RESTAURANT" },
            restaurantUid = str("restaurantUid"),
            restaurantName = str("restaurantName"),
            riderUid = str("riderUid"),
            riderName = str("riderName"),
            riderPhotoUrl = StoreUtils.cleanAndResolveImageUrl(str("riderPhotoUrl")),
            status = str("status").ifBlank { "RINGING" },
            restaurantAnswered = bool("restaurantAnswered", false),
            isMuted = bool("isMuted", false),
            isOnHold = bool("isOnHold", false),
            startedAt = lng("startedAt", 0L),
            updatedAt = lng("updatedAt", 0L)
        )
    }

    private fun DataSnapshot.toTripChatMessage(orderId: String): TripChatMessage? {
        if (!exists()) return null
        val id = str("messageId").ifBlank { key.orEmpty() }
        val text = str("message").ifBlank { str("text") }
        if (id.isBlank() || text.isBlank()) return null
        return TripChatMessage(
            messageId = id,
            orderId = orderId,
            senderUid = str("senderUid"),
            senderRole = str("senderRole").ifBlank { "RESTAURANT" },
            receiverUid = str("receiverUid"),
            messageType = str("messageType").ifBlank { "TEXT" },
            message = text,
            timestamp = lng("timestamp", 0L),
            status = str("status").ifBlank { "SENT" }
        )
    }

    private fun DataSnapshot.toProductItem(): ProductItem? {
        val id = key ?: return null
        val legacyPrice = dbl("price", 0.0)
        val selling = dblOrNull("sellingPrice")?.takeIf { it > 0.0 } ?: legacyPrice
        val updated = lng("lastUpdated", 0L).takeIf { it > 0L } ?: lng("updatedAt", 0L)
        return ProductItem(
            itemId = id,
            name = str("name"),
            price = legacyPrice.takeIf { it > 0.0 } ?: selling,
            sellingPrice = selling,
            purchaseCost = dbl("purchaseCost", 0.0),
            commissionPercentage = dblOrNull("commissionPercentage"),
            commissionAmount = dbl("commissionAmount", 0.0),
            storeEarning = dbl("storeEarning", 0.0),
            estimatedProfit = dbl("estimatedProfit", 0.0),
            commissionRuleSource = str("commissionRuleSource"),
            lastUpdated = updated,
            mrp = dbl("mrp", 0.0),
            discountPercent = intVal("discountPercent", 0),
            isOnSale = bool("isOnSale", false),
            saleBadgeText = str("saleBadgeText"),
            searchKeywords = str("searchKeywords"),
            category = str("category"),
            description = str("description"),
            imageUrl = str("imageUrl"),
            available = bool("available", true),
            adminApproved = bool("adminApproved", false),
            approvalStatus = str("approvalStatus").ifBlank { "PENDING" },
            rejectionReason = str("rejectionReason"),
            ratingAvg = dbl("ratingAvg", 0.0),
            ratingCount = intVal("ratingCount", 0),
            updatedAt = updated,
            stockQty = intOrNull("stockQty"),
            inventoryFlagged = bool("inventoryFlagged", false),
            inventoryFlagReason = str("inventoryFlagReason")
        )
    }

    private fun DataSnapshot.toAdminSettings(): AdminSettings {
        val categories = child("customCategories").children.mapNotNull { catSnap ->
            val id = catSnap.str("id")
            if (id.isBlank()) null
            else CustomCategory(
                id = id,
                name = catSnap.str("name"),
                icon = catSnap.str("icon"),
                active = catSnap.bool("active", true)
            )
        }
        return AdminSettings(
            commissionPercent = dbl("commissionPercent", 5.0),
            allowLossMakingProducts = bool("allowLossMakingProducts", false),
            minStorePayout = dbl("minStorePayout", 10.0),
            maxStorePayout = dbl("maxStorePayout", 50000.0),
            allowStoreProductEdit = bool("allowStoreProductEdit", true),
            allowStoreQuickSeed = bool("allowStoreQuickSeed", true),
            storeAnnouncement = str("storeAnnouncement"),
            supportHours = str("supportHours"),
            supportPhone = str("supportPhone"),
            supportWhatsapp = str("supportWhatsapp"),
            customCategories = categories
        )
    }

    private fun DataSnapshot.toRuleMap(): Map<String, CommissionRuleEntry> = buildMap {
        children.forEach { child ->
            val key = child.key ?: return@forEach
            put(
                key,
                CommissionRuleEntry(
                    targetId = child.str("targetId").ifBlank { key },
                    targetLabel = child.str("targetLabel").ifBlank { key },
                    storeId = child.str("storeId"),
                    percent = child.dbl("percent", 0.0).coerceIn(0.0, 100.0),
                    enabled = child.bool("enabled", true),
                    updatedAt = child.lng("updatedAt", 0L)
                )
            )
        }
    }

    private fun DataSnapshot.toCommissionRules(): CommissionRules {
        if (!exists()) return CommissionRules()
        return CommissionRules(
            globalCommissionPercent = dbl("globalCommissionPercent", 5.0).coerceIn(0.0, 100.0),
            globalRuleEnabled = bool("globalRuleEnabled", true),
            allowLossMakingProducts = bool("allowLossMakingProducts", false),
            storeCommissions = child("storeCommissions").toRuleMap(),
            categoryCommissions = child("categoryCommissions").toRuleMap(),
            productCommissions = child("productCommissions").toRuleMap(),
            updatedAt = lng("updatedAt", 0L)
        )
    }

    private fun DataSnapshot.toLedgerEntry(): OrderEarningsLedgerEntry? {
        val id = key ?: str("orderId")
        if (id.isBlank()) return null
        return OrderEarningsLedgerEntry(
            orderId = str("orderId").ifBlank { id },
            storeId = str("storeId"),
            storeName = str("storeName"),
            orderStatus = str("orderStatus").ifBlank { "PLACED" },
            paymentStatus = str("paymentStatus"),
            ledgerStatus = str("ledgerStatus").ifBlank { "ACTIVE" },
            grossAmount = dbl("grossAmount", 0.0),
            commissionPercentage = dbl("commissionPercentage", 0.0),
            commissionAmount = dbl("commissionAmount", 0.0),
            storeEarning = dbl("storeEarning", 0.0),
            purchaseCostTotal = dbl("purchaseCostTotal", 0.0),
            estimatedProfit = dbl("estimatedProfit", 0.0),
            reversedCommissionAmount = dbl("reversedCommissionAmount", 0.0),
            reversedStoreEarning = dbl("reversedStoreEarning", 0.0),
            commissionRuleSource = str("commissionRuleSource").ifBlank { CommissionRuleSource.GLOBAL.name },
            commissionDeducted = bool("commissionDeducted", false),
            reversalReason = str("reversalReason"),
            createdAt = lng("createdAt", 0L),
            updatedAt = lng("updatedAt", 0L),
            reversedAt = lng("reversedAt", 0L)
        )
    }
}

