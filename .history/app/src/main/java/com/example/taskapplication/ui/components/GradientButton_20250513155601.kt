package com.example.taskapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.theme.ButtonGradientEnd
import com.example.taskapplication.ui.theme.ButtonGradientStart
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme

/**
 * Nút gradient hiện đại với hiệu ứng nhấn
 * @param text Văn bản hiển thị trên nút
 * @param onClick Callback khi nhấn nút
 * @param modifier Modifier cho nút
 * @param enabled Trạng thái kích hoạt của nút
 * @param icon Icon hiển thị bên trái văn bản (null nếu không muốn hiển thị)
 * @param cornerRadius Bo góc của nút
 * @param height Chiều cao của nút
 * @param gradient Gradient cho nút (mặc định lấy từ theme)
 * @param showLoading Hiển thị trạng thái đang tải (thay thế icon bằng CircularProgressIndicator)
 * @param content Nội dung tùy chỉnh của nút (mặc định là văn bản)
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    cornerRadius: RoundedCornerShape = RoundedCornerShape(12.dp),
    height: Int = 56,
    gradient: Brush? = null,
    showLoading: Boolean = false,
    content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        }
    }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Hiệu ứng scale khi nhấn
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "button_scale"
    )

    // Sử dụng gradient từ theme
    val extendedColorScheme = LocalExtendedColorScheme.current
    val buttonGradient = when {
        !enabled -> Brush.horizontalGradient(
            listOf(
                Color.Gray.copy(alpha = 0.5f),
                Color.Gray.copy(alpha = 0.3f)
            )
        )
        gradient != null -> gradient
        else -> extendedColorScheme.buttonGradient
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = cornerRadius,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            .clip(cornerRadius)
            .background(buttonGradient)
            .height(height.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = cornerRadius,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            )
        ) {
            content()
        }
    }
}

/**
 * Nút với icon và văn bản
 */
@Composable
fun IconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLoading: Boolean = false
) {
    GradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        showLoading = showLoading
    )
}

/**
 * Nút thêm mới với icon dấu cộng
 */
@Composable
fun AddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLoading: Boolean = false
) {
    IconButton(
        text = text,
        icon = Icons.Default.Add,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        showLoading = showLoading
    )
}

/**
 * Nút nhỏ với gradient
 */
@Composable
fun SmallGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    showLoading: Boolean = false
) {
    GradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        cornerRadius = RoundedCornerShape(8.dp),
        height = 40,
        showLoading = showLoading
    )
}
