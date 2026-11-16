package io.hexforge.feature.deepprobe

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object DeepProbeModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M4_DEEPPROBE,
        summaryEn = "5-stage import inspector: packer fingerprinting, structural map, engine detection, IL2CPP/Mono Value Atlas & Forge Plan.",
        summaryFa = "تحلیلگر ۵مرحله‌ای فایل‌های واردشده: تشخیص پکر و موتور، نقشهٔ ساختاری، اطلس مقادیر IL2CPP/Mono و برنامهٔ فورج.",
        milestoneTarget = "M6",
        isOperationalInM1 = true,
    )
}
