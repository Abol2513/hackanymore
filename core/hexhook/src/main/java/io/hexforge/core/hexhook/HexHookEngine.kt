package io.hexforge.core.hexhook

enum class HookBackend(
    val id: String,
    val minApi: Int,
    val maxApi: Int,
    val notesEn: String,
    val notesFa: String,
) {
    LSPLANT(
        id = "lsplant",
        minApi = 24,
        maxApi = 36,
        notesEn = "Primary ART ArtMethod inline/backup hooking backend for Android 7–16.",
        notesFa = "بک‌اند اصلی قلاب‌گذاری متدهای ART برای اندروید ۷ تا ۱۶.",
    ),
    PINE(
        id = "pine",
        minApi = 24,
        maxApi = 35,
        notesEn = "Alternative ART hook backend selectable for legacy or OEM-specific ART builds.",
        notesFa = "بک‌اند جایگزین ART برای رام‌ها و نسخه‌های خاص تولیدکنندگان.",
    ),
    NATIVE_INLINE_ARM64(
        id = "dobby-arm64",
        minApi = 24,
        maxApi = 36,
        notesEn = "ARM64 native trampoline & PLT/GOT interposition for ChronoForge speedhack.",
        notesFa = "ترامپولین بومی ARM64 و بازنویسی PLT/GOT برای کنترل سرعت کرونوفورج.",
    ),
    XDL_SYMBOLS(
        id = "xdl",
        minApi = 24,
        maxApi = 36,
        notesEn = "Enhanced linker-namespace symbol resolver inside HexSpace child processes.",
        notesFa = "حل‌کنندهٔ نمادهای بومی در فضای نام لینکِر داخل فرایندهای هگزاسپیس.",
    ),
}

object HookCompatibilityMatrix {
    fun supportedBackendsForApi(apiLevel: Int): List<HookBackend> =
        HookBackend.entries.filter { apiLevel in it.minApi..it.maxApi }
}
