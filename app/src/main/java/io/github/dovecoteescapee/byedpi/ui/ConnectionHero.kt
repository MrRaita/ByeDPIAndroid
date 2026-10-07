package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val ButtonSize = 168.dp
private val RingSize = 204.dp

/**
 * The connect / disconnect control. All choreography lives in [ConnectionFx]; this only draws it:
 *
 *  - idle: a muted circle with the label and the proxy address
 *  - busy: a wavy ring around the circle's outline (fills from the top on connect, drains on disconnect)
 *  - connected: calm, irregular, wobbling ripples keep radiating from the button
 *  - transitions: a wave sweeps from the button to the screen sides (connect) or is sucked back in (disconnect)
 */
@Composable
fun ConnectionHero(
    fx: ConnectionFx,
    connectLabel: String,
    disconnectLabel: String,
    connectingLabel: String,
    disconnectingLabel: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val reduce = LocalReduceMotion.current

    val e = fx.energy.value.coerceIn(0f, 1f)
    val container = lerpColor(colors.surfaceContainerHighest, colors.primary, e)
    val content = lerpColor(colors.onSurface, colors.onPrimary, e)

    val label = when (fx.phase) {
        FxPhase.Connecting -> connectingLabel
        FxPhase.Disconnecting -> disconnectingLabel
        FxPhase.Idle -> if (fx.shown) disconnectLabel else connectLabel
    }
    val busy = fx.phase != FxPhase.Idle
    val fxVisible = !reduce && (fx.shown || e > 0.001f || fx.waveMode != WaveMode.None)

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        if (fxVisible) {
            RippleCanvas(
                energy = fx.energy,
                wave = fx.wave,
                waveMode = fx.waveMode,
                buttonRadius = ButtonSize / 2,
                modifier = Modifier.matchParentSize(),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = fx.pop.value
                scaleY = fx.pop.value
            },
        ) {
            if (busy) {
                if (fx.ringLooping || reduce) {
                    ExpressiveWavyRing(color = colors.primary, modifier = Modifier.size(RingSize))
                } else {
                    ExpressiveWavyRing(
                        color = colors.primary,
                        modifier = Modifier.size(RingSize),
                        progress = { fx.ring.value.coerceIn(0f, 1f) },
                    )
                }
            }
            ExpressiveCircleButton(
                text = label,
                subtitle = subtitle,
                containerColor = container,
                contentColor = content,
                onClick = onClick,
                enabled = !busy,
                size = ButtonSize,
            )
        }
    }
}

@Composable
private fun RippleCanvas(
    energy: Animatable<Float, AnimationVector1D>,
    wave: Animatable<Float, AnimationVector1D>,
    waveMode: WaveMode,
    buttonRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val clock = rememberInfiniteTransition(label = "ripples")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "ripple-clock",
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val palette = listOf(primary, tertiary, secondary)

    Canvas(modifier = modifier) {
        val c = center
        val btnR = buttonRadius.toPx()
        val maxR = hypot(size.width, size.height) / 2f
        val e = energy.value.coerceIn(0f, 1f)

        // Soft glow behind the button, dies with the power.
        if (e > 0f) {
            val glowR = btnR * 2.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.30f * e), Color.Transparent),
                    center = c,
                    radius = glowR,
                ),
                radius = glowR,
                center = c,
            )
        }

        // Calm ambient ripples: three staggered, irregular rings. Their reach scales with the energy, so on
        // power-off they collapse into the button.
        if (e > 0.01f) {
            val reach = (btnR + 150.dp.toPx() - btnR) * e
            for (k in 0 until 3) {
                val u = fract(t * 3f + k / 3f)
                val radius = btnR + u * reach
                val fadeIn = (u * 5f).coerceAtMost(1f)
                val alpha = (1f - u) * fadeIn * 0.5f * e
                val stroke = lerp(6.dp.toPx(), 1.5.dp.toPx(), u)
                val wobble = (3.dp.toPx() + 12.dp.toPx() * u) * e
                val phase = (2.0 * PI * t * (k + 1)).toFloat()
                wobblyRing(c, radius, wobble, phase, k * 2.1f, palette[k % 3], stroke, alpha)
            }
        }

        // One-shot wave.
        when (waveMode) {
            WaveMode.Out -> {
                val p = wave.value
                for (i in 0 until 3) {
                    val pi = (p - i * 0.07f).coerceIn(0f, 1f)
                    if (pi <= 0f) continue
                    val radius = btnR + EaseOutCubic.transform(pi) * (maxR - btnR)
                    val alpha = (1f - pi) * (0.65f - i * 0.15f)
                    val stroke = lerp(12.dp.toPx(), 2.dp.toPx(), pi) * (1f - i * 0.2f)
                    val wobble = 4.dp.toPx() + 22.dp.toPx() * pi
                    val phase = (2.0 * PI * (pi * 1.5f + i * 0.3f)).toFloat()
                    wobblyRing(c, radius, wobble, phase, i * 1.7f, palette[i % 3], stroke, alpha)
                }
            }

            WaveMode.In -> {
                val q = wave.value
                for (i in 0 until 3) {
                    val qi = (q - i * 0.06f).coerceIn(0f, 1f)
                    if (qi <= 0f) continue
                    val radius = btnR + (1f - EaseInCubic.transform(qi)) * (maxR - btnR)
                    val alpha = sin(PI * qi).toFloat() * (0.55f - i * 0.12f)
                    val stroke = lerp(2.dp.toPx(), 10.dp.toPx(), qi)
                    val wobble = 2.dp.toPx() + 22.dp.toPx() * (1f - qi)
                    val phase = (-2.0 * PI * (qi * 1.5f + i * 0.3f)).toFloat()
                    wobblyRing(c, radius, wobble, phase, i * 1.7f, palette[i % 3], stroke, alpha)
                }
                // The last flash as the energy hits the button.
                if (q > 0.85f) {
                    val pulse = (q - 0.85f) / 0.15f
                    drawCircle(
                        color = primary.copy(alpha = 0.28f * (1f - pulse)),
                        radius = btnR * (1f + 0.25f * pulse),
                        center = c,
                    )
                }
            }

            WaveMode.None -> Unit
        }
    }
}

private fun fract(x: Float): Float = x - kotlin.math.floor(x)

/**
 * A closed ring whose radius wobbles around the circle: a sum of low harmonics that drift with [phase], so the
 * outline keeps changing shape instead of being a perfect circle. Integer harmonics keep the path closed and
 * the animation seamless when [phase] is a multiple of 2*PI.
 */
private fun DrawScope.wobblyRing(
    center: Offset,
    radius: Float,
    wobble: Float,
    phase: Float,
    seed: Float,
    color: Color,
    strokeWidth: Float,
    alpha: Float,
) {
    if (radius <= 0f || alpha <= 0.002f || strokeWidth <= 0f) return
    val steps = 120
    val path = Path()
    for (i in 0..steps) {
        val a = (i.toFloat() / steps) * 2f * PI.toFloat()
        val w = 0.55f * sin(3f * a + phase + seed) +
            0.30f * sin(5f * a - 2f * phase + 2f * seed) +
            0.35f * sin(2f * a + phase - seed)
        val r = radius + wobble * w
        val x = center.x + r * cos(a)
        val y = center.y + r * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(
        path = path,
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        style = Stroke(width = strokeWidth, join = StrokeJoin.Round),
    )
}
