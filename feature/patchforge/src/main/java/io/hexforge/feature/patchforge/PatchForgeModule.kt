package io.hexforge.feature.patchforge

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object PatchForgeModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M2_PATCHFORGE,
        summaryEn = "Static APK engine: P-LVL, Google & Iranian IAB (Bazaar/Myket), P-ADS, P-PERM, P-SIG, smali surgery & apksig signing.",
        summaryFa = "موتور پچ استاتیک APK: دور زدن مجوز، شبیه‌سازی پرداخت بازار/مایکت/گوگل، حذف تبلیغات، جراحی اسمالی و امضای نسخه.",
        milestoneTarget = "M5",
        isOperationalInM1 = false,
    )
}
