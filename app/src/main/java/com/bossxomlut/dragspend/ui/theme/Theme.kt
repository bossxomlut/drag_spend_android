package com.bossxomlut.dragspend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = White,
    primaryContainer = BrandBluePale,
    onPrimaryContainer = BrandBlueDeep,
    secondary = Slate600,
    onSecondary = White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = IncomeGreen,
    onTertiary = White,
    tertiaryContainer = IncomeGreenLight,
    onTertiaryContainer = IncomeGreenDark,
    error = ExpenseRed,
    onError = White,
    errorContainer = ExpenseRedLight,
    onErrorContainer = ExpenseRedDark,
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate400,
    outlineVariant = Slate100,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueLighter,
    onPrimary = BrandBlueDeep,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandBluePale,
    secondary = Slate400,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = IncomeGreenLighter,
    onTertiary = IncomeGreenDark,
    tertiaryContainer = IncomeGreenDark,
    onTertiaryContainer = IncomeGreenLight,
    error = ExpenseRedLighter,
    onError = ExpenseRedDark,
    errorContainer = ExpenseRedDark,
    onErrorContainer = ExpenseRedLight,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate600,
    outlineVariant = Slate700,
)

@Composable
fun DragSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
