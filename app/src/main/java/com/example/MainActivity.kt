package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StoreViewModel
import com.example.ui.components.PartnerToastBanner
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PartnerConsoleScaffold
import com.example.ui.theme.GossaigaonStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            GossaigaonStoreTheme {
                val storeViewModel: StoreViewModel = viewModel()
                val uiState by storeViewModel.uiState.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!uiState.isAuthenticated) {
                        AuthScreen(
                            isLoading = uiState.authLoading,
                            authError = uiState.authError,
                            authSuccessMessage = uiState.authSuccessMessage,
                            authLockoutSeconds = uiState.authLockoutSeconds,
                            pendingEmailVerification = uiState.pendingEmailVerification,
                            verificationEmail = uiState.verificationEmail,
                            resendCooldownSeconds = uiState.resendVerificationCooldownSeconds,
                            onClearError = { storeViewModel.clearAuthError() },
                            onLogin = { email, password ->
                                storeViewModel.login(email, password)
                            },
                            onRegister = { name, email, cuisine, addr, phone, upi, pass, lat, lng ->
                                storeViewModel.registerStore(
                                    name, email, cuisine, addr, phone, upi, pass, lat, lng
                                )
                            },
                            onGoogleSignInToken = { idToken ->
                                storeViewModel.signInWithGoogleIdToken(idToken)
                            },
                            onGoogleSignInError = { err ->
                                storeViewModel.reportGoogleSignInError(err)
                            },
                            onSendPasswordReset = { email, onDone ->
                                storeViewModel.sendPasswordReset(email, onDone)
                            },
                            onResendVerification = {
                                storeViewModel.resendVerificationEmail()
                            },
                            onCheckEmailVerified = {
                                storeViewModel.checkEmailVerification()
                            },
                            onSignOutVerification = {
                                storeViewModel.logout()
                            },
                            onToast = { msg, kind -> storeViewModel.showToast(msg, kind) }
                        )
                    } else {
                        PartnerConsoleScaffold(
                            state = uiState,
                            viewModel = storeViewModel
                        )
                    }

                    PartnerToastBanner(
                        toast = uiState.activeToast,
                        onDismiss = { storeViewModel.onToastTapped() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                    )
                }
            }
        }
    }
}
