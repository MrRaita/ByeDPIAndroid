package io.github.dovecoteescapee.byedpi.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.dovecoteescapee.byedpi.utility.getStringNotNull

/**
 * Compose-friendly view over the app's SharedPreferences. Reading through [string]/[bool]
 * inside a composable subscribes it to changes, so the UI updates when a value is written.
 */
@Stable
class PrefState internal constructor(
    private val prefs: SharedPreferences,
    private val version: MutableState<Int>,
) {
    fun string(key: String, default: String): String {
        version.value // subscribe
        return prefs.getStringNotNull(key, default)
    }

    fun bool(key: String, default: Boolean): Boolean {
        version.value // subscribe
        return prefs.getBoolean(key, default)
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun putBool(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
        version.value++ // clear() does not notify listeners on every API level
    }
}

@Composable
fun rememberPrefState(prefs: SharedPreferences): PrefState {
    val version = remember { mutableStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            version.value++
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return remember(prefs) { PrefState(prefs, version) }
}
