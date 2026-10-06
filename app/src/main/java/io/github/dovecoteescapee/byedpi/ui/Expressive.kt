@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/*
 * ==========================================================================================
 *  THE ONLY FILE THAT TOUCHES Material 3 Expressive (alpha) APIs.
 *
 *  Everything else in the app uses plain, long-stable Material 3 APIs. If a future alpha
 *  renames something, or you want to fall back to stable Material3 1.4.0, only this file
 *  changes. Fallback for each wrapper is noted in its comment.
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

/**
 * Big connect / disconnect button whose shape morphs while pressed.
 * Fallback: drop the `shapes = ...` argument (plain pill buttons).
 */
@Composable
fun ExpressiveStatusButton(
    text: String,
    running: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val padding = PaddingValues(horizontal = 40.dp, vertical = 20.dp)
    val buttonModifier = modifier.heightIn(min = 72.dp)
    if (running) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            contentPadding = padding,
            modifier = buttonModifier,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleLarge)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            contentPadding = padding,
            modifier = buttonModifier,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Morphing-polygon loading indicator.
 * Fallback: `CircularProgressIndicator(modifier = modifier)`.
 */
@Composable
fun ExpressiveLoading(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier)
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
