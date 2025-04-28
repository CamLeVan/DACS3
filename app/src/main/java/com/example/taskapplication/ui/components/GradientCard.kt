package com.example.taskapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme

/**
 * Card với nền gradient hiện đại
 * @param modifier Modifier cho card
 * @param onClick Callback khi nhấn vào card (null nếu không muốn card có thể nhấn)
 * @param cornerRadius Bo góc của card
 * @param elevation Độ nổi của card
 * @param gradient Gradient cho nền card (mặc định lấy từ theme)
 * @param content Nội dung của card
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    gradient: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val extendedColorScheme = LocalExtendedColorScheme.current
    val cardGradient = gradient ?: extendedColorScheme.cardGradient
    
    val baseModifier = modifier
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(cardGradient)
        .fillMaxWidth()
    
    val cardModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }
    
    Box(
        modifier = cardModifier.padding(16.dp),
        content = content
    )
}

/**
 * Card với viền gradient hiện đại
 * @param modifier Modifier cho card
 * @param onClick Callback khi nhấn vào card (null nếu không muốn card có thể nhấn)
 * @param cornerRadius Bo góc của card
 * @param elevation Độ nổi của card
 * @param borderWidth Độ dày của viền
 * @param gradient Gradient cho viền card (mặc định lấy từ theme)
 * @param backgroundColor Màu nền của card
 * @param content Nội dung của card
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    borderWidth: Dp = 2.dp,
    gradient: Brush? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable BoxScope.() -> Unit
) {
    val extendedColorScheme = LocalExtendedColorScheme.current
    val borderGradient = gradient ?: extendedColorScheme.primaryGradient
    
    val baseModifier = modifier
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(borderGradient)
        .padding(borderWidth)
        .clip(RoundedCornerShape(cornerRadius - borderWidth))
        .background(backgroundColor)
        .fillMaxWidth()
    
    val cardModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }
    
    Box(
        modifier = cardModifier.padding(16.dp),
        content = content
    )
}
