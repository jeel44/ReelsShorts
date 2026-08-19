package reelsdrama.freedrama.videosdrama.presentation.home.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import reelsdrama.freedrama.videosdrama.domain.model.FeedConfig
import reelsdrama.freedrama.videosdrama.domain.repository.FeedConfigRepository
import javax.inject.Inject

/** Which of the two sibling feeds [ReelsFeedScreen] can show. */
enum class FeedTab(val label: String) {
    Videos("Videos"),
    Stories("Stories")
}

/**
 * @property config The current remote `/feedConfig` visibility toggles - see [FeedTabViewModel].
 * @property selectedTab The tab [ReelsFeedScreen] should currently render. Only meaningful when
 * at least one of [FeedConfig.videosEnabled]/[FeedConfig.storiesEnabled] is true - see
 * [bothDisabled].
 */
data class FeedTabUiState(
    val config: FeedConfig = FeedConfig(),
    val selectedTab: FeedTab = FeedTab.Videos
) {
    /**
     * True only when neither feed is enabled - a misconfiguration (the defaults are both `true`,
     * so this can't happen without someone deliberately setting both to `false` in Firebase).
     * [ReelsFeedScreen] shows an explicit empty state instead of either pager when this is true,
     * rather than falling back to rendering a feed the config says shouldn't be visible.
     */
    val bothDisabled: Boolean get() = !config.videosEnabled && !config.storiesEnabled
}

/**
 * Owns the Home screen's Videos/Stories tab selection AND the remote `/feedConfig` visibility
 * toggles that govern it.
 *
 * Neither [FeedViewModel] nor [StoriesViewModel] is a natural owner for this: both are
 * independent sibling ViewModels, one per feed, and which tab is selected / whether the switcher
 * shows at all is cross-feed UI state that belongs to neither of them specifically - it's a
 * concern of the screen that hosts both, not of either feed's own data/coin/ad plumbing. This is
 * a small screen-scoped `HiltViewModel` for exactly that, obtained via `hiltViewModel()` in
 * [reelsdrama.freedrama.videosdrama.presentation.home.HomeRoute] the same way [FeedViewModel]/
 * [StoriesViewModel] already are, and threaded down into [ReelsFeedScreen] as plain state - the
 * same pattern that route already uses for those two.
 */
@HiltViewModel
class FeedTabViewModel @Inject constructor(
    feedConfigRepository: FeedConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedTabUiState())
    val uiState: StateFlow<FeedTabUiState> = _uiState.asStateFlow()

    init {
        feedConfigRepository.getFeedConfig()
            .onEach { config -> applyFeedConfig(config) }
            .launchIn(viewModelScope)
    }

    /**
     * Re-derives [FeedTabUiState.selectedTab] against every new [config] - not just once at
     * startup - so a live remote toggle actually reaches a running app, same as
     * [reelsdrama.freedrama.videosdrama.domain.repository.AdConfigRepository]'s own live-update
     * contract. If the currently-selected tab just became disabled, this moves the user to
     * whichever tab IS enabled instead of leaving them on a hidden/broken one - e.g. a user on
     * Stories when `storiesEnabled` flips to `false` remotely is moved to Videos automatically.
     * When both are enabled, the user's own selection is left untouched (this isn't a reason to
     * ever bounce someone off a tab they're already validly on). When both are disabled, the
     * previous [FeedTabUiState.selectedTab] is left as-is too - it's not rendered at all in that
     * case (see [FeedTabUiState.bothDisabled]), so there's nothing meaningful to pick, and
     * leaving it means whichever tab reappears is whatever was last valid once config recovers.
     */
    private fun applyFeedConfig(config: FeedConfig) {
        _uiState.update { state ->
            val selectedTab = when {
                config.videosEnabled && config.storiesEnabled -> state.selectedTab
                config.videosEnabled -> FeedTab.Videos
                config.storiesEnabled -> FeedTab.Stories
                else -> state.selectedTab
            }
            state.copy(config = config, selectedTab = selectedTab)
        }
    }

    /** User-driven tab switch, from [ReelsFeedScreen]'s `FeedTabSwitcher` tap. */
    fun onTabSelected(tab: FeedTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
