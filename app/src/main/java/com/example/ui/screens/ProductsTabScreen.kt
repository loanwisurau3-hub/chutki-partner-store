package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminSettings
import com.example.model.CommissionRuleSource
import com.example.model.CommissionRules
import com.example.model.ProductItem
import com.example.model.ProductSalesStat
import com.example.model.ToastKind
import com.example.ui.components.PartnerBadge
import com.example.ui.components.PartnerImage
import com.example.ui.components.ProductEditorDialog
import com.example.ui.components.partnerFieldColors
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
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
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductsTabScreen(
    storeId: String,
    products: List<ProductItem>,
    searchQuery: String,
    adminSettings: AdminSettings,
    commissionRules: CommissionRules,
    salesStats: Map<String, ProductSalesStat>,
    onSearchChange: (String) -> Unit,
    onToggleStock: (ProductItem) -> Unit,
    onSaveProduct: (
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
        available: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteProduct: (ProductItem) -> Unit,
    onQuickSeed: () -> Unit,
    onToast: (String, ToastKind) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductItem?>(null) }
    var confirmSeedDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ProductItem?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) products
        else products.filter { item ->
            "${item.name} ${item.searchKeywords} ${item.description}".lowercase().contains(q)
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            editingItem = editingProduct,
            storeId = storeId,
            commissionRules = commissionRules,
            availableCategories = adminSettings.customCategories,
            onDismiss = {
                showEditor = false
                editingProduct = null
            },
            onSave = { id, name, purchaseCost, sellingPrice, commPct, ruleSource, mrp, cat, onSale, saleTag, kw, desc, img, avail ->
                onSaveProduct(
                    id, name, purchaseCost, sellingPrice, commPct, ruleSource,
                    mrp, cat, onSale, saleTag, kw, desc, img, avail
                ) {
                    showEditor = false
                    editingProduct = null
                }
            },
            onToast = onToast
        )
    }

    if (confirmSeedDialog) {
        AlertDialog(
            onDismissRequest = { confirmSeedDialog = false },
            containerColor = BgSurface,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Add 6 Popular Items?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text("Add 6 daily grocery items (Potatoes, Onions, Tomatoes, Atta, Mustard Oil, Milk)? Admin verification is required before customer sale.")
            },
            confirmButton = {
                Button(onClick = {
                    confirmSeedDialog = false
                    onQuickSeed()
                }) {
                    Text("Add 6 Items", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSeedDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = BgSurface,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Delete Product?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Delete \"${target.name}\" from your store catalog?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                color = BgSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderSlate),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Store Catalog",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                            Text(
                                text = "Products are admin-verified before live sale",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        PartnerBadge(
                            text = "${products.size} items",
                            containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                            contentColor = EmeraldPrimary
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        placeholder = { Text("Search product or keyword...") },
                        colors = partnerFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_search_input")
                    )

                    if (adminSettings.allowStoreProductEdit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (adminSettings.allowStoreQuickSeed) {
                                OutlinedButton(
                                    onClick = { confirmSeedDialog = true },
                                    border = BorderStroke(1.dp, InfoSky),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("quick_seed_button")
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = InfoSky,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "+6 Items",
                                        color = InfoSky,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    editingProduct = null
                                    showEditor = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("add_product_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Add Product", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } else {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WarningAmber)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = WarningSoft)
                                Text(
                                    text = "Admin ne store owners ke liye catalog editing temporarily lock kiya hai.",
                                    color = WarningSoft,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                            "No grocery items listed yet. Tap \"Add Product\" above."
                        } else {
                            "No products matching search."
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
                val soldStat = salesStats[item.itemId] ?: salesStats[item.name] ?: ProductSalesStat()
                val resolvedComm = remember(item, storeId, commissionRules) {
                    if (item.commissionRuleSource == CommissionRuleSource.CUSTOM_OVERRIDE.name &&
                        item.commissionPercentage != null
                    ) {
                        EarningsCalculator.calculate(
                            purchaseCost = item.purchaseCost,
                            sellingPrice = if (item.sellingPrice > 0.0) item.sellingPrice else item.price,
                            commissionPercent = item.commissionPercentage,
                            ruleSource = CommissionRuleSource.CUSTOM_OVERRIDE
                        )
                    } else {
                        val rule = EarningsCalculator.resolveCommission(
                            productId = item.itemId,
                            storeId = storeId,
                            category = item.category,
                            rules = commissionRules
                        )
                        EarningsCalculator.calculate(
                            purchaseCost = item.purchaseCost,
                            sellingPrice = if (item.sellingPrice > 0.0) item.sellingPrice else item.price,
                            commissionPercent = rule.percent,
                            ruleSource = rule.source
                        )
                    }
                }
                ProductCatalogCard(
                    item = item,
                    soldStat = soldStat,
                    earnings = resolvedComm,
                    allowEdit = adminSettings.allowStoreProductEdit,
                    onToggleStock = { onToggleStock(item) },
                    onEdit = {
                        editingProduct = item
                        showEditor = true
                    },
                    onDelete = { deleteTarget = item }
                )
            }
        }
    }
}

@Composable
private fun ProductCatalogCard(
    item: ProductItem,
    soldStat: ProductSalesStat,
    earnings: com.example.util.EarningsBreakdown,
    allowEdit: Boolean,
    onToggleStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isRejected = item.approvalStatus == "REJECTED"
    val discPct = if (item.mrp > item.price && item.price > 0) {
        (((item.mrp - item.price) / item.mrp) * 100.0).roundToInt()
    } else item.discountPercent

    val approvalText = when {
        item.adminApproved -> "Live for Sale"
        isRejected -> "Rejected"
        else -> "Pending Admin Verify"
    }
    val approvalBg = when {
        item.adminApproved -> Color(0xFFECFDF5)
        isRejected -> Color(0xFFFEF2F2)
        else -> Color(0xFFFFFBEB)
    }
    val approvalFg = when {
        item.adminApproved -> FreshGreen
        isRejected -> DangerSoft
        else -> WarningSoft
    }

    val ratingText = if (item.ratingCount > 0) {
        "★ ${String.format(Locale.US, "%.1f", item.ratingAvg)} (${item.ratingCount})"
    } else {
        "No ratings"
    }

    Surface(
        color = BgCard,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderSlate),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                PartnerImage(
                    rawUrl = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                )
                if (item.isOnSale || discPct > 0) {
                    val tagText = item.saleBadgeText.ifBlank {
                        if (discPct > 0) "$discPct% OFF" else "SALE"
                    }
                    PartnerBadge(
                        text = tagText,
                        containerColor = DangerRed.copy(alpha = 0.92f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PartnerBadge(
                        text = approvalText,
                        containerColor = approvalBg.copy(alpha = 0.92f),
                        contentColor = approvalFg
                    )
                    PartnerBadge(
                        text = if (item.available) "In Stock" else "Out of Stock",
                        containerColor = if (item.available) {
                            SuccessTeal.copy(alpha = 0.9f)
                        } else {
                            DangerRed.copy(alpha = 0.9f)
                        },
                        contentColor = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description.ifBlank { "Fresh Stock" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = SunnyAmberInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Sold: ${soldStat.qty} qty (₹${soldStat.revenue.toInt()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = InfoSky
                    )
                }
                if (item.searchKeywords.isNotBlank()) {
                    Text(
                        text = "KW: ${item.searchKeywords}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (earnings.sellingPrice > 0.0) {
                    Surface(
                        color = FreshGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FreshGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "You Receive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = EarningsCalculator.formatRupees(earnings.storeEarning),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Chutki Fee",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = EarningsCalculator.formatPercent(earnings.commissionPercentage),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SunnyAmberInk
                                )
                            }
                            if (item.purchaseCost > 0.0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Net Profit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = EarningsCalculator.formatRupees(earnings.estimatedProfit),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (earnings.estimatedProfit >= 0.0) FreshGreen else DangerSoft
                                    )
                                }
                            }
                        }
                    }
                }
                if (isRejected && item.rejectionReason.isNotBlank()) {
                    Text(
                        text = "Reason: ${item.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .height(1.dp)
                        .background(BorderSlate)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "₹${item.price.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                        if (item.mrp > item.price) {
                            Text(
                                text = "₹${item.mrp.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onToggleStock,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, BorderSlate),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (item.available) EmeraldPrimary else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (item.available) "Off" else "On",
                                color = TextMain,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (allowEdit) {
                            OutlinedButton(
                                onClick = onEdit,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, BorderSlate),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit ${item.name}",
                                    tint = TextMain,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Button(
                                onClick = onDelete,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(9.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${item.name}",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
