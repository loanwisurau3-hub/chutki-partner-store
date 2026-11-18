package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.model.ToastKind
import com.example.ui.components.partnerFieldColors
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.util.SecurityGuard
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthScreen(
    isLoading: Boolean,
    authError: String,
    authSuccessMessage: String = "",
    authLockoutSeconds: Int = 0,
    pendingEmailVerification: Boolean = false,
    verificationEmail: String = "",
    resendCooldownSeconds: Int = 0,
    onClearError: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (
        name: String,
        email: String,
        cuisine: String,
        address: String,
        phone: String,
        upiId: String,
        password: String,
        lat: Double,
        lng: Double
    ) -> Unit,
    onGoogleSignInToken: (idToken: String) -> Unit = {},
    onGoogleSignInError: (String) -> Unit = {},
    onSendPasswordReset: (email: String, onDone: () -> Unit) -> Unit = { _, _ -> },
    onResendVerification: () -> Unit = {},
    onCheckEmailVerified: () -> Unit = {},
    onSignOutVerification: () -> Unit = {},
    onToast: (String, ToastKind) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignupMode by remember { mutableStateOf(false) }

    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }

    var googleSignInBusy by remember { mutableStateOf(false) }

    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regCuisine by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regUpi by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regLat by remember { mutableDoubleStateOf(26.4524) }
    var regLng by remember { mutableDoubleStateOf(89.9723) }
    var gpsBusy by remember { mutableStateOf(false) }

    fun launchDirectGoogleSignIn() {
        val clientId = runCatching {
            context.getString(R.string.default_web_client_id)
        }.getOrNull().orEmpty()
        if (clientId.isBlank()) {
            onGoogleSignInError("Google Sign-In client ID is not configured.")
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            onGoogleSignInError("Unable to open Google account chooser from this screen.")
            return
        }
        googleSignInBusy = true
        onClearError()
        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    googleSignInBusy = false
                    onGoogleSignInToken(googleIdToken)
                } else {
                    googleSignInBusy = false
                    onGoogleSignInError("Unexpected Google credential response.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled by user: ${e.message}", e)
                googleSignInBusy = false
            } catch (e: NoCredentialException) {
                Log.w("Auth", "No Google credentials found on device: ${e.message}", e)
                googleSignInBusy = false
                onGoogleSignInError("No Google account found on device. Add a Google account or sign in with email.")
            } catch (e: Exception) {
                Log.e("Auth", "Direct Google Sign-In failed", e)
                googleSignInBusy = false
                onGoogleSignInError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGps() {
        gpsBusy = true
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    regLat = loc.latitude
                    regLng = loc.longitude
                    scope.launch {
                        val addr = withContext(Dispatchers.IO) {
                            runCatching {
                                @Suppress("DEPRECATION")
                                val list = Geocoder(context, Locale.getDefault())
                                    .getFromLocation(loc.latitude, loc.longitude, 1)
                                list?.firstOrNull()?.getAddressLine(0)
                            }.getOrNull()
                        }
                        if (!addr.isNullOrBlank()) {
                            regAddress = addr.split(",").take(3).joinToString(", ")
                        } else if (regAddress.isBlank()) {
                            regAddress = "Gossaigaon Bazar"
                        }
                        gpsBusy = false
                        onToast("Store GPS Attached!", ToastKind.SUCCESS)
                    }
                } else {
                    gpsBusy = false
                    onToast("Unable to lock GPS right now.", ToastKind.WARNING)
                }
            }
            .addOnFailureListener {
                gpsBusy = false
                onToast("GPS Error: ${it.localizedMessage}", ToastKind.DANGER)
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentGps()
        } else {
            onToast("Location permission denied.", ToastKind.WARNING)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE6F7EE), BgDark)
                )
            )
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            color = BgSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderSlate),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Gossaigaon Partner",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Gossaigaon Partner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Store Partner & Kirana Command Center",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))

                if (pendingEmailVerification) {
                    EmailVerificationGateCard(
                        email = verificationEmail,
                        isLoading = isLoading,
                        resendCooldownSeconds = resendCooldownSeconds,
                        onCheckVerified = onCheckEmailVerified,
                        onResendEmail = onResendVerification,
                        onSignOut = onSignOutVerification
                    )
                } else {
                    // Direct Google Sign-In Card Action
                    DirectGoogleLoginButton(
                        busy = googleSignInBusy || isLoading,
                        onClick = { launchDirectGoogleSignIn() }
                    )

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSlate)
                        Text(
                            text = "OR USE STORE EMAIL",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSlate)
                    }
                    Spacer(Modifier.height(14.dp))

                    Surface(
                        color = BgInput,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            AuthTabPill(
                                text = "Store Login",
                                selected = !isSignupMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_login")
                            ) {
                                isSignupMode = false
                                onClearError()
                            }
                            AuthTabPill(
                                text = "Register Store",
                                selected = isSignupMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_signup")
                            ) {
                                isSignupMode = true
                                onClearError()
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (!isSignupMode) {
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("Registered Store Email") },
                            placeholder = { Text("store@gossaigaonbazar.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = partnerFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            visualTransformation = if (loginPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { loginPasswordVisible = !loginPasswordVisible },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (loginPasswordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (loginPasswordVisible) "Hide password" else "Show password",
                                        tint = TextMuted
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = partnerFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmailInput = loginEmail.trim()
                                    onClearError()
                                    showForgotDialog = true
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("forgot_password_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Forgot Password?",
                                    color = EmeraldPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { onLogin(loginEmail, loginPassword) },
                            enabled = !isLoading && !googleSignInBusy && authLockoutSeconds == 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (authLockoutSeconds > 0) {
                                Text("Locked (${authLockoutSeconds}s)", fontWeight = FontWeight.ExtraBold)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Launch Store Dashboard", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("Store Name") },
                                placeholder = { Text("e.g. Maa Durga Grocery Store") },
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_name_input")
                            )
                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text("Store Email Address") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_email_input")
                            )
                            OutlinedTextField(
                                value = regCuisine,
                                onValueChange = { regCuisine = it },
                                label = { Text("Category") },
                                placeholder = { Text("Vegetables, Staples, Daily Goods") },
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_category_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PHYSICAL ADDRESS IN GOSSAIGAON",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                OutlinedButton(
                                    onClick = {
                                        val fineGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        val coarseGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (fineGranted || coarseGranted) {
                                            fetchCurrentGps()
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    border = BorderStroke(1.dp, EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !gpsBusy
                                ) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (gpsBusy) "Locating..." else "Auto GPS",
                                        color = EmeraldPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = regAddress,
                                onValueChange = { regAddress = it },
                                label = { Text("Store Address") },
                                placeholder = { Text("e.g. Main Road Bazar, Gossaigaon") },
                                colors = partnerFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_address_input")
                            )
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { if (it.length <= 10) regPhone = it.filter(Char::isDigit) },
                                label = { Text("10-Digit Store Mobile Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_phone_input")
                            )
                            OutlinedTextField(
                                value = regUpi,
                                onValueChange = { regUpi = it },
                                label = { Text("Store UPI ID (For Payouts)") },
                                placeholder = { Text("e.g. storeowner@okicici") },
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_upi_input")
                            )
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("Create Strong Password (min 8 chars, letters + digits)") },
                                visualTransformation = if (regPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { regPasswordVisible = !regPasswordVisible },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (regPasswordVisible) "Hide password" else "Show password",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input")
                            )
                            if (regPassword.isNotEmpty()) {
                                val pwStrength = remember(regPassword) {
                                    SecurityGuard.evaluatePasswordStrength(regPassword)
                                }
                                val meterColor = when {
                                    pwStrength.score >= 3 -> FreshGreen
                                    pwStrength.score == 2 -> SunnyAmberInk
                                    else -> DangerRed
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_strength_indicator"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (pwStrength.score / 4f).coerceIn(0.15f, 1f) },
                                        color = meterColor,
                                        trackColor = BgInput,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Text(
                                        text = "${pwStrength.label}: ${pwStrength.message}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = meterColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "A verification link will be sent to your store email upon registration.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    onRegister(
                                        regName,
                                        regEmail,
                                        regCuisine,
                                        regAddress,
                                        regPhone,
                                        regUpi,
                                        regPassword,
                                        regLat,
                                        regLng
                                    )
                                },
                                enabled = !isLoading && !googleSignInBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("reg_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Store, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Register Partner Store", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                if (authSuccessMessage.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = EmeraldSoft,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FreshGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_success_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FreshGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = authSuccessMessage,
                                color = FreshGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (authError.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = authError,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("auth_error_text")
                    )
                }

                Spacer(Modifier.height(14.dp))
                Surface(
                    color = EmeraldSoft.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = FreshGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Protected by Merchant Shield • Verified Auth & TLS",
                            style = MaterialTheme.typography.labelSmall,
                            color = FreshGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showForgotDialog) {
            BackHandler(enabled = !isLoading) {
                showForgotDialog = false
            }
            val backdropInteraction = remember { MutableInteractionSource() }
            val cardInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.56f))
                    .clickable(
                        interactionSource = backdropInteraction,
                        indication = null,
                        onClick = { if (!isLoading) showForgotDialog = false }
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = BgSurface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = {}
                        )
                        .testTag("forgot_password_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Text(
                                text = "Reset Store Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }

                        Text(
                            text = "Enter your registered store email and we will send you a secure Firebase password reset link.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = { resetEmailInput = it },
                            label = { Text("Registered Store Email") },
                            placeholder = { Text("store@gossaigaonbazar.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = partnerFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_password_email_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showForgotDialog = false },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("cancel_reset_dialog_button")
                            ) {
                                Text("Cancel", color = TextMuted)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onSendPasswordReset(resetEmailInput) {
                                        showForgotDialog = false
                                    }
                                },
                                enabled = !isLoading && resetEmailInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("send_reset_link_button")
                            ) {
                                Text(
                                    text = if (isLoading) "Sending..." else "Send Reset Link",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectGoogleLoginButton(
    busy: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, BorderSlate),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = BgCard,
            contentColor = TextMain
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_signin_button")
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = EmeraldPrimary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
            Text("Connecting Google Account...", fontWeight = FontWeight.Bold)
        } else {
            Surface(
                color = Color(0xFFE8F0FE),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.4f)),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "G",
                        color = Color(0xFF1A73E8),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )
        }
    }
}

@Composable
private fun EmailVerificationGateCard(
    email: String,
    isLoading: Boolean,
    resendCooldownSeconds: Int,
    onCheckVerified: () -> Unit,
    onResendEmail: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_verification_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = EmeraldSoft,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = FreshGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "Verify Your Store Email",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain,
                textAlign = TextAlign.Center
            )

            Text(
                text = "To protect merchant payouts and prevent spam signups, verify your email before opening the dashboard:",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Surface(
                color = EmeraldSoft.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = email.ifBlank { "Your registered store email" },
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = FreshGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("verification_email_text")
                )
            }

            Text(
                text = "1. Open your inbox (and spam folder)\n2. Tap the Firebase verification link\n3. Return here and tap Continue",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = onCheckVerified,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("check_email_verified_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.MarkEmailRead, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("I've Verified — Continue", fontWeight = FontWeight.ExtraBold)
                }
            }

            OutlinedButton(
                onClick = onResendEmail,
                enabled = !isLoading && resendCooldownSeconds == 0,
                border = BorderStroke(1.dp, EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resend_verification_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (resendCooldownSeconds == 0) EmeraldPrimary else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (resendCooldownSeconds > 0) {
                        "Resend Link in ${resendCooldownSeconds}s"
                    } else {
                        "Resend Verification Email"
                    },
                    color = if (resendCooldownSeconds == 0) EmeraldPrimary else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onSignOut,
                enabled = !isLoading,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("verification_sign_out_button")
            ) {
                Text(
                    text = "Use Another Account / Back to Login",
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuthTabPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EmeraldPrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
