package uk.makemydaylondon.mmd.ui.account

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.ui.CodePurpose
import uk.makemydaylondon.mmd.ui.common.CODE_LENGTH
import uk.makemydaylondon.mmd.ui.common.CodeField
import uk.makemydaylondon.mmd.ui.common.FormField
import uk.makemydaylondon.mmd.ui.common.GeneralError
import uk.makemydaylondon.mmd.ui.common.MIN_PASSWORD_LENGTH
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.PasswordField
import uk.makemydaylondon.mmd.ui.common.ReadableColumn
import uk.makemydaylondon.mmd.ui.common.SubmitButton

/** Where the login form can lead */
class LoginActions(
    val onLoggedIn: () -> Unit,
    val onNeedsVerification: (email: String) -> Unit,
    val onForgotPassword: (email: String) -> Unit,
    val onRegister: () -> Unit,
)

/** The login form, pushed when something needs an account (e.g. saving an event). */
@Composable
fun LoginScreen(onBack: () -> Unit, actions: LoginActions) {
    MmdScaffold(stringResource(R.string.login_title), onBack = onBack) { modifier ->
        ReadableColumn(modifier) { LoginForm(actions) }
    }
}

/** Email and password login, and the ways to create an account or reset a password. */
@Composable
fun ColumnScope.LoginForm(actions: LoginActions, viewModel: LoginViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSubmit = email.isNotBlank() && password.isNotEmpty()

    LaunchedEffect(outcome) {
        when (val o = outcome) {
            LoginOutcome.LoggedIn -> actions.onLoggedIn()
            is LoginOutcome.NeedsVerification -> actions.onNeedsVerification(o.email)
            null -> return@LaunchedEffect
        }
        password = ""
        viewModel.consumeOutcome()
    }

    Text(stringResource(R.string.login_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
    FormField(
        value = email,
        onValueChange = { email = it },
        label = stringResource(R.string.field_email),
        error = error,
        field = "email",
        keyboardType = KeyboardType.Email,
        contentType = ContentType.EmailAddress + ContentType.Username,
    )
    PasswordField(
        value = password,
        onValueChange = { password = it },
        error = error,
        onDone = { viewModel.logIn(email, password) },
    )
    GeneralError(error, shownFields = listOf("email", "password"))
    SubmitButton(stringResource(R.string.action_log_in), busy, canSubmit, onClick = { viewModel.logIn(email, password) })
    TextButton(onClick = { actions.onForgotPassword(email.trim()) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_forgot_password))
    }
    HorizontalDivider()
    OutlinedButton(onClick = actions.onRegister, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_create_account))
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onCodeSent: (email: String) -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSubmit = firstName.isNotBlank() && email.isNotBlank() && password.length >= MIN_PASSWORD_LENGTH
    val submit = { viewModel.register(firstName, lastName, email, password) }

    LaunchedEffect(outcome) {
        outcome?.let {
            onCodeSent(it)
            viewModel.consumeOutcome()
        }
    }

    MmdScaffold(stringResource(R.string.register_title), onBack = onBack) { modifier ->
        ReadableColumn(modifier) {
            FormField(firstName, { firstName = it }, stringResource(R.string.field_first_name), error, "first_name", contentType = ContentType.PersonFirstName)
            FormField(lastName, { lastName = it }, stringResource(R.string.field_last_name_optional), error, "last_name", contentType = ContentType.PersonLastName)
            FormField(
                email, { email = it }, stringResource(R.string.field_email), error, "email",
                keyboardType = KeyboardType.Email, contentType = ContentType.EmailAddress,
            )
            PasswordField(password, { password = it }, error, isNew = true, help = stringResource(R.string.register_password_help), onDone = submit)
            GeneralError(error, shownFields = RegisterViewModel.FIELDS)
            SubmitButton(stringResource(R.string.action_create_account), busy, canSubmit, onClick = submit)
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    initialEmail: String,
    onBack: () -> Unit,
    onCodeSent: (email: String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf(initialEmail) }

    LaunchedEffect(outcome) {
        outcome?.let {
            onCodeSent(it)
            viewModel.consumeOutcome()
        }
    }

    MmdScaffold(stringResource(R.string.forgot_password_title), onBack = onBack) { modifier ->
        ReadableColumn(modifier) {
            FormField(
                email, { email = it }, stringResource(R.string.field_email), error, "email",
                keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, onDone = { viewModel.sendCode(email) },
                contentType = ContentType.EmailAddress + ContentType.Username,
                help = stringResource(R.string.forgot_password_help),
            )
            GeneralError(error, shownFields = listOf("email"))
            SubmitButton(stringResource(R.string.action_send_code), busy, email.isNotBlank(), onClick = { viewModel.sendCode(email) })
        }
    }
}

/** Entering an emailed code: confirming an address (which logs in), or setting a new password. */
@Composable
fun EmailCodeScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onPasswordReset: () -> Unit,
    viewModel: CodeViewModel = hiltViewModel(),
) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val verifying = viewModel.purpose == CodePurpose.VerifyEmail

    LaunchedEffect(outcome) {
        if (outcome == CodeOutcome.LoggedIn) {
            viewModel.consumeOutcome()
            onLoggedIn()
        }
    }

    MmdScaffold(
        stringResource(if (verifying) R.string.verify_email_title else R.string.reset_password_title),
        onBack = onBack,
    ) { modifier ->
        ReadableColumn(modifier) {
            Text(stringResource(R.string.code_sent_to, viewModel.email), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (verifying) R.string.code_help_verify else R.string.code_help_reset),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CodeField(code, onCodeChange = { new ->
                code = new
                // Confirming an address needs nothing else, so go as soon as the code is complete
                if (verifying && new.length == CODE_LENGTH) viewModel.submit(new, "")
            }, error = error)
            if (!verifying) {
                PasswordField(
                    password, { password = it }, error,
                    label = stringResource(R.string.field_new_password), isNew = true,
                    onDone = { viewModel.submit(code, password) },
                )
            }
            GeneralError(error, shownFields = listOf("code", "password"))
            SubmitButton(
                stringResource(if (verifying) R.string.action_confirm else R.string.action_set_new_password),
                busy, viewModel.canSubmit(code, password),
                onClick = { viewModel.submit(code, password) },
            )
            TextButton(onClick = {
                code = ""
                viewModel.resend()
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_send_new_code))
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    (outcome as? CodeOutcome.PasswordChanged)?.let { changed ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.password_changed_title)) },
            text = { Text(changed.message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumeOutcome()
                    onPasswordReset()
                }) { Text(stringResource(R.string.action_log_in)) }
            },
        )
    }
}
