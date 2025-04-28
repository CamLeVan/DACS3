package com.example.taskapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom color scheme for gradients and other custom colors
data class ExtendedColorScheme(
    val primaryGradient: Brush,
    val cardGradient: Brush,
    val buttonGradient: Brush,
    val taskHighPriority: Color,
    val taskMediumPriority: Color,
    val taskLowPriority: Color,
    val taskCompleted: Color,
    val taskInProgress: Color,
    val taskPending: Color,
    val taskOverdue: Color,
    val divider: Color,
    val textSecondary: Color
)

// Create a composition local for our extended color scheme
val LocalExtendedColorScheme = staticCompositionLocalOf {
    ExtendedColorScheme(
        primaryGradient = Brush.horizontalGradient(listOf(Primary, Secondary)),
        cardGradient = Brush.verticalGradient(listOf(CardGradientStart, CardGradientEnd)),
        buttonGradient = Brush.horizontalGradient(listOf(ButtonGradientStart, ButtonGradientEnd)),
        taskHighPriority = HighPriority,
        taskMediumPriority = MediumPriority,
        taskLowPriority = LowPriority,
        taskCompleted = Completed,
        taskInProgress = InProgress,
        taskPending = Pending,
        taskOverdue = Overdue,
        divider = Divider,
        textSecondary = TextSecondary
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onPrimary = TextPrimaryDark,
    onSecondary = TextPrimaryDark,
    onTertiary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onError = TextPrimaryDark,
    primaryContainer = PrimaryDark.copy(alpha = 0.7f),
    secondaryContainer = SecondaryDark.copy(alpha = 0.7f),
    tertiaryContainer = TertiaryDark.copy(alpha = 0.7f),
    surfaceVariant = SurfaceDark.copy(alpha = 0.7f),
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    error = Error,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White,
    primaryContainer = Primary.copy(alpha = 0.1f),
    secondaryContainer = Secondary.copy(alpha = 0.1f),
    tertiaryContainer = Tertiary.copy(alpha = 0.1f),
    surfaceVariant = Surface.copy(alpha = 0.7f),
    onSurfaceVariant = TextSecondary
)

private val DarkExtendedColorScheme = ExtendedColorScheme(
    primaryGradient = Brush.horizontalGradient(listOf(PrimaryDark, SecondaryDark)),
    cardGradient = Brush.verticalGradient(listOf(CardGradientStartDark, CardGradientEndDark)),
    buttonGradient = Brush.horizontalGradient(listOf(ButtonGradientStartDark, ButtonGradientEndDark)),
    taskHighPriority = HighPriority,
    taskMediumPriority = MediumPriority,
    taskLowPriority = LowPriority,
    taskCompleted = Completed,
    taskInProgress = InProgress,
    taskPending = Pending,
    taskOverdue = Overdue,
    divider = DividerDark,
    textSecondary = TextSecondaryDark
)

private val LightExtendedColorScheme = ExtendedColorScheme(
    primaryGradient = Brush.horizontalGradient(listOf(Primary, Secondary)),
    cardGradient = Brush.verticalGradient(listOf(CardGradientStart, CardGradientEnd)),
    buttonGradient = Brush.horizontalGradient(listOf(ButtonGradientStart, ButtonGradientEnd)),
    taskHighPriority = HighPriority,
    taskMediumPriority = MediumPriority,
    taskLowPriority = LowPriority,
    taskCompleted = Completed,
    taskInProgress = InProgress,
    taskPending = Pending,
    taskOverdue = Overdue,
    divider = Divider,
    textSecondary = TextSecondary
)

@Composable
fun TaskApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColorScheme = if (darkTheme) DarkExtendedColorScheme else LightExtendedColorScheme

    // Apply status bar color and appearance
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use a gradient color for status bar
            window.statusBarColor = if (darkTheme) PrimaryDark.toArgb() else Primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalExtendedColorScheme provides extendedColorScheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}