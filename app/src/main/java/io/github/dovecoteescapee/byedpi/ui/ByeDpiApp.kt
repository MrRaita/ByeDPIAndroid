package io.github.dovecoteescapee.byedpi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.data.Mode
import kotlinx.coroutines.delay

private enum class Tab(val icon: ImageVector, val label: Int) {
    Connection(Icons.Filled.Lock, R.string.tab_connection),
    Configuration(Icons.Filled.Build, R.string.tab_configuration),
    Settings(Icons.Filled.Settings, R.string.title_settings),
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
    var tab by rememberSaveable { mutableStateOf(Tab.Connection) }
    val running = status == AppStatus.Running
    val reduce = LocalReduceMotion.current

    // Safety net: never leave the busy ring up forever if a broadcast is missed.
    LaunchedEffect(pending) {
        if (pending) {
            delay(10_000)
            onPendingTimeout()
        }
    }

    BackHandler(enabled = tab != Tab.Connection) { tab = Tab.Connection }

    // Springy, expressive tab transitions (specs come from the theme's motion scheme).
    val spatial = expressiveSpatial<IntOffset>()
    val effects = expressiveEffects<Float>()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(imageVector = entry.icon, contentDescription = null) },
                        label = { Text(stringResource(entry.label)) },
                    )
                }
            }
        },
    ) { inner ->
        Box(modifier = Modifier.padding(inner)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(effects) togetherWith fadeOut(effects)
                    } else {
                        val sign = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        (slideInHorizontally(spatial) { fullWidth -> sign * fullWidth / 5 } + fadeIn(effects)) togetherWith
                            (slideOutHorizontally(spatial) { fullWidth -> -sign * fullWidth / 5 } + fadeOut(effects))
                    }
                },
                label = "tab",
            ) { target ->
                when (target) {
                    Tab.Connection -> ConnectionScreen(
                        status = status,
                        runningMode = runningMode,
                        pending = pending,
                        onToggle = onToggle,
                        onSaveLogs = onSaveLogs,
                    )

                    Tab.Configuration -> ConfigurationScreen(running = running)
                    Tab.Settings -> SettingsScreen(running = running)
                }
            }
        }
    }
}
