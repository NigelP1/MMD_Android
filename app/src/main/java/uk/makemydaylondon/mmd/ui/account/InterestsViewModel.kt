package uk.makemydaylondon.mmd.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.model.Interest
import uk.makemydaylondon.mmd.data.model.Profile
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.MeRepository
import uk.makemydaylondon.mmd.di.ApplicationScope
import uk.makemydaylondon.mmd.ui.common.LoadState

/**
 * Interests and email preferences. Changes save shortly after the last one,
 * so tapping several interests in a row sends one request.
 */
@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val me: MeRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /** What's on screen */
    private val _state = MutableStateFlow<LoadState<Profile>>(LoadState.Loading)
    val state: StateFlow<LoadState<Profile>> = _state.asStateFlow()

    private val _saveError = MutableStateFlow<ApiException?>(null)
    val saveError: StateFlow<ApiException?> = _saveError.asStateFlow()

    /** What the server has */
    private var stored: Profile? = null
    private var saveJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            _state.value = try {
                me.profile().also { stored = it }.let { LoadState.Loaded(it) }
            } catch (e: ApiException) {
                LoadState.Failed(e)
            }
        }
    }

    fun setInterest(interest: Interest, on: Boolean) = edit {
        val interests = if (on) it.interests + interest else it.interests - interest
        // The map filter needs at least one interest
        it.copy(interests = interests, onlyMyInterests = it.onlyMyInterests && interests.isNotEmpty())
    }

    fun setOnlyMyInterests(on: Boolean) = edit { it.copy(onlyMyInterests = on && it.interests.isNotEmpty()) }

    fun setEmailNewEvents(on: Boolean) = edit { it.copy(emailNewEvents = on) }

    fun setEmailEndingReminders(on: Boolean) = edit { it.copy(emailEndingReminders = on) }

    private fun edit(change: (Profile) -> Profile) {
        val current = (_state.value as? LoadState.Loaded)?.value ?: return
        _state.value = LoadState.Loaded(change(current))
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_PAUSE_MS)
            save()
        }
    }

    private suspend fun save() {
        val profile = (_state.value as? LoadState.Loaded)?.value ?: return
        if (profile == stored) return
        try {
            stored = me.updateProfile(profile)
            _saveError.value = null
        } catch (e: ApiException) {
            _saveError.value = e
        }
    }

    /** Leaving during the pause cancels the save above, so finish it outside the screen */
    override fun onCleared() {
        val profile = (_state.value as? LoadState.Loaded)?.value
        if (profile != null && profile != stored) {
            appScope.launch { runCatching { me.updateProfile(profile) } }
        }
    }

    private companion object {
        const val SAVE_PAUSE_MS = 600L
    }
}
