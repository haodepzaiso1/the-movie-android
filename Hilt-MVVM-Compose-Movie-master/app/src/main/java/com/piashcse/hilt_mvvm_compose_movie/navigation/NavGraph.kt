package com.piashcse.hilt_mvvm_compose_movie.navigation

// Composable imports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.piashcse.hilt_mvvm_compose_movie.R
import com.piashcse.hilt_mvvm_compose_movie.data.model.moviedetail.Genre
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.artist_detail.ArtistDetail
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.favorite.movie.FavoriteMovie
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.movies.movie_detail.MovieDetail
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.movies.nowplaying.NowPlayingMovie
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.movies.popular.PopularMovie
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.movies.toprated.TopRatedMovie
import com.piashcse.hilt_mvvm_compose_movie.ui.screens.movies.upcoming.UpcomingMovie

@Composable
fun Navigation(
    navController: NavHostController, genres: List<Genre>? = null,
) {
    NavHost(navController = navController, startDestination = NowPlayingMovieRoute) {
        composable<NowPlayingMovieRoute> {
            NowPlayingMovie(
                navController = navController,
                genres
            )
        }
        composable<PopularMovieRoute> {
            PopularMovie(
                navController = navController,
                genres
            )
        }
        composable<TopRatedMovieRoute> {
            TopRatedMovie(
                navController = navController,
                genres
            )
        }
        composable<UpcomingMovieRoute> {
            UpcomingMovie(
                navController = navController,
                genres
            )
        }
        composable<MovieDetailRoute> {
            val args = it.toRoute<MovieDetailRoute>()
            MovieDetail(
                navController = navController, args.movieId
            )
        }
        composable<ArtistDetailRoute> {
            val args = it.toRoute<ArtistDetailRoute>()
            ArtistDetail(
                navController = navController,
                args.artistId
            )
        }
        // TV series screens have been archived/removed from navigation
        composable<FavoriteMovieRoute> {
            FavoriteMovie(navController)
        }
        // Favorite TV series and Celebrities screens have been archived/removed from navigation
    }
}

@Composable
fun navigationTitle(navController: NavController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination ?: return stringResource(R.string.app_name)

    return when {
        destination.hasRoute<MovieDetailRoute>() -> stringResource(id = R.string.movie_detail)
        // TV series detail removed
        destination.hasRoute<ArtistDetailRoute>() -> stringResource(id = R.string.artist_detail)
        destination.hasRoute<FavoriteMovieRoute>() -> stringResource(id = R.string.favorite_movie)
        // Favorite TV series removed
        else -> stringResource(R.string.app_name)
    }
}


