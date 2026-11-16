package io.hexforge.feature.hexspace

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object HexSpaceModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M0_HEXSPACE,
        summaryEn = "Rootless virtual container hosting offline target games for T0 memory, speed, and sandbox ops.",
        summaryFa = "کانتینر مجازی بدون روت برای اجرای بازی‌های آفلاین و دسترسی کامل سطح T0 به حافظه و فایل‌ها.",
        milestoneTarget = "M2",
        isOperationalInM1 = false,
    )
}
