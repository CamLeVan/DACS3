package com.example.taskapplication.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme

/**
 * Card với nền gradient hiện đại và hiệu ứng hover
 * @param modifier Modifier cho card
 * @param onClick Callback khi nhấn vào card (null nếu không muốn card có thể nhấn)
 * @param cornerRadius Bo góc của card
 * @param elevation Độ nổi của card
 * @param gradient Gradient cho nền card (mặc định lấy từ theme)
 * @param contentPadding Padding cho nội dung bên trong card
 * @param content Nội dung của card
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    gradient: Brush? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val extendedColorScheme = LocalExtendedColorScheme.current
    val cardGradient = gradient ?: extendedColorScheme.cardGradient

    // Thêm hiệu ứng hover
    var isHovered by remember { mutableStateOf(false) }
    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered && onClick != null) elevation + 2.dp else elevation,
        animationSpec = tween(durationMillis = 150),
        label = "Card Elevation Animation"
    )

    val baseModifier = modifier
        .shadow(
            elevation = animatedElevation,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(cardGradient)
        .fillMaxWidth()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isHovered = when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move -> true
                        PointerEventType.Exit -> false
                        else -> isHovered
                    }
                }
            }
        }

    val cardModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Box(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Card với viền gradient hiện đại và hiệu ứng hover
 * @param modifier Modifier cho card
 * @param onClick Callback khi nhấn vào card (null nếu không muốn card có thể nhấn)
 * @param cornerRadius Bo góc của card
 * @param elevation Độ nổi của card
 * @param borderWidth Độ dày của viền
 * @param gradient Gradient cho viền card (mặc định lấy từ theme)
 * @param backgroundColor Màu nền của card
 * @param contentPadding Padding cho nội dung bên trong card
 * @param content Nội dung của card
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    gradient: Brush? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val extendedColorScheme = LocalExtendedColorScheme.current
    val borderGradient = gradient ?: extendedColorScheme.cardBorderGradient

    // Thêm hiệu ứng hover
    var isHovered by remember { mutableStateOf(false) }
    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered && onClick != null) elevation + 2.dp else elevation,
        animationSpec = tween(durationMillis = 150),
        label = "Card Elevation Animation"
    )

    val baseModifier = modifier
        .shadow(
            elevation = animatedElevation,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(
            width = borderWidth,
            brush = borderGradient,
            shape = RoundedCornerShape(cornerRadius)
        )
        .fillMaxWidth()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isHovered = when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move -> true
                        PointerEventType.Exit -> false
                        else -> isHovered
                    }
                }
            }
        }

    val cardModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Box(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Card với nền màu đơn sắc và hiệu ứng hover
 * @param modifier Modifier cho card
 * @param onClick Callback khi nhấn vào card (null nếu không muốn card có thể nhấn)
 * @param cornerRadius Bo góc của card
 * @param elevation Độ nổi của card
 * @param backgroundColor Màu nền của card
 * @param contentPadding Padding cho nội dung bên trong card
 * @param content Nội dung của card
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 2.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Thêm hiệu ứng hover
    var isHovered by remember { mutableStateOf(false) }
    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered && onClick != null) elevation + 2.dp else elevation,
        animationSpec = tween(durationMillis = 150),
        label = "Card Elevation Animation"
    )

    val baseModifier = modifier
        .shadow(
            elevation = animatedElevation,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .fillMaxWidth()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isHovered = when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move -> true
                        PointerEventType.Exit -> false
                        else -> isHovered
                    }
                }
            }
        }

    val cardModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Box(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}
