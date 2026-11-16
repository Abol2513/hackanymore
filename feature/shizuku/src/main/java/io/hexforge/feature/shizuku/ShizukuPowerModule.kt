package io.hexforge.feature.shizuku

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object ShizukuPowerModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M7_SHIZUKU,
        summaryEn = "UID-2000 capability library via Shizuku Binder & self-contained ADB-Direct wireless pairing.",
        summaryFa = "کتابخانهٔ قابلیت‌های UID-2000 از طریق شیزوکو و کلاینت داخلی ADB مستقیم بی‌سیم.",
        milestoneTarget = "M9",
        isOperationalInM1 = false,
    )
}
