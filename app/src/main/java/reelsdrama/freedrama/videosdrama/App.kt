package reelsdrama.freedrama.videosdrama

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdManager
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var adInitializer: AdInitializer

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    // App-process-lifetime scope, same rationale as AdInitializer's own initScope: there's
    // no owner to cancel this against, and it only ever needs to fire once per process.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Foundation layer only: starts GMA Next-Gen SDK init on a background thread.
        // No ad-loading/display logic here - that's built on top of this later.
        adInitializer.initialize()

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
    }

    private companion object {
        const val TAG = "AdDebug"
    }
}
