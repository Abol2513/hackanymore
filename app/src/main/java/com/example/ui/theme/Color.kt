package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Exact HEXFORGE Dark-Classic design tokens (Part 20 M13).
 * Aesthetic: Victorian instrument panel meets modern terminal.
 */
object HexForgePalette {
    val ObsidianBg = Color(0xFF0B0E11)
    val InstrumentSurface = Color(0xFF12161B)
    val RaisedPlate = Color(0xFF1A2027)
    val HairlineBorder = Color(0xFF2A323C)
    val WarmPaperInk = Color(0xFFE8E3D5)
    val MutedSlate = Color(0xFF9AA3AD)

    val AntiqueBrass = Color(0xFFC9A227)
    val BrassDim = Color(0xFF8E721D)
    val EmberWarning = Color(0xFFD96C3F)
    val VerdigrisSuccess = Color(0xFF3FA37A)
    val BloodDestructive = Color(0xFFB33B3B)
    val AzureSteelInfo = Color(0xFF5B8BB5)
}

@Immutable
data class ForgeColors(
    val bg: Color = HexForgePalette.ObsidianBg,
    val surface: Color = HexForgePalette.InstrumentSurface,
    val raised: Color = HexForgePalette.RaisedPlate,
    val hairline: Color = HexForgePalette.HairlineBorder,
    val ink: Color = HexForgePalette.WarmPaperInk,
    val muted: Color = HexForgePalette.MutedSlate,
    val brass: Color = HexForgePalette.AntiqueBrass,
    val brassDim: Color = HexForgePalette.BrassDim,
    val ember: Color = HexForgePalette.EmberWarning,
    val verdigris: Color = HexForgePalette.VerdigrisSuccess,
    val blood: Color = HexForgePalette.BloodDestructive,
    val azureSteel: Color = HexForgePalette.AzureSteelInfo,
)

val LocalForgeColors = staticCompositionLocalOf { ForgeColors() }
