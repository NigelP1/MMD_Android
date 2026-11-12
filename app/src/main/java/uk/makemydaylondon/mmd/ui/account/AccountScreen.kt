package uk.makemydaylondon.mmd.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.GeneralError
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableColumn

/** The Account tab: the login form when signed out, the account when signed in. */
@Composable
fun AccountScreen(
    loginActions: LoginActions,
    onOpenInterests: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onOpenInfo: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    MmdScaffold(stringResource(R.string.tab_account), onBack = null) { modifier ->
        when (val s = session) {
            SessionState.Loading -> CenteredProgress(modifier)
            SessionState.SignedOut -> ReadableColumn(modifier) {
                LoginForm(loginActions)
                HorizontalDivider()
                MenuRow(Icons.Outlined.Info, stringResource(R.string.info_title), onOpenInfo)
            }
            is SessionState.SignedIn -> ReadableColumn(modifier) {
                AccountDetails(s.account, viewModel, onOpenInterests, onOpenDeleteAccount, onOpenInfo)
            }
        }
    }
}

@Composable
private fun AccountDetails(
    account: Account,
    viewModel: AccountViewModel,
    onOpenInterests: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var confirmingLogOutAll by rememberSaveable { mutableStateOf(false) }

    Column {
        Text(account.fullName, style = MaterialTheme.typography.headlineSmall)
        Text(account.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider()
    MenuRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.interests_menu), onOpenInterests)
    MenuRow(Icons.Outlined.Info, stringResource(R.string.info_title), onOpenInfo)
    HorizontalDivider()
    MenuRow(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.action_log_out), viewModel::logOut, enabled = !busy)
    MenuRow(Icons.Outlined.DevicesOther, stringResource(R.string.action_log_out_everywhere), { confirmingLogOutAll = true }, enabled = !busy)
    GeneralError(error, shownFields = emptyList())
    HorizontalDivider()
    MenuRow(Icons.Outlined.DeleteOutline, stringResource(R.string.delete_account_title), onOpenDeleteAccount, destructive = true)

    if (confirmingLogOutAll) {
        AlertDialog(
            onDismissRequest = { confirmingLogOutAll = false },
            title = { Text(stringResource(R.string.log_out_everywhere_title)) },
            text = { Text(stringResource(R.string.log_out_everywhere_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingLogOutAll = false
                    viewModel.logOutEverywhere()
                }) { Text(stringResource(R.string.action_log_out_everywhere)) }
            },
            dismissButton = { TextButton(onClick = { confirmingLogOutAll = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** A tappable line in a menu, e.g. "Interests and emails" */
@Composable
fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit, enabled: Boolean = true, destructive: Boolean = false) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        headlineContent = { Text(text, color = color) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
