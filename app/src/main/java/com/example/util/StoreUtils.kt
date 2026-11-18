package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.CustomCategory
import com.example.model.StoreOrder
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object StoreUtils {

    val DEFAULT_CATEGORIES = listOf(
        CustomCategory(id = "VEG", name = "Vegetables & Fruits", icon = "🥬"),
        CustomCategory(id = "DAIRY", name = "Milk & Dairy", icon = "🥛"),
        CustomCategory(id = "STAPLES", name = "Atta, Rice & Dal", icon = "🌾"),
        CustomCategory(id = "OIL_MASALA", name = "Oil & Spices", icon = "🫗"),
        CustomCategory(id = "SNACKS", name = "Snacks & Drinks", icon = "🍪"),
        CustomCategory(id = "CLEANING", name = "Cleaning & Care", icon = "🧼")
    )

    const val DEFAULT_STORE_BANNER =
        "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=600&auto=format&fit=crop&q=60"

    const val DEFAULT_PRODUCT_IMAGE =
        "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&auto=format&fit=crop&q=60"

    fun sanitizeText(input: String?, maxLength: Int = 500): String {
        val cleaned = (input ?: "")
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), " ")
            .replace(Regex("[<>\"'`]"), "")
            .replace(Regex("(?i)javascript\\s*:"), "")
            .replace(Regex("(?i)data\\s*:\\s*text/html"), "")
            .trim()
        return if (cleaned.length > maxLength) cleaned.take(maxLength) else cleaned
    }

    fun isValidIndianMobile(phone: String?): Boolean {
        val digits = (phone ?: "").replace(Regex("\\D"), "").takeLast(10)
        return Regex("^[6-9]\\d{9}$").matches(digits)
    }

    fun isValidUpiId(upi: String?): Boolean {
        return Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$").matches((upi ?: "").trim())
    }

    fun getStorePickupCode(pickupOtp: String?, orderId: String): String {
        if (!pickupOtp.isNullOrBlank()) return pickupOtp
        var hash = 0
        for (ch in orderId) {
            hash = ((hash shl 5) - hash) + ch.code
        }
        return ((abs(hash) % 9000) + 1000).toString()
    }

    fun cleanAndResolveImageUrl(rawUrl: String?): String {
        var url = (rawUrl ?: "").trim()
        if (url.isEmpty()) return ""
        val lower = url.lowercase(Locale.US)
        if (lower.startsWith("javascript:") || lower.startsWith("vbscript:") ||
            lower.startsWith("file:") || lower.startsWith("content:") ||
            lower.startsWith("data:text/")
        ) {
            return ""
        }
        if (lower.startsWith("data:image/")) {
            val safeImagePrefix = lower.startsWith("data:image/jpeg;base64,") ||
                lower.startsWith("data:image/jpg;base64,") ||
                lower.startsWith("data:image/png;base64,") ||
                lower.startsWith("data:image/webp;base64,")
            return if (safeImagePrefix) url else ""
        }
        if (url.contains("google.") && url.contains("imgurl=")) {
            val match = Regex("[?&]imgurl=([^&]+)").find(url)
            if (match != null) {
                runCatching {
                    url = URLDecoder.decode(match.groupValues[1], "UTF-8")
                }
            }
        }
        if (url.contains("drive.google.com/file/d/")) {
            val match = Regex("/file/d/([a-zA-Z0-9_-]+)").find(url)
            if (match != null) {
                return "https://drive.google.com/uc?export=view&id=${match.groupValues[1]}"
            }
        }
        if (url.contains("dropbox.com")) {
            url = url.replace("dl=0", "raw=1")
        }
        val imgurMatch = Regex("https?://(?:www\\.)?imgur\\.com/([a-zA-Z0-9]+)$").find(url)
        if (imgurMatch != null) {
            return "https://i.imgur.com/${imgurMatch.groupValues[1]}.jpg"
        }
        if (url.startsWith("http://", ignoreCase = true)) {
            url = "https://" + url.substring(7)
        }
        return if (url.startsWith("https://", ignoreCase = true)) url else ""
    }

    fun decodeBase64DataUri(dataUri: String): Bitmap? {
        if (!dataUri.startsWith("data:image")) return null
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex == -1) return null
        return runCatching {
            val base64Part = dataUri.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun compressImageUriToDataUrl(context: Context, uri: Uri): String? {
        return runCatching {
            val sourceBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val maxDim = 450
            var width = sourceBitmap.width
            var height = sourceBitmap.height
            if (width > height && width > maxDim) {
                height = ((height * maxDim).toFloat() / width).toInt().coerceAtLeast(1)
                width = maxDim
            } else if (height >= width && height > maxDim) {
                width = ((width * maxDim).toFloat() / height).toInt().coerceAtLeast(1)
                height = maxDim
            }
            val scaled = Bitmap.createScaledBitmap(sourceBitmap, width, height, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        }.getOrNull()
    }

    fun calculateDistanceKm(lat1: Double?, lon1: Double?, lat2: Double?, lon2: Double?): Double? {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
        if (lat1 == 0.0 && lon1 == 0.0) return null
        if (lat2 == 0.0 && lon2 == 0.0) return null
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * (2 * atan2(sqrt(a), sqrt(1 - a)))
    }

    fun formatDate(millis: Long): String {
        val ts = if (millis > 0L) millis else System.currentTimeMillis()
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ts))
    }

    fun formatTime(millis: Long): String {
        val ts = if (millis > 0L) millis else System.currentTimeMillis()
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))
    }

    fun formatIsoDate(millis: Long): String {
        val ts = if (millis > 0L) millis else System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(ts))
    }

    fun buildSalesCsv(orders: List<StoreOrder>): String {
        val sb = StringBuilder("Order ID,Date,Customer Ref,Payment Mode,Items Subtotal,Status\n")
        orders.forEach { o ->
            val idShort = o.orderId.takeLast(6)
            val date = formatIsoDate(o.createdAt)
            val customerRef = o.customerUid.takeLast(6).ifBlank { "PROTECTED" }
            val pay = o.paymentMode.ifBlank { "COD" }
            val sub = String.format(Locale.US, "%.2f", o.itemsValue)
            val status = o.status.ifBlank { "PLACED" }
            sb.append("$idShort,$date,$customerRef,$pay,$sub,$status\n")
        }
        return sb.toString()
    }

    fun openDialer(context: Context, phone: String) {
        val clean = phone.trim()
        if (clean.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
        runCatching { context.startActivity(intent) }
    }

    fun openWhatsApp(context: Context, phone: String) {
        val digits = phone.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return
        val waNum = if (digits.length == 10) "91$digits" else digits
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNum"))
        runCatching { context.startActivity(intent) }
    }

    fun openExternalUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val lower = url.trim().lowercase(Locale.US)
        if (lower.startsWith("tel:") || lower.contains("wa.me/") || lower.contains("whatsapp.com/")) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { context.startActivity(intent) }
    }

    fun escapeHtml(text: String?): String =
        (text ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")

    fun printKotSlip(context: Context, order: StoreOrder) {
        val pickupCode = getStorePickupCode(order.pickupOtp, order.orderId)
        val payNotice = when (order.paymentMode) {
            "WALLET" -> "PREPAID (WALLET)"
            "UPI" -> if (order.paymentStatus == "VERIFIED") "PREPAID ONLINE (UPI)" else "UPI UNVERIFIED (HOLD)"
            else -> "CASH ON DELIVERY (COD)"
        }
        val safeZoneAddress = RiderCommPolicy.formatPrivacySafeAddress(order.address)
        val itemsHtml = order.items.joinToString("") { item ->
            "<tr><td style=\"padding:4px 0;\">[  ] <strong>${item.qty}x</strong> ${escapeHtml(item.name)}</td>" +
                "<td style=\"text-align:right;\">&#8377;${(item.price * item.qty).toInt()}</td></tr>"
        }
        val html = """
            <html><body style="font-family:monospace;font-size:13px;padding:12px;color:#000;">
            <h2 style="text-align:center;margin:0;">GOSSAIGAON BAZAR</h2>
            <p style="text-align:center;font-weight:bold;margin:4px 0;">STORE PACKING KOT</p>
            <hr style="border:none;border-top:1px dashed #000;margin:6px 0;">
            <p><strong>Order:</strong> #${escapeHtml(order.orderId.takeLast(6))}</p>
            <div style="border:2px dashed #000;padding:8px;text-align:center;margin:8px 0;">
              <div>RIDER HANDOVER CODE:</div>
              <div style="font-size:22px;font-weight:bold;letter-spacing:3px;">[ ${escapeHtml(pickupCode)} ]</div>
            </div>
            <p><strong>Payment:</strong> $payNotice</p>
            <p><strong>Contact:</strong> In-App VoIP &amp; Chat Only (Number Protected)</p>
            <p><strong>Delivery Area:</strong> ${escapeHtml(safeZoneAddress)}</p>
            <hr style="border:none;border-top:1px dashed #000;margin:6px 0;">
            <table style="width:100%;">$itemsHtml</table>
            <hr style="border:none;border-top:1px dashed #000;margin:6px 0;">
            <div style="display:flex;justify-content:space-between;font-weight:bold;">
              <span>TOTAL ITEMS VALUE:</span>
              <span>&#8377;${String.format(Locale.US, "%.2f", order.itemsValue)}</span>
            </div>
            </body></html>
        """.trimIndent()

        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "KOT_${order.orderId.takeLast(6)}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun isActionableKotOrder(order: StoreOrder): Boolean {
        if (!order.status.equals("PLACED", ignoreCase = true)) return false
        val isUnverifiedUpi = order.paymentMode.equals("UPI", ignoreCase = true) &&
            !order.paymentStatus.equals("VERIFIED", ignoreCase = true)
        return !isUnverifiedUpi
    }

    fun formatIncomingKotAlertText(order: StoreOrder): String {
        val code = order.orderId.takeLast(6).ifBlank { "NEW" }
        val qtyTotal = order.items.sumOf { it.qty.coerceAtLeast(1) }
        val itemsLabel = if (qtyTotal > 0) "$qtyTotal item${if (qtyTotal > 1) "s" else ""} • " else ""
        val valueLabel = if (order.itemsValue > 0.0) {
            " (${EarningsCalculator.formatRupees(order.itemsValue)})"
        } else ""
        return "New KOT #$code: ${itemsLabel}${order.paymentMode.ifBlank { "COD" }}$valueLabel"
    }

    @Volatile
    private var sharedToneGenerator: ToneGenerator? = null

    fun playOrderChimePulse() {
        runCatching {
            val toneGen = sharedToneGenerator ?: synchronized(this) {
                sharedToneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).also {
                    sharedToneGenerator = it
                }
            }
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 420)
        }
    }
}
