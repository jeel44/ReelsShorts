package reelsdrama.freedrama.videosdrama.presentation.splash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdManager
import reelsdrama.freedrama.videosdrama.core.ads.AppOpenAdOutcome
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository,
    private val adInitializer: AdInitializer,
    private val appOpenAdManager: AppOpenAdManager
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        preloadAppOpenAdWhenReady()
    }

    private fun preloadAppOpenAdWhenReady() {
        viewModelScope.launch {
            adInitializer.isInitialized.first { it }
            appOpenAdManager.preload(AdConstants.APP_OPEN_UNIT_ID)
        }
    }

    /**
     * @param activity Needed only in case an app open ad ends up eligible and ready to show -
     * never stored beyond this call.
     */
    fun startTimer(activity: Activity?) {
        viewModelScope.launch {
            // Read isFirstLaunch BEFORE initRewards() - initRewards() is what flips the
            // underlying flag to false on a fresh install, so the order matters.
            val isFirstLaunch = rewardsRepository.isFirstLaunch()
            rewardsRepository.initRewards()
            delay(AnimationConstants.SPLASH_DELAY_MS)

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
        if (isFirstLaunch || activity == null || !appOpenAdManager.isAdReady.value) return

        suspendCancellableCoroutine<Unit> { continuation ->
            appOpenAdManager.show(activity, AdConstants.APP_OPEN_UNIT_ID) { outcome ->
                when (outcome) {
                    AppOpenAdOutcome.Shown -> Unit
                    AppOpenAdOutcome.Dismissed, AppOpenAdOutcome.NotAvailable -> {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
    }
}
