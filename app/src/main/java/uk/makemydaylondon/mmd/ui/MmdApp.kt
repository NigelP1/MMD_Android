package uk.makemydaylondon.mmd.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlin.reflect.KClass
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.ui.account.AccountScreen
import uk.makemydaylondon.mmd.ui.account.DeleteAccountScreen
import uk.makemydaylondon.mmd.ui.account.EmailCodeScreen
import uk.makemydaylondon.mmd.ui.account.ForgotPasswordScreen
import uk.makemydaylondon.mmd.ui.account.InterestsScreen
import uk.makemydaylondon.mmd.ui.account.LoginActions
import uk.makemydaylondon.mmd.ui.account.LoginScreen
import uk.makemydaylondon.mmd.ui.account.RegisterScreen
import uk.makemydaylondon.mmd.ui.event.EventScreen
import uk.makemydaylondon.mmd.ui.explore.ExploreScreen
import uk.makemydaylondon.mmd.ui.info.ContactScreen
import uk.makemydaylondon.mmd.ui.info.InfoScreen
import uk.makemydaylondon.mmd.ui.info.WelcomeScreen
import uk.makemydaylondon.mmd.ui.myday.MyDayScreen
import uk.makemydaylondon.mmd.ui.search.SearchScreen
import uk.makemydaylondon.mmd.ui.venue.VenueScreen

private enum class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    Explore(ExploreRoute, ExploreRoute::class, R.string.tab_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    Search(SearchRoute, SearchRoute::class, R.string.tab_search, Icons.Filled.Search, Icons.Outlined.Search),
    MyDay(SavedRoute, SavedRoute::class, R.string.tab_my_day, Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    Account(AccountRoute, AccountRoute::class, R.string.tab_account, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
    ;

    companion object {
        fun of(entry: NavBackStackEntry): TopLevel? = entries.firstOrNull { entry.destination.hasRoute(it.routeClass) }
    }
}

/**
 * The app shell. NavigationSuiteScaffold shows a bottom bar on phones and a
 * navigation rail on tablets, foldables and landscape windows. Detail
 * screens push onto whichever tab opened them.
 */
@Composable
fun MmdApp(appViewModel: AppViewModel = hiltViewModel()) {
    val showWelcome by appViewModel.showWelcome.collectAsStateWithLifecycle()
    when (showWelcome) {
        null -> return
        true -> {
            BackHandler(onBack = appViewModel::welcomeDone)
            WelcomeScreen(onDone = appViewModel::welcomeDone)
            return
        }
        false -> Unit
    }

    val navController = rememberNavController()
    // Detail screens belong to the tab that opened them, so the tab changes
    // only when one is chosen or when a tab's first screen comes back into view
    var currentTab by rememberSaveable { mutableStateOf(TopLevel.Explore) }
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry) {
        currentEntry?.let(TopLevel::of)?.let { currentTab = it }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevel.entries.forEach { tab ->
                val selected = tab == currentTab
                item(
                    selected = selected,
                    onClick = {
                        if (selected) {
                            // Tapping the current tab goes back to its first screen
                            navController.popBackStack(tab.route, inclusive = false)
                        } else {
                            currentTab = tab
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        MmdNavHost(navController, onShowWelcome = appViewModel::showWelcomeAgain)
    }
}

@Composable
private fun MmdNavHost(navController: NavHostController, onShowWelcome: () -> Unit) {
    val back: () -> Unit = { navController.popBackStack() }
    val openVenue = { id: Int -> navController.navigate(VenueRoute(id)) }
    val openEvent = { venueId: Int, eventId: Int -> navController.navigate(EventRoute(venueId, eventId)) }
    val logIn = { navController.navigate(LoginRoute) { launchSingleTop = true } }
    val loginActions = LoginActions(
        onLoggedIn = navController::finishSignIn,
        onNeedsVerification = { navController.navigate(EmailCodeRoute(it, CodePurpose.VerifyEmail)) },
        onForgotPassword = { navController.navigate(ForgotPasswordRoute(it)) },
        onRegister = { navController.navigate(RegisterRoute) },
    )

    NavHost(navController, startDestination = ExploreRoute) {
        // Tabs
        composable<ExploreRoute> {
            ExploreScreen(onOpenVenue = openVenue, onOpenInfo = { navController.navigate(InfoRoute) })
        }
        composable<SearchRoute> {
            SearchScreen(
                onOpenVenue = openVenue,
                onOpenEvent = openEvent,
                onOpenStation = { navController.navigate(NearbyRoute(it.name, it.latitude, it.longitude)) },
            )
        }
        composable<SavedRoute> { MyDayScreen(onOpenEvent = openEvent, onOpenVenue = openVenue, onLogIn = logIn) }
        composable<AccountRoute> {
            AccountScreen(
                loginActions = loginActions,
                onOpenInterests = { navController.navigate(InterestsRoute) },
                onOpenDeleteAccount = { navController.navigate(DeleteAccountRoute) },
                onOpenInfo = { navController.navigate(InfoRoute) },
            )
        }

        // Venues and events
        composable<NearbyRoute> { ExploreScreen(onOpenVenue = openVenue, onBack = back) }
        composable<VenueRoute> { entry ->
            val date = entry.toRoute<VenueRoute>().date
            VenueScreen(
                onBack = back,
                onOpenEvent = { venueId, eventId -> navController.navigate(EventRoute(venueId, eventId, date)) },
                onLogIn = logIn,
            )
        }
        composable<EventRoute> { EventScreen(onBack = back, onOpenVenue = openVenue, onLogIn = logIn) }

        // Accounts
        composable<LoginRoute> { LoginScreen(onBack = back, actions = loginActions) }
        composable<RegisterRoute> {
            RegisterScreen(onBack = back, onCodeSent = { navController.navigate(EmailCodeRoute(it, CodePurpose.VerifyEmail)) })
        }
        composable<ForgotPasswordRoute> { entry ->
            ForgotPasswordScreen(
                initialEmail = entry.toRoute<ForgotPasswordRoute>().email,
                onBack = back,
                onCodeSent = { navController.navigate(EmailCodeRoute(it, CodePurpose.ResetPassword)) },
            )
        }
        composable<EmailCodeRoute> {
            EmailCodeScreen(
                onBack = back,
                onLoggedIn = navController::finishSignIn,
                onPasswordReset = navController::backToLogin,
            )
        }
        composable<InterestsRoute> { InterestsScreen(onBack = back) }
        composable<DeleteAccountRoute> {
            DeleteAccountScreen(onBack = back, onDeleted = { navController.popBackStack<AccountRoute>(inclusive = false) })
        }

        // Information
        composable<InfoRoute> {
            InfoScreen(onBack = back, onOpenContact = { navController.navigate(ContactRoute) }, onShowWelcome = onShowWelcome)
        }
        composable<ContactRoute> { ContactScreen(onBack = back) }
    }
}

/** After signing in, return to whatever asked for an account (or the Account tab). */
private fun NavController.finishSignIn() {
    if (!popBackStack<LoginRoute>(inclusive = true)) popBackStack<AccountRoute>(inclusive = false)
}

/** After a password reset, back to the login form to use it. */
private fun NavController.backToLogin() {
    if (!popBackStack<LoginRoute>(inclusive = false)) popBackStack<AccountRoute>(inclusive = false)
}
