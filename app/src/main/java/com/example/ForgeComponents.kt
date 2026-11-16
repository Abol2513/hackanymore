package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ForgeTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import io.hexforge.core.chronicle.ChronicleEvent
import io.hexforge.core.chronicle.ChronicleSeverity
import io.hexforge.core.common.AppLanguage
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.PersianNumeralFormatter
import io.hexforge.core.common.ReportLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun biText(language: AppLanguage, en: String, fa: String): String = when (language) {
    AppLanguage.ENGLISH -> en
    AppLanguage.PERSIAN -> fa
    AppLanguage.BILINGUAL_LEARNING -> "$en  ·  $fa"
}

fun formatNumberString(raw: String, prefs: ForgePreferences): String {
    val useFaDigits = prefs.usePersianDigits && prefs.appLanguage != AppLanguage.ENGLISH
    return PersianNumeralFormatter.format(raw, useFaDigits)
}

@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ForgeTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (highlighted) colors.brass else colors.hairline,
                shape = RoundedCornerShape(12.dp),
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) colors.raised else colors.surface,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/**
 * Gear-and-circuit engraved hairline motif from Part 20 design language.
 */
@Composable
fun EngravedDivider(modifier: Modifier = Modifier) {
    val hairline = ForgeTheme.colors.hairline
    val brass = ForgeTheme.colors.brass
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val midY = size.height / 2f
        val midX = size.width / 2f
        drawLine(
            color = hairline,
            start = Offset(0f, midY),
            end = Offset(midX - 18f, midY),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = hairline,
            start = Offset(midX + 18f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(
            color = brass,
            radius = 4.dp.toPx(),
            center = Offset(midX, midY),
        )
        drawCircle(
            color = hairline,
            radius = 7.dp.toPx(),
            center = Offset(midX, midY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
fun TierStampChip(
    tier: ExecutionTier,
    isActive: Boolean,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    val colors = ForgeTheme.colors
    val borderColor = if (isActive) colors.brass else colors.hairline
    val bgColor = if (isActive) colors.brass.copy(alpha = 0.16f) else colors.raised
    val textColor = if (isActive) colors.brass else colors.muted

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isActive) colors.verdigris else colors.muted.copy(alpha = 0.4f)),
        )
        Text(
            text = "${tier.code} · ${if (language == AppLanguage.PERSIAN) tier.titleFa else tier.titleEn}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PrivilegeTierStatusStrip(
    state: HexForgeUiState,
    onOpenChronicle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.hairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TierStampChip(ExecutionTier.T0_HEXSPACE, isActive = true, language = lang)
                TierStampChip(ExecutionTier.T1_SHIZUKU, isActive = state.shizukuAvailable, language = lang)
                TierStampChip(ExecutionTier.T2_ADB_DIRECT, isActive = state.adbDirectAvailable, language = lang)
                TierStampChip(ExecutionTier.T3_ROOT, isActive = state.rootAvailable, language = lang)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .testTag("open_chronicle_strip_button")
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.raised)
                    .border(1.dp, colors.brass.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenChronicle)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = stringResource(R.string.chronicle_bar_title),
                    tint = colors.brass,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = formatNumberString("${state.chronicleEvents.size}", state.preferences),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronicleBottomSheet(
    state: HexForgeUiState,
    onDismiss: () -> Unit,
    onSelectSeverity: (ChronicleSeverity?) -> Unit,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = colors.brass,
                    )
                    Text(
                        text = biText(lang, "Chronicle Live Telemetry", "گزارش زندهٔ کرونیکل"),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_chronicle_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = biText(lang, "Close Chronicle", "بستن کرونیکل"),
                        tint = colors.muted,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.chronicleSeverityFilter == null,
                    onClick = { onSelectSeverity(null) },
                    label = { Text(biText(lang, "All", "همه")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.brass.copy(alpha = 0.2f),
                        selectedLabelColor = colors.brass,
                    ),
                )
                ChronicleSeverity.entries.forEach { sev ->
                    FilterChip(
                        selected = state.chronicleSeverityFilter == sev,
                        onClick = { onSelectSeverity(if (state.chronicleSeverityFilter == sev) null else sev) },
                        label = {
                            Text(if (lang == AppLanguage.PERSIAN) sev.tagFa else sev.tagEn.uppercase())
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.brass.copy(alpha = 0.2f),
                            selectedLabelColor = colors.brass,
                        ),
                    )
                }
            }

            if (state.chronicleEvents.isEmpty()) {
                ForgeCard {
                    Text(
                        text = biText(lang, "No Chronicle events match this filter.", "رویدادی با این فیلتر یافت نشد."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.chronicleEvents, key = { it.uuid }) { event ->
                        ChronicleEventRow(
                            event = event,
                            prefs = state.preferences,
                            timeText = timeFormatter.format(Date(event.timestampMs)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChronicleEventRow(
    event: ChronicleEvent,
    prefs: ForgePreferences,
    timeText: String,
) {
    val colors = ForgeTheme.colors
    val sevColor = when (event.severity) {
        ChronicleSeverity.INFO -> colors.azureSteel
        ChronicleSeverity.SUCCESS -> colors.verdigris
        ChronicleSeverity.WARN -> colors.ember
        ChronicleSeverity.ERROR -> colors.blood
    }
    val reportLang = when (prefs.appLanguage) {
        AppLanguage.PERSIAN -> ReportLanguage.PERSIAN
        AppLanguage.ENGLISH -> ReportLanguage.ENGLISH
        AppLanguage.BILINGUAL_LEARNING -> prefs.reportLanguage
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${event.module.code} · ${event.tier.code} · ${event.targetId}",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = sevColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatNumberString(timeText, prefs),
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
        Text(
            text = event.formatLine(reportLang, prefs.usePersianDigits),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
        )
        AnimatedVisibility(visible = prefs.appLanguage == AppLanguage.BILINGUAL_LEARNING) {
            val secondary = if (reportLang == ReportLanguage.PERSIAN) {
                event.formatLine(ReportLanguage.ENGLISH, false)
            } else {
                event.formatLine(ReportLanguage.PERSIAN, prefs.usePersianDigits)
            }
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
    }
}

@Composable
fun LegalOnboardingScreen(
    state: HexForgeUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onAccept: () -> Unit,
) {
    val colors = ForgeTheme.colors
    val lang = state.preferences.appLanguage

    androidx.compose.material3.Scaffold(
        containerColor = colors.bg,
        contentColor = colors.ink,
        bottomBar = {
            Surface(
                color = colors.surface,
                border = BorderStroke(1.dp, colors.hairline),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("accept_legal_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brass,
                            contentColor = colors.bg,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = biText(lang, "Accept & Enter Forge", "پذیرش و ورود به فورج"),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.brass.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_forge_banner),
                        contentDescription = biText(lang, "HEXFORGE Instrument Workbench", "میز کار مهندسی HEXFORGE"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.bg.copy(alpha = 0.45f))
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Column {
                            Text(
                                text = "HEXFORGE",
                                style = MaterialTheme.typography.displaySmall,
                                color = colors.brass,
                            )
                            Text(
                                text = biText(
                                    lang,
                                    "Rootless Offline Game Modding Suite",
                                    "مجموعهٔ بدون روت تغییر و تحلیل بازی‌های آفلاین",
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink,
                            )
                        }
                    }
                }

                // Bilingual selector right at first launch (F-21 / PD-9)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { option ->
                        val selected = lang == option
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(option) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_lang_${option.tag}"),
                            label = {
                                Text(
                                    text = when (option) {
                                        AppLanguage.ENGLISH -> "English"
                                        AppLanguage.PERSIAN -> "فارسی (RTL)"
                                        AppLanguage.BILINGUAL_LEARNING -> "EN + فا"
                                    },
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

                ForgeCard(highlighted = true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = colors.brass)
                        Text(
                            text = biText(
                                lang,
                                "The HEXFORGE Covenant",
                                "پیمان چهارگانهٔ HEXFORGE",
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.brass,
                        )
                    }
                    PromiseRow(
                        lang,
                        "You tick what you want unlimited — the engine figures out HOW.",
                        "شما تیک می‌زنید چه چیزی نامحدود شود — موتور راه‌حل را پیدا می‌کند.",
                    )
                    PromiseRow(
                        lang,
                        "No root. Shizuku optional. Your games, your device, your rules.",
                        "بدون روت. شیزوکو اختیاری. بازی شما، دستگاه شما، قوانین شما.",
                    )
                    PromiseRow(
                        lang,
                        "Every single step is explained, reported, and reversible.",
                        "تمام مراحل با گزارش دقیق و امکان بازگردانی کامل انجام می‌شود.",
                    )
                    PromiseRow(
                        lang,
                        "OFFLINE games only. Online/competitive targets are refused, always.",
                        "فقط بازی‌های آفلاین. اهداف آنلاین و رقابتی همیشه رد می‌شوند.",
                    )
                }

                ForgeCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = colors.ember)
                        Text(
                            text = biText(
                                lang,
                                "Legal & Ethical Scope (PD-1 · PD-4 · PD-5)",
                                "محدودهٔ قانونی و اخلاقی (PD-1 · PD-4 · PD-5)",
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                        )
                    }
                    Text(
                        text = biText(
                            lang,
                            "HEXFORGE targets strictly personal, offline single-player games on your own device. Gatekeeper blocks competitive/server-authoritative titles. Zero copyrighted files are shipped; zero telemetry leaves your phone.",
                            "HEXFORGE منحصراً برای بازی‌های آفلاین و تک‌نفره روی دستگاه شخصی شماست. گیت‌کیپر بازی‌های رقابتی و سرورمحور را مسدود می‌کند. هیچ فایل دارای حق‌نشر توزیع نمی‌شود و هیچ دادهٔ تله‌متری از دستگاه خارج نمی‌گردد.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PromiseRow(lang: AppLanguage, en: String, fa: String) {
    val colors = ForgeTheme.colors
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = colors.verdigris,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = biText(lang, en, fa),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
        )
    }
}
