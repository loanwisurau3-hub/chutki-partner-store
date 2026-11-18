package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.OrderRiskAssessment
import com.example.model.OrderRiskLevel
import com.example.model.PhishingScanResult
import com.example.model.SecurityPosture
import com.example.model.SecurityShieldConfig
import com.example.model.StoreOrder
import com.example.model.StoreProfile
import com.example.ui.components.PartnerBadge
import com.example.ui.components.partnerFieldColors
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgDark
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningSoft
import com.example.util.EarningsCalculator
import com.example.util.SecurityGuard

@Composable
fun SecurityShieldScreen(
    storeProfile: StoreProfile,
    securityConfig: SecurityShieldConfig,
    securityPosture: SecurityPosture,
    orders: List<StoreOrder>,
    riskAssessments: Map<String, OrderRiskAssessment>,
    onBack: () -> Unit,
    onUpdateConfig: ((SecurityShieldConfig) -> SecurityShieldConfig) -> Unit,
    onTogglePanicSafeMode: () -> Unit,
    onMarkOrderSafe: (String) -> Unit,
    onReportSpamOrder: (StoreOrder, OrderRiskAssessment) -> Unit
) {
    BackHandler(onBack = onBack)

    var scannerInput by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<PhishingScanResult?>(null) }

    val flaggedOrders = remember(orders, riskAssessments) {
        orders.mapNotNull { order ->
            val assessment = riskAssessments[order.orderId] ?: return@mapNotNull null
            if (assessment.level != OrderRiskLevel.LOW) order to assessment else null
        }.sortedByDescending { it.second.score }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Surface(
            color = BgSurface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("security_shield_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextMain
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Security & Anti-Scam Shield",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Hacker Defense • Spam & Scam Guard",
                            style = MaterialTheme.typography.labelSmall,
                            color = FreshGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PartnerBadge(
                        text = "${securityPosture.overallScore}/100",
                        containerColor = if (securityPosture.overallScore >= 80) EmeraldSoft else SunnyYellow.copy(alpha = 0.25f),
                        contentColor = if (securityPosture.overallScore >= 80) FreshGreen else SunnyAmberInk
                    )
                }
                HorizontalDivider(color = BorderSlate)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .testTag("security_shield_list")
        ) {
            // 1. Security Posture Score Hero Card
            item {
                SecurityPostureHeroCard(
                    posture = securityPosture,
                    storeProfile = storeProfile,
                    config = securityConfig,
                    modifier = Modifier.widthIn(max = 880.dp)
                )
            }

            // 2. Panic Safe-Mode Card
            item {
                PanicSafeModeCard(
                    panicActive = securityConfig.panicSafeModeActive,
                    onTogglePanic = onTogglePanicSafeMode,
                    modifier = Modifier.widthIn(max = 880.dp)
                )
            }

            // 3. Smart Scam & Hacker Protection Switches
            item {
                SecurityControlsCard(
                    config = securityConfig,
                    onUpdateConfig = onUpdateConfig,
                    modifier = Modifier.widthIn(max = 880.dp)
                )
            }

            // 4. Runtime Hacker & Transport Audit
            item {
                RuntimeIntegrityAuditCard(
                    posture = securityPosture,
                    config = securityConfig,
                    modifier = Modifier.widthIn(max = 880.dp)
                )
            }

            // 5. Smart Phishing, Link & Message Scam Scanner
            item {
                PhishingScannerCard(
                    input = scannerInput,
                    onInputChange = { scannerInput = it },
                    scanResult = scanResult,
                    onRunScan = {
                        scanResult = SecurityGuard.scanPhishingOrScamMessage(scannerInput)
                    },
                    modifier = Modifier.widthIn(max = 880.dp)
                )
            }

            // 6. Flagged Spam / Scam Orders Header
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 880.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = if (flaggedOrders.isNotEmpty()) DangerRed else FreshGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Flagged Spam & Scam Orders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain
                        )
                    }
                    PartnerBadge(
                        text = "${flaggedOrders.size} flagged",
                        containerColor = if (flaggedOrders.isNotEmpty()) DangerRed.copy(alpha = 0.14f) else EmeraldSoft,
                        contentColor = if (flaggedOrders.isNotEmpty()) DangerSoft else FreshGreen
                    )
                }
            }

            if (flaggedOrders.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 880.dp)
                            .fillMaxWidth()
                            .testTag("security_no_threats_card"),
                        color = BgCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppGood,
                                contentDescription = null,
                                tint = FreshGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "No Suspicious Orders Detected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain
                                )
                                Text(
                                    text = "Live fraud scoring monitors COD limits, order bursts, duplicates, tampered totals, and phishing notes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = flaggedOrders,
                    key = { "flagged_${it.first.orderId}" }
                ) { (order, assessment) ->
                    FlaggedOrderRiskCard(
                        order = order,
                        assessment = assessment,
                        onMarkSafe = { onMarkOrderSafe(order.orderId) },
                        onReportSpam = { onReportSpamOrder(order, assessment) },
                        modifier = Modifier.widthIn(max = 880.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityPostureHeroCard(
    posture: SecurityPosture,
    storeProfile: StoreProfile,
    config: SecurityShieldConfig,
    modifier: Modifier = Modifier
) {
    val isHealthy = posture.overallScore >= 80 && !posture.runtimeTamperDetected && !config.panicSafeModeActive
    val accent = when {
        config.panicSafeModeActive || posture.overallScore < 60 -> DangerRed
        posture.overallScore < 80 || posture.flaggedOrderCount > 0 -> SunnyAmberInk
        else -> FreshGreen
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("security_score_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.65f)),
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            if (isHealthy) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                            BgSurface
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "MERCHANT SECURITY POSTURE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
                PartnerBadge(
                    text = if (config.panicSafeModeActive) "PANIC LOCK ON" else if (isHealthy) "PROTECTED" else "REVIEW",
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${posture.overallScore}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    modifier = Modifier.testTag("security_posture_score")
                )
                Text(
                    text = "/ 100 Shield Rating",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecurityStatBox(
                    label = "Flagged Orders",
                    value = "${posture.flaggedOrderCount}",
                    color = if (posture.flaggedOrderCount > 0) DangerSoft else FreshGreen,
                    modifier = Modifier.weight(1f)
                )
                SecurityStatBox(
                    label = "Runtime Check",
                    value = if (posture.runtimeTamperDetected) "Alert" else "Clean",
                    color = if (posture.runtimeTamperDetected) DangerSoft else FreshGreen,
                    modifier = Modifier.weight(1f)
                )
                SecurityStatBox(
                    label = "UPI Lock",
                    value = if (config.upiChangeLockEnabled) "Locked" else "Off",
                    color = if (config.upiChangeLockEnabled) FreshGreen else SunnyAmberInk,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!storeProfile.approved) {
                Text(
                    text = "Tip: Admin verification and UPI Change Lock boost your store protection score.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SecurityStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BgSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun PanicSafeModeCard(
    panicActive: Boolean,
    onTogglePanic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("panic_safe_mode_card"),
        color = if (panicActive) DangerRed.copy(alpha = 0.08f) else BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (panicActive) DangerRed else BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (panicActive) DangerRed else SunnyAmberInk,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency Panic Lock",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "Instantly closes Store Duty and freezes UPI payout requests if under spam attack or account threat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Button(
                onClick = onTogglePanic,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (panicActive) FreshGreen else DangerRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("panic_lock_button")
            ) {
                Icon(
                    imageVector = if (panicActive) Icons.Default.GppGood else Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (panicActive) "Deactivate Panic Lock" else "Activate Emergency Panic Lock",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SecurityControlsCard(
    config: SecurityShieldConfig,
    onUpdateConfig: ((SecurityShieldConfig) -> SecurityShieldConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("security_controls_card"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Smart Anti-Spam, Anti-Scam & Hacker Rules",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextMain
            )

            SecurityToggleRow(
                title = "Smart Order Scam Shield",
                subtitle = "Score incoming COD/UPI orders for tampered totals, fake phones, and scam notes",
                checked = config.scamShieldEnabled,
                tag = "toggle_scam_shield",
                onCheckedChange = { enabled -> onUpdateConfig { it.copy(scamShieldEnabled = enabled) } }
            )
            HorizontalDivider(color = BorderSlate)

            SecurityToggleRow(
                title = "Order Bombing & Duplicate Guard",
                subtitle = "Flag rapid bursts (3+ orders/30m), repeat cancellers, and accidental duplicates",
                checked = config.velocityGuardEnabled && config.duplicateOrderGuardEnabled,
                tag = "toggle_velocity_guard",
                onCheckedChange = { enabled ->
                    onUpdateConfig {
                        it.copy(velocityGuardEnabled = enabled, duplicateOrderGuardEnabled = enabled)
                    }
                }
            )
            HorizontalDivider(color = BorderSlate)

            SecurityToggleRow(
                title = "UPI Payout Change Lock",
                subtitle = "Block unauthorized hackers from silently swapping your payout UPI handle",
                checked = config.upiChangeLockEnabled,
                tag = "toggle_upi_lock",
                onCheckedChange = { enabled -> onUpdateConfig { it.copy(upiChangeLockEnabled = enabled) } }
            )
            HorizontalDivider(color = BorderSlate)

            SecurityToggleRow(
                title = "Strict Link & XSS Sanitizer",
                subtitle = "Block cleartext HTTP, javascript: URIs, and shortened phishing links",
                checked = config.strictLinkSanitizer,
                tag = "toggle_link_sanitizer",
                onCheckedChange = { enabled -> onUpdateConfig { it.copy(strictLinkSanitizer = enabled) } }
            )

            HorizontalDivider(color = BorderSlate)
            Text(
                text = "High-Value COD Verification Limit",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1000.0, 1500.0, 2500.0).forEach { limit ->
                    val selected = config.codVerificationLimit == limit
                    Surface(
                        color = if (selected) FreshGreen else BgInput,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) FreshGreen else BorderSlate),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateConfig { it.copy(codVerificationLimit = limit) } }
                            .testTag("cod_limit_${limit.toInt()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = EarningsCalculator.formatRupees(limit),
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (selected) Color.White else TextMain,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FreshGreen
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun RuntimeIntegrityAuditCard(
    posture: SecurityPosture,
    config: SecurityShieldConfig,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("runtime_integrity_card"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = InfoSky,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Hacker Defense & App Hardening Audit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
            }

            AuditLine("HTTPS / TLS Transport", "Enforced (Cleartext Blocked)", true)
            AuditLine("ADB Backup Extraction", "Blocked (allowBackup=false)", true)
            AuditLine("KOT Print WebView Sandbox", "JS & File Access Disabled", true)
            AuditLine("Login Brute-Force Guard", "5-Try Cooldown Lockout Active", true)
            AuditLine("URL & Firebase Key Sanitizer", if (config.strictLinkSanitizer) "Strict Mode Active" else "Standard Mode", config.strictLinkSanitizer)
            AuditLine(
                "Device Root / Hook / Debugger",
                if (posture.runtimeTamperDetected) {
                    posture.runtimeFindings.joinToString("; ")
                } else {
                    "Clean Runtime"
                },
                !posture.runtimeTamperDetected
            )
        }
    }
}

@Composable
private fun AuditLine(
    label: String,
    status: String,
    ok: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        PartnerBadge(
            text = status,
            containerColor = if (ok) EmeraldSoft else DangerRed.copy(alpha = 0.14f),
            contentColor = if (ok) FreshGreen else DangerSoft
        )
    }
}

@Composable
private fun PhishingScannerCard(
    input: String,
    onInputChange: (String) -> Unit,
    scanResult: PhishingScanResult?,
    onRunScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("phishing_scanner_card"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Smart Phishing, Link & Scam Checker",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Text(
                        text = "Paste a suspicious SMS, WhatsApp refund link, or caller number to verify before replying.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("Suspicious message, URL, or phone") },
                placeholder = { Text("e.g. Install AnyDesk for KYC or bit.ly/refund") },
                colors = partnerFieldColors(),
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phishing_scanner_input")
            )

            Button(
                onClick = onRunScan,
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = InfoSky),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("phishing_scan_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan for Scam / Phishing", fontWeight = FontWeight.ExtraBold)
            }

            if (scanResult != null) {
                val resBg = if (scanResult.isThreat) DangerRed.copy(alpha = 0.12f) else EmeraldSoft
                val resFg = if (scanResult.isThreat) DangerSoft else FreshGreen
                Surface(
                    color = resBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, resFg.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phishing_scan_result")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (scanResult.isThreat) {
                                "${scanResult.level.label.uppercase()} (${scanResult.riskScore}/100)"
                            } else {
                                "SAFE — No known scam or phishing pattern detected"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = resFg
                        )
                        if (scanResult.findings.isNotEmpty()) {
                            Text(
                                text = scanResult.findings.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = resFg
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlaggedOrderRiskCard(
    order: StoreOrder,
    assessment: OrderRiskAssessment,
    onMarkSafe: () -> Unit,
    onReportSpam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = assessment.level == OrderRiskLevel.CRITICAL || assessment.level == OrderRiskLevel.HIGH
    val badgeBg = if (isCritical) DangerRed.copy(alpha = 0.14f) else SunnyYellow.copy(alpha = 0.28f)
    val badgeFg = if (isCritical) DangerSoft else WarningSoft

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("flagged_order_card_${order.orderId}"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isCritical) DangerRed.copy(alpha = 0.55f) else BorderSlate),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.orderId.takeLast(6)} • ${EarningsCalculator.formatRupees(order.itemsValue)} (${order.paymentMode})",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
                PartnerBadge(
                    text = "${assessment.level.label} (${assessment.score})",
                    containerColor = badgeBg,
                    contentColor = badgeFg
                )
            }

            Text(
                text = assessment.reasons.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = badgeFg,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkSafe,
                    border = BorderStroke(1.dp, FreshGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("mark_safe_${order.orderId}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FreshGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Safe", color = FreshGreen, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onReportSpam,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("report_spam_${order.orderId}")
                ) {
                    Icon(Icons.Default.GppBad, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Report & Block", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
