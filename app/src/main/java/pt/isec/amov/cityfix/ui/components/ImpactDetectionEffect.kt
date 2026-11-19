package pt.isec.amov.cityfix.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Starts impact detection while the app is in front and [enabled] (brief I.2.F), with the
 * DisposableEffect + LifecycleEventObserver of the class demo Sensors.kt: ON_START calls
 * [onStart], ON_STOP calls [onStop] ("always unregister in onPause()/onStop()", slides 10/11).
 * Turning [enabled] off, or leaving the composition, calls [onStop]. The sensor itself lives in
 * ImpactViewModel; this only says when.
 */
@Composable
fun ImpactDetectionEffect(enabled: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        var started = false

        fun start() {
            if (!started) {
                onStart()
                started = true
            }
        }

        fun stop() {
            if (started) {
                onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            start()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }
}
