package uk.makemydaylondon.mmd.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.network.ApiException

/** Forms and text stay a comfortable width on tablets */
val ReadableWidth = 560.dp

/** A screen with a title and, unless it's a tab's first screen, a back button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmdScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = { actions() },
            )
        },
    ) { padding -> content(Modifier.fillMaxSize().padding(padding)) }
}

/** A scrolling, centred column of readable width, for forms and details. */
@Composable
fun ReadableColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = ReadableWidth).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

/** Shown in place of an empty list: an icon, a title, a line of help and maybe a button. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

@Composable
fun CenteredProgress(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

// Forms

/** The server's message for one field */
fun ApiException?.fieldMessage(field: String): String? = (this as? ApiException.Validation)?.messageFor(field)

/** Any error not already shown beside one of [shownFields]. */
@Composable
fun GeneralError(error: ApiException?, shownFields: Collection<String>, modifier: Modifier = Modifier) {
    if (error == null) return
    val fields = (error as? ApiException.Validation)?.fields?.keys.orEmpty()
    if (fields.isNotEmpty() && shownFields.containsAll(fields)) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(error.userMessage(), color = MaterialTheme.colorScheme.error)
    }
}

/** A form's main button, showing progress while it works. */
@Composable
fun SubmitButton(text: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = modifier.fillMaxWidth()) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            Spacer(Modifier.width(12.dp))
        }
        Text(text)
    }
}

/** A single-line text field showing the server's message for [field] beneath it. */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: ApiException?,
    field: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    contentType: ContentType? = null,
    help: String? = null,
) {
    val message = error.fieldMessage(field)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = message != null,
        supportingText = (message ?: help)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onGo = { onDone?.invoke() }),
        modifier = modifier.fillMaxWidth().semantics { if (contentType != null) this.contentType = contentType },
    )
}

/** A password field with a show/hide button. [isNew] tells autofill to offer a strong password. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: ApiException?,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.field_password),
    field: String = "password",
    isNew: Boolean = false,
    help: String? = null,
    onDone: (() -> Unit)? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val message = error.fieldMessage(field)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = message != null,
        supportingText = (message ?: help)?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(if (visible) R.string.action_hide_password else R.string.action_show_password),
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier.fillMaxWidth().semantics {
            contentType = if (isNew) ContentType.NewPassword else ContentType.Password
        },
    )
}

/** The 6-digit codes the API emails. Keeps digits only. */
@Composable
fun CodeField(code: String, onCodeChange: (String) -> Unit, error: ApiException?, modifier: Modifier = Modifier) {
    val message = error.fieldMessage("code")
    OutlinedTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(CODE_LENGTH)) },
        label = { Text(stringResource(R.string.field_code)) },
        singleLine = true,
        isError = message != null,
        supportingText = message?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = modifier.fillMaxWidth().semantics { contentType = ContentType.SmsOtpCode },
    )
}

const val CODE_LENGTH = 6
const val MIN_PASSWORD_LENGTH = 8

/** "Couldn't update" with the reason, for a change that failed */
@Composable
fun ActionErrorDialog(error: ApiException?, onDismiss: () -> Unit) {
    if (error == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.error_update_title)) },
        text = { Text(error.userMessage()) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

/**
 * Chooses a London day from today to a year ahead. The Material date picker
 * works in UTC milliseconds, so days convert at UTC midnight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(initialDay: LocalDate, confirmLabel: String, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = London.today()
    val first = today.toUtcMillis()
    val last = today.plusDays(365).toUtcMillis()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = maxOf(initialDay, today).toUtcMillis(),
        yearRange = today.year..today.plusDays(365).year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in first..last
            override fun isSelectableYear(year: Int) = year in today.year..today.plusDays(365).year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    onDismiss()
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
