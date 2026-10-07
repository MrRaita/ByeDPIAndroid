@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * ==========================================================================================
 *  THE ONLY FILE THAT TOUCHES Material 3 Expressive (alpha) APIs.
 *
 *  Everything else in the app uses plain, long-stable Material 3 / Compose APIs. If a future
 *  alpha renames something, or you want to fall back to stable Material3 1.4.0, only this file
 *  changes. The stable fallback for each wrapper is noted in its comment.
 * ==========================================================================================
 */

/**
 * Expressive theme: expressive motion scheme (springy, shape-morphing components).
 * Fallback: `MaterialTheme(colorScheme = colorScheme, content = content)`.
 */
@Composable
fun ExpressiveTheme(colorScheme: ColorScheme, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

// ---- Motion specs from the theme's motion scheme (springs when the expressive scheme is active) ----

/** Spring for movement / size / shape changes. Fallback: `spring()`. */
@Composable
fun <T> expressiveSpatial(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

/** Faster spring, used for press feedback. Fallback: `spring(stiffness = Spring.StiffnessMediumLow)`. */
@Composable
fun <T> expressiveFastSpatial(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastSpatialSpec()

/** Non-bouncy spec for colors / alpha. Fallback: `tween(300)`. */
@Composable
fun <T> expressiveEffects(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()

/**
 * Circular connect/disconnect button. The shape morphs from a circle to a rounded square while pressed.
 * [subtitle] is a small second line (the proxy address).
 * Fallback: drop the `shapes = ...` argument and add `shape = CircleShape`.
 */
@Composable
fun ExpressiveCircleButton(
    text: String,
    subtitle: String?,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 168.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(
            shape = CircleShape,
            pressedShape = RoundedCornerShape(size * 0.3f),
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.size(size),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = if (text.length > 11) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Wavy circular ring. With [progress] it fills clockwise from the top (determinate); without it, it runs
 * around endlessly (indeterminate).
 * Fallback: `CircularProgressIndicator` with the same arguments.
 */
@Composable
fun ExpressiveWavyRing(
    color: Color,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    if (progress == null) {
        CircularWavyProgressIndicator(modifier = modifier, color = color)
    } else {
        CircularWavyProgressIndicator(progress = progress, modifier = modifier, color = color)
    }
}

/**
 * Large top bar with the expressive (flexible) layout.
 * Fallback: `LargeTopAppBar(title = { Text(title) }, ...)` with the same arguments.
 */
@Composable
fun ExpressiveLargeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeFlexibleTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

/** Collapses the large top bar while the content scrolls. */
@Composable
fun rememberExpressiveTopBarBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
