package com.renamdroid.batchrename.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.renamdroid.batchrename.data.ThemeColor
import com.renamdroid.batchrename.data.ThemeMode

// ---------- 五套配色方案（浅色） ----------

private val IndigoLight = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = IndigoOnPrimaryContainer,
    secondary = IndigoSecondary,
    onSecondary = IndigoOnSecondary,
    secondaryContainer = IndigoSecondaryContainer,
    onSecondaryContainer = IndigoOnSecondaryContainer,
    tertiary = IndigoTertiary,
    onTertiary = IndigoOnTertiary,
    tertiaryContainer = IndigoTertiaryContainer,
    onTertiaryContainer = IndigoOnTertiaryContainer,
    error = ColorError,
    onError = ColorOnError,
    errorContainer = ColorErrorContainer,
    onErrorContainer = ColorOnErrorContainer,
    background = NeutralBackground,
    onBackground = NeutralOnBackground,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
)

private val GreenLight = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = GreenTertiary,
    onTertiary = GreenOnTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,
    error = ColorError,
    onError = ColorOnError,
    errorContainer = ColorErrorContainer,
    onErrorContainer = ColorOnErrorContainer,
    background = NeutralBackground,
    onBackground = NeutralOnBackground,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
)

private val PurpleLight = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    secondary = PurpleSecondary,
    onSecondary = PurpleOnSecondary,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    tertiary = PurpleTertiary,
    onTertiary = PurpleOnTertiary,
    tertiaryContainer = PurpleTertiaryContainer,
    onTertiaryContainer = PurpleOnTertiaryContainer,
    error = ColorError,
    onError = ColorOnError,
    errorContainer = ColorErrorContainer,
    onErrorContainer = ColorOnErrorContainer,
    background = NeutralBackground,
    onBackground = NeutralOnBackground,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
)

private val OrangeLight = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = OrangeOnSecondary,
    secondaryContainer = OrangeSecondaryContainer,
    onSecondaryContainer = OrangeOnSecondaryContainer,
    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    tertiaryContainer = OrangeTertiaryContainer,
    onTertiaryContainer = OrangeOnTertiaryContainer,
    error = ColorError,
    onError = ColorOnError,
    errorContainer = ColorErrorContainer,
    onErrorContainer = ColorOnErrorContainer,
    background = NeutralBackground,
    onBackground = NeutralOnBackground,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
)

private val RoseLight = lightColorScheme(
    primary = RosePrimary,
    onPrimary = RoseOnPrimary,
    primaryContainer = RosePrimaryContainer,
    onPrimaryContainer = RoseOnPrimaryContainer,
    secondary = RoseSecondary,
    onSecondary = RoseOnSecondary,
    secondaryContainer = RoseSecondaryContainer,
    onSecondaryContainer = RoseOnSecondaryContainer,
    tertiary = RoseTertiary,
    onTertiary = RoseOnTertiary,
    tertiaryContainer = RoseTertiaryContainer,
    onTertiaryContainer = RoseOnTertiaryContainer,
    error = ColorError,
    onError = ColorOnError,
    errorContainer = ColorErrorContainer,
    onErrorContainer = ColorOnErrorContainer,
    background = NeutralBackground,
    onBackground = NeutralOnBackground,
    surface = NeutralSurface,
    onSurface = NeutralOnSurface,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
)

// ---------- 五套配色方案（深色） ----------

private val IndigoDark = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = IndigoOnPrimaryContainerDark,
    secondary = IndigoSecondaryDark,
    onSecondary = IndigoOnSecondaryDark,
    secondaryContainer = IndigoSecondaryContainerDark,
    onSecondaryContainer = IndigoOnSecondaryContainerDark,
    tertiary = IndigoTertiaryDark,
    onTertiary = IndigoOnTertiaryDark,
    tertiaryContainer = IndigoTertiaryContainerDark,
    onTertiaryContainer = IndigoOnTertiaryContainerDark,
    error = ColorErrorDark,
    onError = ColorOnErrorDark,
    errorContainer = ColorErrorContainerDark,
    onErrorContainer = ColorOnErrorContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
)

private val GreenDark = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    onSecondary = GreenOnSecondaryDark,
    secondaryContainer = GreenSecondaryContainerDark,
    onSecondaryContainer = GreenOnSecondaryContainerDark,
    tertiary = GreenTertiaryDark,
    onTertiary = GreenOnTertiaryDark,
    tertiaryContainer = GreenTertiaryContainerDark,
    onTertiaryContainer = GreenOnTertiaryContainerDark,
    error = ColorErrorDark,
    onError = ColorOnErrorDark,
    errorContainer = ColorErrorContainerDark,
    onErrorContainer = ColorOnErrorContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
)

private val PurpleDark = darkColorScheme(
    primary = PurplePrimaryDark,
    onPrimary = PurpleOnPrimaryDark,
    primaryContainer = PurplePrimaryContainerDark,
    onPrimaryContainer = PurpleOnPrimaryContainerDark,
    secondary = PurpleSecondaryDark,
    onSecondary = PurpleOnSecondaryDark,
    secondaryContainer = PurpleSecondaryContainerDark,
    onSecondaryContainer = PurpleOnSecondaryContainerDark,
    tertiary = PurpleTertiaryDark,
    onTertiary = PurpleOnTertiaryDark,
    tertiaryContainer = PurpleTertiaryContainerDark,
    onTertiaryContainer = PurpleOnTertiaryContainerDark,
    error = ColorErrorDark,
    onError = ColorOnErrorDark,
    errorContainer = ColorErrorContainerDark,
    onErrorContainer = ColorOnErrorContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
)

private val OrangeDark = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = OrangeOnPrimaryDark,
    primaryContainer = OrangePrimaryContainerDark,
    onPrimaryContainer = OrangeOnPrimaryContainerDark,
    secondary = OrangeSecondaryDark,
    onSecondary = OrangeOnSecondaryDark,
    secondaryContainer = OrangeSecondaryContainerDark,
    onSecondaryContainer = OrangeOnSecondaryContainerDark,
    tertiary = OrangeTertiaryDark,
    onTertiary = OrangeOnTertiaryDark,
    tertiaryContainer = OrangeTertiaryContainerDark,
    onTertiaryContainer = OrangeOnTertiaryContainerDark,
    error = ColorErrorDark,
    onError = ColorOnErrorDark,
    errorContainer = ColorErrorContainerDark,
    onErrorContainer = ColorOnErrorContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
)

private val RoseDark = darkColorScheme(
    primary = RosePrimaryDark,
    onPrimary = RoseOnPrimaryDark,
    primaryContainer = RosePrimaryContainerDark,
    onPrimaryContainer = RoseOnPrimaryContainerDark,
    secondary = RoseSecondaryDark,
    onSecondary = RoseOnSecondaryDark,
    secondaryContainer = RoseSecondaryContainerDark,
    onSecondaryContainer = RoseOnSecondaryContainerDark,
    tertiary = RoseTertiaryDark,
    onTertiary = RoseOnTertiaryDark,
    tertiaryContainer = RoseTertiaryContainerDark,
    onTertiaryContainer = RoseOnTertiaryContainerDark,
    error = ColorErrorDark,
    onError = ColorOnErrorDark,
    errorContainer = ColorErrorContainerDark,
    onErrorContainer = ColorOnErrorContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
)

@Composable
fun RenamdriodTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColor: ThemeColor = ThemeColor.DYNAMIC,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // 动态取色：Android 12+ 跟随壁纸；低版本自动回退到默认的靛蓝
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = if (themeColor == ThemeColor.DYNAMIC && dynamicAvailable) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val fallbackDark = when (themeColor) {
            ThemeColor.DYNAMIC, ThemeColor.INDIGO -> if (darkTheme) IndigoDark else IndigoLight
            ThemeColor.GREEN -> if (darkTheme) GreenDark else GreenLight
            ThemeColor.PURPLE -> if (darkTheme) PurpleDark else PurpleLight
            ThemeColor.ORANGE -> if (darkTheme) OrangeDark else OrangeLight
            ThemeColor.ROSE -> if (darkTheme) RoseDark else RoseLight
        }
        fallbackDark
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
