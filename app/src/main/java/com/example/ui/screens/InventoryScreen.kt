package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ProductItem
import com.example.ui.components.PartnerBadge
import com.example.ui.components.PartnerImage
import com.example.ui.components.partnerFieldColors
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessTeal
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.util.StoreUtils

enum class InventoryFilter(val label: String) {
    ALL("All"),
    IN_STOCK("In Stock"),
    LOW_OR_OUT("Low / Out"),
    PENDING("Pending"),
    APPROVED("Approved"),
    FLAGGED("Flagged")
}

@Composable
fun InventoryScreen(
    products: List<ProductItem>,
    onToggleApprove: (ProductItem) -> Unit,
    onToggleFlag: (ProductItem, String) -> Unit,
    onUpdateStockQty: (ProductItem, Int) -> Unit,
    onToggleStock: (ProductItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InventoryFilter.ALL) }
    var flagTargetItem by remember { mutableStateOf<ProductItem?>(null) }

    val totalCount = products.size
    val inStockCount = products.count { isItemInStock(it) }
    val lowOrOutCount = products.count { !isItemInStock(it) || isLowStock(it) }
    val approvedCount = products.count { it.adminApproved && !it.inventoryFlagged }
    val flaggedCount = products.count { it.inventoryFlagged }

    val filteredProducts = remember(products, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        products.filter { item ->
            val matchesQuery = q.isEmpty() ||
                "${item.name} ${item.category} ${item.searchKeywords}".lowercase().contains(q)
            val matchesFilter = when (selectedFilter) {
                InventoryFilter.ALL -> true
                InventoryFilter.IN_STOCK -> isItemInStock(item)
                InventoryFilter.LOW_OR_OUT -> !isItemInStock(item) || isLowStock(item)
                InventoryFilter.PENDING -> !item.adminApproved && !item.inventoryFlagged
                InventoryFilter.APPROVED -> item.adminApproved && !item.inventoryFlagged
                InventoryFilter.FLAGGED -> item.inventoryFlagged
            }
            matchesQuery && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 310.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("inventory_grid")
        ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Column {
                            Text(
                                text = "Inventory Stock & Update Review",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            Text(
                                text = "Monitor stock levels and approve or flag inventory updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryKpiPill(
                            label = "Total",
                            count = totalCount,
                            accent = InfoSky,
                            modifier = Modifier.widthIn(min = 74.dp)
                        )
                        InventoryKpiPill(
                            label = "In Stock",
                            count = inStockCount,
                            accent = SuccessTeal,
                            modifier = Modifier.widthIn(min = 74.dp)
                        )
                        InventoryKpiPill(
                            label = "Low / Out",
                            count = lowOrOutCount,
                            accent = WarningAmber,
                            modifier = Modifier.widthIn(min = 78.dp)
                        )
                        InventoryKpiPill(
                            label = "Approved",
                            count = approvedCount,
                            accent = EmeraldPrimary,
                            modifier = Modifier.widthIn(min = 78.dp)
                        )
                        InventoryKpiPill(
                            label = "Flagged",
                            count = flaggedCount,
                            accent = DangerRed,
                            modifier = Modifier.widthIn(min = 74.dp)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        placeholder = { Text("Search product or category...") },
                        colors = partnerFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryFilter.entries.forEach { filter ->
                            val selected = selectedFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = filter.label,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = EmeraldPrimary,
                                    containerColor = BgInput,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderSlate,
                                    selectedBorderColor = EmeraldPrimary
                                ),
                                modifier = Modifier.testTag("inventory_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = if (products.isEmpty()) {
                            "No inventory items available yet."
                        } else {
                            "No products match the selected filter."
                        },
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        } else {
            items(filteredProducts, key = { it.itemId }) { item ->
                InventoryStockCard(
                    item = item,
                    onApproveToggle = { onToggleApprove(item) },
                    onFlagToggle = {
                        if (item.inventoryFlagged) {
                            onToggleFlag(item, "")
                        } else {
                            flagTargetItem = item
                        }
                    },
                    onUpdateStockQty = { newQty -> onUpdateStockQty(item, newQty) },
                    onToggleStock = { onToggleStock(item) }
                )
            }
        }
        }

        flagTargetItem?.let { item ->
            FlagInventoryDialog(
                productName = item.name,
                onDismiss = { flagTargetItem = null },
                onConfirm = { reason ->
                    onToggleFlag(item, reason)
                    flagTargetItem = null
                }
            )
        }
    }
}

@Composable
private fun InventoryKpiPill(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BgInput,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InventoryStockCard(
    item: ProductItem,
    onApproveToggle: () -> Unit,
    onFlagToggle: () -> Unit,
    onUpdateStockQty: (Int) -> Unit,
    onToggleStock: () -> Unit
) {
    val inStock = isItemInStock(item)
    val lowStock = isLowStock(item)
    val effectiveQty = item.stockQty ?: if (item.available) 20 else 0

    val stockLabel = when {
        !inStock -> "Out of Stock"
        lowStock -> "Low Stock (${item.stockQty} units)"
        item.stockQty != null -> "${item.stockQty} units in stock"
        else -> "In Stock"
    }
    val stockColor = when {
        !inStock -> DangerRed
        lowStock -> WarningAmber
        else -> SuccessTeal
    }
    val stockProgress = when {
        !inStock -> 0f
        item.stockQty != null -> (item.stockQty.coerceIn(0, 50) / 50f).coerceAtLeast(0.08f)
        else -> 0.85f
    }

    val reviewStatusLabel = when {
        item.inventoryFlagged -> "Flagged Update"
        item.adminApproved -> "Approved"
        item.approvalStatus == "REJECTED" -> "Rejected"
        else -> "Pending Review"
    }
    val reviewStatusBg = when {
        item.inventoryFlagged -> DangerRed.copy(alpha = 0.18f)
        item.adminApproved -> SuccessTeal.copy(alpha = 0.18f)
        item.approvalStatus == "REJECTED" -> DangerRed.copy(alpha = 0.18f)
        else -> WarningAmber.copy(alpha = 0.18f)
    }
    val reviewStatusFg = when {
        item.inventoryFlagged -> DangerSoft
        item.adminApproved -> FreshGreen
        item.approvalStatus == "REJECTED" -> DangerSoft
        else -> WarningSoft
    }

    Surface(
        color = BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.inventoryFlagged) DangerRed.copy(alpha = 0.65f) else BorderSlate
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PartnerImage(
                    rawUrl = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        PartnerBadge(
                            text = reviewStatusLabel,
                            containerColor = reviewStatusBg,
                            contentColor = reviewStatusFg,
                            icon = when {
                                item.inventoryFlagged -> Icons.Default.Flag
                                item.adminApproved -> Icons.Default.Verified
                                else -> null
                            }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹${item.price.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                        if (item.category.isNotBlank()) {
                            Text(
                                text = "• ${item.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        if (item.updatedAt > 0L) {
                            Text(
                                text = "• ${StoreUtils.formatTime(item.updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Stock level indicator + stepper
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "STOCK LEVEL",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = stockLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = stockColor
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateStockQty((effectiveQty - 1).coerceAtLeast(0)) },
                                modifier = Modifier.testTag("stock_dec_${item.itemId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease stock for ${item.name}",
                                    tint = TextMain
                                )
                            }
                            Text(
                                text = item.stockQty?.toString() ?: if (item.available) "ON" else "0",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            IconButton(
                                onClick = { onUpdateStockQty(effectiveQty + 1) },
                                modifier = Modifier.testTag("stock_inc_${item.itemId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase stock for ${item.name}",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { stockProgress },
                        color = stockColor,
                        trackColor = BorderSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (item.available) "Listed In Stock" else "Marked Out of Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        TextButton(
                            onClick = onToggleStock,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("inventory_stock_toggle_${item.itemId}")
                        ) {
                            Text(
                                text = if (item.available) "Mark Out of Stock" else "Mark In Stock",
                                color = InfoSky,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (item.inventoryFlagged) {
                Surface(
                    color = DangerRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Flagged: ${item.inventoryFlagReason.ifBlank { "Needs verification" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Approve & Flag Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (item.adminApproved) {
                    Button(
                        onClick = onApproveToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("inventory_approve_toggle_${item.itemId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Approved", fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onApproveToggle,
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("inventory_approve_toggle_${item.itemId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Approve", color = EmeraldPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                }

                if (item.inventoryFlagged) {
                    Button(
                        onClick = onFlagToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningAmber,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("inventory_flag_toggle_${item.itemId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Flagged", fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onFlagToggle,
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("inventory_flag_toggle_${item.itemId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OutlinedFlag,
                            contentDescription = null,
                            tint = WarningSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Flag", color = WarningSoft, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagInventoryDialog(
    productName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val presets = listOf(
        "Stock Mismatch",
        "Price Discrepancy",
        "Damaged / Expired",
        "Image Unclear"
    )
    var reason by remember { mutableStateOf(presets.first()) }
    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = BgSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Flag Inventory Update",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Flag \"$productName\" for review:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                presets.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val selected = reason == preset
                            FilterChip(
                                selected = selected,
                                onClick = { reason = preset },
                                label = {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = WarningSoft,
                                    containerColor = BgInput,
                                    labelColor = TextMuted
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Flag Reason") },
                    colors = partnerFieldColors(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flag_reason_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(reason) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningAmber,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_flag_inventory_button")
                    ) {
                        Text("Flag Update", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

internal fun isItemInStock(item: ProductItem): Boolean =
    item.available && (item.stockQty == null || item.stockQty > 0)

internal fun isLowStock(item: ProductItem): Boolean =
    item.available && item.stockQty != null && item.stockQty in 1..5
