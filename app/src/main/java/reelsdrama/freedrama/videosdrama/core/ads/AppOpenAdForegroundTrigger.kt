package reelsdrama.freedrama.videosdrama.core.ads

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground-return trigger for App Open ads - a SEPARATE [ProcessLifecycleOwner] observer from
 * [reelsdrama.freedrama.videosdrama.core.player.PlayerLifecycleObserver] (registered by
 * [reelsdrama.freedrama.videosdrama.core.player.VideoPlayerManager]). That one deliberately uses
 * ON_PAUSE/ON_RESUME to dodge ON_STOP's OEM lag for audio - the opposite of what's needed here.
 * ON_START is Google's own documented App Open signal: it fires once per foreground transition,
 * cold start included, which is exactly the case this class has to guard against (see
 * [hasBeenBackgrounded] below) since [reelsdrama.freedrama.videosdrama.presentation.splash.SplashViewModel]
 * already owns the cold-start show attempt.
 *
 * Registered once, for the process lifetime, via [attach] - not the constructor, and not
 * `init` - because it needs a way to look up the currently-resumed [Activity], which only
 * [reelsdrama.freedrama.videosdrama.App]'s own `Application.ActivityLifecycleCallbacks` can
 * provide; Hilt has nothing to inject that from. [reelsdrama.freedrama.videosdrama.App.onCreate]
 * calls [attach] once, passing a lambda that reads its own tracked-Activity field.
 *
 * Frequency cap ([AdConstants.APP_OPEN_MIN_INTERVAL_MS]) and the re-entrancy guard
 * (`isShowing`) both live in [AppOpenAdManager.show] itself, not here - that way every caller
 * (this trigger AND [reelsdrama.freedrama.videosdrama.presentation.splash.SplashViewModel]'s
 * cold-start path) gets the same protection for free, rather than duplicating the checks per
 * call site.
 *
 * KNOWN GAP: if the user leaves the app by tapping an ad (any placement) and returns, ON_START
 * fires exactly like a normal Home/recents return, and this would stack an App Open ad on top of
 * that. None of [AppOpenAdManager]/[InterstitialAdManager]/[RewardedAdManager] expose an
 * `onAdClicked` signal to their callers (all three have empty `onAdClicked() {}` overrides on
 * their SDK event callbacks) - there is currently no reliable way to distinguish "returned via an
 * ad click" from "returned via Home/recents" here, so this is left unhandled rather than papered
 * over with a fragile heuristic (e.g. a short cooldown after any ad-consuming action).
 */
@Singleton
class AppOpenAdForegroundTrigger @Inject constructor(
    private val appOpenAdManager: AppOpenAdManager
) : LifecycleEventObserver {

    /**
     * Set true the first time ON_STOP fires, and never cleared. ON_START also fires on cold
     * start (before this flag is ever set) - gating on it is what stops this trigger from firing
     * before Splash has run its own gated show attempt.
     */
    private var hasBeenBackgrounded = false

    private var getResumedActivity: () -> Activity? = { null }

    /** Wires [resumedActivityProvider] in and starts observing [ProcessLifecycleOwner]. */
    fun attach(resumedActivityProvider: () -> Activity?) {
        getResumedActivity = resumedActivityProvider
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> hasBeenBackgrounded = true

            Lifecycle.Event.ON_START -> {
                if (!hasBeenBackgrounded) {
                    Log.d(TAG, "ON_START: skipping, process has not been backgrounded yet (cold start - Splash owns this)")
                    return
                }
                val activity = getResumedActivity()
                if (activity == null) {
                    Log.d(TAG, "ON_START: skipping, no resumed Activity tracked")
                    return
                }
                appOpenAdManager.show(activity, AdConstants.APP_OPEN_UNIT_ID) { outcome ->
                    Log.d(TAG, "foreground-return show outcome: $outcome")
                }
            }

            else -> {}
        }
    }

    private companion object {
        const val TAG = "AdDebug"
    }
}
