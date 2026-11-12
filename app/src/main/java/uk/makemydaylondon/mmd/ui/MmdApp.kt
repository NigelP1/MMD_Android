package uk.makemydaylondon.mmd.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.ui.account.AccountScreen
import uk.makemydaylondon.mmd.ui.common.ComingSoon
import uk.makemydaylondon.mmd.ui.explore.ExploreScreen

@Serializable data object ExploreRoute
@Serializable data object SearchRoute
@Serializable data object SavedRoute
@Serializable data object AccountRoute

private enum class TopLevel(val route: Any, val routeClass: KClass<*>, @StringRes val label: Int, val icon: ImageVector) {
    Explore(ExploreRoute, ExploreRoute::class, R.string.tab_explore, Icons.Filled.Explore),
    Search(SearchRoute, SearchRoute::class, R.string.tab_search, Icons.Filled.Search),
    Saved(SavedRoute, SavedRoute::class, R.string.tab_my_day, Icons.Filled.Bookmarks),
    Account(AccountRoute, AccountRoute::class, R.string.tab_account, Icons.Filled.AccountCircle),
}

/**
 * The app shell. NavigationSuiteScaffold shows a bottom bar on phones and a
 * navigation rail on tablets, foldables and landscape windows.
 */
@Composable
fun MmdApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevel.entries.forEach { tab ->
                item(
                    selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = ExploreRoute) {
            composable<ExploreRoute> { ExploreScreen() }
            composable<SearchRoute> { ComingSoon(R.string.tab_search) }
            composable<SavedRoute> { ComingSoon(R.string.tab_my_day) }
            composable<AccountRoute> { AccountScreen() }
        }
    }
}
