package reelsdrama.freedrama.videosdrama.core.notifications

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.constants.OneSignalConstants
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foundation layer for OneSignal push-notification integration - the OneSignal sibling of
 * [reelsdrama.freedrama.videosdrama.core.ads.AdInitializer], same "init once, expose readiness"
 * shape. Centralizes every direct OneSignal SDK call, per OneSignal's own Android AI-integration
 * guide (https://github.com/OneSignal/sdk-ai-prompts, `docs/android/ai-prompt.md`, "Step 4 -
 * Centralized OneSignal Integration"): nothing outside this class should call `com.onesignal.*`
 * directly.
 *
 * Also implements that same guide's "Push Subscription Verification Dialog" requirement (see
 * [setupPushSubscriptionObserver]) - the ONLY place this app requests push (POST_NOTIFICATIONS)
 * permission, per the guide's explicit constraint against prompting anywhere else.
 *
 * Uses `android.app.AlertDialog`, not the guide's own `androidx.appcompat.app.AlertDialog`
 * example - this project has no AppCompat dependency/theme anywhere (a Compose-only app,
 * `MainActivity` is a bare `ComponentActivity`), and the framework class exposes the exact same
 * Builder API the guide's example actually uses (setTitle/setMessage/setPositiveButton/
 * setCancelable/show), so no new dependency is needed for this one dialog.
 */
@Singleton
class OneSignalManager @Inject constructor(
    @ApplicationContext private val context: Context,
    // Same shared DataStore<Preferences> instance SettingsModule already provides as a
    // @Singleton (also consumed directly by RewardsRepositoryImpl) - injected straight into
    // this class rather than routed through RewardsRepository or a new abstraction. The
    // one preference this class owns (ONESIGNAL_DIALOG_SHOWN below) has nothing to do with
    // the rewards/coins domain RewardsRepository's interface is scoped to, and this project
    // has no separate "AppPreferences"-style repository today - mirroring
    // RewardsRepositoryImpl/SettingsRepositoryImpl's own constructor shape (both take
    // DataStore<Preferences> directly) is the smallest change consistent with how this
    // codebase already does DataStore-backed persistence.
    private val dataStore: DataStore<Preferences>
) {
    // App-process-lifetime scope, same idiom as AdInitializer.initScope - initialization
    // happens once and this is a Hilt singleton, so there's no owner to cancel this against.
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Separate main-thread scope for requestPushPermission below - it's only ever invoked from
    // the confirmation dialog's button callback, not from a suspend context.
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var hasStarted = false
    private val observerSetUp = AtomicBoolean(false)

    // In-memory only, deliberately NOT the source of truth for "has the user ever been shown
    // (and acknowledged) this dialog" anymore - that's now [PreferencesKeys.ONESIGNAL_DIALOG_SHOWN]
    // in DataStore, which survives across separate app launches. This AtomicBoolean's remaining
    // job is narrower: dedupe the dialog *within a single process*, since
    // maybeShowIntegrationCompleteDialog can legitimately be reached twice in quick succession
    // for the same already-registered subscription ID - setupPushSubscriptionObserver's own
    // immediate current-value check (line ~124) runs unconditionally on every call (e.g. Activity
    // recreation on a config change calling setupPushSubscriptionObserver again), independent of
    // observerSetUp's guard on the observer registration itself. Without this, both call sites
    // could race past the persisted-flag check (both see "not yet acknowledged", since it's only
    // written on the "Got it" tap, not the moment the dialog is decided to show) and each build
    // and show their own dialog.
    private val dialogShown = AtomicBoolean(false)

    private object PreferencesKeys {
        val ONESIGNAL_DIALOG_SHOWN = booleanPreferencesKey("onesignal_dialog_shown")
    }

    // OneSignal stores push subscription observers weakly (see setupPushSubscriptionObserver's
    // doc comment) - held here, not as a local variable, so it survives for this Hilt
    // singleton's (i.e. the process's) lifetime rather than being garbage-collected right after
    // registration.
    private var pushSubscriptionObserver: IPushSubscriptionObserver? = null

    private val _isInitialized = MutableStateFlow(false)

    /** True once [OneSignal.initWithContext] has completed. */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Kicks off OneSignal SDK initialization on a background thread - mirrors
     * [reelsdrama.freedrama.videosdrama.core.ads.AdInitializer.initialize]'s shape/rationale.
     * Safe to call more than once - only the first call actually initializes. Must complete
     * before any other OneSignal call - callers of [setupPushSubscriptionObserver] should wait
     * on [isInitialized] first.
     */
    fun initialize() {
        if (hasStarted) return
        hasStarted = true

        initScope.launch {
            // Verbose logging for debugging, per the integration guide's own threading-model
            // example - drop this (or lower it) before a real production release.
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
            OneSignal.initWithContext(context, OneSignalConstants.ONESIGNAL_APP_ID)
            Log.d(TAG, "OneSignal.initWithContext completed")
            _isInitialized.value = true
        }
    }

    /**
     * Required "Push Subscription Verification Dialog" flow from OneSignal's integration guide
     * (see this class's own doc comment for the link) - registers a push-subscription observer
     * and shows a one-time confirmation dialog the first time this device gets a real,
     * server-assigned subscription ID, as opposed to the `local-`-prefixed placeholder
     * [OneSignal.initWithContext] assigns immediately, before the device has actually
     * registered with OneSignal's servers.
     *
     * [context] must be an Activity context (a [android.app.Dialog] needs a window to attach
     * to) and this must not be called before [isInitialized] is true.
     *
     * Idempotent: safe to call again (e.g. after Activity recreation on a config change) -
     * only the first call actually registers the observer, guarded by [observerSetUp]. The
     * confirmation dialog itself is never shown more than once *ever* (not just per-process) -
     * gated on the persisted `onesignal_dialog_shown` DataStore flag (set only once the user
     * actually taps "Got it"), with [dialogShown] as a secondary same-process guard against
     * this method's two call sites racing each other before that flag is written.
     */
    fun setupPushSubscriptionObserver(context: Context) {
        if (observerSetUp.compareAndSet(false, true)) {
            val observer = object : IPushSubscriptionObserver {
                override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                    maybeShowIntegrationCompleteDialog(context, state.current.id)
                }
            }
            pushSubscriptionObserver = observer
            OneSignal.User.pushSubscription.addObserver(observer)
        }

        // The ID may already be server-assigned before the observer attaches (or before this
        // particular call, e.g. on a later Activity instance after recreation) - evaluate the
        // current value too, not just the change event, or that transition could be missed.
        maybeShowIntegrationCompleteDialog(context, OneSignal.User.pushSubscription.id)
    }

    /** A real, server-assigned subscription ID is non-empty and not the `local-` placeholder. */
    private fun isRegistered(subscriptionId: String?): Boolean =
        !subscriptionId.isNullOrEmpty() && !subscriptionId.startsWith("local-")

    /**
     * Called from a non-suspend context both times (an [IPushSubscriptionObserver] callback,
     * and directly from [setupPushSubscriptionObserver]) - the persisted-flag read below needs
     * a coroutine, so this launches on [initScope] (already this class's process-lifetime
     * IO-dispatched scope, same one [initialize] uses) rather than requiring a suspend fun this
     * deep in an SDK callback chain.
     */
    private fun maybeShowIntegrationCompleteDialog(context: Context, subscriptionId: String?) {
        if (!isRegistered(subscriptionId)) return

        initScope.launch {
            val alreadyAcknowledged =
                dataStore.data.first()[PreferencesKeys.ONESIGNAL_DIALOG_SHOWN] == true
            if (alreadyAcknowledged) {
                // TEMP DIAGNOSTIC (OneSignalDebug)
                Log.d(TAG, "maybeShowIntegrationCompleteDialog: already acknowledged on a prior launch, skipping")
                return@launch
            }

            // Same-process dedupe only - see dialogShown's own doc comment above.
            if (!dialogShown.compareAndSet(false, true)) return@launch

            // TEMP DIAGNOSTIC (OneSignalDebug)
            Log.d(TAG, "maybeShowIntegrationCompleteDialog: showing dialog for subscriptionId=$subscriptionId")
            Handler(Looper.getMainLooper()).post {
                AlertDialog.Builder(context)
                    .setTitle("Your OneSignal SDK integration is complete!")
                    .setMessage(
                        "You can now send Push Notifications & In-App Messages through OneSignal. " +
                            "Tap below to enable push notifications."
                    )
                    .setPositiveButton("Got it") { _, _ ->
                        // TEMP DIAGNOSTIC (OneSignalDebug) - confirms the onClick actually fires
                        // and calls requestPushPermission(), exactly once.
                        Log.d(TAG, "maybeShowIntegrationCompleteDialog: 'Got it' onClick fired, calling requestPushPermission()")
                        requestPushPermission()

                        // Persisted only on acknowledgement, not the moment the dialog was shown -
                        // a user who kills the app before tapping "Got it" must still see it again
                        // on their next launch, since they never actually confirmed anything.
                        initScope.launch {
                            dataStore.edit { it[PreferencesKeys.ONESIGNAL_DIALOG_SHOWN] = true }
                        }
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    /**
     * The ONLY place push permission is requested, per the integration guide's explicit
     * constraint ("do NOT prompt for permission at app launch or anywhere else") - fired from
     * the confirmation dialog's "Got it" button above. [OneSignal.Notifications.requestPermission]
     * shows Android 13+'s system POST_NOTIFICATIONS prompt itself; no separate
     * `ActivityResultContracts.RequestPermission` launcher is used (or wanted - see this class's
     * own doc comment on centralizing every OneSignal call here, and the constraint above).
     */
    private fun requestPushPermission() {
        mainScope.launch {
            // TEMP DIAGNOSTIC (OneSignalDebug) - confirms this coroutine body actually starts
            // executing (rules out mainScope being cancelled/never scheduled before this runs).
            Log.d(TAG, "requestPushPermission: coroutine body started")

            val permBefore = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            Log.d(
                TAG,
                "requestPushPermission: checkSelfPermission BEFORE=$permBefore " +
                    "(GRANTED=${PackageManager.PERMISSION_GRANTED}, DENIED=${PackageManager.PERMISSION_DENIED}) " +
                    "- about to call OneSignal.Notifications.requestPermission(true)"
            )

            val result = OneSignal.Notifications.requestPermission(true)
            Log.d(TAG, "requestPushPermission: OneSignal.Notifications.requestPermission(true) returned $result")

            val permAfter = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            Log.d(
                TAG,
                "requestPushPermission: checkSelfPermission AFTER=$permAfter " +
                    "(GRANTED=${PackageManager.PERMISSION_GRANTED}, DENIED=${PackageManager.PERMISSION_DENIED})"
            )
        }
    }

    private companion object {
        const val TAG = "OneSignalDebug"
    }
}
