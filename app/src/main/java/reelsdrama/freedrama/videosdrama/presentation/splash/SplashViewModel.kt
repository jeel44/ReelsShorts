package reelsdrama.freedrama.videosdrama.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.constants.AnimationConstants
import reelsdrama.freedrama.videosdrama.domain.repository.RewardsRepository
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun startTimer() {
        viewModelScope.launch {
            rewardsRepository.initRewards()
            delay(AnimationConstants.SPLASH_DELAY_MS)
            _navigationEvent.emit(Unit)
        }
    }
}
