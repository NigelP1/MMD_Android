package uk.makemydaylondon.mmd.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.network.ApiException

/**
 * A form that sends one request at a time. [busy] and [error] drive the
 * button and messages; [outcome] is set when the form has done its job (the
 * screen acts on it, e.g. moves on, then calls [consumeOutcome]).
 */
abstract class FormViewModel<T : Any> : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<ApiException?>(null)
    val error: StateFlow<ApiException?> = _error.asStateFlow()

    private val _outcome = MutableStateFlow<T?>(null)
    val outcome: StateFlow<T?> = _outcome.asStateFlow()

    fun consumeOutcome() {
        _outcome.value = null
    }

    fun clearError() {
        _error.value = null
    }

    /** Runs [block] unless a request is already in flight; its result becomes the outcome. */
    protected fun submit(block: suspend () -> T?) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                block()?.let { _outcome.value = it }
            } catch (e: ApiException) {
                _error.value = e
            } finally {
                _busy.value = false
            }
        }
    }
}
