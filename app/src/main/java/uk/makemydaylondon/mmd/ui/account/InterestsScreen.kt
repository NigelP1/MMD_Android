package uk.makemydaylondon.mmd.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Interest
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.GeneralError
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableWidth
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueStyle

/** Interests and email preferences. Changes save as they're made. */
@Composable
fun InterestsScreen(onBack: () -> Unit, viewModel: InterestsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()

    MmdScaffold(stringResource(R.string.interests_title), onBack = onBack) { modifier ->
        when (val s = state) {
            LoadState.Loading -> CenteredProgress(modifier)
            is LoadState.Failed -> ErrorMessage(s.error, onRetry = viewModel::load, modifier = modifier)
            is LoadState.Loaded -> Box(modifier, contentAlignment = Alignment.TopCenter) {
                val profile = s.value
                LazyColumn(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) {
                    item { SectionHeader(stringResource(R.string.interests_header)) }
                    items(Interest.entries) { interest ->
                        SwitchRow(
                            title = stringResource(VenueStyle.title(interest)),
                            icon = VenueStyle.icon(interest),
                            checked = interest in profile.interests,
                            onCheckedChange = { viewModel.setInterest(interest, it) },
                        )
                    }
                    item { Footnote(stringResource(R.string.interests_footer)) }

                    item {
                        SwitchRow(
                            title = stringResource(R.string.interests_only_mine),
                            checked = profile.onlyMyInterests,
                            enabled = profile.interests.isNotEmpty(),
                            onCheckedChange = viewModel::setOnlyMyInterests,
                        )
                        Footnote(
                            stringResource(
                                if (profile.interests.isEmpty()) R.string.interests_only_mine_none else R.string.interests_only_mine_help,
                            ),
                        )
                    }

                    item { SectionHeader(stringResource(R.string.emails_header)) }
                    item {
                        SwitchRow(
                            title = stringResource(R.string.emails_new_events),
                            subtitle = stringResource(R.string.emails_new_events_help),
                            checked = profile.emailNewEvents,
                            onCheckedChange = viewModel::setEmailNewEvents,
                        )
                        SwitchRow(
                            title = stringResource(R.string.emails_ending_soon),
                            subtitle = stringResource(R.string.emails_ending_soon_help),
                            checked = profile.emailEndingReminders,
                            onCheckedChange = viewModel::setEmailEndingReminders,
                        )
                    }
                    item { GeneralError(saveError, shownFields = emptyList(), modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }
}

/** A whole-row switch, so the label is part of the touch target and read with it */
@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        leadingContent = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}
