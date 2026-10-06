package io.github.dovecoteescapee.byedpi.utility

import android.content.Context
import android.content.SharedPreferences
import io.github.dovecoteescapee.byedpi.data.Mode

/**
 * Same file that `PreferenceManager.getDefaultSharedPreferences()` used before the Compose
 * rewrite ("<package>_preferences"), so settings of existing installs carry over untouched.
 */
fun Context.getPreferences(): SharedPreferences =
    getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

fun SharedPreferences.getStringNotNull(key: String, defValue: String): String =
    getString(key, defValue) ?: defValue

fun SharedPreferences.mode(): Mode =
    Mode.fromString(getStringNotNull("byedpi_mode", "vpn"))
