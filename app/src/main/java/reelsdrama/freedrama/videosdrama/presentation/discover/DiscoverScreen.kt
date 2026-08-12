package reelsdrama.freedrama.videosdrama.presentation.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import reelsdrama.freedrama.videosdrama.data.fake.FakeDiscoverRepository
import reelsdrama.freedrama.videosdrama.presentation.components.AdBannerView
import reelsdrama.freedrama.videosdrama.presentation.discover.components.DiscoverSection
import reelsdrama.freedrama.videosdrama.presentation.discover.components.SearchHeader

@Composable
fun DiscoverScreen(
    onDramaClick: (String) -> Unit
) {
    // In a real production app, this would be provided via ViewModel.
    // For this UI foundation phase, we use the fake repository directly.
    val repository = remember { FakeDiscoverRepository() }
    val sections = remember { repository.getDiscoverSections() }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchHeader()

        // weight(1f) lets this scroll independently while AdBannerView stays fixed below it.
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(sections.toList()) { (title, dramas) ->
                DiscoverSection(
                    title = title,
                    dramas = dramas,
                    onDramaClick = onDramaClick
                )
            }
        }

        // Fixed at the bottom, above the bottom nav bar (MainScreen's outer Scaffold already
        // insets this whole screen above it). Collapses to zero height if no ad loads.
        AdBannerView(adUnitId = AdConstants.BANNER_FALLBACK_UNIT_ID)
    }
}
