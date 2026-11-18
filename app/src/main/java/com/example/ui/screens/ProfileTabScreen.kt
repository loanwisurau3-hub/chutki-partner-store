package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AdminSettings
import com.example.model.CommissionRules
import com.example.model.SecurityPosture
import com.example.model.SecurityShieldConfig
import com.example.model.StoreFeedback
import com.example.model.StoreProfile
import com.example.model.ToastKind
import com.example.ui.components.StoreFeedbackDialog
import com.example.ui.components.partnerFieldColors
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.util.EarningsCalculator
import com.example.util.StoreUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileTabScreen(
    storeProfile: StoreProfile,
    adminSettings: AdminSettings,
    commissionRules: CommissionRules,
    feedbacks: List<StoreFeedback>,
    securityPosture: SecurityPosture = SecurityPosture(),
    securityConfig: SecurityShieldConfig = SecurityShieldConfig(),
    onOpenSecurityShield: () -> Unit = {},
    onOpenAdminCommission: () -> Unit,
    onSaveProfile: (
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
    ) -> Unit,
    onSubmitFeedback: (subject: String, message: String, onDone: () -> Unit) -> Unit,
    onToast: (String, ToastKind) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(storeProfile.name) { mutableStateOf(storeProfile.name) }
    var upiId by remember(storeProfile.upiId) { mutableStateOf(storeProfile.upiId) }
    var cuisine by remember(storeProfile.cuisine) { mutableStateOf(storeProfile.cuisine) }
    var phone by remember(storeProfile.phone) { mutableStateOf(storeProfile.phone) }
    var discountPct by remember(storeProfile.storeDiscountPercent) {
        mutableStateOf(storeProfile.storeDiscountPercent.toString())
    }
    var discountMin by remember(storeProfile.storeDiscountMinOrder) {
        mutableStateOf(storeProfile.storeDiscountMinOrder.toInt().toString())
    }
    var address by remember(storeProfile.address) { mutableStateOf(storeProfile.address) }
    var hours by remember(storeProfile.hours) { mutableStateOf(storeProfile.hours) }
    var imageUrl by remember(storeProfile.imageUrl) { mutableStateOf(storeProfile.imageUrl) }
    var latText by remember(storeProfile.lat) { mutableStateOf(storeProfile.lat.toString()) }
    var lngText by remember(storeProfile.lng) { mutableStateOf(storeProfile.lng.toString()) }
    var gpsStatus by remember { mutableStateOf("") }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun detectStoreGps() {
        gpsStatus = "Locating..."
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    latText = String.format(Locale.US, "%.6f", loc.latitude)
                    lngText = String.format(Locale.US, "%.6f", loc.longitude)
                    gpsStatus = "GPS Pinned"
                    scope.launch {
                        val resolved = withContext(Dispatchers.IO) {
                            runCatching {
                                @Suppress("DEPRECATION")
                                Geocoder(context, Locale.getDefault())
                                    .getFromLocation(loc.latitude, loc.longitude, 1)
                                    ?.firstOrNull()?.getAddressLine(0)
                            }.getOrNull()
                        }
                        if (!resolved.isNullOrBlank()) {
                            address = "${resolved.split(",").take(3).joinToString(", ")}, Gossaigaon"
                        }
                        onToast("Store GPS Fixed! Tap Save.", ToastKind.SUCCESS)
                    }
                } else {
                    gpsStatus = "Retry GPS"
                    onToast("Could not get GPS fix.", ToastKind.WARNING)
                }
            }
            .addOnFailureListener {
                gpsStatus = "Retry GPS"
                onToast("GPS Error: ${it.localizedMessage}", ToastKind.DANGER)
            }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) detectStoreGps()
        else onToast("Location permission denied.", ToastKind.WARNING)
    }

    if (showFeedbackDialog) {
        StoreFeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { subject, msg ->
                onSubmitFeedback(subject, msg) {
                    showFeedbackDialog = false
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Security, Anti-Scam & Hacker Shield Card
        item {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, FreshGreen.copy(alpha = 0.65f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = FreshGreen)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hacker, Spam & Scam Shield",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshGreen
                            )
                            Text(
                                text = "Score ${securityPosture.overallScore}/100 • ${securityPosture.flaggedOrderCount} flagged order(s) • UPI Lock ${if (securityConfig.upiChangeLockEnabled) "ON" else "OFF"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Button(
                        onClick = onOpenSecurityShield,
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_open_security_shield_button")
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Security & Anti-Scam Center", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Admin Commission Management & Reports Card
        item {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SunnyYellow.copy(alpha = 0.75f)),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SunnyAmberInk)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chutki Commission & Earnings Admin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SunnyAmberInk
                            )
                            Text(
                                text = "Global ${EarningsCalculator.formatPercent(commissionRules.globalCommissionPercent)} • Store (${commissionRules.storeCommissions.size}) • Category (${commissionRules.categoryCommissions.size}) • Product (${commissionRules.productCommissions.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Button(
                        onClick = onOpenAdminCommission,
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_open_admin_commission_button")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage Commission Rules & Reports", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Helpline & Feedback Card
        item {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.55f)),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = InfoSky)
                        Column {
                            Text(
                                text = "Gossaigaon Bazar Helpline & Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = InfoSky
                            )
                            Text(
                                text = if (adminSettings.supportHours.isNotBlank()) {
                                    "Helpline Hours: ${adminSettings.supportHours}"
                                } else {
                                    "Need hub help or want to suggest an improvement?"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (adminSettings.supportPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = { StoreUtils.openDialer(context, adminSettings.supportPhone) },
                                border = BorderStroke(1.dp, BorderSlate),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = TextMain, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call Admin", color = TextMain)
                            }
                        }
                        val waTarget = adminSettings.supportWhatsapp.ifBlank { adminSettings.supportPhone }
                        if (waTarget.isNotBlank()) {
                            Button(
                                onClick = { StoreUtils.openWhatsApp(context, waTarget) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessTeal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { showFeedbackDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfoSky,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("open_feedback_button")
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Feedback", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    feedbacks.take(3).forEach { fb ->
                        Surface(
                            color = BgInput,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = fb.subject,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextMain
                                    )
                                    Text(
                                        text = fb.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (fb.status == "RESOLVED") SuccessTeal else WarningSoft
                                    )
                                }
                                if (fb.adminReply.isNotBlank()) {
                                    Text(
                                        text = "Admin: ${fb.adminReply}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InfoSky
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Store Profile, Store-Wide Discount & GPS Location Card
        item {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderSlate),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Store Profile, Offers & GPS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val fine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fine || coarse) detectStoreGps()
                                else gpsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            },
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("detect_store_gps_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = gpsStatus.ifBlank { "Auto-Detect GPS" },
                                color = EmeraldPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Store Name") },
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_store_name_input")
                    )
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = {
                            Text(
                                if (securityConfig.upiChangeLockEnabled) {
                                    "Store Payout UPI ID (Locked by Shield)"
                                } else {
                                    "Store Payout UPI ID"
                                }
                            )
                        },
                        enabled = !securityConfig.upiChangeLockEnabled || storeProfile.upiId.isBlank(),
                        leadingIcon = if (securityConfig.upiChangeLockEnabled) {
                            { Icon(Icons.Default.Lock, contentDescription = "UPI Locked", tint = SunnyAmberInk) }
                        } else null,
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_upi_input")
                    )
                    OutlinedTextField(
                        value = cuisine,
                        onValueChange = { cuisine = it },
                        label = { Text("Categories") },
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10) phone = it.filter(Char::isDigit) },
                        label = { Text("Contact Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Store-Wide Customer Discount Offer
                    Surface(
                        color = WarningAmber.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Store-Wide Customer Discount Offer",
                                    color = WarningAmber,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = discountPct,
                                    onValueChange = { discountPct = it.filter(Char::isDigit) },
                                    label = { Text("Discount (% OFF)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = partnerFieldColors(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = discountMin,
                                    onValueChange = { discountMin = it.filter(Char::isDigit) },
                                    label = { Text("Min Order (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = partnerFieldColors(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Set 0% to disable store-wide discount. Automatically applies in Customer checkout!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Store Physical Address") },
                        colors = partnerFieldColors(),
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // GPS Pinpoint Box
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Store GPS Pinpoint (Rider Navigation)",
                                color = EmeraldPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = latText,
                                    onValueChange = { latText = it },
                                    label = { Text("Latitude") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = partnerFieldColors(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = lngText,
                                    onValueChange = { lngText = it },
                                    label = { Text("Longitude") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = partnerFieldColors(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Operating Hours") },
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Store Banner Image URL") },
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveProfile(
                                name,
                                upiId,
                                cuisine,
                                phone,
                                discountPct,
                                discountMin,
                                address,
                                hours,
                                imageUrl,
                                latText,
                                lngText
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_store_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Store Profile, Offers & GPS", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
