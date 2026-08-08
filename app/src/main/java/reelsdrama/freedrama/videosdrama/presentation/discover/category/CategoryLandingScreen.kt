package reelsdrama.freedrama.videosdrama.presentation.discover.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import reelsdrama.freedrama.videosdrama.R
import reelsdrama.freedrama.videosdrama.data.fake.FakeDiscoverRepository
import reelsdrama.freedrama.videosdrama.presentation.discover.category.components.CategoryHeader
import reelsdrama.freedrama.videosdrama.presentation.discover.category.components.CategorySection
import reelsdrama.freedrama.videosdrama.presentation.discover.category.components.ContinueWatchingCard
import reelsdrama.freedrama.videosdrama.presentation.discover.category.components.FeaturedBanner

@Composable
fun CategoryLandingScreen(
    categoryId: String,
    onBackClick: () -> Unit,
    onPosterClick: (String) -> Unit
) {
    val repository = remember { FakeDiscoverRepository() }
    val sections = remember { repository.getDiscoverSections() }
    val dramas = sections[categoryId] ?: emptyList()
    
    // Derived sub-sections for the landing page
    val featured = dramas.firstOrNull()
    val continueWatching = dramas.take(2)
    val trending = dramas.takeLast(5)
    val newArrivals = dramas.shuffled().take(5)
    val popular = dramas.shuffled().take(5)
    val recommended = dramas.shuffled().take(5)

    Scaffold(
        topBar = {
            CategoryHeader(
                title = categoryId,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Featured Banner
            featured?.let {
                item {
                    FeaturedBanner(
                        drama = it,
                        onDramaClick = { onPosterClick(categoryId) }
                    )
                }
            }

            // 2. Continue Watching (UI Only)
            if (continueWatching.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.category_continue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(continueWatching) { drama ->
                                ContinueWatchingCard(
                                    drama = drama,
                                    progress = 0.4f,
                                    onDramaClick = { onPosterClick(categoryId) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Trending
            item {
                CategorySection(
                    title = stringResource(R.string.category_trending),
                    dramas = trending,
                    onDramaClick = { onPosterClick(categoryId) }
                )
            }

            // 4. New Arrivals
            item {
                CategorySection(
                    title = stringResource(R.string.category_new),
                    dramas = newArrivals,
                    onDramaClick = { onPosterClick(categoryId) }
                )
            }

            // 5. Popular
            item {
                CategorySection(
                    title = stringResource(R.string.category_popular),
                    dramas = popular,
                    onDramaClick = { onPosterClick(categoryId) }
                )
            }

            // 6. Recommended
            item {
                CategorySection(
                    title = stringResource(R.string.category_recommended),
                    dramas = recommended,
                    onDramaClick = { onPosterClick(categoryId) }
                )
            }
        }
    }
}
