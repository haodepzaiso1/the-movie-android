package com.piashcse.hilt_mvvm_compose_movie.ui.screens.mainscreen.bottom_navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.piashcse.hilt_mvvm_compose_movie.navigation.Screen
import com.piashcse.hilt_mvvm_compose_movie.utils.singleTopNavigator

@Composable
fun BottomNavigationUI(navController: NavController, pagerState: PagerState) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        val items = listOf(
            Screen.NowPlayingNav,
            Screen.PopularNav,
            Screen.TopRatedNav,
            Screen.UpcomingNav,
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = item.navIcon,
                label = { Text(text = stringResource(id = item.title)) },
                selected = currentDestination?.hasRoute(item.route::class) == true,
                onClick = { navController.singleTopNavigator(item.route) }
            )
        }
    }
}
