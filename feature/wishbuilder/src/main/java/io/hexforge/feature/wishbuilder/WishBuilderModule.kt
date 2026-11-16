package io.hexforge.feature.wishbuilder

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object WishBuilderModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M11_WISHBUILDER,
        summaryEn = "Outcome-driven intent orchestrator resolving user wish checkboxes into ranked execution DAGs.",
        summaryFa = "هماهنگ‌کنندهٔ خواسته‌های کاربر که تیک‌های نامحدود را به گراف اجرایی رتبه‌بندی‌شده تبدیل می‌کند.",
        milestoneTarget = "M11",
        isOperationalInM1 = false,
    )
}
