package io.hexforge.core.common

import kotlinx.serialization.Serializable

/**
 * Execution privilege ladder defined in HEXFORGE Part 6 (PD-2, PD-3).
 */
@Serializable
enum class ExecutionTier(
    val code: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
) {
    T0_HEXSPACE(
        code = "T0",
        titleEn = "HexSpace (Rootless)",
        titleFa = "هگزاسپیس (بدون روت)",
        descriptionEn = "Virtual container ownership — memory lab, speedhack, and sandbox file ops work without root or Shizuku.",
        descriptionFa = "مالکیت فرایند در کانتینر مجازی؛ اسکن حافظه، کنترل سرعت و ویرایش سندباکس بدون روت و بدون شیزوکو.",
    ),
    T1_SHIZUKU(
        code = "T1",
        titleEn = "Shizuku Bound",
        titleFa = "شیزوکو متصل",
        descriptionEn = "UID-2000 shell privileges for silent APK installs, pm path pulls, appops grants, and run-as browsing.",
        descriptionFa = "دسترسی پوسته UID-2000 برای نصب بی‌صدا، استخراج APK با pm path، مجوزهای appops و مرور run-as.",
    ),
    T2_ADB_DIRECT(
        code = "T2",
        titleEn = "ADB-Direct",
        titleFa = "ADB مستقیم",
        descriptionEn = "Self-contained Android 11+ wireless debugging client providing UID-2000 shell access without Shizuku.",
        descriptionFa = "کلاینت داخلی دیباگ بی‌سیم اندروید ۱۱+ برای دسترسی UID-2000 بدون نیاز به اپ شیزوکو.",
    ),
    T3_ROOT(
        code = "T3",
        titleEn = "Root (Optional)",
        titleFa = "روت (اختیاری)",
        descriptionEn = "Optional booster for attaching to external processes outside HexSpace or direct /data/data access.",
        descriptionFa = "افزونهٔ اختیاری برای اتصال به فرایندهای خارج از کانتینر یا دسترسی مستقیم به /data/data.",
    ),
}

/**
 * Complete 16-module architecture identifier registry (M0..M15, Part 4).
 */
@Serializable
enum class ModuleId(
    val code: String,
    val titleEn: String,
    val titleFa: String,
    val minimumTier: ExecutionTier,
) {
    M0_HEXSPACE("M0", "HexSpace", "هگزاسپیس", ExecutionTier.T0_HEXSPACE),
    M1_GHOSTSCOPE("M1", "GhostScope", "گوست‌اسکوپ", ExecutionTier.T0_HEXSPACE),
    M2_PATCHFORGE("M2", "PatchForge", "پچ‌فورج", ExecutionTier.T0_HEXSPACE),
    M3_SKINWALKER("M3", "SkinWalker", "اسکین‌واکر", ExecutionTier.T0_HEXSPACE),
    M4_DEEPPROBE("M4", "DeepProbe", "دیپ‌پروب", ExecutionTier.T0_HEXSPACE),
    M5_OBSIDIANFILES("M5", "ObsidianFiles", "آبسیدین‌فایلز", ExecutionTier.T0_HEXSPACE),
    M6_GATEKEEPER("M6", "Gatekeeper", "گیت‌کیپر", ExecutionTier.T0_HEXSPACE),
    M7_SHIZUKU("M7", "Shizuku Power Core", "هستهٔ شیزوکو", ExecutionTier.T1_SHIZUKU),
    M8_ORACLE("M8", "The Oracle", "اوراکل", ExecutionTier.T0_HEXSPACE),
    M9_CHRONICLE("M9", "Chronicle", "کرونیکل", ExecutionTier.T0_HEXSPACE),
    M10_NIGHTSHIFT("M10", "NightShift", "نایت‌شیفت", ExecutionTier.T0_HEXSPACE),
    M11_WISHBUILDER("M11", "WishBuilder", "ویش‌بیلدر", ExecutionTier.T0_HEXSPACE),
    M12_VAULT("M12", "Vault", "والت", ExecutionTier.T0_HEXSPACE),
    M13_FORGE_UI("M13", "Forge UI", "رابط فورج", ExecutionTier.T0_HEXSPACE),
    M14_PLUGINS("M14", "Plugin System", "سامانهٔ افزونه", ExecutionTier.T0_HEXSPACE),
    M15_FORGE_DEPOT("M15", "Forge Depot", "مخزن فورج", ExecutionTier.T0_HEXSPACE),
}

@Serializable
enum class EngineType(val labelEn: String, val labelFa: String) {
    UNITY_IL2CPP("Unity IL2CPP", "یونیتی IL2CPP"),
    UNITY_MONO("Unity Mono", "یونیتی Mono"),
    UNREAL("Unreal Engine", "آنریل انجین"),
    COCOS2D("Cocos2d-x / Creator", "کوکوس ۲دی"),
    GODOT("Godot", "گودو"),
    NATIVE_CPP("Native C/C++", "بومی C/C++"),
    ANDROID_JAVA("Android JVM / Dalvik", "جاوا / دالویک"),
    UNKNOWN("Unknown / Inspecting", "نامشخص / در حال بررسی"),
}

@Serializable
enum class AppLanguage(val tag: String, val labelEn: String, val labelFa: String) {
    ENGLISH("en", "English (LTR)", "انگلیسی (LTR)"),
    PERSIAN("fa", "Persian (RTL)", "فارسی (RTL)"),
    BILINGUAL_LEARNING("bi", "Bilingual EN+FA", "دوزبانه (آموزشی)"),
}

@Serializable
enum class ReportLanguage(val labelEn: String, val labelFa: String) {
    ENGLISH("English", "انگلیسی"),
    PERSIAN("Persian (فارسی)", "فارسی"),
    ASK_PER_JOB("Ask per job", "پرسش برای هر عملیات"),
}

@Serializable
enum class OperationScope(val labelEn: String, val labelFa: String) {
    DEEP_PROBE_INSPECT("DeepProbe Read-Only Inspection", "تحلیل فقط-خواندنی دیپ‌پروب"),
    HEXSPACE_LAUNCH("HexSpace Container Launch", "اجرا در کانتینر هگزاسپیس"),
    MEMORY_SCAN_WRITE("GhostScope Memory Scan & Edit", "اسکن و ویرایش حافظه گوست‌اسکوپ"),
    STATIC_APK_PATCH("PatchForge Static APK Patch", "پچ استاتیک APK در پچ‌فورج"),
    AD_AND_TRACKER_HYGIENE("Ad & Tracker Removal Only", "حذف تبلیغات و ردیاب‌ها فقط"),
    FILE_OR_SAVE_INJECT("SkinWalker File & Save Injection", "تزریق فایل و ذخیره در اسکین‌واکر"),
    ORACLE_TRIAGE("Oracle AI Strategy Consultation", "مشاورهٔ هوش مصنوعی اوراکل"),
}

@Serializable
enum class GatekeeperVerdict {
    CLEARED_OFFLINE,
    LOCAL_HYGIENE_ONLY,
    DENIED_ONLINE_COMPETITIVE,
}

/**
 * Target game or imported binary metadata evaluated by M6 Gatekeeper and M4 DeepProbe.
 */
@Serializable
data class TargetDescriptor(
    val packageName: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val versionName: String,
    val versionCode: Long = 1L,
    val engineType: EngineType = EngineType.UNKNOWN,
    val publisherHint: String = "",
    val hasAlwaysOnSockets: Boolean = false,
    val hasAccountLoginFlow: Boolean = false,
    val hasAntiCheatSdk: Boolean = false,
    val detectedStoreSdk: String = "none",
    val isOfflineVerified: Boolean = true,
)

/**
 * Unforgeable clearance token issued exclusively by GatekeeperEngine (PD-1, Part 13).
 * Engines require an instance of ClearanceToken before mutating or attaching to any target.
 */
data class ClearanceToken internal constructor(
    val tokenId: String,
    val targetPackage: String,
    val operationScope: OperationScope,
    val verdict: GatekeeperVerdict,
    val issuedAtEpochMs: Long,
    val signatureDigest: String,
) {
    companion object {
        /**
         * Mints a ClearanceToken only when provided a non-blank Gatekeeper seal digest.
         * Callers outside :core:gatekeeper cannot mint valid tokens recognized by engines.
         */
        fun mintInternal(
            tokenId: String,
            targetPackage: String,
            operationScope: OperationScope,
            verdict: GatekeeperVerdict,
            issuedAtEpochMs: Long,
            sealSecret: String,
            signatureDigest: String,
        ): ClearanceToken {
            require(sealSecret == GATEKEEPER_INTERNAL_SEAL) {
                "ClearanceToken can only be minted by GatekeeperEngine"
            }
            return ClearanceToken(
                tokenId = tokenId,
                targetPackage = targetPackage,
                operationScope = operationScope,
                verdict = verdict,
                issuedAtEpochMs = issuedAtEpochMs,
                signatureDigest = signatureDigest,
            )
        }

        const val GATEKEEPER_INTERNAL_SEAL = "HEXFORGE_GK_V1_BOUNDARY_SEAL"
    }
}

@Serializable
enum class ReversibleKind(val labelEn: String, val labelFa: String) {
    FILE_SNAPSHOT("File Snapshot", "نسخهٔ پشتیبان فایل"),
    APK_ORIGINAL("Original APK Backup", "نسخهٔ اصلی APK"),
    MEMORY_JOURNAL("Memory Write Journal", "ژورنال تغییرات حافظه"),
    INSTALL_RECEIPT("Install Receipt", "رسید نصب"),
    MANIFEST_DIFF("Manifest & Permission Diff", "تفاوت مانیفست و مجوزها"),
}

@Serializable
data class FeatureModuleDescriptor(
    val id: ModuleId,
    val summaryEn: String,
    val summaryFa: String,
    val milestoneTarget: String,
    val isOperationalInM1: Boolean,
)

object PersianNumeralFormatter {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun format(input: String, usePersianDigits: Boolean): String {
        if (!usePersianDigits) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
