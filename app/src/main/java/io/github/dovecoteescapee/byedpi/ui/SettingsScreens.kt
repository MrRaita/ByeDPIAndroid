package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.os.Build
import io.github.dovecoteescapee.byedpi.BuildConfig
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.utility.checkIp
import io.github.dovecoteescapee.byedpi.utility.checkNotLocalIp
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import io.github.dovecoteescapee.byedpi.utility.intInRange

private const val SOURCE_CODE_URL = "https://github.com/dovecoteescapee/ByeDPIAndroid"

@Composable
private fun LockedBanner() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_locked_banner),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * @param onBack null for the top-level tabs (no back arrow)
 * @param locked true while the service runs: rows are shown but cannot be changed
 * @param navInset add the navigation-bar inset (needed on screens without the bottom bar)
 */
@Composable
private fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)?,
    locked: Boolean,
    navInset: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val behavior = rememberExpressiveTopBarBehavior()
    CompositionLocalProvider(LocalPrefsEnabled provides !locked) {
        Scaffold(
            modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                ExpressiveLargeTopBar(
                    title = title,
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    scrollBehavior = behavior,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(if (navInset) Modifier.navigationBarsPadding() else Modifier)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (locked) LockedBanner()
                content()
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun rememberPrefs(): PrefState {
    val context = LocalContext.current
    return rememberPrefState(remember { context.getPreferences() })
}

// ---------------------------------------------------------------------------------------------
// "Configuration" tab
// ---------------------------------------------------------------------------------------------

@Composable
fun ConfigurationScreen(
    running: Boolean,
    onOpenUiSettings: () -> Unit,
    onOpenCmdSettings: () -> Unit,
) {
    val prefs = rememberPrefs()
    val cmdEnabled = prefs.bool("byedpi_enable_cmd_settings", false)

    SettingsScaffold(
        title = stringResource(R.string.tab_configuration),
        onBack = null,
        locked = running,
    ) {
        PrefGroup(title = stringResource(R.string.byedpi_category)) {
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.use_command_line_settings),
                    checked = cmdEnabled,
                    onChange = { prefs.putBool("byedpi_enable_cmd_settings", it) },
                )
            }
            item { shape ->
                ActionPref(
                    shape = shape,
                    title = stringResource(R.string.ui_editor),
                    enabled = !cmdEnabled,
                    onClick = onOpenUiSettings,
                )
            }
            item { shape ->
                ActionPref(
                    shape = shape,
                    title = stringResource(R.string.command_line_editor),
                    enabled = cmdEnabled,
                    onClick = onOpenCmdSettings,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// "Settings" tab: General, About, Reset
// ---------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(running: Boolean) {
    val prefs = rememberPrefs()
    val openUrl = openUrl()
    var confirmReset by remember { mutableStateOf(false) }

    val mode = Mode.fromString(prefs.string("byedpi_mode", "vpn"))

    val themeEntries = listOf(
        "system" to stringResource(R.string.theme_system),
        "light" to stringResource(R.string.theme_light),
        "dark" to stringResource(R.string.theme_dark),
    )
    val colorEntries = listOf(
        "dynamic" to stringResource(R.string.color_dynamic),
        "brand" to stringResource(R.string.color_brand),
    )
    val modeEntries = listOf(
        "vpn" to stringResource(R.string.mode_vpn),
        "proxy" to stringResource(R.string.mode_proxy),
    )

    SettingsScaffold(
        title = stringResource(R.string.title_settings),
        onBack = null,
        locked = running,
    ) {
        PrefGroup(title = stringResource(R.string.general_category)) {
            // Appearance is harmless while connected, so these two stay editable.
            item { shape ->
                CompositionLocalProvider(LocalPrefsEnabled provides true) {
                    ChoicePref(
                        shape = shape,
                        title = stringResource(R.string.theme_settings),
                        value = prefs.string("app_theme", "system"),
                        entries = themeEntries,
                        onSelect = { prefs.putString("app_theme", it) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item { shape ->
                    CompositionLocalProvider(LocalPrefsEnabled provides true) {
                        ChoicePref(
                            shape = shape,
                            title = stringResource(R.string.color_source),
                            value = prefs.string("app_color", "dynamic"),
                            entries = colorEntries,
                            onSelect = { prefs.putString("app_color", it) },
                        )
                    }
                }
            }
            item { shape ->
                ChoicePref(
                    shape = shape,
                    title = stringResource(R.string.mode_setting),
                    value = prefs.string("byedpi_mode", "vpn"),
                    entries = modeEntries,
                    onSelect = { prefs.putString("byedpi_mode", it) },
                )
            }
            if (mode == Mode.VPN) {
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.dbs_ip_setting),
                        value = prefs.string("dns_ip", "1.1.1.1"),
                        onSave = { prefs.putString("dns_ip", it) },
                        validate = { it.isBlank() || checkNotLocalIp(it) },
                    )
                }
                item { shape ->
                    SwitchPref(
                        shape = shape,
                        title = stringResource(R.string.ipv6_setting),
                        checked = prefs.bool("ipv6_enable", false),
                        onChange = { prefs.putBool("ipv6_enable", it) },
                    )
                }
            }
        }

        PrefGroup(title = stringResource(R.string.about_category)) {
            item { shape ->
                InfoPref(
                    shape = shape,
                    title = stringResource(R.string.version),
                    summary = BuildConfig.VERSION_NAME,
                )
            }
            item { shape ->
                ActionPref(
                    shape = shape,
                    title = stringResource(R.string.source_code_link),
                    githubIcon = true,
                    onClick = { openUrl(SOURCE_CODE_URL) },
                )
            }
        }

        PrefGroup {
            item { shape ->
                ActionPref(
                    shape = shape,
                    title = stringResource(R.string.reset_settings),
                    onClick = { confirmReset = true },
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_settings)) },
            text = { Text(stringResource(R.string.reset_settings_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.clear()
                        confirmReset = false
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// ByeDPI UI editor
// ---------------------------------------------------------------------------------------------

@Composable
fun UiSettingsScreen(running: Boolean, onBack: () -> Unit) {
    val prefs = rememberPrefs()
    val openUrl = openUrl()
    val docsUrl = stringResource(R.string.byedpi_docs)

    val hostsMode = prefs.string("byedpi_hosts_mode", "disable")
    val desyncMethod = prefs.string("byedpi_desync_method", "disorder")
    val desyncHttp = prefs.bool("byedpi_desync_http", true)
    val desyncHttps = prefs.bool("byedpi_desync_https", true)
    val desyncUdp = prefs.bool("byedpi_desync_udp", false)
    val tlsRec = prefs.bool("byedpi_tlsrec_enabled", false)

    // Same enable/visibility rules as the old preference screen.
    val desyncEnabled = desyncMethod != "none"
    val isFake = desyncMethod == "fake"
    val isOob = desyncMethod == "oob" || desyncMethod == "disoob"
    val allProtocols = !desyncHttp && !desyncHttps && !desyncUdp
    val httpEnabled = allProtocols || desyncHttp
    val udpEnabled = allProtocols || desyncUdp
    val httpsEnabled = allProtocols || desyncHttps
    val tlsRecEnabled = httpsEnabled && tlsRec

    val hostsEntries = listOf(
        "disable" to stringResource(R.string.hosts_mode_disable),
        "blacklist" to stringResource(R.string.hosts_mode_blacklist),
        "whitelist" to stringResource(R.string.hosts_mode_whitelist),
    )
    val desyncEntries = listOf(
        "none" to stringResource(R.string.desync_method_none),
        "split" to stringResource(R.string.desync_method_split),
        "disorder" to stringResource(R.string.desync_method_disorder),
        "fake" to stringResource(R.string.desync_method_fake),
        "oob" to stringResource(R.string.desync_method_oob),
        "disoob" to stringResource(R.string.desync_method_disoob),
    )

    SettingsScaffold(
        title = stringResource(R.string.ui_editor),
        onBack = onBack,
        locked = running,
        navInset = true,
    ) {
        PrefGroup {
            item { shape ->
                ActionPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_readme_link),
                    githubIcon = true,
                    onClick = { openUrl(docsUrl) },
                )
            }
        }

        PrefGroup(title = stringResource(R.string.byedpi_proxy)) {
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.bye_dpi_proxy_ip_setting),
                    value = prefs.string("byedpi_proxy_ip", "127.0.0.1"),
                    onSave = { prefs.putString("byedpi_proxy_ip", it) },
                    validate = { checkIp(it) },
                )
            }
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_proxy_port_setting),
                    value = prefs.string("byedpi_proxy_port", "1080"),
                    onSave = { prefs.putString("byedpi_proxy_port", it) },
                    keyboardType = KeyboardType.Number,
                    validate = intInRange(1, 65535),
                )
            }
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_max_connections_setting),
                    value = prefs.string("byedpi_max_connections", "512"),
                    onSave = { prefs.putString("byedpi_max_connections", it) },
                    keyboardType = KeyboardType.Number,
                    validate = intInRange(1, Short.MAX_VALUE.toInt()),
                )
            }
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_buffer_size_setting),
                    value = prefs.string("byedpi_buffer_size", "16384"),
                    onSave = { prefs.putString("byedpi_buffer_size", it) },
                    keyboardType = KeyboardType.Number,
                    validate = intInRange(1, Int.MAX_VALUE / 4),
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_no_domain_setting),
                    checked = prefs.bool("byedpi_no_domain", false),
                    onChange = { prefs.putBool("byedpi_no_domain", it) },
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_tcp_fast_open_setting),
                    checked = prefs.bool("byedpi_tcp_fast_open", false),
                    onChange = { prefs.putBool("byedpi_tcp_fast_open", it) },
                )
            }
        }

        PrefGroup(title = stringResource(R.string.byedpi_desync)) {
            item { shape ->
                ChoicePref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_hosts_mode_setting),
                    value = hostsMode,
                    entries = hostsEntries,
                    onSelect = { prefs.putString("byedpi_hosts_mode", it) },
                )
            }
            if (hostsMode == "blacklist") {
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.byedpi_hosts_blacklist_setting),
                        value = prefs.string("byedpi_hosts_blacklist", ""),
                        onSave = { prefs.putString("byedpi_hosts_blacklist", it) },
                        multiline = true,
                    )
                }
            }
            if (hostsMode == "whitelist") {
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.byedpi_hosts_whitelist_setting),
                        value = prefs.string("byedpi_hosts_whitelist", ""),
                        onSave = { prefs.putString("byedpi_hosts_whitelist", it) },
                        multiline = true,
                    )
                }
            }
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_default_ttl_setting),
                    value = prefs.string("byedpi_default_ttl", "0"),
                    onSave = { prefs.putString("byedpi_default_ttl", it) },
                    keyboardType = KeyboardType.Number,
                    validate = intInRange(0, 255),
                )
            }
            item { shape ->
                ChoicePref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_desync_method_setting),
                    value = desyncMethod,
                    entries = desyncEntries,
                    onSelect = { prefs.putString("byedpi_desync_method", it) },
                )
            }
            if (desyncEnabled) {
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.byedpi_split_position_setting),
                        value = prefs.string("byedpi_split_position", "1"),
                        onSave = { prefs.putString("byedpi_split_position", it) },
                        validate = intInRange(Int.MIN_VALUE, Int.MAX_VALUE),
                    )
                }
                item { shape ->
                    SwitchPref(
                        shape = shape,
                        title = stringResource(R.string.byedpi_split_at_host_setting),
                        checked = prefs.bool("byedpi_split_at_host", false),
                        onChange = { prefs.putBool("byedpi_split_at_host", it) },
                    )
                }
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_drop_sack_setting),
                    checked = prefs.bool("byedpi_drop_sack", false),
                    onChange = { prefs.putBool("byedpi_drop_sack", it) },
                )
            }
            if (isFake) {
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.byedpi_fake_ttl_setting),
                        value = prefs.string("byedpi_fake_ttl", "8"),
                        onSave = { prefs.putString("byedpi_fake_ttl", it) },
                        keyboardType = KeyboardType.Number,
                        validate = intInRange(1, 255),
                    )
                }
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.byedpi_fake_offset_setting),
                        value = prefs.string("byedpi_fake_offset", "0"),
                        onSave = { prefs.putString("byedpi_fake_offset", it) },
                        validate = intInRange(Int.MIN_VALUE, Int.MAX_VALUE),
                    )
                }
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.sni_of_fake_packet),
                        value = prefs.string("byedpi_fake_sni", "www.iana.org"),
                        onSave = { prefs.putString("byedpi_fake_sni", it) },
                    )
                }
            }
            if (isOob) {
                item { shape ->
                    TextPref(
                        shape = shape,
                        title = stringResource(R.string.oob_data),
                        value = prefs.string("byedpi_oob_data", "a"),
                        onSave = { prefs.putString("byedpi_oob_data", it) },
                        maxLength = 1,
                        validate = { it.length == 1 },
                    )
                }
            }
        }

        PrefGroup(
            title = stringResource(R.string.byedpi_protocols_category),
            hint = stringResource(R.string.byedpi_protocols_hint),
        ) {
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.desync_http),
                    checked = desyncHttp,
                    onChange = { prefs.putBool("byedpi_desync_http", it) },
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.desync_https),
                    checked = desyncHttps,
                    onChange = { prefs.putBool("byedpi_desync_https", it) },
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.desync_udp),
                    checked = desyncUdp,
                    onChange = { prefs.putBool("byedpi_desync_udp", it) },
                )
            }
        }

        PrefGroup(title = stringResource(R.string.desync_http_category)) {
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_host_mixed_case_setting),
                    checked = prefs.bool("byedpi_host_mixed_case", false),
                    enabled = httpEnabled,
                    onChange = { prefs.putBool("byedpi_host_mixed_case", it) },
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_domain_mixed_case_setting),
                    checked = prefs.bool("byedpi_domain_mixed_case", false),
                    enabled = httpEnabled,
                    onChange = { prefs.putBool("byedpi_domain_mixed_case", it) },
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_host_remove_spaces_setting),
                    checked = prefs.bool("byedpi_host_remove_spaces", false),
                    enabled = httpEnabled,
                    onChange = { prefs.putBool("byedpi_host_remove_spaces", it) },
                )
            }
        }

        PrefGroup(title = stringResource(R.string.desync_https_category)) {
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_tlsrec_enabled_setting),
                    checked = tlsRec,
                    enabled = httpsEnabled,
                    onChange = { prefs.putBool("byedpi_tlsrec_enabled", it) },
                )
            }
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_tlsrec_position_setting),
                    value = prefs.string("byedpi_tlsrec_position", "0"),
                    onSave = { prefs.putString("byedpi_tlsrec_position", it) },
                    enabled = tlsRecEnabled,
                    validate = intInRange(2 * Short.MIN_VALUE, 2 * Short.MAX_VALUE),
                )
            }
            item { shape ->
                SwitchPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_tlsrec_at_sni_setting),
                    checked = prefs.bool("byedpi_tlsrec_at_sni", false),
                    enabled = tlsRecEnabled,
                    onChange = { prefs.putBool("byedpi_tlsrec_at_sni", it) },
                )
            }
        }

        PrefGroup(title = stringResource(R.string.desync_udp_category)) {
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.byedpi_udp_fake_count),
                    value = prefs.string("byedpi_udp_fake_count", "0"),
                    onSave = { prefs.putString("byedpi_udp_fake_count", it) },
                    enabled = udpEnabled,
                    keyboardType = KeyboardType.Number,
                    validate = intInRange(0, Int.MAX_VALUE),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Command line editor
// ---------------------------------------------------------------------------------------------

@Composable
fun CmdSettingsScreen(running: Boolean, onBack: () -> Unit) {
    val prefs = rememberPrefs()
    val openUrl = openUrl()
    val docsUrl = stringResource(R.string.byedpi_docs)

    SettingsScaffold(
        title = stringResource(R.string.command_line_editor),
        onBack = onBack,
        locked = running,
        navInset = true,
    ) {
        PrefGroup {
            item { shape ->
                ActionPref(
                    shape = shape,
                    title = stringResource(R.string.documentation),
                    githubIcon = true,
                    onClick = { openUrl(docsUrl) },
                )
            }
        }
        PrefGroup {
            item { shape ->
                TextPref(
                    shape = shape,
                    title = stringResource(R.string.command_line_arguments),
                    value = prefs.string("byedpi_cmd_args", ""),
                    onSave = { prefs.putString("byedpi_cmd_args", it) },
                    multiline = true,
                )
            }
        }
    }
}
