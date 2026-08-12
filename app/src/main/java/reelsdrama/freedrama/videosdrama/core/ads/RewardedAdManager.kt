package reelsdrama.freedrama.videosdrama.core.ads

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of a single rewarded-ad show attempt. [Earned] is the only outcome that should
 * ever be used to grant a reward - every other outcome means the user did not complete the
 * ad and nothing should be unlocked/credited.
 */
sealed interface RewardedAdOutcome {
    /** The user watched the ad to completion; the SDK-reported reward should be granted. */
    data class Earned(val reward: RewardItem) : RewardedAdOutcome

    /** The user closed the ad before it finished playing - no reward earned. */
    data object DismissedWithoutReward : RewardedAdOutcome

    /** No ad was loaded for this placement, or it failed to show. */
    data object NotAvailable : RewardedAdOutcome
}

/**
 * Loads and shows GMA Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk`) rewarded
 * ads, one at a time per ad unit ID.
 *
 * This is a thin, generic wrapper around [RewardedAd.load]/[RewardedAd.show]: every rewarded
 * placement (coin-gate unlock, "Watch & Earn", etc.) goes through the same load/show/reload
 * cycle, keyed by its own ad unit ID from [reelsdrama.freedrama.videosdrama.core.constants.AdConstants].
 * Callers decide what to do with the [RewardedAdOutcome] - this class never grants a reward
 * itself, it only reports what happened.
 *
 * Callers must not invoke [preload] or [show] until [AdInitializer.isInitialized] is true.
 */
@Singleton
class RewardedAdManager @Inject constructor() {

    private val loadedAds = mutableMapOf<String, RewardedAd>()
    private val loadingUnitIds = mutableSetOf<String>()

    private val _readyAdUnitIds = MutableStateFlow<Set<String>>(emptySet())

    /** Ad unit IDs that currently have a loaded, unshown ad ready to display. */
    val readyAdUnitIds: StateFlow<Set<String>> = _readyAdUnitIds.asStateFlow()

    /** True if [adUnitId] currently has a loaded ad ready to show right now. */
    fun isAdReady(adUnitId: String): Boolean = loadedAds.containsKey(adUnitId)

    /**
     * Starts loading a rewarded ad for [adUnitId] if one isn't already loaded or currently
     * loading. Safe to call repeatedly (e.g. every time a screen appears) - it's a no-op if
     * a load is already in flight or an ad is already cached and waiting to be shown.
     */
    fun preload(adUnitId: String) {
        if (loadedAds.containsKey(adUnitId) || loadingUnitIds.contains(adUnitId)) return
        loadingUnitIds += adUnitId

        RewardedAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingUnitIds -= adUnitId
                    loadedAds[adUnitId] = ad
                    _readyAdUnitIds.update { it + adUnitId }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loadingUnitIds -= adUnitId
                    loadedAds.remove(adUnitId)
                    _readyAdUnitIds.update { it - adUnitId }
                }
            },
        )
    }

    /**
     * Shows the loaded rewarded ad for [adUnitId] (if any) on [activity] and reports what
     * happened via [onOutcome]. Immediately reports [RewardedAdOutcome.NotAvailable] and
     * does nothing else if no ad is currently loaded for [adUnitId] - it does NOT
     * auto-trigger a load, callers are expected to already be preloading.
     *
     * Whether the ad is dismissed (with or without a reward) or fails to show, this always
     * starts loading a fresh ad for [adUnitId] afterwards so the next attempt isn't stuck on
     * a stale/consumed ad.
     */
    fun show(activity: Activity, adUnitId: String, onOutcome: (RewardedAdOutcome) -> Unit) {
        val ad = loadedAds.remove(adUnitId)
        _readyAdUnitIds.update { it - adUnitId }

        if (ad == null) {
            onOutcome(RewardedAdOutcome.NotAvailable)
            return
        }

        var earnedReward = false

        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {}

            override fun onAdDismissedFullScreenContent() {
                if (!earnedReward) {
                    onOutcome(RewardedAdOutcome.DismissedWithoutReward)
                }
                // The ad we just showed is consumed either way - line up a fresh one.
                preload(adUnitId)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                onOutcome(RewardedAdOutcome.NotAvailable)
                preload(adUnitId)
            }

            override fun onAdImpression() {}

            override fun onAdClicked() {}
        }

        ad.show(
            activity,
            object : OnUserEarnedRewardListener {
                override fun onUserEarnedReward(reward: RewardItem) {
                    earnedReward = true
                    onOutcome(RewardedAdOutcome.Earned(reward))
                }
            },
        )
    }
}
