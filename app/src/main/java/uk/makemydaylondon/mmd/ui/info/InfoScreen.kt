package uk.makemydaylondon.mmd.ui.info

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.BuildConfig
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.NewsItem
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.ui.account.MenuRow
import uk.makemydaylondon.mmd.ui.common.ExternalLinks
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableColumn

@HiltViewModel
class InfoViewModel @Inject constructor(private val venues: VenueRepository) : ViewModel() {
    /** The current news page, if there is one */
    private val _news = MutableStateFlow<NewsItem?>(null)
    val news: StateFlow<NewsItem?> = _news.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _news.value = venues.news()
            } catch (_: ApiException) {
                // No news link is fine
            }
        }
    }
}

/** About the app: the website's information pages, news, contact, the welcome tips and the version. */
@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onOpenContact: () -> Unit,
    onShowWelcome: () -> Unit,
    viewModel: InfoViewModel = hiltViewModel(),
) {
    val news by viewModel.news.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val open = { path: String -> ExternalLinks.openWebPage(context, WEBSITE + path) }

    MmdScaffold(stringResource(R.string.info_title), onBack = onBack) { modifier ->
        ReadableColumn(modifier) {
            news?.let { item ->
                MenuRow(Icons.Outlined.Newspaper, stringResource(R.string.info_news), { ExternalLinks.openWebPage(context, item.url) })
                HorizontalDivider()
            }
            // In the app: the website's contact form no longer works
            MenuRow(Icons.Outlined.Email, stringResource(R.string.contact_menu), onOpenContact)
            MenuRow(Icons.Outlined.Info, stringResource(R.string.info_about), { open("about/") })
            MenuRow(Icons.Outlined.PrivacyTip, stringResource(R.string.info_privacy), { open("privacy/") })
            MenuRow(Icons.Outlined.ReportProblem, stringResource(R.string.info_disclaimer), { open("disclaimer/") })
            MenuRow(Icons.Outlined.Copyright, stringResource(R.string.info_credits), { open("credits/") })
            HorizontalDivider()
            MenuRow(Icons.Outlined.Lightbulb, stringResource(R.string.info_show_welcome), onShowWelcome)
            Text(
                stringResource(R.string.info_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

private const val WEBSITE = "https://www.makemydaylondon.uk/"
