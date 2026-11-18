package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.StoreUiState
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.InfoSoft
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningSoft
import com.example.util.EarningsCalculator

@Composable
fun PartnerTopBarAndAlerts(
    state: StoreUiState,
    onRingingClick: () -> Unit,
    onToggleDuty: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenEarningsDashboard: () -> Unit,
    onOpenSecurityShield: () -> Unit,
    onOpenAdminCommission: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        color = BgSurface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Suspended Banner
            if (state.storeProfile.suspended) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DangerRed.copy(alpha = 0.14f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Store suspended by Admin — order processing locked.",
                        color = DangerSoft,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Store-Specific Admin Note
            if (state.storeProfile.adminNote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SunnyYellow.copy(alpha = 0.24f))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Admin Note: ${state.storeProfile.adminNote}",
                        color = WarningSoft,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Global Broadcast Banner
            if (state.adminSettings.storeAnnouncement.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InfoSky.copy(alpha = 0.10f))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint = InfoSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.adminSettings.storeAnnouncement,
                        color = InfoSoft,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Main Header Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(EmeraldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = state.storeProfile.name.ifBlank { "Gossaigaon Bazar" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    state.storeProfile.suspended -> "Account Suspended"
                                    state.storeProfile.approved -> "Verified Store Partner"
                                    else -> "Pending Admin Verification"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    state.storeProfile.suspended -> DangerSoft
                                    state.storeProfile.approved -> FreshGreen
                                    else -> WarningSoft
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
                            tint = TextMuted
                        )
                    }
                }

                // Compact horizontal action strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.ringingOrderCount > 0) {
                        val pulse = rememberInfiniteTransition(label = "ring_pulse")
                        val scale by pulse.animateFloat(
                            initialValue = 0.96f,
                            targetValue = 1.04f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(550),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "ring_scale"
                        )
                        Surface(
                            color = DangerRed,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onRingingClick() }
                                .testTag("ringing_alert_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${state.ringingOrderCount} NEW ORDER${if (state.ringingOrderCount > 1) "S" else ""}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Duty Pill
                    Surface(
                        color = if (state.storeProfile.isOpen) EmeraldSoft else BgInput,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.storeProfile.isOpen) EmeraldPrimary.copy(alpha = 0.45f) else BorderSlate
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onToggleDuty() }
                            .testTag("duty_toggle_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.storeProfile.isOpen) EmeraldPrimary else DangerRed)
                            )
                            Text(
                                text = if (state.storeProfile.isOpen) "Store Open" else "Store Closed",
                                color = if (state.storeProfile.isOpen) Color(0xFF14532D) else TextMain,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Sound Toggle Pill
                    Surface(
                        color = BgInput,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onToggleSound() }
                            .testTag("sound_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (state.soundEnabled) {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                },
                                contentDescription = "Toggle order sound",
                                tint = if (state.soundEnabled) EmeraldPrimary else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (state.soundEnabled) "Sound On" else "Muted",
                                color = TextMain,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Security Shield Quick Pill
                    val hasRiskAlerts = state.securityPosture.flaggedOrderCount > 0 || state.securityConfig.panicSafeModeActive
                    Surface(
                        color = if (hasRiskAlerts) DangerRed.copy(alpha = 0.14f) else EmeraldSoft,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            1.dp,
                            if (hasRiskAlerts) DangerRed.copy(alpha = 0.55f) else EmeraldPrimary.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenSecurityShield() }
                            .testTag("open_security_shield_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Open Security Shield",
                                tint = if (hasRiskAlerts) DangerSoft else EmeraldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = when {
                                    state.securityConfig.panicSafeModeActive -> "Panic Lock"
                                    state.securityPosture.flaggedOrderCount > 0 -> "${state.securityPosture.flaggedOrderCount} Risk Alert"
                                    else -> "Shield ${state.securityPosture.overallScore}%"
                                },
                                color = if (hasRiskAlerts) DangerSoft else Color(0xFF14532D),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Earnings Dashboard Quick Pill
                    Surface(
                        color = EmeraldSoft,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenEarningsDashboard() }
                            .testTag("open_earnings_dashboard_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Open Earnings Dashboard",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Earnings ${EarningsCalculator.formatRupees(state.netStoreShare)}",
                                color = Color(0xFF14532D),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Admin Commission Management Pill
                    val resolvedStoreComm = remember(state.uid, state.commissionRules) {
                        EarningsCalculator.resolveCommission(null, state.uid, null, state.commissionRules)
                    }
                    Surface(
                        color = SunnyYellow.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SunnyYellow),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenAdminCommission() }
                            .testTag("admin_commission_hub_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Commission Management",
                                tint = SunnyAmberInk,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Comm ${EarningsCalculator.formatPercent(resolvedStoreComm.percent)}",
                                color = SunnyAmberInk,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderSlate)
        }
    }
}
