package com.piashcse.hilt_mvvm_compose_movie.navigation

import kotlinx.serialization.Serializable

@Serializable
object NowPlayingMovieRoute

@Serializable
object PopularMovieRoute

@Serializable
object TopRatedMovieRoute

@Serializable
object UpcomingMovieRoute

// TV series and Celebrities routes archived/removed

@Serializable
data class MovieDetailRoute(val movieId: Int)

@Serializable
data class ArtistDetailRoute(val artistId: Int)

@Serializable
object FavoriteMovieRoute

// Favorite TV series route archived/removed
