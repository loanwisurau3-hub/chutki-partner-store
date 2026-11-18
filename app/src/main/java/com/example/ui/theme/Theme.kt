package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PartnerLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = Color(0xFF14532D),
    secondary = InfoSky,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = InfoSoft,
    tertiary = SunnyYellow,
    onTertiary = TextMain,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = WarningSoft,
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = DangerSoft,
    background = BgDark,
    onBackground = TextMain,
    surface = BgSurface,
    onSurface = TextMain,
    surfaceVariant = BgInput,
    onSurfaceVariant = TextMuted,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgCardElevated,
    surfaceContainerLowest = BgInput,
    outline = BorderSlate,
    outlineVariant = Color(0xFFE2E8F0)
)

private val PartnerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun GossaigaonStoreTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PartnerLightColorScheme,
        typography = AppTypography,
        shapes = PartnerShapes,
        content = content
    )
}
