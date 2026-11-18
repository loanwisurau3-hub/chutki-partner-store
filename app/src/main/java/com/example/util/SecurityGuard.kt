package com.example.util

import android.os.Build
import android.os.Debug
import com.example.model.CustomerRecord
import com.example.model.OrderRiskAssessment
import com.example.model.OrderRiskLevel
import com.example.model.PhishingScanResult
import com.example.model.SecurityPosture
import com.example.model.SecurityShieldConfig
import com.example.model.StoreOrder
import com.example.model.StoreProfile
import java.io.File
import java.util.Locale
import kotlin.math.abs

data class PasswordStrengthResult(
    val score: Int,
    val label: String,
    val isAcceptable: Boolean,
    val message: String
)

object SecurityGuard {

    private val COMMON_WEAK_PASSWORDS = setOf(
        "123456", "1234567", "12345678", "123456789", "1234567890",
        "password", "password1", "qwerty", "qwerty123", "admin123",
        "welcome", "letmein", "iloveyou", "gossaigaon", "chutki123"
    )

    private val ROOT_BINARY_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/su/bin/su",
        "/sbin/.magisk"
    )

    private val SCAM_PHISHING_KEYWORDS = listOf(
        "otp" to "Asks for OTP code",
        "upi pin" to "Asks for UPI PIN",
        "mpin" to "Asks for MPIN",
        "anydesk" to "Remote access app scam (AnyDesk)",
        "teamviewer" to "Remote access app scam (TeamViewer)",
        "quicksupport" to "Remote screen-share scam (QuickSupport)",
        "rustdesk" to "Remote control scam (RustDesk)",
        "kyc update" to "Fake KYC urgency lure",
        "kyc expire" to "Fake KYC suspension lure",
        "account block" to "Fake account block urgency",
        "refund link" to "Fake refund phishing lure",
        "claim prize" to "Lottery/reward scam lure",
        "lottery" to "Lottery scam lure",
        "bit.ly/" to "Obfuscated short URL (bit.ly)",
        "tinyurl.com/" to "Obfuscated short URL (tinyurl)",
        "cutt.ly/" to "Obfuscated short URL (cutt.ly)",
        "t.co/" to "Redirect shortener link"
    )

    private val SUSPICIOUS_PHONE_SEQUENCES = setOf(
        "9876543210", "9999999999", "8888888888", "7777777777",
        "6666666666", "9000000000", "9123456789", "6000000000"
    )

    fun evaluatePasswordStrength(password: String): PasswordStrengthResult {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) {
            return PasswordStrengthResult(0, "Empty", false, "Enter a password.")
        }
        val lower = trimmed.lowercase(Locale.US)
        if (lower in COMMON_WEAK_PASSWORDS) {
            return PasswordStrengthResult(
                score = 1,
                label = "Weak",
                isAcceptable = false,
                message = "Common password blocked. Use letters + numbers."
            )
        }
        val hasLetter = trimmed.any { it.isLetter() }
        val hasDigit = trimmed.any { it.isDigit() }
        val hasSpecial = trimmed.any { !it.isLetterOrDigit() }

        if (trimmed.length < 8 || !hasLetter || !hasDigit) {
            return PasswordStrengthResult(
                score = if (trimmed.length >= 6) 2 else 1,
                label = "Weak",
                isAcceptable = false,
                message = "Use 8+ chars with letters and numbers."
            )
        }
        val hasUpper = trimmed.any { it.isUpperCase() }
        val hasLower = trimmed.any { it.isLowerCase() }
        val score = when {
            (trimmed.length >= 12 && hasSpecial) ||
                (trimmed.length >= 10 && hasSpecial && hasUpper && hasLower) -> 4
            trimmed.length >= 10 || hasSpecial -> 3
            else -> 2
        }
        val label = when (score) {
            4 -> "Strong"
            3 -> "Good"
            else -> "Fair"
        }
        return PasswordStrengthResult(
            score = score,
            label = label,
            isAcceptable = true,
            message = "Strong merchant password ready."
        )
    }

    fun isSafeExternalUrl(rawUrl: String): Boolean {
        val url = rawUrl.trim()
        if (url.isEmpty()) return false
        val lower = url.lowercase(Locale.US)
        if (!lower.startsWith("https://")) return false
        val host = runCatching {
            val withoutProto = lower.removePrefix("https://")
            withoutProto.substringBefore('/').substringBefore('?').substringBefore(':')
        }.getOrDefault("")
        if (host.isBlank()) return false
        // Reject raw IPv4 phishing links and known abuse URL shorteners
        val isRawIp = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host)
        val isShortener = host in setOf("bit.ly", "tinyurl.com", "cutt.ly", "rb.gy", "is.gd")
        return !isRawIp && !isShortener
    }

    fun sanitizeFirebaseKey(input: String): String =
        input.replace(Regex("[.\\$#\\[\\]/\\u0000-\\u001F\\u007F]"), "").trim()

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun inspectRuntimeTamper(): Pair<Boolean, List<String>> {
        val findings = mutableListOf<String>()
        runCatching {
            if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
                findings.add("Active debugger attached to process")
            }
        }
        runCatching {
            val tags = Build.TAGS.orEmpty()
            if (tags.contains("test-keys", ignoreCase = true)) {
                findings.add("Unofficial OS test-keys build detected")
            }
        }
        runCatching {
            val hitRoot = ROOT_BINARY_PATHS.firstOrNull { path -> File(path).exists() }
            if (hitRoot != null) {
                findings.add("Root binary detected on device ($hitRoot)")
            }
        }
        runCatching {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists() && mapsFile.canRead()) {
                val content = mapsFile.readText().lowercase(Locale.US)
                if (content.contains("frida") || content.contains("xposed") || content.contains("lsposed")) {
                    findings.add("Runtime instrumentation hook (Frida/Xposed) detected")
                }
            }
        }
        return (findings.isNotEmpty()) to findings
    }

    fun scanPhishingOrScamMessage(input: String): PhishingScanResult {
        val text = input.trim()
        if (text.isEmpty()) {
            return PhishingScanResult(false, 0, OrderRiskLevel.LOW, emptyList())
        }
        val lower = text.lowercase(Locale.US)
        val findings = mutableListOf<String>()
        var score = 0

        SCAM_PHISHING_KEYWORDS.forEach { (keyword, label) ->
            if (lower.contains(keyword)) {
                findings.add(label)
                score += if (keyword in setOf("anydesk", "teamviewer", "quicksupport", "rustdesk", "otp", "upi pin", "mpin")) 45 else 30
            }
        }

        if (lower.contains("http://")) {
            findings.add("Insecure cleartext HTTP link (potential phishing)")
            score += 30
        }
        if (Regex("https?://\\d{1,3}(\\.\\d{1,3}){3}").containsMatchIn(lower)) {
            findings.add("Raw IP link detected instead of verified domain")
            score += 40
        }
        if (lower.contains("upi://pay") && (lower.contains("collect") || lower.contains("refund"))) {
            findings.add("Suspicious UPI collect/refund deep link")
            score += 45
        }
        // Check if user pasted a standalone phone number
        val digitsOnly = text.filter { it.isDigit() }
        if (text.length <= 16 && digitsOnly.length in 10..12) {
            val last10 = digitsOnly.takeLast(10)
            if (!StoreUtils.isValidIndianMobile(last10)) {
                findings.add("Invalid Indian mobile number pattern")
                score += 25
            } else if (last10 in SUSPICIOUS_PHONE_SEQUENCES || last10.toSet().size <= 2) {
                findings.add("Suspicious repetitive/sequential phone number")
                score += 30
            }
        }

        val clamped = score.coerceIn(0, 100)
        val level = scoreToLevel(clamped)
        return PhishingScanResult(
            isThreat = clamped >= 25,
            riskScore = clamped,
            level = level,
            findings = findings.distinct()
        )
    }

    fun evaluateOrderRisk(
        order: StoreOrder,
        allOrders: List<StoreOrder>,
        customers: Map<String, CustomerRecord>,
        config: SecurityShieldConfig,
        safeMarkedOrderIds: Set<String> = emptySet()
    ): OrderRiskAssessment {
        if (!config.scamShieldEnabled || order.orderId in safeMarkedOrderIds) {
            return OrderRiskAssessment(
                orderId = order.orderId,
                score = 0,
                level = OrderRiskLevel.LOW,
                reasons = emptyList()
            )
        }

        val reasons = mutableListOf<String>()
        var score = 0
        var isDuplicateSuspect = false
        var isPhishingTextSuspect = false

        val customerBlocked = customers[order.customerUid]?.blocked == true
        if (customerBlocked) {
            score += 65
            reasons.add("Blocked/banned customer account")
        }
        if (order.isSpamSuspect) {
            score += 50
            reasons.add("Flagged as Spam Suspect")
        }
        if (order.emergencyAlert) {
            score += 20
            reasons.add("Active emergency alert on order")
        }

        // Phone verification
        val digits = order.phone.replace(Regex("\\D"), "").takeLast(10)
        if (order.phone.isNotBlank()) {
            if (!StoreUtils.isValidIndianMobile(digits)) {
                score += 25
                reasons.add("Invalid phone format")
            } else if (digits in SUSPICIOUS_PHONE_SEQUENCES || digits.toSet().size <= 2) {
                score += 25
                reasons.add("Synthetic/repeated phone pattern")
            }
        }

        // Payment spoofing / payload tampering check
        if (order.paymentMode.equals("UPI", ignoreCase = true) &&
            !order.paymentStatus.equals("VERIFIED", ignoreCase = true)
        ) {
            score += 30
            reasons.add("Unverified UPI payment")
        }
        if (order.items.isNotEmpty()) {
            val computedItemsSum = order.items.sumOf { it.price * it.qty.coerceAtLeast(1) }
            if (computedItemsSum > 0.0 && order.itemsValue > 0.0 &&
                abs(computedItemsSum - order.itemsValue) > 10.0
            ) {
                score += 40
                reasons.add("Order total tampered vs item prices")
            }
        }

        // High-value or bulk COD risk
        if (order.paymentMode.equals("COD", ignoreCase = true)) {
            if (order.itemsValue >= config.codVerificationLimit) {
                score += 35
                reasons.add("High-value COD (${EarningsCalculator.formatRupees(order.itemsValue)})")
            }
            val maxQty = order.items.maxOfOrNull { it.qty } ?: 0
            if (maxQty >= 15) {
                score += 25
                reasons.add("Unusual bulk quantity ($maxQty units) on COD")
            }
        }

        // Gibberish / missing address check
        val cleanAddr = order.address.trim()
        if (cleanAddr.isNotEmpty() && (cleanAddr.length < 6 || cleanAddr.none { it.isLetter() })) {
            score += 20
            reasons.add("Low-quality or incomplete delivery address")
        }

        // Phishing / social engineering scan in address or cancel reason
        val textScan = scanPhishingOrScamMessage("${order.address} ${order.cancelReason}")
        if (textScan.isThreat) {
            isPhishingTextSuspect = true
            score += 35
            reasons.addAll(textScan.findings.take(2))
        }

        // Order bombing / repeat velocity & duplicate check
        if (config.velocityGuardEnabled || config.duplicateOrderGuardEnabled) {
            val sameCustomerOrders = allOrders.filter { other ->
                other.orderId != order.orderId && (
                    (order.customerUid.isNotBlank() && other.customerUid == order.customerUid) ||
                        (digits.length == 10 && other.phone.takeLast(10) == digits)
                    )
            }
            if (config.velocityGuardEnabled) {
                val cancelledCount = sameCustomerOrders.count {
                    it.status.equals("CANCELLED", ignoreCase = true) ||
                        it.status.equals("REFUNDED", ignoreCase = true)
                }
                if (cancelledCount >= 2) {
                    score += 35
                    reasons.add("$cancelledCount prior cancelled/refunded orders")
                }

                val recentWindowMs = 30L * 60L * 1000L
                if (order.createdAt > 0L) {
                    val burstCount = sameCustomerOrders.count {
                        it.createdAt > 0L && abs(order.createdAt - it.createdAt) <= recentWindowMs
                    }
                    if (burstCount >= 2) {
                        score += 30
                        reasons.add("Rapid order burst (${burstCount + 1} orders in 30m)")
                    }
                }
            }

            if (config.duplicateOrderGuardEnabled && order.createdAt > 0L) {
                val dupWindowMs = 5L * 60L * 1000L
                val hasDuplicate = sameCustomerOrders.any { other ->
                    other.createdAt > 0L &&
                        abs(order.createdAt - other.createdAt) <= dupWindowMs &&
                        abs(other.itemsValue - order.itemsValue) < 1.0 &&
                        other.items.size == order.items.size &&
                        !other.status.equals("CANCELLED", ignoreCase = true)
                }
                if (hasDuplicate) {
                    isDuplicateSuspect = true
                    score += 25
                    reasons.add("Possible duplicate order within 5 mins")
                }
            }
        }

        val clamped = score.coerceIn(0, 100)
        return OrderRiskAssessment(
            orderId = order.orderId,
            score = clamped,
            level = scoreToLevel(clamped),
            reasons = reasons.distinct(),
            isDuplicateSuspect = isDuplicateSuspect,
            isPhishingTextSuspect = isPhishingTextSuspect
        )
    }

    fun evaluateMerchantPosture(
        profile: StoreProfile,
        config: SecurityShieldConfig,
        assessments: Map<String, OrderRiskAssessment>,
        activeOrders: List<StoreOrder>,
        cachedRuntimeTamper: Pair<Boolean, List<String>>? = null
    ): SecurityPosture {
        val (tamperDetected, runtimeFindings) = cachedRuntimeTamper ?: inspectRuntimeTamper()
        var score = 100
        if (!profile.approved) score -= 10
        if (profile.suspended) score -= 25
        if (!config.scamShieldEnabled) score -= 20
        if (!config.velocityGuardEnabled) score -= 10
        if (!config.strictLinkSanitizer) score -= 10
        if (!config.upiChangeLockEnabled) score -= 5
        if (tamperDetected) score -= 20

        val activeIds = activeOrders.map { it.orderId }.toSet()
        val activeRisks = assessments.filterKeys { it in activeIds }.values
        val flaggedCount = activeRisks.count { it.level != OrderRiskLevel.LOW }
        val criticalCount = activeRisks.count { it.level == OrderRiskLevel.CRITICAL }

        score -= (criticalCount * 8).coerceAtMost(24)

        return SecurityPosture(
            overallScore = score.coerceIn(0, 100),
            runtimeTamperDetected = tamperDetected,
            runtimeFindings = runtimeFindings,
            flaggedOrderCount = flaggedCount,
            criticalOrderCount = criticalCount
        )
    }

    private fun scoreToLevel(score: Int): OrderRiskLevel = when {
        score >= 70 -> OrderRiskLevel.CRITICAL
        score >= 45 -> OrderRiskLevel.HIGH
        score >= 25 -> OrderRiskLevel.MEDIUM
        else -> OrderRiskLevel.LOW
    }
}
