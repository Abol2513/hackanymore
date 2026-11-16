package io.hexforge.feature.skinwalker

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object SkinWalkerModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M3_SKINWALKER,
        summaryEn = "Reversible .hexpack mod injector, OBB repacker, Unity asset-mount loader, and SaveForge save-game editor.",
        summaryFa = "تزریق‌گر بازگشت‌پذیر بسته‌های .hexpack، ویرایشگر OBB، بارگذار دارایی یونیتی و آزمایشگاه فایل ذخیره SaveForge.",
        milestoneTarget = "M7",
        isOperationalInM1 = false,
    )
}
