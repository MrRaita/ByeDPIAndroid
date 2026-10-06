package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.utility.getPreferences

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

    val running = status == AppStatus.Running
    // While running, the mode that was actually started wins; otherwise show the configured one.
    val mode = if (running) {
        runningMode
    } else {
        Mode.fromString(prefs.string("byedpi_mode", "vpn"))
    }

    val statusText = when {
        running && mode == Mode.VPN -> R.string.vpn_connected
        running -> R.string.proxy_up
        mode == Mode.VPN -> R.string.vpn_disconnected
        else -> R.string.proxy_down
    }
    val buttonText = when {
        running && mode == Mode.VPN -> R.string.vpn_disconnect
        running -> R.string.proxy_stop
        mode == Mode.VPN -> R.string.vpn_connect
        else -> R.string.proxy_start
    }

    val colors = MaterialTheme.colorScheme
    val cardColor by animateColorAsState(
        targetValue = if (running) colors.primaryContainer else colors.surfaceContainerHigh,
        animationSpec = expressiveEffects(),
        label = "card-color",
    )
    val cardContent by animateColorAsState(
        targetValue = if (running) colors.onPrimaryContainer else colors.onSurface,
        animationSpec = expressiveEffects(),
        label = "card-content",
    )
    // The card morphs its corners when the state flips.
    val corner: Dp by animateDpAsState(
        targetValue = if (running) 56.dp else 32.dp,
        animationSpec = expressiveSpatial(),
        label = "card-corner",
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
            Surface(
                shape = RoundedCornerShape(corner.coerceAtLeast(0.dp)),
                color = cardColor,
                contentColor = cardContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(
                            if (mode == Mode.VPN) R.string.mode_vpn else R.string.mode_proxy
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(statusText),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.proxy_address,
                            prefs.string("byedpi_proxy_ip", "127.0.0.1"),
                            prefs.string("byedpi_proxy_port", "1080"),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            ConnectionHero(
                running = running,
                pending = pending,
                label = stringResource(buttonText),
                onClick = onToggle,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
