package io.hexforge.feature.obsidianfiles

import io.hexforge.core.common.FeatureModuleDescriptor
import io.hexforge.core.common.ModuleId

object ObsidianFilesModule {
    val descriptor = FeatureModuleDescriptor(
        id = ModuleId.M5_OBSIDIANFILES,
        summaryEn = "Dual-pane file commander with hex, DEX/smali, ARSC, AXML, SQLite editors and APK co-editing.",
        summaryFa = "مدیریت فایل دوپنلی با ویرایشگرهای هگز، DEX/اسمالی، ARSC، AXML، SQLite و ویرایش هم‌زمان APK.",
        milestoneTarget = "M8",
        isOperationalInM1 = false,
    )
}
