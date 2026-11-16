package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.ForgeTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import io.hexforge.core.common.AppLanguage
import io.hexforge.core.common.OperationScope
import io.hexforge.core.common.ReportLanguage
import io.hexforge.core.common.TargetDescriptor
import io.hexforge.core.vault.VaultBackupRecord

private data class NavDestinationSpec(
    val route: ForgeRoute,
    val tag: String,
    val icon: ImageVector,
    val labelEn: String,
    val labelFa: String,
)

private val forgeDestinations = listOf(
    NavDestinationSpec(ForgeRoute.Targets, "nav_targets", Icons.Default.SportsEsports, "Targets", "اهداف"),
    NavDestinationSpec(ForgeRoute.WishBuilder, "nav_wishbuilder", Icons.Default.AutoFixHigh, "WishBuilder", "ویش‌بیلدر"),
    NavDestinationSpec(ForgeRoute.Lab, "nav_lab", Icons.Default.Biotech, "Lab", "آزمایشگاه"),
    NavDestinationSpec(ForgeRoute.Files, "nav_files", Icons.Default.FolderOpen, "Files", "فایل‌ها"),
    NavDestinationSpec(ForgeRoute.Oracle, "nav_oracle", Icons.Default.Psychology, "Oracle", "اوراکل"),
)

@Composable
fun HexForgeShell(
    state: HexForgeUiState,
    viewModel: HexForgeViewModel,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    var showChronicleSheet by remember { mutableStateOf(false) }

    if (!state.preferences.legalAccepted) {
        LegalOnboardingScreen(
            state = state,
            onSelectLanguage = viewModel::setAppLanguage,
            onAccept = viewModel::acceptLegalNotice,
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val navigateTo: (ForgeRoute) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = colors.bg,
            contentColor = colors.ink,
            topBar = {
                PrivilegeTierStatusStrip(
                    state = state,
                    onOpenChronicle = { showChronicleSheet = true },
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = colors.surface,
                        contentColor = colors.ink,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .border(1.dp, colors.hairline),
                    ) {
                        forgeDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                modifier = Modifier.testTag(dest.tag),
                                icon = { Icon(dest.icon, contentDescription = dest.labelEn) },
                                label = {
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) dest.labelFa else dest.labelEn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.brass,
                                    selectedTextColor = colors.brass,
                                    indicatorColor = colors.brass.copy(alpha = 0.16f),
                                    unselectedIconColor = colors.muted,
                                    unselectedTextColor = colors.muted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = colors.surface,
                        contentColor = colors.ink,
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(1.dp, colors.hairline),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        forgeDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                modifier = Modifier.testTag(dest.tag),
                                icon = { Icon(dest.icon, contentDescription = dest.labelEn) },
                                label = {
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) dest.labelFa else dest.labelEn,
                                        maxLines = 1,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.brass,
                                    selectedTextColor = colors.brass,
                                    indicatorColor = colors.brass.copy(alpha = 0.16f),
                                    unselectedIconColor = colors.muted,
                                    unselectedTextColor = colors.muted,
                                ),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ForgeRoute.Targets,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 860.dp),
                    ) {
                        composable<ForgeRoute.Targets> {
                            TargetsScreen(state = state, viewModel = viewModel)
                        }
                        composable<ForgeRoute.WishBuilder> {
                            WishBuilderScreen(state = state, viewModel = viewModel)
                        }
                        composable<ForgeRoute.Lab> {
                            LabScreen(state = state)
                        }
                        composable<ForgeRoute.Files> {
                            FilesVaultScreen(state = state, viewModel = viewModel)
                        }
                        composable<ForgeRoute.Oracle> {
                            OracleSettingsScreen(state = state, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showChronicleSheet) {
        ChronicleBottomSheet(
            state = state,
            onDismiss = { showChronicleSheet = false },
            onSelectSeverity = viewModel::setChronicleSeverityFilter,
        )
    }
}

@Composable
private fun TargetsScreen(
    state: HexForgeUiState,
    viewModel: HexForgeViewModel,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    var customPackageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.lastTargetFeedback?.let { feedback ->
            Spacer(Modifier.height(4.dp))
            ForgeCard(highlighted = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (feedback.cleared) Icons.Default.GppGood else Icons.Default.Block,
                        contentDescription = null,
                        tint = if (feedback.cleared) colors.verdigris else colors.blood,
                    )
                    Text(
                        text = feedback.packageName,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (feedback.cleared) colors.verdigris else colors.blood,
                    )
                }
                Text(
                    text = biText(lang, feedback.messageEn, feedback.messageFa),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    modifier = Modifier.testTag("gatekeeper_feedback_text"),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("targets_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = biText(lang, "Targets & Gatekeeper", "اهداف و دروازه‌بان اخلاقی"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.brass,
                )
                Text(
                    text = biText(
                        lang,
                        "Verify offline clearance (PD-1) before any engine attaches or writes.",
                        "پیش از اتصال هر موتور، مجوز آفلاین بودن هدف (PD-1) را بررسی کنید.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }

            item {
                ForgeCard {
                    Text(
                        text = biText(lang, "Check Any Package ID", "بررسی هر شناسهٔ بسته (Package ID)"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = customPackageInput,
                            onValueChange = { customPackageInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_package_input"),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = "com.example.offlinegame",
                                    fontFamily = JetBrainsMonoFontFamily,
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.brass,
                                unfocusedBorderColor = colors.hairline,
                                focusedTextColor = colors.ink,
                                unfocusedTextColor = colors.ink,
                            ),
                        )
                        Button(
                            onClick = { viewModel.verifyCustomPackage(customPackageInput) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("verify_custom_package_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brass,
                                contentColor = colors.bg,
                            ),
                        ) {
                            Text(biText(lang, "Check", "بررسی"))
                        }
                    }
                }
            }

            items(state.sampleTargets, key = { it.packageName }) { target ->
                TargetCard(
                    target = target,
                    prefs = state.preferences,
                    onVerify = { viewModel.verifyTargetClearance(target, OperationScope.MEMORY_SCAN_WRITE) },
                    onBackupSnapshot = { viewModel.createSampleVaultSnapshot(target.packageName) },
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TargetCard(
    target: TargetDescriptor,
    prefs: ForgePreferences,
    onVerify: () -> Unit,
    onBackupSnapshot: () -> Unit,
) {
    val colors = ForgeTheme.colors
    val lang = prefs.appLanguage
    ForgeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = biText(lang, target.displayNameEn, target.displayNameFa),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = "${target.packageName} · v${formatNumberString(target.versionName, prefs)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (target.isOfflineVerified) colors.verdigris.copy(alpha = 0.16f) else colors.blood.copy(alpha = 0.16f))
                    .border(
                        1.dp,
                        if (target.isOfflineVerified) colors.verdigris else colors.blood,
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (target.isOfflineVerified) {
                        biText(lang, "OFFLINE", "آفلاین")
                    } else {
                        biText(lang, "ONLINE BLOCKED", "آنلاین مسدود")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (target.isOfflineVerified) colors.verdigris else colors.blood,
                )
            }
        }

        Text(
            text = "${if (lang == AppLanguage.PERSIAN) target.engineType.labelFa else target.engineType.labelEn} · SDK: ${target.detectedStoreSdk}",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )

        EngravedDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onVerify,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("verify_target_${target.packageName}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brass,
                    contentColor = colors.bg,
                ),
            ) {
                Icon(Icons.Default.GppGood, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(biText(lang, "Gatekeeper Check", "بررسی مجوز"), maxLines = 1)
            }
            if (target.isOfflineVerified) {
                OutlinedButton(
                    onClick = onBackupSnapshot,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("snapshot_target_${target.packageName}"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.hairline),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = colors.brass, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(biText(lang, "Vault Backup", "پشتیبان"), color = colors.ink, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun WishBuilderScreen(
    state: HexForgeUiState,
    viewModel: HexForgeViewModel,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    val wishSelections = remember {
        mutableStateMapOf(
            "coins" to true,
            "gems" to false,
            "ads" to true,
            "bazaar" to false,
            "speed" to false,
        )
    }
    val wishCatalog = listOf(
        Triple("coins", "Unlimited Coins (GhostScope + Atlas)", "سکهٔ نامحدود (گوست‌اسکوپ + اطلس)"),
        Triple("gems", "Unlimited Gems / Diamonds", "الماس و جم نامحدود"),
        Triple("ads", "Remove All Ads (P-ADS)", "حذف کامل تبلیغات (P-ADS)"),
        Triple("bazaar", "Unlock Cafe Bazaar / Myket IAB (P-IAB-IR)", "بازکردن پرداخت بازار / مایکت (P-IAB-IR)"),
        Triple("speed", "ChronoForge Speed Control (0.1x–50x)", "کنترل سرعت کرونوفورج (۰.۱ تا ۵۰ برابر)"),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = biText(lang, "WishBuilder Orchestrator", "هماهنگ‌کنندهٔ خواسته‌ها (WishBuilder)"),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.brass,
            )
            Text(
                text = biText(
                    lang,
                    "Tick desired outcomes; M11 resolves the execution DAG across M1–M4 in Milestone M11.",
                    "خواسته‌های خود را تیک بزنید؛ در مرحلهٔ M11 موتور DAG مسیر اجرا را می‌سازد.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }

        item {
            ForgeCard(highlighted = true) {
                wishCatalog.forEach { (key, en, fa) ->
                    val checked = wishSelections[key] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { wishSelections[key] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.brass,
                                checkmarkColor = colors.bg,
                            ),
                        )
                        Text(
                            text = biText(lang, en, fa),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                        )
                    }
                }
                EngravedDivider()
                val offlineTarget = state.sampleTargets.first()
                Button(
                    onClick = {
                        viewModel.verifyTargetClearance(offlineTarget, OperationScope.MEMORY_SCAN_WRITE)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("wishbuilder_preflight_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brass,
                        contentColor = colors.bg,
                    ),
                ) {
                    Text(
                        text = biText(
                            lang,
                            "Run Gatekeeper Pre-Flight on Demo Target",
                            "اجرای پیش‌بررسی گیت‌کیپر روی بازی دمو",
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabScreen(state: HexForgeUiState) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    val diag = state.diagnosticSnapshot

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = biText(lang, "Foundations Lab & Module Matrix", "آزمایشگاه پایه و ماتریس ۱۶ ماژول"),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.brass,
            )
        }

        item {
            ForgeCard(highlighted = true) {
                Text(
                    text = biText(lang, "M1 Core Engine Self-Diagnostics", "خودآزمایی موتورهای پایه M1"),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.brass,
                )
                DiagLine(
                    biText(lang, "Chronicle Bilingual Catalog", "کاتالوگ دوزبانه کرونیکل"),
                    formatNumberString("${diag.catalogPairCount} EN+FA pairs", state.preferences),
                )
                DiagLine(
                    biText(lang, "hexcore /proc/maps Parser", "تحلیلگر /proc/maps در hexcore"),
                    formatNumberString("${diag.parsedRegionsCount} regions", state.preferences),
                )
                DiagLine(
                    biText(lang, "hexscan Exact Int32 Probe", "اسکن دقیق Int32 در hexscan"),
                    formatNumberString("${diag.scanHitsCount} ranked hits", state.preferences),
                )
                DiagLine(
                    biText(lang, "hexelf Magic Sniffer", "تشخیص فرمت در hexelf"),
                    diag.sniffedFormatsSummary,
                )
            }
        }

        items(state.modules, key = { it.id.code }) { mod ->
            ForgeCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${mod.id.code} · ${biText(lang, mod.id.titleEn, mod.id.titleFa)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    val active = mod.isOperationalInM1
                    Text(
                        text = if (active) {
                            biText(lang, "ACTIVE", "فعال")
                        } else {
                            mod.milestoneTarget
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) colors.verdigris else colors.brass,
                    )
                }
                Text(
                    text = biText(lang, mod.summaryEn, mod.summaryFa),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DiagLine(label: String, value: String) {
    val colors = ForgeTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = colors.verdigris,
        )
    }
}

@Composable
private fun FilesVaultScreen(
    state: HexForgeUiState,
    viewModel: HexForgeViewModel,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    val totalOrig = state.vaultEntries.sumOf { it.originalSizeBytes }
    val totalComp = state.vaultEntries.sumOf { it.compressedSizeBytes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = biText(lang, "Vault & ObsidianFiles", "گنجینهٔ بازگردانی (Vault)"),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.brass,
            )
            Text(
                text = biText(
                    lang,
                    "Content-addressed SHA-256 + LZ4 snapshots. One tap restores exact original bytes.",
                    "پشتیبان‌های فشرده‌شده با LZ4 و یکتاسازی SHA-256 برای بازگردانی دقیق.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }

        item {
            ForgeCard(highlighted = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = biText(lang, "Vault Capacity", "ظرفیت والت"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = formatNumberString(
                                "${state.vaultEntries.size} entries · $totalComp B LZ4 (raw $totalOrig B)",
                                state.preferences,
                            ),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                    Button(
                        onClick = { viewModel.createSampleVaultSnapshot("io.hexforge.sandbox.knightquest") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("create_vault_snapshot_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brass,
                            contentColor = colors.bg,
                        ),
                    ) {
                        Text(biText(lang, "New Snapshot", "پشتیبان جدید"))
                    }
                }
                state.lastVaultMessageEn?.let { en ->
                    Text(
                        text = biText(lang, en, state.lastVaultMessageFa ?: en),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.verdigris,
                        modifier = Modifier.testTag("vault_status_banner"),
                    )
                }
            }
        }

        if (state.vaultEntries.isEmpty()) {
            item {
                ForgeCard {
                    Text(
                        text = biText(
                            lang,
                            "Vault is empty. Create a snapshot or run any mutating operation to archive an automatic backup.",
                            "والت خالی است. یک پشتیبان نمونه بسازید یا عملیات تغییردهنده اجرا کنید.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }
        } else {
            items(state.vaultEntries, key = { it.vaultId }) { entry ->
                VaultEntryCard(
                    entry = entry,
                    prefs = state.preferences,
                    onRollback = { viewModel.rollbackVaultEntry(entry.vaultId) },
                    onToggleProtect = { viewModel.toggleVaultProtection(entry.vaultId) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun VaultEntryCard(
    entry: VaultBackupRecord,
    prefs: ForgePreferences,
    onRollback: () -> Unit,
    onToggleProtect: () -> Unit,
) {
    val colors = ForgeTheme.colors
    val lang = prefs.appLanguage
    ForgeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = biText(lang, entry.summaryEn, entry.summaryFa),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = "${entry.vaultId} · SHA ${entry.sha256Hex.take(12)} · ${entry.compressedSizeBytes}B",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
            IconButton(
                onClick = onToggleProtect,
                modifier = Modifier.testTag("pin_vault_${entry.vaultId}"),
            ) {
                Icon(
                    imageVector = if (entry.isProtected) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = biText(lang, "Toggle Vault protection", "تغییر وضعیت محافظت"),
                    tint = if (entry.isProtected) colors.brass else colors.muted,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (entry.isRolledBack) {
                    biText(lang, "Restored & verified", "بازگردانی و تأیید شده")
                } else {
                    biText(lang, "Ready for rollback", "آمادهٔ بازگردانی")
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.isRolledBack) colors.verdigris else colors.azureSteel,
            )
            OutlinedButton(
                onClick = onRollback,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("rollback_vault_${entry.vaultId}"),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.brass),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = colors.brass, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(biText(lang, "Rollback", "بازگردانی"), color = colors.brass)
            }
        }
    }
}

@Composable
private fun OracleSettingsScreen(
    state: HexForgeUiState,
    viewModel: HexForgeViewModel,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = biText(lang, "Oracle & Forge Preferences", "اوراکل و تنظیمات فورج"),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.brass,
            )
        }

        item {
            ForgeCard(highlighted = true) {
                Text(
                    text = biText(lang, "Interface Language & RTL Layout", "زبان رابط کاربری و چیدمان راست‌به‌چپ"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { option ->
                        FilterChip(
                            selected = lang == option,
                            onClick = { viewModel.setAppLanguage(option) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_lang_${option.tag}"),
                            label = {
                                Text(
                                    text = when (option) {
                                        AppLanguage.ENGLISH -> "English"
                                        AppLanguage.PERSIAN -> "فارسی"
                                        AppLanguage.BILINGUAL_LEARNING -> "EN + فا"
                                    },
                                    maxLines = 1,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.brass.copy(alpha = 0.22f),
                                selectedLabelColor = colors.brass,
                            ),
                        )
                    }
                }

                EngravedDivider()

                Text(
                    text = biText(lang, "Chronicle Report Language", "زبان گزارش‌های کرونیکل"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportLanguage.entries.forEach { rep ->
                        FilterChip(
                            selected = state.preferences.reportLanguage == rep,
                            onClick = { viewModel.setReportLanguage(rep) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) rep.labelFa else rep.labelEn,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.brass.copy(alpha = 0.22f),
                                selectedLabelColor = colors.brass,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = biText(lang, "Use Persian Numerals (۰-۹)", "استفاده از ارقام فارسی (۰ تا ۹)"),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = state.preferences.usePersianDigits,
                        onCheckedChange = viewModel::togglePersianDigits,
                        modifier = Modifier.testTag("persian_digits_switch"),
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.brass),
                    )
                }
            }
        }

        item {
            ForgeCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.azureSteel)
                    Text(
                        text = biText(
                            lang,
                            "Oracle AI & CodexVault (Scheduled for M10)",
                            "هوش مصنوعی اوراکل و حافظهٔ CodexVault (مرحلهٔ M10)",
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = biText(
                        lang,
                        "In M10, your personal Gemini API key is stored locally in Keystore-backed EncryptedSharedPreferences and talks directly to generativelanguage.googleapis.com with 12 local RE tools and CodexVault FTS4 memory.",
                        "در مرحلهٔ M10 کلید شخصی جمینی به صورت محلی در حافظهٔ رمزنگاری‌شده ذخیره شده و مستقیماً با ۱۲ ابزار محلی مهندسی معکوس و حافظهٔ CodexVault کار خواهد کرد.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
