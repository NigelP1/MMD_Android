package uk.makemydaylondon.mmd.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.settings.AppSettings

/** App-wide UI state: whether the welcome tips are showing. */
@HiltViewModel
class AppViewModel @Inject constructor(private val settings: AppSettings) : ViewModel() {

    private val requested = MutableStateFlow(false)

    /** Null until the setting has loaded; true on first launch or when asked for from Info */
    val showWelcome: StateFlow<Boolean?> = combine(settings.welcomeSeen, requested) { seen, asked -> !seen || asked }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun showWelcomeAgain() {
        requested.value = true
    }

    fun welcomeDone() {
        requested.value = false
        viewModelScope.launch { settings.setWelcomeSeen() }
    }
}
