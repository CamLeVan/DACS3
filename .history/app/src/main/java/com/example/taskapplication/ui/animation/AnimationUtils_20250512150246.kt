package com.example.taskapplication.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Các tiện ích hoạt ảnh cho ứng dụng
 */
object AnimationUtils {

    /**
     * Thời lượng hoạt ảnh mặc định
     */
    const val DEFAULT_ANIMATION_DURATION = 300

    /**
     * Thời lượng hoạt ảnh mặc định cho hộp thoại
     */
    const val DIALOG_ANIMATION_DURATION = 200

    /**
     * Thời lượng hoạt ảnh mặc định cho các mục danh sách
     */
    const val LIST_ITEM_ANIMATION_DURATION = 150

    /**
     * Thời lượng hoạt ảnh mặc định cho các tab
     */
    const val TAB_ANIMATION_DURATION = 250

    /**
     * Thông số hoạt ảnh mặc định cho hoạt ảnh kiểu lò xo
     */
    val springSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    /**
     * Thông số hoạt ảnh mặc định cho hoạt ảnh kiểu tween
     */
    val tweenSpec = tween<IntOffset>(
        durationMillis = DEFAULT_ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )

    /**
     * Hoạt ảnh hiện dần
     */
    val fadeInAnimation = fadeIn(
        animationSpec = tween(
            durationMillis = DEFAULT_ANIMATION_DURATION,
            easing = LinearOutSlowInEasing
        )
    )

    /**
     * Fade out animation
     */
    val fadeOutAnimation = fadeOut(
        animationSpec = tween(
            durationMillis = DEFAULT_ANIMATION_DURATION,
            easing = FastOutLinearInEasing
        )
    )

    /**
     * Scale in animation
     */
    val scaleInAnimation = scaleIn(
        animationSpec = tween(
            durationMillis = DEFAULT_ANIMATION_DURATION,
            easing = LinearOutSlowInEasing
        ),
        initialScale = 0.8f
    )

    /**
     * Scale out animation
     */
    val scaleOutAnimation = scaleOut(
        animationSpec = tween(
            durationMillis = DEFAULT_ANIMATION_DURATION,
            easing = FastOutLinearInEasing
        ),
        targetScale = 0.8f
    )

    /**
     * Dialog enter animation
     */
    val dialogEnterAnimation: EnterTransition = fadeIn(
        animationSpec = tween(DIALOG_ANIMATION_DURATION)
    ) + scaleIn(
        animationSpec = tween(DIALOG_ANIMATION_DURATION),
        initialScale = 0.9f
    )

    /**
     * Dialog exit animation
     */
    val dialogExitAnimation: ExitTransition = fadeOut(
        animationSpec = tween(DIALOG_ANIMATION_DURATION)
    ) + scaleOut(
        animationSpec = tween(DIALOG_ANIMATION_DURATION),
        targetScale = 0.9f
    )

    /**
     * Tab content transition - slide horizontally
     */
    fun tabContentTransition(
        fromState: Int,
        toState: Int
    ): ContentTransform {
        return if (fromState < toState) {
            // Sliding from right
            slideInHorizontally(
                animationSpec = tween(TAB_ANIMATION_DURATION),
                initialOffsetX = { it }
            ) + fadeIn(
                animationSpec = tween(TAB_ANIMATION_DURATION)
            ) togetherWith slideOutHorizontally(
                animationSpec = tween(TAB_ANIMATION_DURATION),
                targetOffsetX = { -it }
            ) + fadeOut(
                animationSpec = tween(TAB_ANIMATION_DURATION)
            )
        } else {
            // Sliding from left
            slideInHorizontally(
                animationSpec = tween(TAB_ANIMATION_DURATION),
                initialOffsetX = { -it }
            ) + fadeIn(
                animationSpec = tween(TAB_ANIMATION_DURATION)
            ) togetherWith slideOutHorizontally(
                animationSpec = tween(TAB_ANIMATION_DURATION),
                targetOffsetX = { it }
            ) + fadeOut(
                animationSpec = tween(TAB_ANIMATION_DURATION)
            )
        }
    }

    /**
     * List item enter animation
     */
    fun listItemEnterAnimation(index: Int): EnterTransition {
        val delay = index * 50L
        return fadeIn(
            animationSpec = tween(
                durationMillis = LIST_ITEM_ANIMATION_DURATION,
                delayMillis = delay.toInt()
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = LIST_ITEM_ANIMATION_DURATION,
                delayMillis = delay.toInt(),
                easing = LinearOutSlowInEasing
            ),
            initialOffsetY = { it / 5 }
        )
    }

    /**
     * List item exit animation
     */
    val listItemExitAnimation: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = LIST_ITEM_ANIMATION_DURATION / 2
        )
    )
}
