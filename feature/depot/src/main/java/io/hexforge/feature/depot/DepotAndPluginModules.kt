package io.hexforge.feature.depot

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object DepotAndPluginModules {
    val pluginDescriptor = FeatureModuleDescriptor(
        id = ModuleId.M14_PLUGINS,
        summaryEn = "Ed25519-signed sandboxed plugin SDK for HexSpace shims, Iranian IAB adapters, and engine profiles.",
        summaryFa = "سامانهٔ افزونه‌های ایزوله و امضاشده با ed25519 برای شیم‌ها و آداپتورهای پرداخت.",
        milestoneTarget = "M11",
        isOperationalInM1 = false,
    )

    val depotDescriptor = FeatureModuleDescriptor(
        id = ModuleId.M15_FORGE_DEPOT,
        summaryEn = "Opt-in community index distributing signed instruction-only patch packs, recipes, and Codex updates.",
        summaryFa = "مخزن اختیاری جامعه برای بسته‌های دستورالعمل امضاشده، الگوها و به‌روزرسانی‌های Codex (بدون باینری).",
        milestoneTarget = "M11",
        isOperationalInM1 = false,
    )
}
