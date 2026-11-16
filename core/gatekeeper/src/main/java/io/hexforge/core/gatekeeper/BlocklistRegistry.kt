package io.hexforge.core.gatekeeper

data class BlocklistRule(
    val ruleCode: String,
    val pattern: String,
    val isPrefix: Boolean,
    val categoryEn: String,
    val categoryFa: String,
)

/**
 * Local registry of known online/competitive/multiplayer titles and publisher clusters (PD-1, Part 13).
 */
object BlocklistRegistry {
    val rules: List<BlocklistRule> = listOf(
        BlocklistRule("GK-PKG-PUBG", "com.tencent.ig", false, "Competitive online battle royale", "بتل‌رویال آنلاین رقابتی"),
        BlocklistRule("GK-PKG-PUBG-IN", "com.pubg.", true, "Competitive online battle royale", "بتل‌رویال آنلاین رقابتی"),
        BlocklistRule("GK-PKG-CODM", "com.activision.callofduty.", true, "Competitive online shooter", "تیراندازی آنلاین رقابتی"),
        BlocklistRule("GK-PKG-FREEFIRE", "com.dts.freefire", true, "Competitive online shooter", "تیراندازی آنلاین رقابتی"),
        BlocklistRule("GK-PKG-MLBB", "com.mobile.legends", false, "Competitive online MOBA", "موبای آنلاین رقابتی"),
        BlocklistRule("GK-PUB-SUPERCELL", "com.supercell.", true, "Server-authoritative multiplayer publisher", "ناشر بازی‌های چندنفره سرورمحور"),
        BlocklistRule("GK-PUB-RIOT", "com.riotgames.", true, "Competitive multiplayer publisher", "ناشر بازی‌های رقابتی آنلاین"),
        BlocklistRule("GK-PUB-GARENA", "com.garena.game.", true, "Competitive online game cluster", "خوشهٔ بازی‌های آنلاین رقابتی"),
        BlocklistRule("GK-PUB-HOYO", "com.miHoYo.", true, "Server-authoritative live-service RPG", "بازی آنلاین زنده و سرورمحور"),
        BlocklistRule("GK-PUB-COGNOSPHERE", "com.HoYoverse.", true, "Server-authoritative live-service RPG", "بازی آنلاین زنده و سرورمحور"),
        BlocklistRule("GK-PKG-ROBLOX", "com.roblox.client", false, "Online multiplayer platform", "پلتفرم چندنفرهٔ آنلاین"),
        BlocklistRule("GK-PKG-FORTNITE", "com.epicgames.fortnite", false, "Competitive online battle royale", "بتل‌رویال آنلاین رقابتی"),
        BlocklistRule("GK-PKG-BRAWL", "com.ea.gp.fifamobile", false, "Online competitive sports economy", "ورزشی آنلاین با اقتصاد سرورمحور"),
        BlocklistRule("GK-PKG-CLASH", "com.kiloo.subwaysurf.online", false, "Online tournament variant", "نسخهٔ رقابتی آنلاین"),
    )

    fun matchPackage(packageName: String): BlocklistRule? {
        val normalized = packageName.trim().lowercase()
        return rules.firstOrNull { rule ->
            val p = rule.pattern.lowercase()
            if (rule.isPrefix) normalized.startsWith(p) else normalized == p
        }
    }
}
