package uk.makemydaylondon.mmd.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.common.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: AccountViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_account)) }) }) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Keep forms a readable width on tablets
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val s = session) {
                    SessionState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    is SessionState.SignedIn -> SignedIn(s, busy, onLogOut = viewModel::logOut)
                    SessionState.SignedOut -> LoginForm(busy, error, onLogIn = viewModel::logIn)
                }
            }
        }
    }
}

@Composable
private fun SignedIn(state: SessionState.SignedIn, busy: Boolean, onLogOut: () -> Unit) {
    Text(state.account.fullName, style = MaterialTheme.typography.headlineSmall)
    Text(state.account.email, style = MaterialTheme.typography.bodyLarge)
    OutlinedButton(onClick = onLogOut, enabled = !busy) { Text(stringResource(R.string.action_log_out)) }
}

@Composable
private fun LoginForm(busy: Boolean, error: ApiException?, onLogIn: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val validation = error as? ApiException.Validation
    val canSubmit = !busy && email.isNotBlank() && password.isNotEmpty()

    Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.field_email)) },
        singleLine = true,
        isError = validation?.messageFor("email") != null,
        supportingText = validation?.messageFor("email")?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress + ContentType.Username },
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(stringResource(R.string.field_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (canSubmit) onLogIn(email, password) }),
        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
    )
    if (error != null && validation?.fields?.isNotEmpty() != true) {
        Text(error.userMessage(), color = MaterialTheme.colorScheme.error)
    }
    Button(onClick = { onLogIn(email, password) }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_log_in))
    }
}

