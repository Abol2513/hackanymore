package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val HexForgeDarkColorScheme = darkColorScheme(
    primary = HexForgePalette.AntiqueBrass,
    onPrimary = HexForgePalette.ObsidianBg,
    primaryContainer = HexForgePalette.BrassDim,
    onPrimaryContainer = HexForgePalette.WarmPaperInk,
    secondary = HexForgePalette.AzureSteelInfo,
    onSecondary = HexForgePalette.ObsidianBg,
    tertiary = HexForgePalette.VerdigrisSuccess,
    onTertiary = HexForgePalette.ObsidianBg,
    background = HexForgePalette.ObsidianBg,
    onBackground = HexForgePalette.WarmPaperInk,
    surface = HexForgePalette.InstrumentSurface,
    onSurface = HexForgePalette.WarmPaperInk,
    surfaceVariant = HexForgePalette.RaisedPlate,
    onSurfaceVariant = HexForgePalette.MutedSlate,
    outline = HexForgePalette.HairlineBorder,
    error = HexForgePalette.BloodDestructive,
    onError = HexForgePalette.WarmPaperInk,
)

private val HexForgeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
)

object ForgeTheme {
    val colors: ForgeColors
        @Composable get() = LocalForgeColors.current
}

@Composable
fun HexForgeTheme(
    isPersianPrimary: Boolean = false,
    forceRtl: Boolean = isPersianPrimary,
    content: @Composable () -> Unit,
) {
    val layoutDirection = if (forceRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalForgeColors provides ForgeColors(),
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = HexForgeDarkColorScheme,
            typography = forgeTypography(isPersianPrimary = isPersianPrimary),
            shapes = HexForgeShapes,
            content = content,
        )
    }
}
