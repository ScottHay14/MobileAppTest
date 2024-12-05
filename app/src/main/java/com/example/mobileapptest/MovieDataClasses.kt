package com.example.mobileapptest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class Movie (
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("poster_path") val posterPath: String,
    @SerializedName("backdrop_path") val backdropPath: String,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("vote_average") val voteAverage: Float
)
data class getMovieResponse (
    @SerializedName("page") val page: Int,
    @SerializedName("results") val movies: List<Movie>,
    @SerializedName("total_pages") val pages: Int,
)

object MoviesRepository {
    private val api: Api

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(Api::class.java)
    }

    fun getPopularMovies(
        page: Int = 1,
        onSuccess: (movies: List<Movie>) -> Unit,
        onError: () -> Unit
    ) {
        api.getPopularMovies(page = page)
            .enqueue(object : Callback<getMovieResponse> {
                override fun onResponse(
                    call: Call<getMovieResponse>,
                    response: Response<getMovieResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            onSuccess.invoke(responseBody.movies)
                        } else {
                            onError.invoke()
                        }
                    }
                }
                override fun onFailure(call: Call<getMovieResponse>, t: Throwable) {
                    onError.invoke()
                }
            })
    }

    fun searchMovies(
        query: String,
        page: Int = 1,
        onSuccess: (movies: List<Movie>) -> Unit,
        onError: () -> Unit
    ) {
        api.searchMovies(query = query, page = page)
            .enqueue(object : Callback<getMovieResponse> {
                override fun onResponse(
                    call: Call<getMovieResponse>,
                    response: Response<getMovieResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            onSuccess.invoke(responseBody.movies)
                        } else {
                            onError.invoke()
                        }
                    }
                }

                override fun onFailure(call: Call<getMovieResponse>, t: Throwable) {
                    onError.invoke()
                }
            })
    }
}


@Composable
fun MovieContent(
    moviesState: List<Movie>,
    isError: Boolean,
    isLoading: Boolean,
    loadNextPage: () -> Unit,
    query: String,
    favouriteMovies: List<Movie>,
    onAddToFavourites: (Movie) -> Unit,
    onRemoveFromFavourites: (Movie) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(query) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (!isLoading && listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == moviesState.size - 1) {
            loadNextPage()
        }
    }

    if (isError) {
        Text(
            text = "Failed to load movies!",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    text = "Popular Movies",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Most popular movies",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(moviesState) { movie ->
                MovieCard(
                    movie = movie,
                    isFavourite = favouriteMovies.contains(movie),
                    onFavouriteChange = { isSelected ->
                        if (isSelected) {
                            onAddToFavourites(movie)
                        } else {
                            onRemoveFromFavourites(movie)
                        }
                    }
                )
            }

            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    isFavourite: Boolean,
    onFavouriteChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            MoviePoster(
                posterPath = movie.posterPath,
                modifier = Modifier.size(128.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rating: ${movie.voteAverage}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Release Date: ${movie.releaseDate}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Favourite")
                    Checkbox(
                        checked = isFavourite,
                        onCheckedChange = onFavouriteChange
                    )
                }
            }
        }
    }
}

@Composable
fun MoviePoster(posterPath: String?, modifier: Modifier = Modifier) {
    if (posterPath.isNullOrEmpty()) {
        Box(modifier = modifier.background(Color.Gray)) // Just a placeholder image if not loaded
        return
    }
    AsyncImage(
        model = "https://image.tmdb.org/t/p/w342$posterPath",
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

