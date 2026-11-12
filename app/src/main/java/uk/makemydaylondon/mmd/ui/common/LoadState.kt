package uk.makemydaylondon.mmd.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.network.ApiException

/** The state of something loaded from the API. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
    data class Failed(val error: ApiException) : LoadState<Nothing>
}

/** A generic message for each kind of failure, used when the server didn't give its own. */
@StringRes
fun ApiException.messageRes(): Int = when (this) {
    is ApiException.Offline -> R.string.error_offline
    is ApiException.Unauthorized -> R.string.error_unauthorized
    is ApiException.NotFound -> R.string.error_not_found
    is ApiException.RateLimited -> R.string.error_rate_limited
    is ApiException.Validation -> R.string.error_validation
    else -> R.string.error_generic
}

@Composable
fun ApiException.userMessage(): String = serverMessage ?: stringResource(messageRes())

@Composable
fun ErrorMessage(error: ApiException, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(error.userMessage(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Button(onClick = onRetry) { Text(stringResource(R.string.action_try_again)) }
        }
    }
}
