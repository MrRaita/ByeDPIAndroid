package io.github.dovecoteescapee.byedpi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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

private enum class Tab(val icon: ImageVector) {
    Connection(Icons.Filled.Lock),
    Configuration(Icons.Filled.Build),
    Settings(Icons.Filled.Settings),
}

private enum class Dest(val order: Int, val tab: Tab, val topLevel: Boolean) {
    Connection(0, Tab.Connection, true),
    Configuration(1, Tab.Configuration, true),
    Settings(2, Tab.Settings, true),
    UiEditor(3, Tab.Configuration, false),
    CmdEditor(3, Tab.Configuration, false),
}

private fun Tab.destination(): Dest = when (this) {
    Tab.Connection -> Dest.Connection
    Tab.Configuration -> Dest.Configuration
    Tab.Settings -> Dest.Settings
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
    var dest by rememberSaveable { mutableStateOf(Dest.Connection) }
    val running = status == AppStatus.Running

    // Safety net: never leave the loading ring up forever if a broadcast is missed.
    LaunchedEffect(pending) {
        if (pending) {
            delay(10_000)
            onPendingTimeout()
        }
    }

    BackHandler(enabled = dest != Dest.Connection) {
        dest = when (dest) {
            Dest.UiEditor, Dest.CmdEditor -> Dest.Configuration
            else -> Dest.Connection
        }
    }

    // Springy, expressive screen transitions (specs come from the theme's motion scheme).
    val spatial = expressiveSpatial<IntOffset>()
    val effects = expressiveEffects<Float>()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AnimatedVisibility(
                visible = dest.topLevel,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = dest.tab == tab,
                            onClick = { dest = tab.destination() },
                            icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                            label = {
                                Text(
                                    stringResource(
                                        when (tab) {
                                            Tab.Connection -> R.string.tab_connection
                                            Tab.Configuration -> R.string.tab_configuration
                                            Tab.Settings -> R.string.title_settings
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(modifier = Modifier.padding(inner)) {
            AnimatedContent(
                targetState = dest,
                transitionSpec = {
                    val sign = if (targetState.order >= initialState.order) 1 else -1
                    (slideInHorizontally(spatial) { fullWidth -> sign * fullWidth / 5 } + fadeIn(effects)) togetherWith
                        (slideOutHorizontally(spatial) { fullWidth -> -sign * fullWidth / 5 } + fadeOut(effects))
                },
                label = "destination",
            ) { target ->
                when (target) {
                    Dest.Connection -> ConnectionScreen(
                        status = status,
                        runningMode = runningMode,
                        pending = pending,
                        onToggle = onToggle,
                        onSaveLogs = onSaveLogs,
                    )

                    Dest.Configuration -> ConfigurationScreen(
                        running = running,
                        onOpenUiSettings = { dest = Dest.UiEditor },
                        onOpenCmdSettings = { dest = Dest.CmdEditor },
                    )

                    Dest.Settings -> SettingsScreen(running = running)

                    Dest.UiEditor -> UiSettingsScreen(
                        running = running,
                        onBack = { dest = Dest.Configuration },
                    )

                    Dest.CmdEditor -> CmdSettingsScreen(
                        running = running,
                        onBack = { dest = Dest.Configuration },
                    )
                }
            }
        }
    }
}
