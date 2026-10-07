package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * True when the user asked for fewer animations (Settings > Reduce animations) or the system animation scale
 * is 0. Effects check this to skip waves/ripples and use short, plain transitions instead.
 */
val LocalReduceMotion = compositionLocalOf { false }
