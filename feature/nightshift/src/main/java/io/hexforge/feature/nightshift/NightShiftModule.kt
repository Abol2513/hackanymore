package io.hexforge.feature.nightshift

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object NightShiftModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M10_NIGHTSHIFT,
        summaryEn = "Screen-off resilient foreground job engine with Room checkpoints, job queues, and OEM battery guards.",
        summaryFa = "موتور اجرای پس‌زمینه مقاوم در برابر خاموشی صفحه با نقاط بازگشت، صف کارها و هشدار باتری OEM.",
        milestoneTarget = "M11",
        isOperationalInM1 = false,
    )
}
