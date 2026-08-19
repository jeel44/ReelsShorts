package reelsdrama.freedrama.videosdrama.presentation.splash

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdManager
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdOutcome
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants
import reelsdrama.freedrama.videosdrama.core.referrer.InstallReferrerManager
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository,
    private val appOpenAdManager: AppOpenAdManager,
    private val installReferrerManager: InstallReferrerManager
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // NOTE: the app-open-ad preload() call used to be kicked off from this ViewModel's
    // init block, gated on adInitializer.isInitialized. It's been moved to App.onCreate()
    // (see App.kt) so it starts as early as possible in the process lifetime instead of
    // waiting for this ViewModel to be constructed, which only happens once Compose has
    // run far enough to reach the Splash destination - confirmed via AdDebug logs to be
    // too late in some runs (onAdLoaded landing after the post-delay isAdReady check had
    // already given up). This ViewModel still reads appOpenAdManager.isAdReady/show() below,
    // it just no longer triggers the initial load itself.

    /**
     * @param activity Needed only in case an app open ad ends up eligible and ready to show -
     * never stored beyond this call.
     */
    fun startTimer(activity: Activity?) {
        viewModelScope.launch {
            // Fire-and-forget: maybeSendInstallReferrer() launches on its own internal IO
            // scope and returns immediately (same non-suspending shape as
            // adInitializer.initialize()/oneSignalManager.initialize()), so this line adds
            // zero delay here - it does NOT participate in this coroutine's timing at all.
            // Deliberately not moved to App.onCreate (unlike those two): this fires from
            // Splash per the intended wiring, is fully decoupled from SPLASH_DELAY_MS and
            // the ad-ready wait below, and never blocks navigationEvent.
            installReferrerManager.maybeSendInstallReferrer()

            // Read isFirstLaunch BEFORE initRewards() - initRewards() is what flips the
            // underlying flag to false on a fresh install, so the order matters.
            val isFirstLaunch = rewardsRepository.isFirstLaunch()
            Log.d(TAG, "startTimer: isFirstLaunch=$isFirstLaunch")
            rewardsRepository.initRewards()
            delay(AnimationConstants.SPLASH_DELAY_MS)

            // Confirmed via AdDebug logs across multiple cold starts: even with preload()
            // starting from Application.onCreate() (the earliest possible point), the ad
            // consistently takes ~3.7-4.6s to actually load while SPLASH_DELAY_MS only gives
            // it ~3.2s in practice - a real, measured gap, not noise. Rather than padding
            // every launch by 2s, only wait here in the specific case where the ad genuinely
            // isn't ready yet: bounded by APP_OPEN_AD_EXTRA_WAIT_MS, and returns the instant
            // isAdReady flips true rather than always waiting out the full cap.
            if (!appOpenAdManager.isAdReady.value) {
                val waitStartedAt = System.currentTimeMillis()
                val becameReady = withTimeoutOrNull(AnimationConstants.APP_OPEN_AD_EXTRA_WAIT_MS) {
                    appOpenAdManager.isAdReady.first { it }
                } != null
                Log.d(TAG, "post-delay extra wait: becameReady=$becameReady after ${System.currentTimeMillis() - waitStartedAt}ms")
            }

            Log.d(TAG, "post-delay: isAdReady=${appOpenAdManager.isAdReady.value} isFirstLaunch=$isFirstLaunch activity=${activity != null}")
            maybeShowAppOpenAd(activity, isFirstLaunch)

            _navigationEvent.emit(Unit)
        }
    }

    /**
     * Shows the app open ad and suspends until the attempt is actually over -
     * [AppOpenAdOutcome.Dismissed] or [AppOpenAdOutcome.NotAvailable] - so [startTimer] never
     * emits [navigationEvent] while the ad is still on screen. [AppOpenAdOutcome.Shown] alone
     * does NOT resume this coroutine.
     *
     * Skipped entirely (no added delay) when: this is the user's first-ever launch, there's
     * no Activity to show on, or no ad is currently loaded/ready.
     *
     * Cold-start-vs-resume is not tracked separately here: this screen is this app's NavHost
     * start destination and is popped with `inclusive = true` (no saved state) the instant it
     * navigates away, in a single-Activity app - so it structurally only ever runs once per
     * process. A background -> foreground resume never re-enters Splash or re-invokes
     * [startTimer]; only a genuine fresh process start does.
     */
    private suspend fun maybeShowAppOpenAd(activity: Activity?, isFirstLaunch: Boolean) {
        if (isFirstLaunch || activity == null || !appOpenAdManager.isAdReady.value) {
            Log.d(TAG, "maybeShowAppOpenAd: skipping (isFirstLaunch=$isFirstLaunch, activity=${activity != null}, isAdReady=${appOpenAdManager.isAdReady.value})")
            return
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            appOpenAdManager.show(activity, AdConstants.APP_OPEN_UNIT_ID) { outcome ->
                Log.d(TAG, "maybeShowAppOpenAd: outcome=$outcome")
                when (outcome) {
                    AppOpenAdOutcome.Shown -> Unit
                    AppOpenAdOutcome.Dismissed, AppOpenAdOutcome.NotAvailable -> {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "AdDebug"
    }
}
