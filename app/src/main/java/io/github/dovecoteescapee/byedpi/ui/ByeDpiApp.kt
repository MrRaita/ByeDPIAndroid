package io.github.dovecoteescapee.byedpi.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.data.Mode
import kotlinx.coroutines.delay

private enum class Screen(val depth: Int) {
    Home(0),
    Settings(1),
    UiSettings(2),
    CmdSettings(2),
}

@Composable
fun ByeDpiApp(
    status: AppStatus,
    runningMode: Mode,
    pending: Boolean,
    onToggle: () -> Unit,
    onSaveLogs: () -> Unit,
    onPendingTimeout: () -> Unit,
) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    // Safety net: never leave the loading indicator up forever if a broadcast is missed.
    LaunchedEffect(pending) {
        if (pending) {
            delay(10_000)
            onPendingTimeout()
        }
    }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (screen) {
            Screen.UiSettings, Screen.CmdSettings -> Screen.Settings
            else -> Screen.Home
        }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val sign = if (targetState.depth >= initialState.depth) 1 else -1
            (slideInHorizontally(animationSpec = tween(320)) { fullWidth -> sign * fullWidth / 6 } +
                fadeIn(tween(240, delayMillis = 60))) togetherWith
                (slideOutHorizontally(animationSpec = tween(320)) { fullWidth -> -sign * fullWidth / 6 } +
                    fadeOut(tween(140)))
        },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.Home -> HomeScreen(
                status = status,
                runningMode = runningMode,
                pending = pending,
                onToggle = onToggle,
                onSaveLogs = onSaveLogs,
                onOpenSettings = {
                    if (status == AppStatus.Halted) {
                        screen = Screen.Settings
                    } else {
                        Toast.makeText(context, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
                    }
                },
            )

            Screen.Settings -> SettingsScreen(
                onBack = { screen = Screen.Home },
                onOpenUiSettings = { screen = Screen.UiSettings },
                onOpenCmdSettings = { screen = Screen.CmdSettings },
            )

            Screen.UiSettings -> UiSettingsScreen(onBack = { screen = Screen.Settings })
            Screen.CmdSettings -> CmdSettingsScreen(onBack = { screen = Screen.Settings })
        }
    }
}
