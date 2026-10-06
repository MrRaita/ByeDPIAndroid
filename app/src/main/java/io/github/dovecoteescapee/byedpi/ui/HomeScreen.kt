package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.utility.getPreferences

@Composable
fun HomeScreen(
    status: AppStatus,
    runningMode: Mode,
    pending: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit,
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
    val cardColor by animateColorAsState(if (running) colors.primaryContainer else colors.surfaceContainerHigh)
    val cardContent by animateColorAsState(if (running) colors.onPrimaryContainer else colors.onSurface)

    Scaffold(
        topBar = {
            ExpressiveLargeTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = cardColor,
                contentColor = cardContent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(
                            if (mode == Mode.VPN) R.string.mode_vpn else R.string.mode_proxy
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(statusText),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier.height(88.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (pending) {
                    ExpressiveLoading(Modifier.size(64.dp))
                } else {
                    ExpressiveStatusButton(
                        text = stringResource(buttonText),
                        running = running,
                        onClick = onToggle,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            FilledTonalButton(onClick = onSaveLogs) {
                Text(stringResource(R.string.save_logs))
            }
        }
    }
}
