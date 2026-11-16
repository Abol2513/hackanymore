package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.hexforge.core.chronicle.ChronicleEvent
import io.hexforge.core.chronicle.ChronicleMessageCatalog
import io.hexforge.core.chronicle.ChronicleSeverity
import io.hexforge.core.common.AppLanguage
import io.hexforge.core.common.EngineType
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId
import io.hexforge.core.common.OperationScope
import io.hexforge.core.common.ReportLanguage
import io.hexforge.core.common.ReversibleKind
import io.hexforge.core.common.TargetDescriptor
import io.hexforge.core.gatekeeper.GatekeeperAuditRecord
import io.hexforge.core.gatekeeper.GatekeeperResult
import io.hexforge.core.hexcore.ProcMapsParser
import io.hexforge.core.hexelf.BinaryFormatSniffer
import io.hexforge.core.hexscan.InMemoryScanner
import io.hexforge.core.vault.VaultBackupRecord
import io.hexforge.core.vault.VaultRollbackResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class TargetStatusFeedback(
    val packageName: String,
    val cleared: Boolean,
    val messageEn: String,
    val messageFa: String,
)

data class LabDiagnosticSnapshot(
    val catalogPairCount: Int,
    val parsedRegionsCount: Int,
    val scanHitsCount: Int,
    val sniffedFormatsSummary: String,
)

data class HexForgeUiState(
    val preferences: ForgePreferences = ForgePreferences(),
    val activeTier: ExecutionTier = ExecutionTier.T0_HEXSPACE,
    val shizukuAvailable: Boolean = false,
    val adbDirectAvailable: Boolean = false,
    val rootAvailable: Boolean = false,
    val sampleTargets: List<TargetDescriptor> = defaultSampleTargets(),
    val lastTargetFeedback: TargetStatusFeedback? = null,
    val chronicleEvents: List<ChronicleEvent> = emptyList(),
    val chronicleSeverityFilter: ChronicleSeverity? = null,
    val chronicleModuleFilter: ModuleId? = null,
    val vaultEntries: List<VaultBackupRecord> = emptyList(),
    val lastVaultMessageEn: String? = null,
    val lastVaultMessageFa: String? = null,
    val gatekeeperAudit: List<GatekeeperAuditRecord> = emptyList(),
    val modules: List<FeatureModuleDescriptor> = emptyList(),
    val diagnosticSnapshot: LabDiagnosticSnapshot = LabDiagnosticSnapshot(
        catalogPairCount = ChronicleMessageCatalog.size(),
        parsedRegionsCount = 0,
        scanHitsCount = 0,
        sniffedFormatsSummary = "",
    ),
)

private fun defaultSampleTargets(): List<TargetDescriptor> = listOf(
    TargetDescriptor(
        packageName = "io.hexforge.sandbox.knightquest",
        displayNameEn = "Forge Sandbox RPG (MIT Demo)",
        displayNameFa = "بازی تمرینی فورج (نسخهٔ دمو)",
        versionName = "1.0.0",
        versionCode = 100,
        engineType = EngineType.UNITY_IL2CPP,
        detectedStoreSdk = "bazaar-sample",
        isOfflineVerified = true,
    ),
    TargetDescriptor(
        packageName = "ir.offline.desert.rally",
        displayNameEn = "Desert Rally Offline",
        displayNameFa = "رالی کویر آفلاین",
        versionName = "2.4.1",
        versionCode = 241,
        engineType = EngineType.UNITY_MONO,
        detectedStoreSdk = "myket",
        isOfflineVerified = true,
    ),
    TargetDescriptor(
        packageName = "com.tencent.ig",
        displayNameEn = "PUBG Mobile (Online Competitive)",
        displayNameFa = "پابجی موبایل (آنلاین رقابتی)",
        versionName = "3.5.0",
        versionCode = 350,
        engineType = EngineType.UNREAL,
        hasAlwaysOnSockets = true,
        hasAccountLoginFlow = true,
        hasAntiCheatSdk = true,
        isOfflineVerified = false,
    ),
)

class HexForgeViewModel(
    private val container: HexForgeContainer,
) : ViewModel() {

    private val localFilterState = MutableStateFlow(
        Triple<ChronicleSeverity?, ModuleId?, TargetStatusFeedback?>(null, null, null)
    )
    private val vaultBanner = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val optimisticLegalAccepted = MutableStateFlow<Boolean?>(null)
    private val optimisticLanguage = MutableStateFlow<AppLanguage?>(null)

    private val diagnostics = runSelfDiagnostics()

    val uiState: StateFlow<HexForgeUiState> = combine(
        container.preferences.preferences,
        container.chronicleBus.liveEvents,
        container.vaultEngine.entries,
        container.gatekeeperEngine.auditLog,
        localFilterState,
        vaultBanner,
        optimisticLegalAccepted,
        optimisticLanguage,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val prefs = args[0] as ForgePreferences
        @Suppress("UNCHECKED_CAST")
        val events = args[1] as List<ChronicleEvent>
        @Suppress("UNCHECKED_CAST")
        val vaultList = args[2] as List<VaultBackupRecord>
        @Suppress("UNCHECKED_CAST")
        val audit = args[3] as List<GatekeeperAuditRecord>
        @Suppress("UNCHECKED_CAST")
        val filters = args[4] as Triple<ChronicleSeverity?, ModuleId?, TargetStatusFeedback?>
        @Suppress("UNCHECKED_CAST")
        val banner = args[5] as Pair<String?, String?>
        val legalOverride = args[6] as Boolean?
        val langOverride = args[7] as AppLanguage?

        val effectivePrefs = prefs.copy(
            legalAccepted = legalOverride ?: prefs.legalAccepted,
            appLanguage = langOverride ?: prefs.appLanguage,
        )
        val (sevFilter, modFilter, feedback) = filters
        val filteredEvents = events.filter { ev ->
            (sevFilter == null || ev.severity == sevFilter) &&
                (modFilter == null || ev.module == modFilter)
        }
        HexForgeUiState(
            preferences = effectivePrefs,
            activeTier = ExecutionTier.T0_HEXSPACE,
            sampleTargets = defaultSampleTargets(),
            lastTargetFeedback = feedback,
            chronicleEvents = filteredEvents,
            chronicleSeverityFilter = sevFilter,
            chronicleModuleFilter = modFilter,
            vaultEntries = vaultList,
            lastVaultMessageEn = banner.first,
            lastVaultMessageFa = banner.second,
            gatekeeperAudit = audit,
            modules = container.moduleCatalog,
            diagnosticSnapshot = diagnostics,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HexForgeUiState(
            modules = container.moduleCatalog,
            diagnosticSnapshot = diagnostics,
        ),
    )

    fun acceptLegalNotice() {
        optimisticLegalAccepted.value = true
        viewModelScope.launch {
            container.preferences.setLegalAccepted(true)
            container.chronicleBus.emit(
                messageKey = "M6_LEGAL_ACCEPTED",
                tier = ExecutionTier.T0_HEXSPACE,
                targetId = "io.hexforge",
            )
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        optimisticLanguage.value = language
        viewModelScope.launch {
            container.preferences.setAppLanguage(language)
            container.chronicleBus.emit(
                messageKey = "M13_LANG_SWITCH",
                tier = ExecutionTier.T0_HEXSPACE,
                targetId = "io.hexforge",
                params = mapOf(
                    "lang" to language.labelEn,
                    "rtl" to (language == AppLanguage.PERSIAN).toString(),
                ),
            )
        }
    }

    fun setReportLanguage(language: ReportLanguage) {
        viewModelScope.launch {
            container.preferences.setReportLanguage(language)
            container.chronicleBus.emit(
                messageKey = "M9_LANG_CHANGED",
                tier = ExecutionTier.T0_HEXSPACE,
                targetId = "io.hexforge",
                params = mapOf("lang" to language.labelEn),
            )
        }
    }

    fun togglePersianDigits(enabled: Boolean) {
        viewModelScope.launch {
            container.preferences.setUsePersianDigits(enabled)
            container.chronicleBus.emit(
                messageKey = "M9_PERSIAN_DIGITS_TOGGLE",
                tier = ExecutionTier.T0_HEXSPACE,
                targetId = "io.hexforge",
                params = mapOf("enabled" to enabled.toString()),
            )
        }
    }

    fun verifyTargetClearance(target: TargetDescriptor, scope: OperationScope) {
        val result = container.gatekeeperEngine.requestClearance(
            target = target,
            operationScope = scope,
            tier = ExecutionTier.T0_HEXSPACE,
        )
        val feedback = when (result) {
            is GatekeeperResult.Cleared -> TargetStatusFeedback(
                packageName = target.packageName,
                cleared = true,
                messageEn = "${result.reasonEn} [Token: ${result.token.tokenId}]",
                messageFa = "${result.reasonFa} [توکن: ${result.token.tokenId}]",
            )
            is GatekeeperResult.Denied -> TargetStatusFeedback(
                packageName = target.packageName,
                cleared = false,
                messageEn = result.reasonEn,
                messageFa = result.reasonFa,
            )
        }
        localFilterState.update { it.copy(third = feedback) }
    }

    fun verifyCustomPackage(packageName: String) {
        val clean = packageName.trim()
        if (clean.isEmpty()) return
        val descriptor = TargetDescriptor(
            packageName = clean,
            displayNameEn = clean,
            displayNameFa = clean,
            versionName = "1.0",
        )
        verifyTargetClearance(descriptor, OperationScope.MEMORY_SCAN_WRITE)
    }

    fun createSampleVaultSnapshot(targetPackage: String) {
        viewModelScope.launch {
            val samplePayload = buildString {
                append("HEXFORGE_SAVE_SNAPSHOT_V1\n")
                append("target=$targetPackage\n")
                append("coins=1250\n")
                append("gems=40\n")
                append("timestamp=${System.currentTimeMillis()}\n")
            }.toByteArray(Charsets.UTF_8)

            val record = container.vaultEngine.backupBytes(
                rawBytes = samplePayload,
                originalPath = "/virtual/0/$targetPackage/files/save_profile.dat",
                targetPackage = targetPackage,
                jobId = "m1_snapshot_${System.currentTimeMillis() / 1000}",
                kind = ReversibleKind.FILE_SNAPSHOT,
                summaryEn = "Pre-forge save snapshot for $targetPackage",
                summaryFa = "نسخهٔ پشتیبان پیش از تغییر برای $targetPackage",
            )
            vaultBanner.value =
                "Backed up ${record.originalSizeBytes}B -> ${record.compressedSizeBytes}B (${record.vaultId})" to
                    "پشتیبان‌گیری ${record.originalSizeBytes} به ${record.compressedSizeBytes} بایت (${record.vaultId})"
        }
    }

    fun rollbackVaultEntry(vaultId: String) {
        viewModelScope.launch {
            when (val res = container.vaultEngine.rollbackBytes(vaultId)) {
                is VaultRollbackResult.Success -> {
                    vaultBanner.value =
                        "Rollback verified (${res.restoredBytes.size}B, SHA-256 ${res.sha256Hex.take(10)})" to
                            "بازگردانی با موفقیت تأیید شد (${res.restoredBytes.size} بایت)"
                }
                is VaultRollbackResult.Failure -> {
                    vaultBanner.value = res.reasonEn to res.reasonFa
                }
            }
        }
    }

    fun toggleVaultProtection(vaultId: String) {
        viewModelScope.launch {
            container.vaultEngine.toggleProtected(vaultId)
        }
    }

    fun setChronicleSeverityFilter(severity: ChronicleSeverity?) {
        localFilterState.update { it.copy(first = severity) }
    }

    fun setChronicleModuleFilter(moduleId: ModuleId?) {
        localFilterState.update { it.copy(second = moduleId) }
    }

    private fun runSelfDiagnostics(): LabDiagnosticSnapshot {
        val sampleMaps = """
            70001000-70011000 rw-p 00000000 00:00 0 [anon:dalvik-main space]
            71000000-71200000 rw-p 00000000 00:00 0 [heap]
            74000000-74500000 r-xp 00000000 fd:01 9912 /data/app/lib/arm64/libil2cpp.so
        """.trimIndent()
        val regions = ProcMapsParser.parse(sampleMaps)

        val memBuf = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(100)
            .putInt(1250)
            .putInt(999)
            .putInt(1250)
            .array()
        val scanHits = InMemoryScanner.scanExactInt32(memBuf, 0x7100_0000L, 1250)

        val elfMagic = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 2, 1, 1, 0)
        val elfDetected = BinaryFormatSniffer.sniff(elfMagic)

        return LabDiagnosticSnapshot(
            catalogPairCount = ChronicleMessageCatalog.size(),
            parsedRegionsCount = regions.size,
            scanHitsCount = scanHits.size,
            sniffedFormatsSummary = elfDetected.labelEn,
        )
    }

    class Factory(private val container: HexForgeContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HexForgeViewModel(container) as T
        }
    }
}
