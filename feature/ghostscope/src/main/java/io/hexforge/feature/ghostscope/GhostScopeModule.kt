package io.hexforge.feature.ghostscope

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object GhostScopeModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M1_GHOSTSCOPE,
        summaryEn = "Rootless memory lab: exact, fuzzy, grouped, pointer & AOB scans, freeze patrol, ParamScope HUD, ChronoForge & HexScript.",
        summaryFa = "آزمایشگاه حافظه بدون روت: اسکن دقیق، فازی، گروهی، اشاره‌گر و الگو، قفل مقدار، پنل شناور، کنترل سرعت و اسکریپت لوا.",
        milestoneTarget = "M3–M4",
        isOperationalInM1 = true,
    )
}
