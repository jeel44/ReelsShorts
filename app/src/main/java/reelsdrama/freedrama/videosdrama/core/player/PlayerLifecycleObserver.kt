package reelsdrama.freedrama.videosdrama.core.player

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class PlayerLifecycleObserver(
    private val onPause: () -> Unit,
    private val onResume: () -> Unit
) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_RESUME -> onResume()
            else -> {}
        }
    }
}
