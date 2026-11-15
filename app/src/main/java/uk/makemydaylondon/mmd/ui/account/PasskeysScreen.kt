package uk.makemydaylondon.mmd.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.model.Passkey
import uk.makemydaylondon.mmd.data.model.PasskeyChallenge
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.network.MmdApi
import uk.makemydaylondon.mmd.data.repository.MeRepository
import uk.makemydaylondon.mmd.ui.common.ErrorText
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.FormViewModel
import uk.makemydaylondon.mmd.ui.common.GeneralError
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableWidth

/** The account's passkeys: adding (through the passkey sheet) and removing. */
@HiltViewModel
class PasskeysViewModel @Inject constructor(private val me: MeRepository) : FormViewModel<Unit>() {

    private val _passkeys = MutableStateFlow<LoadState<List<Passkey>>>(LoadState.Loading)
    val passkeys: StateFlow<LoadState<List<Passkey>>> = _passkeys.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _passkeys.value = try {
                LoadState.Loaded(me.passkeys())
            } catch (e: ApiException) {
                LoadState.Failed(e)
            }
        }
    }

    /** The server's challenge for the passkey sheet, or null (with [error] set) if it couldn't be had */
    suspend fun registrationChallenge(): PasskeyChallenge? {
        clearError()
        return try {
            me.passkeyRegistrationOptions()
        } catch (e: ApiException) {
            fail(e)
            null
        }
    }

    fun register(challenge: PasskeyChallenge, credentialJson: String) = submit {
        me.registerPasskey(challenge.challengeId, credentialJson, MmdApi.DEVICE_NAME)
        refreshList()
        null
    }

    fun delete(passkey: Passkey) = submit {
        me.deletePasskey(passkey.id)
        refreshList()
        null
    }

    private suspend fun refreshList() {
        _passkeys.value = LoadState.Loaded(me.passkeys())
    }
}

@Composable
fun PasskeysScreen(onBack: () -> Unit, viewModel: PasskeysViewModel = hiltViewModel()) {
    val passkeys by viewModel.passkeys.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var problem by rememberSaveable { mutableStateOf<Int?>(null) }
    var removing by remember { mutableStateOf<Passkey?>(null) }

    val add: () -> Unit = {
        problem = null
        adding = true
        scope.launch {
            viewModel.registrationChallenge()?.let { challenge ->
                when (val result = PasskeyCredentials.create(context, challenge.options)) {
                    is PasskeyResult.Success -> viewModel.register(challenge, result.json)
                    PasskeyResult.Cancelled -> Unit
                    PasskeyResult.AlreadyAdded -> problem = R.string.passkey_already_added
                    PasskeyResult.NoProvider -> problem = R.string.passkey_no_provider
                    else -> problem = R.string.passkey_failed
                }
            }
            adding = false
        }
    }

    MmdScaffold(stringResource(R.string.passkeys_title), onBack = onBack) { modifier ->
        when (val s = passkeys) {
            LoadState.Loading -> CenteredProgress(modifier)
            is LoadState.Failed -> ErrorMessage(s.error, onRetry = viewModel::load, modifier = modifier)
            is LoadState.Loaded -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) {
                    item { Footnote(stringResource(R.string.passkeys_intro), Modifier.padding(vertical = 8.dp)) }
                    if (s.value.isEmpty()) {
                        item { Footnote(stringResource(R.string.passkeys_none_yet)) }
                    }
                    items(s.value, key = { it.id }) { passkey ->
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.Key, contentDescription = null) },
                            headlineContent = { Text(passkey.name.ifBlank { stringResource(R.string.passkeys_title) }) },
                            supportingContent = { passkeyDates(passkey)?.let { Text(it) } },
                            trailingContent = {
                                IconButton(onClick = { removing = passkey }, enabled = !busy) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_remove_passkey, passkey.name))
                                }
                            },
                        )
                    }
                    item {
                        Button(
                            onClick = add,
                            enabled = !busy && !adding,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            if (busy || adding) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                            } else {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.action_add_passkey))
                        }
                    }
                    item {
                        problem?.let {
                            ErrorText(stringResource(it), Modifier.padding(horizontal = 16.dp))
                        }
                        GeneralError(error, shownFields = emptyList(), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    removing?.let { passkey ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.passkey_remove_title)) },
            text = { Text(stringResource(R.string.passkey_remove_message)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    viewModel.delete(passkey)
                }) { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** "Added 7 Nov 2026 · Last used 9 Nov 2026" */
@Composable
private fun passkeyDates(passkey: Passkey): String? {
    val added = formatDay(passkey.createdAt) ?: return null
    val used = passkey.lastUsedAt?.let(::formatDay)
    return if (used != null) stringResource(R.string.passkey_added_used, added, used) else stringResource(R.string.passkey_added, added)
}

private fun formatDay(iso: String): String? = runCatching {
    Instant.parse(iso).atZone(London.zone).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
}.getOrNull()
