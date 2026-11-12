package uk.makemydaylondon.mmd.ui.info

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.ContactTopic
import uk.makemydaylondon.mmd.data.repository.MeRepository
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.common.FormField
import uk.makemydaylondon.mmd.ui.common.FormViewModel
import uk.makemydaylondon.mmd.ui.common.GeneralError
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableColumn
import uk.makemydaylondon.mmd.ui.common.SubmitButton
import uk.makemydaylondon.mmd.ui.common.fieldMessage

/** Questions, suggestions and corrections, emailed through the API. The outcome is the server's reply. */
@HiltViewModel
class ContactViewModel @Inject constructor(
    private val me: MeRepository,
    session: SessionManager,
) : FormViewModel<String>() {

    /** Signed-in people don't need to type their name and email */
    val account = (session.state.value as? SessionState.SignedIn)?.account

    fun send(name: String, email: String, topic: ContactTopic, message: String) {
        if (!canSend(name, email, message)) return
        submit { me.sendContactMessage(name, email, topic, message) }
    }

    companion object {
        const val MIN_LENGTH = 10
        const val MAX_LENGTH = 5000
        val FIELDS = listOf("name", "email", "topic", "message")

        fun canSend(name: String, email: String, message: String): Boolean =
            name.isNotBlank() && "@" in email && message.trim().length >= MIN_LENGTH && message.length <= MAX_LENGTH
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(onBack: () -> Unit, viewModel: ContactViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val sent by viewModel.outcome.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf(viewModel.account?.fullName.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(viewModel.account?.email.orEmpty()) }
    var topic by rememberSaveable { mutableStateOf(ContactTopic.Question) }
    var message by rememberSaveable { mutableStateOf("") }
    var choosingTopic by rememberSaveable { mutableStateOf(false) }

    MmdScaffold(stringResource(R.string.contact_title), onBack = onBack) { modifier ->
        ReadableColumn(modifier) {
            FormField(name, { name = it }, stringResource(R.string.field_your_name), error, "name", contentType = ContentType.PersonFullName)
            FormField(
                email, { email = it }, stringResource(R.string.field_email), error, "email",
                keyboardType = KeyboardType.Email, contentType = ContentType.EmailAddress,
                help = stringResource(R.string.contact_email_help),
            )
            ExposedDropdownMenuBox(expanded = choosingTopic, onExpandedChange = { choosingTopic = it }) {
                OutlinedTextField(
                    value = stringResource(topic.title()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_topic)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = choosingTopic) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = choosingTopic, onDismissRequest = { choosingTopic = false }) {
                    ContactTopic.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(stringResource(option.title())) }, onClick = {
                            topic = option
                            choosingTopic = false
                        })
                    }
                }
            }
            val messageError = error.fieldMessage("message")
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(ContactViewModel.MAX_LENGTH) },
                label = { Text(stringResource(R.string.field_message)) },
                placeholder = { Text(stringResource(topic.prompt())) },
                isError = messageError != null,
                supportingText = {
                    Text(messageError ?: if (message.length > ContactViewModel.MAX_LENGTH - 500) {
                        stringResource(R.string.contact_length, message.length, ContactViewModel.MAX_LENGTH)
                    } else {
                        ""
                    })
                },
                minLines = 6,
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            )
            GeneralError(error, shownFields = ContactViewModel.FIELDS)
            SubmitButton(
                stringResource(R.string.action_send), busy, ContactViewModel.canSend(name, email, message),
                onClick = { viewModel.send(name, email, topic, message) },
            )
        }
    }

    sent?.let { reply ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.contact_sent_title)) },
            text = { Text(reply) },
            confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

private fun ContactTopic.title(): Int = when (this) {
    ContactTopic.Question -> R.string.topic_question
    ContactTopic.Suggestion -> R.string.topic_suggestion
    ContactTopic.Venue -> R.string.topic_venue
    ContactTopic.Problem -> R.string.topic_problem
    ContactTopic.Other -> R.string.topic_other
}

private fun ContactTopic.prompt(): Int = when (this) {
    ContactTopic.Venue -> R.string.prompt_venue
    ContactTopic.Problem -> R.string.prompt_problem
    ContactTopic.Suggestion -> R.string.prompt_suggestion
    ContactTopic.Question, ContactTopic.Other -> R.string.prompt_question
}
