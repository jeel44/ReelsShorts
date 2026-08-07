package reelsdrama.freedrama.videosdrama.domain.usecase

import reelsdrama.freedrama.videosdrama.domain.repository.VideoRepository
import javax.inject.Inject

class ObserveVideosUseCase @Inject constructor(
    private val repository: VideoRepository,
) {
    operator fun invoke() = repository.observeVideos()
}
