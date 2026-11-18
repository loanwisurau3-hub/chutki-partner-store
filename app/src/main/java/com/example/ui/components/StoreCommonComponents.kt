package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.CustomCategory
import com.example.model.ProductItem
import com.example.model.ToastKind
import com.example.model.ToastMessage
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.InfoSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.util.EarningsCalculator
import com.example.util.PricingValidationResult
import com.example.util.StoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PartnerImage(
    rawUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resolvedUrl = remember(rawUrl) {
        StoreUtils.cleanAndResolveImageUrl(rawUrl)
    }
    if (resolvedUrl.isBlank()) {
        ImagePlaceholderBox(modifier)
    } else if (resolvedUrl.startsWith("data:image")) {
        val bitmap: Bitmap? = remember(resolvedUrl) { StoreUtils.decodeBase64DataUri(resolvedUrl) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        } else {
            ImagePlaceholderBox(modifier)
        }
    } else {
        SubcomposeAsyncImage(
            model = resolvedUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = { ImagePlaceholderBox(Modifier.fillMaxSize()) },
            error = { ImagePlaceholderBox(Modifier.fillMaxSize()) }
        )
    }
}

@Composable
private fun ImagePlaceholderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(BgInput),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun PartnerBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun PartnerToastBanner(
    toast: ToastMessage?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        toast?.let { msg ->
            val accent = when (msg.kind) {
                ToastKind.SUCCESS -> FreshGreen
                ToastKind.DANGER -> DangerRed
                ToastKind.INFO -> InfoSky
                ToastKind.WARNING -> SunnyYellow
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onDismiss() }
                    .testTag(if (msg.isKotAlert) "kot_toast_banner" else "partner_toast_banner"),
                color = if (msg.isKotAlert) Color(0xFFFFFBEB) else BgCard.copy(alpha = 0.98f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (msg.isKotAlert) 1.5.dp else 1.dp,
                    color = if (msg.isKotAlert) SunnyYellow else BorderSlate
                ),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.isKotAlert) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "New KOT Order Alert",
                            tint = SunnyAmberInk,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accent)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (msg.isKotAlert) {
                            Text(
                                text = "INCOMING KOT ORDER",
                                color = SunnyAmberInk,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = msg.text,
                            color = TextMain,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun partnerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = BgSurface,
    unfocusedContainerColor = BgInput,
    focusedBorderColor = FreshGreen,
    unfocusedBorderColor = BorderSlate,
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedLabelColor = FreshGreen,
    unfocusedLabelColor = TextMuted,
    cursorColor = FreshGreen
)

@Composable
fun RejectOrderDialog(
    orderId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("Item Out of Stock / Unverified UPI") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgSurface,
        titleContentColor = TextMain,
        textContentColor = TextMuted,
        title = {
            Text(
                text = "Reject Order #${orderId.takeLast(6)}",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Specify reason for cancelling this order:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Cancellation Reason") },
                    colors = partnerFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reject_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                modifier = Modifier.testTag("confirm_reject_order_button")
            ) {
                Text("Reject Order", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun StoreFeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (subject: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = BgSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Send Feedback / Issue to Admin",
                        style = MaterialTheme.typography.titleMedium,
                        color = InfoSky,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    placeholder = { Text("e.g. Product Approval / Payout / App Issue") },
                    colors = partnerFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_subject_input")
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    placeholder = { Text("Write details...") },
                    colors = partnerFieldColors(),
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_message_input")
                )
                Button(
                    onClick = { onSubmit(subject, message) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_feedback_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send to Admin Hub", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun ProductEarningsPreviewCard(
    validation: PricingValidationResult,
    resolvedRuleSource: CommissionRuleSource,
    resolvedRulePercent: Double,
    isCustomOverride: Boolean,
    onResetToRule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breakdown = validation.breakdown
    val borderColor = when {
        !validation.isValid -> DangerRed.copy(alpha = 0.85f)
        breakdown?.isLossMaking == true -> SunnyYellow
        else -> FreshGreen
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("earnings_preview_card"),
        color = BgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = FreshGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Live Earnings Calculator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshGreen
                    )
                }
                PartnerBadge(
                    text = if (isCustomOverride) "Custom Override" else resolvedRuleSource.label,
                    containerColor = SunnyYellow.copy(alpha = 0.28f),
                    contentColor = SunnyAmberInk
                )
            }

            if (isCustomOverride) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SunnyYellow.copy(alpha = 0.2f))
                        .clickable { onResetToRule() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("reset_commission_rule_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Admin ${resolvedRuleSource.label}: ${EarningsCalculator.formatPercent(resolvedRulePercent)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunnyAmberInk,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Use Admin Rule",
                            tint = SunnyAmberInk,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Use Rule",
                            style = MaterialTheme.typography.labelSmall,
                            color = SunnyAmberInk,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (breakdown != null) {
                HorizontalDivider(color = BorderSlate)

                EarningsPreviewRow(
                    label = "Customer Selling Price:",
                    value = EarningsCalculator.formatRupees(breakdown.sellingPrice),
                    valueColor = TextMain,
                    valueTestTag = "preview_selling_price"
                )
                EarningsPreviewRow(
                    label = "Chutki Commission (${EarningsCalculator.formatPercent(breakdown.commissionPercentage)}):",
                    value = EarningsCalculator.formatRupees(breakdown.commissionAmount),
                    valueColor = SunnyAmberInk,
                    valueTestTag = "preview_commission_amount"
                )
                EarningsPreviewRow(
                    label = "Your Earning After Commission:",
                    value = EarningsCalculator.formatRupees(breakdown.storeEarning),
                    valueColor = FreshGreen,
                    highlight = true,
                    valueTestTag = "preview_store_earning"
                )
                EarningsPreviewRow(
                    label = "Your Estimated Profit:",
                    value = EarningsCalculator.formatRupees(breakdown.estimatedProfit),
                    valueColor = if (breakdown.estimatedProfit < 0.0) DangerSoft else FreshGreen,
                    highlight = true,
                    valueTestTag = "preview_estimated_profit"
                )
            } else {
                Text(
                    text = "Enter Selling Price to preview Chutki commission & net Store Partner earnings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            if (!validation.isValid && !validation.errorMessage.isNullOrBlank()) {
                Surface(
                    color = DangerRed.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("pricing_validation_error")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = DangerSoft,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = validation.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (!validation.warningMessage.isNullOrBlank()) {
                Surface(
                    color = SunnyYellow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SunnyYellow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = SunnyAmberInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = validation.warningMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = SunnyAmberInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsPreviewRow(
    label: String,
    value: String,
    valueColor: Color,
    valueTestTag: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlight) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(valueColor.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) TextMain else TextMuted,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            modifier = Modifier.testTag(valueTestTag)
        )
    }
}

@Composable
fun ProductEditorDialog(
    editingItem: ProductItem?,
    storeId: String,
    commissionRules: CommissionRules,
    availableCategories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onSave: (
        itemId: String?,
        name: String,
        purchaseCostText: String,
        sellingPriceText: String,
        commissionPercentText: String,
        commissionRuleSource: CommissionRuleSource,
        mrpText: String,
        category: String,
        isOnSale: Boolean,
        saleBadgeText: String,
        searchKeywords: String,
        description: String,
        imageUrlRaw: String,
        available: Boolean
    ) -> Unit,
    onToast: (String, ToastKind) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ProductEditorFormContent(
            editingItem = editingItem,
            storeId = storeId,
            commissionRules = commissionRules,
            availableCategories = availableCategories,
            onDismiss = onDismiss,
            onSave = onSave,
            onToast = onToast
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditorFormContent(
    editingItem: ProductItem?,
    storeId: String,
    commissionRules: CommissionRules,
    availableCategories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onSave: (
        itemId: String?,
        name: String,
        purchaseCostText: String,
        sellingPriceText: String,
        commissionPercentText: String,
        commissionRuleSource: CommissionRuleSource,
        mrpText: String,
        category: String,
        isOnSale: Boolean,
        saleBadgeText: String,
        searchKeywords: String,
        description: String,
        imageUrlRaw: String,
        available: Boolean
    ) -> Unit,
    onToast: (String, ToastKind) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(editingItem) { mutableStateOf(editingItem?.name.orEmpty()) }
    var purchaseCostText by remember(editingItem) {
        mutableStateOf(
            if (editingItem != null && editingItem.purchaseCost > 0.0) {
                EarningsCalculator.formatEditableNumber(editingItem.purchaseCost)
            } else if (editingItem != null) {
                "0"
            } else {
                ""
            }
        )
    }
    var sellingPriceText by remember(editingItem) {
        val initialSell = editingItem?.let {
            if (it.sellingPrice > 0.0) it.sellingPrice else it.price
        } ?: 0.0
        mutableStateOf(
            if (initialSell > 0.0) EarningsCalculator.formatEditableNumber(initialSell) else ""
        )
    }
    var category by remember(editingItem) { mutableStateOf(editingItem?.category.orEmpty()) }

    val resolvedCommission = remember(editingItem?.itemId, storeId, category, commissionRules) {
        EarningsCalculator.resolveCommission(
            productId = editingItem?.itemId,
            storeId = storeId,
            category = category,
            rules = commissionRules
        )
    }

    var isCustomCommission by remember(editingItem) {
        mutableStateOf(
            editingItem?.commissionRuleSource == CommissionRuleSource.CUSTOM_OVERRIDE.name &&
                editingItem.commissionPercentage != null
        )
    }
    var commissionText by remember(editingItem) {
        val initialPct = if (
            editingItem?.commissionRuleSource == CommissionRuleSource.CUSTOM_OVERRIDE.name &&
            editingItem.commissionPercentage != null
        ) {
            editingItem.commissionPercentage
        } else {
            resolvedCommission.percent
        }
        mutableStateOf(EarningsCalculator.formatEditableNumber(initialPct).ifBlank { "0" })
    }

    // Automatically recalculate when Admin commission settings or category change
    LaunchedEffect(resolvedCommission.percent, resolvedCommission.source, isCustomCommission) {
        if (!isCustomCommission) {
            commissionText = EarningsCalculator.formatEditableNumber(resolvedCommission.percent)
                .ifBlank { "0" }
        }
    }

    var mrpText by remember(editingItem) {
        mutableStateOf(
            editingItem?.mrp?.takeIf { it > 0.0 }?.let {
                EarningsCalculator.formatEditableNumber(it)
            }.orEmpty()
        )
    }
    var isOnSale by remember(editingItem) { mutableStateOf(editingItem?.isOnSale ?: false) }
    var saleBadgeText by remember(editingItem) { mutableStateOf(editingItem?.saleBadgeText.orEmpty()) }
    var searchKeywords by remember(editingItem) { mutableStateOf(editingItem?.searchKeywords.orEmpty()) }
    var description by remember(editingItem) { mutableStateOf(editingItem?.description.orEmpty()) }
    var imageUrl by remember(editingItem) { mutableStateOf(editingItem?.imageUrl.orEmpty()) }
    var available by remember(editingItem) { mutableStateOf(editingItem?.available ?: true) }
    var catExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onToast("Processing photo...", ToastKind.INFO)
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    StoreUtils.compressImageUriToDataUrl(context, uri)
                }
                if (compressed != null) {
                    imageUrl = compressed
                    onToast("Photo ready!", ToastKind.SUCCESS)
                } else {
                    onToast("Could not process selected photo.", ToastKind.DANGER)
                }
            }
        }
    }

    val categoriesList = remember(availableCategories) {
        availableCategories.filter {
            it.active && it.id !in setOf("ALL", "ALL_STORES", "ON_SALE", "FAVORITES")
        }.ifEmpty { StoreUtils.DEFAULT_CATEGORIES }
    }

    val effectiveSource = if (isCustomCommission) CommissionRuleSource.CUSTOM_OVERRIDE else resolvedCommission.source
    val pricingValidation = remember(
        purchaseCostText,
        sellingPriceText,
        commissionText,
        effectiveSource,
        commissionRules.allowLossMakingProducts
    ) {
        EarningsCalculator.validateAndCalculate(
            purchaseCostText = purchaseCostText,
            sellingPriceText = sellingPriceText,
            commissionPercentText = commissionText,
            ruleSource = effectiveSource,
            allowLossMakingProducts = commissionRules.allowLossMakingProducts
        )
    }

    val priceVal = sellingPriceText.toDoubleOrNull() ?: 0.0
    val mrpVal = mrpText.toDoubleOrNull() ?: 0.0
    val discountPct = if (mrpVal > priceVal && priceVal > 0.0) {
        (((mrpVal - priceVal) / mrpVal) * 100.0).toInt()
    } else 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        color = BgSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingItem == null) "Add Grocery Product" else "Edit Grocery Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Surface(
                color = InfoSky.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "New/edited products are verified & approved by Admin before appearing on the Customer App.",
                    color = InfoSoft,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = "PRODUCT & PRICING",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.ExtraBold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                placeholder = { Text("e.g. Fresh Potato (1 kg)") },
                colors = partnerFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_input")
            )

            // Pricing & Commission Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = purchaseCostText,
                    onValueChange = { purchaseCostText = it },
                    label = { Text("Purchase Cost (₹)") },
                    placeholder = { Text("e.g. 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = partnerFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_purchase_cost_input")
                )
                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = { sellingPriceText = it },
                    label = { Text("Selling Price (₹)") },
                    placeholder = { Text("e.g. 150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = partnerFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_price_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commissionText,
                    onValueChange = {
                        commissionText = it
                        isCustomCommission = true
                    },
                    label = { Text("Chutki Commission (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = partnerFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_commission_input")
                )
                OutlinedTextField(
                    value = mrpText,
                    onValueChange = { mrpText = it },
                    label = { Text("MRP (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = partnerFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_mrp_input")
                )
            }

            if (discountPct > 0) {
                Text(
                    text = "Customer Discount: $discountPct% OFF (Saves ${EarningsCalculator.formatRupees(mrpVal - priceVal)})",
                    color = FreshGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Live Automatic Earnings & Commission Preview Card
            ProductEarningsPreviewCard(
                validation = pricingValidation,
                resolvedRuleSource = resolvedCommission.source,
                resolvedRulePercent = resolvedCommission.percent,
                isCustomOverride = isCustomCommission,
                onResetToRule = {
                    isCustomCommission = false
                    commissionText = EarningsCalculator.formatEditableNumber(resolvedCommission.percent)
                        .ifBlank { "0" }
                }
            )

            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded }
            ) {
                val selectedLabel = categoriesList.find { it.id == category }?.let {
                    "${it.icon} ${it.name}".trim()
                } ?: "Auto-Detect"
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    colors = partnerFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false },
                    containerColor = BgCard
                ) {
                    DropdownMenuItem(
                        text = { Text("Auto-Detect", color = TextMain) },
                        onClick = {
                            category = ""
                            catExpanded = false
                        }
                    )
                    categoriesList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.icon} ${cat.name}".trim(), color = TextMain) },
                            onClick = {
                                category = cat.id
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                color = BgInput,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isOnSale = !isOnSale }
                    ) {
                        Checkbox(
                            checked = isOnSale,
                            onCheckedChange = { isOnSale = it },
                            colors = CheckboxDefaults.colors(checkedColor = DangerRed)
                        )
                        Text(
                            text = "Mark On Flash Sale",
                            color = DangerSoft,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = saleBadgeText,
                        onValueChange = { saleBadgeText = it },
                        label = { Text("Sale Badge (e.g. HOT DEAL)") },
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = searchKeywords,
                onValueChange = { searchKeywords = it },
                label = { Text("Search Keywords (Comma Separated)") },
                placeholder = { Text("e.g. aalu, aloo, potato, sabzi") },
                colors = partnerFieldColors(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Full Product Description / Weight") },
                placeholder = { Text("e.g. 1 kg pack, Fresh Farm Harvest") },
                colors = partnerFieldColors(),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "PRODUCT PHOTO",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                border = BorderStroke(1.dp, FreshGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pick_product_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = FreshGreen)
                Spacer(Modifier.width(8.dp))
                Text("Choose Photo from Gallery", color = FreshGreen, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = if (imageUrl.startsWith("data:image")) "" else imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Or Paste Image URL") },
                placeholder = { Text("https://...") },
                colors = partnerFieldColors(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_image_url_input")
            )

            if (imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgInput)
                        .border(1.dp, FreshGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PartnerImage(
                        rawUrl = imageUrl,
                        contentDescription = "Product preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { available = !available }
            ) {
                Checkbox(
                    checked = available,
                    onCheckedChange = { available = it },
                    colors = CheckboxDefaults.colors(checkedColor = FreshGreen)
                )
                Text(
                    text = "Available (In Stock)",
                    color = TextMain,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", color = TextMuted)
                }
                Button(
                    onClick = {
                        onSave(
                            editingItem?.itemId,
                            name,
                            purchaseCostText,
                            sellingPriceText,
                            commissionText,
                            effectiveSource,
                            mrpText,
                            category,
                            isOnSale,
                            saleBadgeText,
                            searchKeywords,
                            description,
                            imageUrl,
                            available
                        )
                    },
                    enabled = pricingValidation.isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshGreen,
                        contentColor = Color.White,
                        disabledContainerColor = FreshGreen.copy(alpha = 0.35f),
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("submit_product_button")
                ) {
                    Text("Submit for Verify", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
