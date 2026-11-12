package uk.makemydaylondon.mmd.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.repository.AuthRepository
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.common.CODE_LENGTH
import uk.makemydaylondon.mmd.ui.common.CodeField
import uk.makemydaylondon.mmd.ui.common.FormViewModel
import uk.makemydaylondon.mmd.ui.common.GeneralError
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.PasswordField
import uk.makemydaylondon.mmd.ui.common.ReadableColumn
import uk.makemydaylondon.mmd.ui.common.SubmitButton

/**
 * Deletes the account and everything in it (required by Google Play).
 * Confirmed with the password, or a code emailed to the account for people
 * who sign in with Google or a passkey.
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val auth: AuthRepository,
    session: SessionManager,
) : FormViewModel<Unit>() {

    /** Read once: the session is cleared when the deletion succeeds */
    val email: String? = (session.state.value as? SessionState.SignedIn)?.account?.email

    /** The server's "We've emailed you a code", once a code has been sent */
    private val _codeSentMessage = MutableStateFlow<String?>(null)
    val codeSentMessage: StateFlow<String?> = _codeSentMessage.asStateFlow()

    fun sendCode() = submit {
        _codeSentMessage.value = auth.sendDeletionCode()
        null
    }

    fun deleteWithPassword(password: String) = submit { auth.deleteAccount(password = password) }

    fun deleteWithCode(code: String) = submit { auth.deleteAccount(code = code) }
}

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onDeleted: () -> Unit, viewModel: DeleteAccountViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleted by viewModel.outcome.collectAsStateWithLifecycle()
    val codeSentMessage by viewModel.codeSentMessage.collectAsStateWithLifecycle()
    var useCode by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val canDelete = if (useCode) code.length == CODE_LENGTH else password.isNotEmpty()

    MmdScaffold(stringResource(R.string.delete_account_title), onBack = onBack) { modifier ->
        ReadableColumn(modifier) {
            Text(stringResource(R.string.delete_account_warning))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = useCode == option,
                        onClick = {
                            useCode = option
                            viewModel.clearError()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(stringResource(if (option) R.string.delete_with_code else R.string.delete_with_password)) }
                }
            }
            Text(stringResource(R.string.delete_with_code_help), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!useCode) {
                PasswordField(password, { password = it }, error)
            } else if (codeSentMessage == null) {
                viewModel.email?.let { Text(stringResource(R.string.delete_code_will_be_sent, it)) }
                SubmitButton(stringResource(R.string.action_email_me_a_code), busy, enabled = true, onClick = viewModel::sendCode)
            } else {
                codeSentMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                CodeField(code, { code = it }, error)
                TextButton(onClick = {
                    code = ""
                    viewModel.sendCode()
                }, enabled = !busy) { Text(stringResource(R.string.action_send_new_code)) }
            }

            GeneralError(error, shownFields = listOf("password", "code"))
            Button(
                onClick = { confirming = true },
                enabled = canDelete && !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.delete_account_title)) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    if (useCode) viewModel.deleteWithCode(code) else viewModel.deleteWithPassword(password)
                }) { Text(stringResource(R.string.delete_account_title), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (deleted != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.deleted_title)) },
            text = { Text(stringResource(R.string.deleted_message)) },
            confirmButton = { TextButton(onClick = onDeleted) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}
