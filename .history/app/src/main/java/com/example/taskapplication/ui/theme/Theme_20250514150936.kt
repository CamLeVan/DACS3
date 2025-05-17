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

// Lược đồ màu mở rộng cho gradients và các màu tùy chỉnh
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
    val textSecondary: Color,
    // Thêm các màu mới
    val cardBorderGradient: Brush,
    val accentGradient: Brush,
    val surfaceElevated: Color,
    val surfaceElevatedDark: Color,
    // Màu sắc cho các cột Kanban
    val todoColumn: Color,
    val inProgressColumn: Color,
    val doneColumn: Color,
    // Màu sắc cho các thẻ nhiệm vụ
    val taskCardGradient: Brush,
    val taskCardBorder: Color
)

// Tạo composition local cho lược đồ màu mở rộng
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
        textSecondary = TextSecondary,
        // Giá trị mặc định cho các màu mới
        cardBorderGradient = Brush.horizontalGradient(listOf(Primary.copy(alpha = 0.1f), Secondary.copy(alpha = 0.1f))),
        accentGradient = Brush.horizontalGradient(listOf(AccentBlue, AccentGreen)),
        surfaceElevated = Color.White,
        surfaceElevatedDark = Color(0xFF2D2D2D)
    )
}

// Lược đồ màu tối được cập nhật
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onError = Color.White,
    primaryContainer = PrimaryDark.copy(alpha = 0.15f),
    secondaryContainer = SecondaryDark.copy(alpha = 0.15f),
    tertiaryContainer = TertiaryDark.copy(alpha = 0.15f),
    surfaceVariant = SurfaceDark.copy(alpha = 0.8f),
    onSurfaceVariant = TextSecondaryDark
)

// Lược đồ màu sáng được cập nhật
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
    primaryContainer = Primary.copy(alpha = 0.08f),
    secondaryContainer = Secondary.copy(alpha = 0.08f),
    tertiaryContainer = Tertiary.copy(alpha = 0.08f),
    surfaceVariant = Surface.copy(alpha = 0.8f),
    onSurfaceVariant = TextSecondary
)

// Lược đồ màu mở rộng cho chế độ tối
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
    textSecondary = TextSecondaryDark,
    // Các màu mới cho chế độ tối
    cardBorderGradient = Brush.horizontalGradient(listOf(PrimaryDark.copy(alpha = 0.2f), SecondaryDark.copy(alpha = 0.2f))),
    accentGradient = Brush.horizontalGradient(listOf(AccentBlue.copy(alpha = 0.8f), AccentGreen.copy(alpha = 0.8f))),
    surfaceElevated = Color(0xFF2D2D2D),
    surfaceElevatedDark = Color(0xFF3D3D3D)
)

// Lược đồ màu mở rộng cho chế độ sáng
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
    textSecondary = TextSecondary,
    // Các màu mới cho chế độ sáng
    cardBorderGradient = Brush.horizontalGradient(listOf(Primary.copy(alpha = 0.1f), Secondary.copy(alpha = 0.1f))),
    accentGradient = Brush.horizontalGradient(listOf(AccentBlue, AccentGreen)),
    surfaceElevated = Color(0xFFFAFAFA),
    surfaceElevatedDark = Color(0xFF2D2D2D)
)

@Composable
fun TaskApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color có sẵn trên Android 12+
    dynamicColor: Boolean = false, // Đặt thành false để sử dụng màu tùy chỉnh
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

    // Áp dụng màu thanh trạng thái và giao diện
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Sử dụng màu gradient cho thanh trạng thái
            window.statusBarColor = if (darkTheme) BackgroundDark.toArgb() else Background.toArgb()
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