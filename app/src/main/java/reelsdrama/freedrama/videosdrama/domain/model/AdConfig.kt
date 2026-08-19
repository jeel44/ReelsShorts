package reelsdrama.freedrama.videosdrama.domain.model

/**
 * Remotely-controlled toggle for which ad type(s) the reels feed's every-3-reels ad slot uses -
 * sourced from Firebase Realtime Database's `/adConfig` node (see
 * [reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository]), not Remote Config.
 *
 * Both flags are read together, not as independent per-slot switches - see
 * [reelsdrama.freedrama.videosdrama.presentation.home.feed.withAdSlots] for exactly how this
 * combination decides each slot's ad type:
 * - bannerEnabled=true, nativeEnabled=false -> every slot is a banner.
 * - bannerEnabled=false, nativeEnabled=true -> every slot is native (the original behavior,
 *   and this class's default - see the default values below for why).
 * - both true -> slots alternate banner/native/banner/native... in swipe order.
 * - both false -> no ad slot is inserted at all; the feed plays with no every-3 interruption.
 *
 * Defaults to the pre-existing native-only behavior (bannerEnabled=false, nativeEnabled=true)
 * rather than both-true or both-false, since this is what [AdConfigRepository] falls back to
 * before Firebase's first value arrives or if the read fails - matching what shipped before this
 * remote toggle existed means a cold start or a dropped connection never regresses monetization
 * (both false) or surprises the user with a placement never verified together (both true); it's
 * simply what the app already did.
 */
data class AdConfig(
    val bannerEnabled: Boolean = false,
    val nativeEnabled: Boolean = true
)
