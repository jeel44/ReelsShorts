package reelsdrama.freedrama.videosdrama.core.constants

/**
 * Constants used for animations across the application.
 */
object AnimationConstants {
    const val SHIMMER_DURATION = 1200
    const val FADE_DURATION_MS = 250
    const val SPLASH_DELAY_MS = 2500L
    const val LIKE_ANIM_DURATION_MS = 650L
    const val DOUBLE_TAP_TIMEOUT_MS = 300L
    /**
     * Extra bounded wait, on top of [SPLASH_DELAY_MS], for the app open ad to finish loading
     * before Splash gives up and proceeds without it. Only actually waited out when the ad
     * genuinely isn't ready yet at the end of [SPLASH_DELAY_MS] - see
     * [reelsdrama.freedrama.videosdrama.presentation.splash.SplashViewModel.startTimer].
     */
    const val APP_OPEN_AD_EXTRA_WAIT_MS = 2000L
    /** Loop duration for [reelsdrama.freedrama.videosdrama.presentation.components.AdBannerSkeletonLoader]'s shimmer sweep. */
    const val AD_SKELETON_SHIMMER_DURATION_MS = 1400L
}
