package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Predefined color palettes.
 *
 * Every palette except the hand-tuned ByeDPI blue is generated Material-style: from one seed hue we build
 * tonal palettes (primary, secondary, tertiary, neutral, neutral-variant) and map their tones onto the
 * Material 3 color roles (primary = tone 40 / 80, container = 90 / 30, surfaces from the neutral palette ...).
 *
 * The tones are computed in OKLCH (tone -> lightness uses the same curve as CIE L*), a close but simplified
 * stand-in for Material's HCT. Contrast between every role pair was checked to be at least 6:1.
 */
class AppPalette(val id: String, val label: String, val swatch: Color, internal val hue: Double?)

object Palettes {
    const val DEFAULT_ID = "byedpi"

    val all: List<AppPalette> = listOf(
        AppPalette("byedpi", "ByeDPI", Color(0xFF4259C7), null),
        generated("ocean", "Ocean", 235.0),
        generated("violet", "Violet", 300.0),
        generated("orchid", "Orchid", 335.0),
        generated("rose", "Rose", 0.0),
        generated("crimson", "Crimson", 27.0),
        generated("tangerine", "Tangerine", 55.0),
        generated("amber", "Amber", 85.0),
        generated("lime", "Lime", 120.0),
        generated("forest", "Forest", 150.0),
        generated("teal", "Teal", 180.0),
        generated("cyan", "Cyan", 210.0),
    )

    fun find(id: String): AppPalette = all.firstOrNull { it.id == id } ?: all.first()

    fun scheme(palette: AppPalette, dark: Boolean): ColorScheme {
        val hue = palette.hue ?: return if (dark) ByeDpiDarkColors else ByeDpiLightColors
        return generatedScheme(hue, dark)
    }

    private fun generated(id: String, label: String, hue: Double) =
        AppPalette(id, label, toneColor(hue, PRIMARY_CHROMA, 50.0), hue)
}

private const val PRIMARY_CHROMA = 0.12

private class Tonal(val hue: Double, val chroma: Double) {
    fun t(tone: Int): Color = toneColor(hue, chroma, tone.toDouble())
}

private fun generatedScheme(hue: Double, dark: Boolean): ColorScheme {
    val p = Tonal(hue, PRIMARY_CHROMA)
    val s = Tonal(hue, 0.04)
    val t = Tonal((hue + 60.0) % 360.0, 0.08)
    val n = Tonal(hue, 0.008)
    val nv = Tonal(hue, 0.016)

    return if (!dark) {
        lightColorScheme(
            primary = p.t(40), onPrimary = p.t(100),
            primaryContainer = p.t(90), onPrimaryContainer = p.t(10),
            secondary = s.t(40), onSecondary = s.t(100),
            secondaryContainer = s.t(90), onSecondaryContainer = s.t(10),
            tertiary = t.t(40), onTertiary = t.t(100),
            tertiaryContainer = t.t(90), onTertiaryContainer = t.t(10),
            error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
            background = n.t(98), onBackground = n.t(10),
            surface = n.t(98), onSurface = n.t(10),
            surfaceVariant = nv.t(90), onSurfaceVariant = nv.t(30),
            outline = nv.t(50), outlineVariant = nv.t(80),
            inverseSurface = n.t(20), inverseOnSurface = n.t(95), inversePrimary = p.t(80),
            surfaceDim = n.t(87), surfaceBright = n.t(98),
            surfaceContainerLowest = n.t(100), surfaceContainerLow = n.t(96),
            surfaceContainer = n.t(94), surfaceContainerHigh = n.t(92), surfaceContainerHighest = n.t(90),
        )
    } else {
        darkColorScheme(
            primary = p.t(80), onPrimary = p.t(20),
            primaryContainer = p.t(30), onPrimaryContainer = p.t(90),
            secondary = s.t(80), onSecondary = s.t(20),
            secondaryContainer = s.t(30), onSecondaryContainer = s.t(90),
            tertiary = t.t(80), onTertiary = t.t(20),
            tertiaryContainer = t.t(30), onTertiaryContainer = t.t(90),
            error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
            background = n.t(6), onBackground = n.t(90),
            surface = n.t(6), onSurface = n.t(90),
            surfaceVariant = nv.t(30), onSurfaceVariant = nv.t(80),
            outline = nv.t(60), outlineVariant = nv.t(30),
            inverseSurface = n.t(90), inverseOnSurface = n.t(20), inversePrimary = p.t(40),
            surfaceDim = n.t(6), surfaceBright = n.t(24),
            surfaceContainerLowest = n.t(4), surfaceContainerLow = n.t(10),
            surfaceContainer = n.t(12), surfaceContainerHigh = n.t(17), surfaceContainerHighest = n.t(22),
        )
    }
}

// ---- color math (OKLCH -> sRGB) ----

private fun toneToLightness(tone: Double): Double {
    val y = if (tone > 8.0) ((tone + 16.0) / 116.0).pow(3) else tone / 903.3
    return y.pow(1.0 / 3.0)
}

private fun linearToSrgb(c: Double): Double {
    val v = c.coerceIn(0.0, 1.0)
    return if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
}

private fun oklchToLinearRgb(l: Double, c: Double, hueDeg: Double): DoubleArray {
    val rad = Math.toRadians(hueDeg)
    val a = c * cos(rad)
    val b = c * sin(rad)
    val l_ = l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = l - 0.0894841775 * a - 1.2914855480 * b
    val lc = l_ * l_ * l_
    val mc = m_ * m_ * m_
    val sc = s_ * s_ * s_
    return doubleArrayOf(
        4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc,
        -1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc,
        -0.0041960863 * lc - 0.7034186147 * mc + 1.7076147010 * sc,
    )
}

private fun inGamut(rgb: DoubleArray): Boolean = rgb.all { it >= -1e-4 && it <= 1.0 + 1e-4 }

/** A color of the given hue / (maximum) chroma at a Material tone (0 = black, 100 = white), clipped to sRGB. */
internal fun toneColor(hue: Double, chroma: Double, tone: Double): Color {
    if (tone <= 0.0) return Color(0, 0, 0)
    if (tone >= 100.0) return Color(255, 255, 255)

    val l = toneToLightness(tone)
    var c = chroma
    if (!inGamut(oklchToLinearRgb(l, chroma, hue))) {
        var lo = 0.0
        var hi = chroma
        repeat(24) {
            val mid = (lo + hi) / 2.0
            if (inGamut(oklchToLinearRgb(l, mid, hue))) lo = mid else hi = mid
        }
        c = lo
    }
    val rgb = oklchToLinearRgb(l, c, hue)
    return Color(
        (linearToSrgb(rgb[0]) * 255.0).roundToInt(),
        (linearToSrgb(rgb[1]) * 255.0).roundToInt(),
        (linearToSrgb(rgb[2]) * 255.0).roundToInt(),
    )
}
