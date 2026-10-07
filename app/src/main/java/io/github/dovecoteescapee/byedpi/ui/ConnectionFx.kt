package io.github.dovecoteescapee.byedpi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class FxPhase { Idle, Connecting, Disconnecting }

enum class WaveMode { None, Out, In }

/** One lap of the ring (and the matching collapse on disconnect), regardless of how fast the service answers. */
const val RING_MS = 2400

/**
 * Drives the connect / disconnect choreography. It is deliberately decoupled from the real service state:
 *
 *  CONNECT   press -> the ring fills clockwise from the top, taking [RING_MS] no matter what -> when the ring
 *            closes (and the service has answered) the wave bursts out from the button, the button fills and
 *            "pops", ripples start.
 *            If the service is slower than the ring, the ring keeps running (indeterminate) until it answers.
 *  DISCONNECT press -> the reverse: the wave collapses back into the button while the ring drains
 *            counter-clockwise, energy fades, then the button "thunks" off like cut power.
 *  FAILURE   the ring unwinds and nothing else happens.
 *
 * [shown] is the state the UI displays (it lags behind the real state while an animation plays).
 */
@Stable
class ConnectionFx internal constructor(
    initialRunning: Boolean,
    private val scope: CoroutineScope,
) {
    var shown by mutableStateOf(initialRunning)
        private set
    var phase by mutableStateOf(FxPhase.Idle)
        private set
    var ringLooping by mutableStateOf(false)
        private set
    var waveMode by mutableStateOf(WaveMode.None)
        private set

    val energy = Animatable(if (initialRunning) 1f else 0f) // 0 = off, 1 = on
    val ring = Animatable(0f)                               // determinate ring progress
    val wave = Animatable(0f)                               // one-shot wave progress
    val pop = Animatable(1f)                                // button scale kick

    // Latest real inputs, written from composition.
    internal var running by mutableStateOf(initialRunning)
    internal var pending by mutableStateOf(false)
    internal var reduce by mutableStateOf(false)

    private var job: Job? = null

    internal fun onPress() {
        if (phase != FxPhase.Idle) return
        restart { if (!shown) connectSequence() else disconnectSequence() }
    }

    /** The real state changed without a button press (quick tile, service died, ...). */
    internal fun onExternalChange() {
        if (phase != FxPhase.Idle || running == shown) return
        restart { if (running) powerOn(quick = true) else powerOff() }
    }

    private fun restart(block: suspend () -> Unit) {
        job?.cancel()
        waveMode = WaveMode.None
        job = scope.launch { block() }
    }

    private suspend fun awaitSettled() {
        snapshotFlow { pending }.first { !it }
    }

    private suspend fun connectSequence() {
        phase = FxPhase.Connecting
        ringLooping = false
        ring.snapTo(0f)

        if (reduce) {
            ringLooping = true
            awaitSettled()
        } else {
            ring.animateTo(1f, tween(RING_MS, easing = FastOutSlowInEasing))
            if (pending) {
                ringLooping = true
                awaitSettled()
            }
        }
        ringLooping = false

        if (running) {
            phase = FxPhase.Idle // ring hides exactly as the wave starts
            powerOn(quick = false)
        } else {
            if (!reduce) ring.animateTo(0f, tween(250))
            phase = FxPhase.Idle
        }
    }

    private suspend fun disconnectSequence() {
        phase = FxPhase.Disconnecting
        ringLooping = false

        if (reduce) {
            ringLooping = true
            awaitSettled()
            ringLooping = false
            if (!running) {
                energy.snapTo(0f)
                shown = false
            }
            phase = FxPhase.Idle
            return
        }

        ring.snapTo(1f)
        waveMode = WaveMode.In
        coroutineScope {
            launch { ring.animateTo(0f, tween(RING_MS, easing = FastOutSlowInEasing)) }
            launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(RING_MS, easing = FastOutLinearInEasing))
                waveMode = WaveMode.None
            }
            launch { energy.animateTo(0f, tween(RING_MS, easing = FastOutLinearInEasing)) }
        }
        if (pending) {
            ringLooping = true
            awaitSettled()
            ringLooping = false
        }

        if (!running) {
            shown = false
            phase = FxPhase.Idle
            thunk()
        } else {
            // The service did not stop: bring the energy back.
            phase = FxPhase.Idle
            energy.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        }
    }

    private suspend fun powerOn(quick: Boolean) {
        shown = true
        if (reduce) {
            energy.snapTo(1f)
            return
        }
        waveMode = WaveMode.Out
        coroutineScope {
            launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(if (quick) 900 else 1500, easing = LinearOutSlowInEasing))
                waveMode = WaveMode.None
            }
            launch {
                energy.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
            }
            launch {
                pop.animateTo(1.12f, tween(130))
                pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
            }
        }
    }

    private suspend fun powerOff() {
        if (reduce) {
            energy.snapTo(0f)
            shown = false
            return
        }
        waveMode = WaveMode.In
        coroutineScope {
            launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(900, easing = FastOutLinearInEasing))
                waveMode = WaveMode.None
            }
            launch { energy.animateTo(0f, tween(900, easing = FastOutLinearInEasing)) }
        }
        shown = false
        thunk()
    }

    private suspend fun thunk() {
        pop.animateTo(0.88f, tween(70))
        pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
    }
}

@Composable
fun rememberConnectionFx(running: Boolean, pending: Boolean, reduce: Boolean): ConnectionFx {
    val scope = rememberCoroutineScope()
    val fx = remember { ConnectionFx(running, scope) }
    SideEffect {
        fx.running = running
        fx.pending = pending
        fx.reduce = reduce
    }
    LaunchedEffect(pending) { if (pending) fx.onPress() }
    LaunchedEffect(running) { fx.onExternalChange() }
    return fx
}
