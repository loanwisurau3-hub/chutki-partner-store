package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CommissionRuleEntry
import com.example.model.CommissionRules
import com.example.model.CustomCategory
import com.example.model.OrderEarningsLedgerEntry
import com.example.model.ProductItem
import com.example.model.StoreProfile
import com.example.model.StoreSummaryReport
import com.example.ui.components.PartnerBadge
import com.example.ui.components.partnerFieldColors
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgDark
import com.example.ui.theme.BgInput
import com.example.ui.theme.BgSurface
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSoft
import com.example.ui.theme.FreshGreen
import com.example.ui.theme.InfoSky
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SunnyAmberInk
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.TextMain
import com.example.ui.theme.TextMuted
import com.example.util.EarningsCalculator
import com.example.util.StoreUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminCommissionScreen(
    currentStoreId: String,
    currentStoreName: String,
    commissionRules: CommissionRules,
    products: List<ProductItem>,
    availableCategories: List<CustomCategory>,
    allStores: Map<String, StoreProfile>,
    storeReports: List<StoreSummaryReport>,
    ledgerEntries: List<OrderEarningsLedgerEntry>,
    onBack: () -> Unit,
    onSaveGlobal: (percentText: String, enabled: Boolean, allowLossMaking: Boolean) -> Unit,
    onSaveStoreRule: (storeId: String, storeName: String, percentText: String, enabled: Boolean) -> Unit,
    onDeleteStoreRule: (storeId: String) -> Unit,
    onSaveCategoryRule: (categoryId: String, categoryName: String, percentText: String, enabled: Boolean) -> Unit,
    onDeleteCategoryRule: (categoryId: String) -> Unit,
    onSaveProductRule: (productId: String, storeId: String, productName: String, percentText: String, enabled: Boolean) -> Unit,
    onDeleteProductRule: (productId: String) -> Unit
) {
    BackHandler { onBack() }

    // Global form state
    var globalPctText by remember(commissionRules.globalCommissionPercent) {
        mutableStateOf(
            EarningsCalculator.formatEditableNumber(commissionRules.globalCommissionPercent).ifBlank { "0" }
        )
    }
    var globalEnabled by remember(commissionRules.globalRuleEnabled) {
        mutableStateOf(commissionRules.globalRuleEnabled)
    }
    var allowLossMaking by remember(commissionRules.allowLossMakingProducts) {
        mutableStateOf(commissionRules.allowLossMakingProducts)
    }

    // Store rule form state
    val storeChoices = remember(allStores, currentStoreId, currentStoreName) {
        buildMap {
            if (currentStoreId.isNotBlank()) {
                put(currentStoreId, currentStoreName.ifBlank { "Current Store" })
            }
            allStores.forEach { (id, prof) ->
                put(id, prof.name.ifBlank { id.takeLast(6) })
            }
        }
    }
    var selectedStoreId by remember(currentStoreId) {
        mutableStateOf(currentStoreId.ifBlank { storeChoices.keys.firstOrNull().orEmpty() })
    }
    var storePctText by remember { mutableStateOf("8") }
    var storeRuleEnabled by remember { mutableStateOf(true) }
    var storeDropdownExpanded by remember { mutableStateOf(false) }

    // Category rule form state
    val categoryChoices = remember(availableCategories) {
        availableCategories.filter { it.active }.ifEmpty { StoreUtils.DEFAULT_CATEGORIES }
    }
    var selectedCatId by remember(categoryChoices) {
        mutableStateOf(categoryChoices.firstOrNull()?.id ?: "VEG")
    }
    var catPctText by remember { mutableStateOf("6") }
    var catRuleEnabled by remember { mutableStateOf(true) }
    var catDropdownExpanded by remember { mutableStateOf(false) }

    // Product rule form state
    var selectedProductId by remember(products) {
        mutableStateOf(products.firstOrNull()?.itemId.orEmpty())
    }
    var productPctText by remember { mutableStateOf("12") }
    var productRuleEnabled by remember { mutableStateOf(true) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    // Report totals
    val activeLedgers = remember(ledgerEntries) {
        ledgerEntries.filter { it.ledgerStatus != "REVERSED" }
    }
    val platformGross = remember(activeLedgers) {
        EarningsCalculator.round2(activeLedgers.sumOf { it.grossAmount })
    }
    val platformCommission = remember(activeLedgers) {
        EarningsCalculator.round2(activeLedgers.sumOf { it.commissionAmount })
    }
    val platformStoreEarnings = remember(activeLedgers) {
        EarningsCalculator.round2(activeLedgers.sumOf { it.storeEarning })
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Surface(color = BgSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_commission_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextMain
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = SunnyAmberInk,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Commission & Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Chutki Priority Commission Engine & Earnings Ledger",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_commission_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Documented Commission Priority Card
            item {
                Surface(
                    color = BgSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SunnyYellow.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active Commission Priority Order",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SunnyAmberInk
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PartnerBadge("1. Product Override", FreshGreen.copy(alpha = 0.16f), FreshGreen)
                            PartnerBadge("2. Store Rule", InfoSky.copy(alpha = 0.16f), InfoSky)
                            PartnerBadge("3. Category Rule", SunnyYellow.copy(alpha = 0.28f), SunnyAmberInk)
                            PartnerBadge("4. Global Default", BgInput, TextMain)
                        }
                        Text(
                            text = "First enabled rule in this order applies automatically. Historical order earnings remain locked when rules change.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // 2. Global Default Commission & Loss-Making Policy
            item {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, FreshGreen.copy(alpha = 0.75f))
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
                            Icon(Icons.Default.Public, contentDescription = null, tint = FreshGreen)
                            Text(
                                text = "Global Default Commission",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }

                        OutlinedTextField(
                            value = globalPctText,
                            onValueChange = { globalPctText = it },
                            label = { Text("Global Commission (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = partnerFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_global_commission_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Global Rule", color = TextMain, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Disable to default to 0% unless overridden",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = globalEnabled,
                                onCheckedChange = { globalEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen),
                                modifier = Modifier.testTag("admin_global_rule_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Allow Loss-Making Products", color = TextMain, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Permits saving when Store Partner receives < Purchase Cost",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = allowLossMaking,
                                onCheckedChange = { allowLossMaking = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = SunnyYellow),
                                modifier = Modifier.testTag("admin_allow_loss_switch")
                            )
                        }

                        Button(
                            onClick = { onSaveGlobal(globalPctText, globalEnabled, allowLossMaking) },
                            colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_global_commission_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save Global Commission & Policy", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 3. Store-Specific Commission Rule
            item {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderSlate)
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
                            Icon(Icons.Default.Store, contentDescription = null, tint = InfoSky)
                            Text(
                                text = "Store-Specific Commission (Priority 2)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = storeDropdownExpanded,
                            onExpandedChange = { storeDropdownExpanded = !storeDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = storeChoices[selectedStoreId] ?: selectedStoreId.ifBlank { "Select Store" },
                                onValueChange = { selectedStoreId = it },
                                label = { Text("Store") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeDropdownExpanded) },
                                colors = partnerFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = storeDropdownExpanded,
                                onDismissRequest = { storeDropdownExpanded = false },
                                containerColor = BgSurface
                            ) {
                                storeChoices.forEach { (id, label) ->
                                    DropdownMenuItem(
                                        text = { Text("$label (${id.takeLast(6)})", color = TextMain) },
                                        onClick = {
                                            selectedStoreId = id
                                            commissionRules.storeCommissions[id]?.let { existing ->
                                                storePctText = EarningsCalculator.formatEditableNumber(existing.percent)
                                                storeRuleEnabled = existing.enabled
                                            }
                                            storeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = storePctText,
                                onValueChange = { storePctText = it },
                                label = { Text("Store Commission (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_store_commission_input")
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Switch(
                                    checked = storeRuleEnabled,
                                    onCheckedChange = { storeRuleEnabled = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val storeName = storeChoices[selectedStoreId] ?: selectedStoreId
                                onSaveStoreRule(selectedStoreId, storeName, storePctText, storeRuleEnabled)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = InfoSky, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_store_commission_button")
                        ) {
                            Text("Save Store Commission Rule", fontWeight = FontWeight.ExtraBold)
                        }

                        RuleEntriesList(
                            entries = commissionRules.storeCommissions.values.toList(),
                            onToggle = { entry ->
                                onSaveStoreRule(
                                    entry.targetId,
                                    entry.targetLabel,
                                    EarningsCalculator.formatEditableNumber(entry.percent).ifBlank { "0" },
                                    !entry.enabled
                                )
                            },
                            onDelete = { onDeleteStoreRule(it.targetId) }
                        )
                    }
                }
            }

            // 4. Category-Specific Commission Rule
            item {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderSlate)
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
                            Icon(Icons.Default.Category, contentDescription = null, tint = SunnyAmberInk)
                            Text(
                                text = "Category Commission (Priority 3)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = catDropdownExpanded,
                            onExpandedChange = { catDropdownExpanded = !catDropdownExpanded }
                        ) {
                            val catLabel = categoryChoices.firstOrNull { it.id == selectedCatId }?.let {
                                "${it.icon} ${it.name} (${it.id})".trim()
                            } ?: selectedCatId
                            OutlinedTextField(
                                value = catLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                                colors = partnerFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = catDropdownExpanded,
                                onDismissRequest = { catDropdownExpanded = false },
                                containerColor = BgSurface
                            ) {
                                categoryChoices.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.icon} ${cat.name} (${cat.id})", color = TextMain) },
                                        onClick = {
                                            selectedCatId = cat.id
                                            commissionRules.categoryCommissions[cat.id]?.let { existing ->
                                                catPctText = EarningsCalculator.formatEditableNumber(existing.percent)
                                                catRuleEnabled = existing.enabled
                                            }
                                            catDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = catPctText,
                                onValueChange = { catPctText = it },
                                label = { Text("Category Commission (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = partnerFieldColors(),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_category_commission_input")
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Switch(
                                    checked = catRuleEnabled,
                                    onCheckedChange = { catRuleEnabled = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val catName = categoryChoices.firstOrNull { it.id == selectedCatId }?.name ?: selectedCatId
                                onSaveCategoryRule(selectedCatId, catName, catPctText, catRuleEnabled)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SunnyYellow, contentColor = TextMain),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_category_commission_button")
                        ) {
                            Text("Save Category Commission Rule", fontWeight = FontWeight.ExtraBold)
                        }

                        RuleEntriesList(
                            entries = commissionRules.categoryCommissions.values.toList(),
                            onToggle = { entry ->
                                onSaveCategoryRule(
                                    entry.targetId,
                                    entry.targetLabel,
                                    EarningsCalculator.formatEditableNumber(entry.percent).ifBlank { "0" },
                                    !entry.enabled
                                )
                            },
                            onDelete = { onDeleteCategoryRule(it.targetId) }
                        )
                    }
                }
            }

            // 5. Product-Specific Commission Override
            item {
                Surface(
                    color = BgCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderSlate)
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
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = FreshGreen)
                            Text(
                                text = "Product Commission Override (Priority 1)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }

                        if (products.isEmpty()) {
                            Text(
                                text = "Add products to your store catalog to configure product-level overrides.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = productDropdownExpanded,
                                onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                            ) {
                                val selectedProdLabel = products.firstOrNull { it.itemId == selectedProductId }?.name
                                    ?: "Select Product"
                                OutlinedTextField(
                                    value = selectedProdLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Product") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                    colors = partnerFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = productDropdownExpanded,
                                    onDismissRequest = { productDropdownExpanded = false },
                                    containerColor = BgSurface
                                ) {
                                    products.forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text("${prod.name} (${EarningsCalculator.formatRupees(prod.sellingPrice)})", color = TextMain) },
                                            onClick = {
                                                selectedProductId = prod.itemId
                                                commissionRules.productCommissions[prod.itemId]?.let { existing ->
                                                    productPctText = EarningsCalculator.formatEditableNumber(existing.percent)
                                                    productRuleEnabled = existing.enabled
                                                }
                                                productDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = productPctText,
                                    onValueChange = { productPctText = it },
                                    label = { Text("Product Override (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = partnerFieldColors(),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_product_commission_input")
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Active", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Switch(
                                        checked = productRuleEnabled,
                                        onCheckedChange = { productRuleEnabled = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val prod = products.firstOrNull { it.itemId == selectedProductId }
                                    if (prod != null) {
                                        onSaveProductRule(
                                            prod.itemId,
                                            currentStoreId,
                                            prod.name,
                                            productPctText,
                                            productRuleEnabled
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FreshGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_product_commission_button")
                            ) {
                                Text("Save Product Commission Override", fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        RuleEntriesList(
                            entries = commissionRules.productCommissions.values.toList(),
                            onToggle = { entry ->
                                onSaveProductRule(
                                    entry.targetId,
                                    entry.storeId,
                                    entry.targetLabel,
                                    EarningsCalculator.formatEditableNumber(entry.percent).ifBlank { "0" },
                                    !entry.enabled
                                )
                            },
                            onDelete = { onDeleteProductRule(it.targetId) }
                        )
                    }
                }
            }

            // 6. Store Partner Earnings & Chutki Commission Reports
            item {
                Surface(
                    color = BgSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, FreshGreen.copy(alpha = 0.65f))
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
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = FreshGreen)
                            Text(
                                text = "Chutki Commission & Store Earnings Reports",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMain
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMiniMetric(
                                label = "GROSS SALES",
                                value = EarningsCalculator.formatRupees(platformGross),
                                color = TextMain,
                                modifier = Modifier.weight(1f)
                            )
                            ReportMiniMetric(
                                label = "CHUTKI COMM.",
                                value = EarningsCalculator.formatRupees(platformCommission),
                                color = SunnyAmberInk,
                                modifier = Modifier.weight(1f)
                            )
                            ReportMiniMetric(
                                label = "STORE EARN",
                                value = EarningsCalculator.formatRupees(platformStoreEarnings),
                                color = FreshGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (storeReports.isEmpty()) {
                item {
                    Surface(
                        color = BgCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Text(
                            text = "No store order ledger data yet.",
                            color = TextMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        )
                    }
                }
            } else {
                items(storeReports, key = { "rep_${it.storeId}" }) { report ->
                    Surface(
                        color = BgCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.storeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextMain
                                )
                                PartnerBadge(
                                    text = "${report.activeOrders} active • ${report.reversedOrders} rev",
                                    containerColor = BgInput,
                                    contentColor = InfoSky
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Gross: ${EarningsCalculator.formatRupees(report.grossSales)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Chutki: ${EarningsCalculator.formatRupees(report.totalChutkiCommission)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SunnyAmberInk
                                )
                                Text(
                                    text = "Store: ${EarningsCalculator.formatRupees(report.totalStoreEarning)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshGreen
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
private fun RuleEntriesList(
    entries: List<CommissionRuleEntry>,
    onToggle: (CommissionRuleEntry) -> Unit,
    onDelete: (CommissionRuleEntry) -> Unit
) {
    if (entries.isEmpty()) return
    HorizontalDivider(color = BorderSlate)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.sortedBy { it.targetLabel }.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgInput, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.targetLabel.ifBlank { entry.targetId },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${EarningsCalculator.formatPercent(entry.percent)} • ${if (entry.enabled) "Enabled" else "Disabled"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.enabled) FreshGreen else DangerSoft
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = entry.enabled,
                        onCheckedChange = { onToggle(entry) },
                        colors = SwitchDefaults.colors(checkedTrackColor = FreshGreen)
                    )
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Rule",
                            tint = DangerSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMiniMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(BgInput, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
        }
    }
}
