package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import kotlin.random.Random

/** "Connection" tab. */
@Composable
fun ConnectionScreen(
    status: AppStatus,
    runningMode: Mode,
    pending: Boolean,
    onToggle: () -> Unit,
    onSaveLogs: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = rememberPrefState(remember { context.getPreferences() })
    val reduce = LocalReduceMotion.current

    val running = status == AppStatus.Running
    val fx = rememberConnectionFx(running = running, pending = pending, reduce = reduce)
    val shown = fx.shown // what the UI displays; lags behind the real state while an animation plays

    // Remember the mode that was actually started, so the label stays right while disconnecting.
    val lastRunningMode = remember { arrayOf(runningMode) }
    if (running) lastRunningMode[0] = runningMode

    val configuredMode = Mode.fromString(prefs.string("byedpi_mode", "vpn"))
    val displayMode = if (shown) lastRunningMode[0] else configuredMode

    val connectLabel = stringResource(
        if (configuredMode == Mode.VPN) R.string.vpn_connect else R.string.proxy_start
    )
    val disconnectLabel = stringResource(
        if (displayMode == Mode.VPN) R.string.vpn_disconnect else R.string.proxy_stop
    )
    val statusText = stringResource(
        when {
            shown && displayMode == Mode.VPN -> R.string.vpn_connected
            shown -> R.string.proxy_up
            displayMode == Mode.VPN -> R.string.vpn_disconnected
            else -> R.string.proxy_down
        }
    )
    val modeText = stringResource(
        if (displayMode == Mode.VPN) R.string.mode_vpn else R.string.mode_proxy
    )
    val address = stringResource(
        R.string.proxy_address,
        prefs.string("byedpi_proxy_ip", "127.0.0.1"),
        prefs.string("byedpi_proxy_port", "1080"),
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { ExpressiveLargeTopBar(title = stringResource(R.string.app_name)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroNote(
                shown = shown,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
            )

            ConnectionHero(
                fx = fx,
                connectLabel = connectLabel,
                disconnectLabel = disconnectLabel,
                connectingLabel = stringResource(R.string.connecting),
                disconnectingLabel = stringResource(R.string.disconnecting),
                subtitle = address,
                onClick = onToggle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Text(
                text = "$modeText · $statusText",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            FilledTonalButton(
                onClick = onSaveLogs,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(stringResource(R.string.save_logs))
            }
        }
    }
}

/**
 * A short friendly line above the button. A new one is picked on every app launch and whenever the
 * (displayed) state flips, from a different pool for "not connected" and "connected".
 */
@Composable
private fun HeroNote(shown: Boolean, modifier: Modifier = Modifier) {
    val idleNotes = stringArrayResource(R.array.notes_idle)
    val connectedNotes = stringArrayResource(R.array.notes_connected)
    val seed = rememberSaveable(shown) { Random.nextInt(Int.MAX_VALUE) }
    val pool = if (shown) connectedNotes else idleNotes
    val note = pool[seed % pool.size]

    val reduce = LocalReduceMotion.current
    val effects = expressiveEffects<Float>()
    val spatial = expressiveSpatial<androidx.compose.ui.unit.IntOffset>()

    Box(modifier = modifier.heightIn(min = 84.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = note,
            transitionSpec = {
                if (reduce) {
                    fadeIn(effects) togetherWith fadeOut(effects)
                } else {
                    (fadeIn(effects) + slideInVertically(spatial) { it / 3 }) togetherWith
                        (fadeOut(effects) + slideOutVertically(spatial) { -it / 3 })
                }
            },
            label = "hero-note",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
