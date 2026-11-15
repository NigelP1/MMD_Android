package uk.makemydaylondon.mmd.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
import java.time.LocalDate
import kotlin.reflect.KClass
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.StationSuggestion
import uk.makemydaylondon.mmd.ui.account.AccountScreen
import uk.makemydaylondon.mmd.ui.account.DeleteAccountScreen
import uk.makemydaylondon.mmd.ui.account.EmailCodeScreen
import uk.makemydaylondon.mmd.ui.account.ForgotPasswordScreen
import uk.makemydaylondon.mmd.ui.account.InterestsScreen
import uk.makemydaylondon.mmd.ui.account.PasskeysScreen
import uk.makemydaylondon.mmd.ui.account.LoginActions
import uk.makemydaylondon.mmd.ui.account.LoginScreen
import uk.makemydaylondon.mmd.ui.account.RegisterScreen
import uk.makemydaylondon.mmd.ui.common.DetailSelection
import uk.makemydaylondon.mmd.ui.common.EmptyState
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
    /** On wide screens the tab's list stays beside what's chosen from it */
    val hasListPane: Boolean = false,
) {
    Explore(ExploreRoute, ExploreRoute::class, R.string.tab_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    Search(SearchRoute, SearchRoute::class, R.string.tab_search, Icons.Filled.Search, Icons.Outlined.Search, hasListPane = true),
    MyDay(SavedRoute, SavedRoute::class, R.string.tab_my_day, Icons.Filled.WbSunny, Icons.Outlined.WbSunny, hasListPane = true),
    Account(AccountRoute, AccountRoute::class, R.string.tab_account, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
    ;

    companion object {
        fun of(entry: NavBackStackEntry): TopLevel? = entries.firstOrNull { entry.destination.hasRoute(it.routeClass) }
    }
}

/**
 * The app shell. NavigationSuiteScaffold shows a bottom bar on phones and a
 * navigation rail on tablets, foldables and landscape windows. Detail
 * screens push onto whichever tab opened them. On wide screens, Search and My
 * Day keep their list in a pane on the left, with the venue or event chosen
 * from it on the right.
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
        // Walking directions are drawn on the Explore tab's map
        val showMap = {
            if (currentTab != TopLevel.Explore) {
                currentTab = TopLevel.Explore
                navController.navigate(ExploreRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            navController.popBackStack(ExploreRoute, inclusive = false)
            Unit
        }
        val nav = AppNavigation(navController, onShowWelcome = appViewModel::showWelcomeAgain, onShowMap = showMap)

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val twoPane = currentTab.hasListPane && maxWidth >= TwoPaneMinWidth
            Row(Modifier.fillMaxSize()) {
                if (twoPane) {
                    ListPane(currentTab, navController, nav, Modifier.width(ListPaneWidth).fillMaxHeight())
                    VerticalDivider()
                }
                MmdNavHost(navController, nav, twoPane, Modifier.weight(1f))
            }
        }
    }
}

/** Below this content width, one screen at a time */
private val TwoPaneMinWidth = 760.dp
private val ListPaneWidth = 360.dp

/**
 * Where screens go. In two-pane mode, choosing something from the list
 * replaces what's beside it rather than stacking up behind it.
 */
private class AppNavigation(
    val navController: NavHostController,
    val onShowWelcome: () -> Unit,
    val onShowMap: () -> Unit,
) {
    fun openVenue(id: Int, date: LocalDate? = null) = navController.navigate(VenueRoute(id, date?.toString()))
    fun openEvent(venueId: Int, eventId: Int, date: String? = null) = navController.navigate(EventRoute(venueId, eventId, date))
    fun openStation(station: StationSuggestion) =
        navController.navigate(NearbyRoute(station.name, station.latitude, station.longitude))
    fun logIn() = navController.navigate(LoginRoute) { launchSingleTop = true }

    /** Opens [route] from [tab]'s list pane, in place of whatever was showing beside it */
    fun openBeside(tab: TopLevel, route: Any) = navController.navigate(route) {
        popUpTo(tab.route) { inclusive = false }
    }
}

/**
 * A tab's list, kept on screen beside its detail. It shares the ViewModel
 * of the tab's first screen, so it's the same list as on a phone.
 */
@Composable
private fun ListPane(tab: TopLevel, navController: NavHostController, nav: AppNavigation, modifier: Modifier) {
    val current by navController.currentBackStackEntryAsState()
    val rootEntry = remember(tab, current) { runCatching { navController.getBackStackEntry(tab.route) }.getOrNull() } ?: return
    val selection = current?.let(::detailSelection)
    val states = rememberSaveableStateHolder()

    Box(modifier) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides rootEntry) {
            states.SaveableStateProvider(tab.name) {
                when (tab) {
                    TopLevel.Search -> SearchScreen(
                        onOpenVenue = { nav.openBeside(tab, VenueRoute(it)) },
                        onOpenEvent = { venueId, eventId -> nav.openBeside(tab, EventRoute(venueId, eventId)) },
                        onOpenStation = { nav.openBeside(tab, NearbyRoute(it.name, it.latitude, it.longitude)) },
                        selection = selection,
                    )
                    TopLevel.MyDay -> MyDayScreen(
                        onOpenEvent = { venueId, eventId -> nav.openBeside(tab, EventRoute(venueId, eventId)) },
                        onOpenVenue = { nav.openBeside(tab, VenueRoute(it)) },
                        onLogIn = nav::logIn,
                        selection = selection,
                    )
                    TopLevel.Explore, TopLevel.Account -> Unit
                }
            }
        }
    }
}

/** The venue or event a back stack entry shows, to highlight in the list */
private fun detailSelection(entry: NavBackStackEntry): DetailSelection? = when {
    entry.destination.hasRoute(VenueRoute::class) -> DetailSelection(venueId = entry.toRoute<VenueRoute>().id)
    entry.destination.hasRoute(EventRoute::class) -> entry.toRoute<EventRoute>().let { DetailSelection(it.venueId, it.eventId) }
    else -> null
}

/** Beside a list, before anything has been chosen from it */
@Composable
private fun DetailPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(Icons.Outlined.TouchApp, stringResource(R.string.detail_placeholder), message = null)
    }
}

@Composable
private fun MmdNavHost(navController: NavHostController, nav: AppNavigation, twoPane: Boolean, modifier: Modifier) {
    val back: () -> Unit = { navController.popBackStack() }
    val loginActions = LoginActions(
        onLoggedIn = navController::finishSignIn,
        onNewAccount = {
            navController.finishSignIn()
            navController.navigate(InterestsRoute)
        },
        onNeedsVerification = { navController.navigate(EmailCodeRoute(it, CodePurpose.VerifyEmail)) },
        onForgotPassword = { navController.navigate(ForgotPasswordRoute(it)) },
        onRegister = { navController.navigate(RegisterRoute) },
    )

    NavHost(navController, startDestination = ExploreRoute, modifier = modifier) {
        // Tabs. In two-pane mode the list pane shows Search and My Day, and
        // this side waits for something to be chosen.
        composable<ExploreRoute> {
            ExploreScreen(onOpenVenue = nav::openVenue, onOpenInfo = { navController.navigate(InfoRoute) })
        }
        composable<SearchRoute> {
            if (twoPane) {
                DetailPlaceholder()
            } else {
                SearchScreen(
                    onOpenVenue = { nav.openVenue(it) },
                    onOpenEvent = { venueId, eventId -> nav.openEvent(venueId, eventId) },
                    onOpenStation = nav::openStation,
                )
            }
        }
        composable<SavedRoute> {
            if (twoPane) {
                DetailPlaceholder()
            } else {
                MyDayScreen(
                    onOpenEvent = { venueId, eventId -> nav.openEvent(venueId, eventId) },
                    onOpenVenue = { nav.openVenue(it) },
                    onLogIn = nav::logIn,
                )
            }
        }
        composable<AccountRoute> {
            AccountScreen(
                loginActions = loginActions,
                onOpenInterests = { navController.navigate(InterestsRoute) },
                onOpenDeleteAccount = { navController.navigate(DeleteAccountRoute) },
                onOpenInfo = { navController.navigate(InfoRoute) },
                onOpenPasskeys = { navController.navigate(PasskeysRoute) },
            )
        }

        // Venues and events
        composable<NearbyRoute> { ExploreScreen(onOpenVenue = nav::openVenue, onBack = back) }
        composable<VenueRoute> { entry ->
            val date = entry.toRoute<VenueRoute>().date
            VenueScreen(
                onBack = back,
                onOpenEvent = { venueId, eventId -> nav.openEvent(venueId, eventId, date) },
                onLogIn = nav::logIn,
                onShowMap = nav.onShowMap,
            )
        }
        composable<EventRoute> {
            EventScreen(onBack = back, onOpenVenue = { nav.openVenue(it) }, onLogIn = nav::logIn, onShowMap = nav.onShowMap)
        }

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
        composable<PasskeysRoute> { PasskeysScreen(onBack = back) }
        composable<DeleteAccountRoute> {
            DeleteAccountScreen(onBack = back, onDeleted = { navController.popBackStack<AccountRoute>(inclusive = false) })
        }

        // Information
        composable<InfoRoute> {
            InfoScreen(onBack = back, onOpenContact = { navController.navigate(ContactRoute) }, onShowWelcome = nav.onShowWelcome)
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
