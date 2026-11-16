package com.example

import android.content.Context
import io.hexforge.core.chronicle.ChronicleBus
import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId
import io.hexforge.core.database.HexForgeDatabase
import io.hexforge.core.gatekeeper.GatekeeperEngine
import io.hexforge.core.vault.VaultEngine
import io.hexforge.feature.deepprobe.DeepProbeModule
import io.hexforge.feature.depot.DepotAndPluginModules
import io.hexforge.feature.ghostscope.GhostScopeModule
import io.hexforge.feature.hexspace.HexSpaceModule
import io.hexforge.feature.nightshift.NightShiftModule
import io.hexforge.feature.obsidianfiles.ObsidianFilesModule
import io.hexforge.feature.oracle.OracleModule
import io.hexforge.feature.patchforge.PatchForgeModule
import io.hexforge.feature.shizuku.ShizukuPowerModule
import io.hexforge.feature.skinwalker.SkinWalkerModule
import io.hexforge.feature.wishbuilder.WishBuilderModule
import java.io.File

/**
 * Clean constructor-injection service locator for HEXFORGE engines and module catalog.
 */
class HexForgeContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: HexForgeDatabase = HexForgeDatabase.getInstance(appContext)

    val chronicleBus: ChronicleBus = ChronicleBus(
        chronicleDao = database.chronicleDao(),
    )

    val vaultEngine: VaultEngine = VaultEngine(
        vaultRootDir = File(appContext.filesDir, "vault"),
        vaultDao = database.vaultDao(),
        chronicleBus = chronicleBus,
    )

    val gatekeeperEngine: GatekeeperEngine = GatekeeperEngine(
        gatekeeperDao = database.gatekeeperDao(),
        chronicleBus = chronicleBus,
    )

    val preferences: ForgePreferencesRepository = ForgePreferencesRepository(appContext)

    val moduleCatalog: List<FeatureModuleDescriptor> = listOf(
        HexSpaceModule.descriptor,
        GhostScopeModule.descriptor,
        PatchForgeModule.descriptor,
        SkinWalkerModule.descriptor,
        DeepProbeModule.descriptor,
        ObsidianFilesModule.descriptor,
        FeatureModuleDescriptor(
            id = ModuleId.M6_GATEKEEPER,
            summaryEn = "Structural offline-only policy engine enforcing PD-1 blocklist & server-authority checks.",
            summaryFa = "موتور ساختاری سیاست آفلاین‌محور برای مسدودسازی بازی‌های آنلاین و سرورمحور.",
            milestoneTarget = "M1",
            isOperationalInM1 = true,
        ),
        ShizukuPowerModule.descriptor,
        OracleModule.descriptor,
        FeatureModuleDescriptor(
            id = ModuleId.M9_CHRONICLE,
            summaryEn = "Universal bilingual EN/FA action telemetry stream with Room persistence and terminal view.",
            summaryFa = "جریان گزارش دوزبانهٔ فارسی و انگلیسی برای تمام عملیات ماژول‌ها با نمای ترمینال.",
            milestoneTarget = "M1",
            isOperationalInM1 = true,
        ),
        NightShiftModule.descriptor,
        WishBuilderModule.descriptor,
        FeatureModuleDescriptor(
            id = ModuleId.M12_VAULT,
            summaryEn = "Content-addressed SHA-256 + LZ4 backup vault with verified one-tap rollback.",
            summaryFa = "گنجینهٔ پشتیبان یکتاسازی‌شده با SHA-256 و فشرده‌سازی LZ4 با بازگردانی یک‌کلیکی.",
            milestoneTarget = "M1",
            isOperationalInM1 = true,
        ),
        FeatureModuleDescriptor(
            id = ModuleId.M13_FORGE_UI,
            summaryEn = "Dark-classic Victorian instrument shell with full Persian RTL & Vazirmatn typography.",
            summaryFa = "پوستهٔ کلاسیک-تیره با چیدمان کامل راست‌به‌چپ فارسی و تایپوگرافی وزیرمتن.",
            milestoneTarget = "M1",
            isOperationalInM1 = true,
        ),
        DepotAndPluginModules.pluginDescriptor,
        DepotAndPluginModules.depotDescriptor,
    )

    init {
        chronicleBus.emit(
            messageKey = "M13_UI_BOOT",
            tier = ExecutionTier.T0_HEXSPACE,
            targetId = "io.hexforge",
            params = mapOf(
                "locale" to appContext.resources.configuration.locales[0].language,
                "tier" to ExecutionTier.T0_HEXSPACE.code,
            ),
        )
    }
}
