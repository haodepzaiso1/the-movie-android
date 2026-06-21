package com.piashcse.hilt_mvvm_compose_movie.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piashcse.hilt_mvvm_compose_movie.R

sealed class Screen(
    val route: Any,
    @StringRes val title: Int = R.string.app_name,
    val navIcon: (@Composable () -> Unit) = {
        Icon(
            Icons.Filled.Home, contentDescription = "home"
        )
    }
) {
    data object NowPlaying : Screen(NowPlayingMovieRoute)
    data object Popular : Screen(PopularMovieRoute)
    data object TopRated : Screen(TopRatedMovieRoute)
    data object Upcoming : Screen(UpcomingMovieRoute)
    // TV series and Celebrities screens archived/removed

    data object MovieDetail : Screen(MovieDetailRoute::class)
    // TvSeriesDetail removed
    data object ArtistDetail : Screen(ArtistDetailRoute::class)

    // Bottom Navigation
    data object NowPlayingNav : Screen(NowPlayingMovieRoute, title = R.string.now_playing, navIcon = {
        Icon(
            Icons.Filled.Home,
            contentDescription = "search",
            modifier = Modifier
                .padding(end = 16.dp)
                .offset(x = 10.dp)
        )
    })

    data object PopularNav : Screen(PopularMovieRoute, title = R.string.popular, navIcon = {
        Icon(
            Icons.Filled.Home,
            contentDescription = "search",
            modifier = Modifier
                .padding(end = 16.dp)
                .offset(x = 10.dp)
        )
    })

    data object TopRatedNav : Screen(TopRatedMovieRoute, title = R.string.top_rated, navIcon = {
        Icon(
            Icons.Filled.Star,
            contentDescription = "search",
            modifier = Modifier
                .padding(end = 16.dp)
                .offset(x = 10.dp)
        )
    })

    data object UpcomingNav : Screen(UpcomingMovieRoute, title = R.string.up_coming, navIcon = {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "search",
            modifier = Modifier
                .padding(end = 16.dp)
                .offset(x = 10.dp)
        )
    })
    // Bottom navigation entries for TV series and celebrities removed
    data object FavoriteMovie :
        Screen(FavoriteMovieRoute)
    // FavoriteTvSeries removed
}