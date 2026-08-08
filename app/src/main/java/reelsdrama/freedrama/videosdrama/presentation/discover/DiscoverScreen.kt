package reelsdrama.freedrama.videosdrama.presentation.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.data.fake.FakeDiscoverRepository
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
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
    }
}
