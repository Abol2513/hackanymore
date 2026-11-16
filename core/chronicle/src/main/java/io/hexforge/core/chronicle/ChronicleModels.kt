package io.hexforge.core.chronicle

import io.hexforge.core.common.ExecutionTier
import io.hexforge.core.common.ModuleId
import io.hexforge.core.common.PersianNumeralFormatter
import io.hexforge.core.common.ReportLanguage

enum class ChronicleSeverity(val tagEn: String, val tagFa: String) {
    INFO("info", "اطلاع"),
    SUCCESS("success", "موفق"),
    WARN("warn", "هشدار"),
    ERROR("error", "خطا"),
}

enum class ChroniclePhase(val tagEn: String, val tagFa: String) {
    PRE("pre", "پیش‌آغاز"),
    EXECUTE("execute", "اجرا"),
    POST("post", "پایان"),
    ROLLBACK("rollback", "بازگردانی"),
}

data class BilingualMessageTemplate(
    val key: String,
    val module: ModuleId,
    val defaultPhase: ChroniclePhase,
    val defaultSeverity: ChronicleSeverity,
    val templateEn: String,
    val templateFa: String,
)

data class ChronicleEvent(
    val uuid: String,
    val timestampMs: Long,
    val module: ModuleId,
    val tier: ExecutionTier,
    val severity: ChronicleSeverity,
    val phase: ChroniclePhase,
    val targetId: String,
    val messageKey: String,
    val messageEn: String,
    val messageFa: String,
    val payloadJson: String = "{}",
    val reversibleRefId: String? = null,
) {
    fun formatLine(language: ReportLanguage, usePersianDigits: Boolean): String {
        val raw = when (language) {
            ReportLanguage.ENGLISH ->
                "[${module.titleEn}][${tier.code}][${phase.tagEn}] $messageEn"
            ReportLanguage.PERSIAN, ReportLanguage.ASK_PER_JOB ->
                "[${module.titleFa}][${tier.code}][${phase.tagFa}] $messageFa"
        }
        return PersianNumeralFormatter.format(raw, usePersianDigits && language != ReportLanguage.ENGLISH)
    }
}
