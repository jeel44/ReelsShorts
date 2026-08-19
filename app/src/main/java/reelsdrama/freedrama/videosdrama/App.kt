package reelsdrama.freedrama.videosdrama

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdForegroundTrigger
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdManager
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.core.notifications.OneSignalManager
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var adInitializer: AdInitializer

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    @Inject
    lateinit var appOpenAdForegroundTrigger: AppOpenAdForegroundTrigger

    @Inject
    lateinit var oneSignalManager: OneSignalManager

    // App-process-lifetime scope, same rationale as AdInitializer's own initScope: there's
    // no owner to cancel this against, and it only ever needs to fire once per process.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // The Activity currently in ON_RESUME, per registerActivityLifecycleCallbacks below - the
    // Activity reference [appOpenAdForegroundTrigger]'s ON_START handler shows on. Deliberately
    // NOT read via Compose's LocalActivity.current from a lifecycle observer: that only resolves
    // correctly inside live composition (e.g. SplashRoute's own use of it), which is exactly what
    // isn't guaranteed to be around/current by the time a process-level ON_START observer fires.
    @Volatile
    private var resumedActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        // Foundation layer only: starts GMA Next-Gen SDK init on a background thread.
        // No ad-loading/display logic here - that's built on top of this later.
        adInitializer.initialize()

        // OneSignal push notifications - independent of AdMob, started here too per OneSignal's
        // own Android integration guide ("Initialization happens BEFORE any other OneSignal
        // calls"). MainActivity's push-subscription-observer setup waits on
        // oneSignalManager.isInitialized before calling any further OneSignal API.
        oneSignalManager.initialize()

        // Kick off the app-open-ad preload the instant MobileAds finishes initializing -
        // from here, not from SplashViewModel's init. SplashViewModel is only constructed
        // once MainActivity has run setContent() and Compose has composed far enough to
        // create the Splash NavHost destination's hiltViewModel() - measurably later than
        // Application.onCreate(). AdDebug logs confirmed the ad load itself was fine, it
        // just didn't have enough of a head start before splash's post-delay isAdReady
        // check ran (onAdLoaded landed ~2.8s after the check had already given up). Moving
        // the trigger here gives preload() the maximum possible lead time.
        appScope.launch {
            adInitializer.isInitialized.first { it }
            Log.d(TAG, "App.onCreate: adInitializer.isInitialized=true, calling appOpenAdManager.preload() from Application scope")
            appOpenAdManager.preload(AdConstants.APP_OPEN_UNIT_ID)
        }

        // Tracks the resumed Activity for appOpenAdForegroundTrigger's ON_START handler (see
        // resumedActivity's own doc comment). onActivityDestroyed only clears it if it's still
        // the same instance - this is a single-Activity app today, but guards against a stale
        // reference if that ever changes (e.g. a second Activity destroyed after a third resumed).
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                resumedActivity = activity
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (resumedActivity === activity) resumedActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })

        // Separate ProcessLifecycleOwner observer from VideoPlayerManager's ON_PAUSE/ON_RESUME
        // one - see AppOpenAdForegroundTrigger's own doc comment for why ON_START and why it's
        // not folded into that existing observer.
        appOpenAdForegroundTrigger.attach { resumedActivity }
    }

    private companion object {
        const val TAG = "AdDebug"
    }
}
