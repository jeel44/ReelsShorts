package reelsdrama.freedrama.videosdrama.core.constants

/**
 * OneSignal push-notification identifiers - the OneSignal sibling of [AdConstants] (which is
 * explicitly scoped to AdMob identifiers only, per its own doc comment, so this stays a
 * separate object rather than being folded into it). Centralizes every OneSignal identifier
 * used across the app so this is the only file that needs editing if the App ID ever changes.
 */
object OneSignalConstants {

    /**
     * OneSignal App ID, passed to `OneSignal.initWithContext()` - see
     * [reelsdrama.freedrama.videosdrama.core.notifications.OneSignalManager.initialize].
     */
    const val ONESIGNAL_APP_ID = "81d09876-f88d-466c-884d-0a8276388388"
}
