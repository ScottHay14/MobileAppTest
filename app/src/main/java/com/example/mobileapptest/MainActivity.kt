package com.example.mobileapptest

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobileapptest.ui.theme.MobileAppTestTheme
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// API interface which shows the most popular movies and has function for searching
interface Api {
    @GET("movie/popular")
    fun getPopularMovies(
        @Query("api_key") apiKey: String = "c032b935ba10aa6539852ef83b07b730",
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean = false
    ): Call<getMovieResponse>

    @GET("search/movie")
    fun searchMovies(
        @Query("api_key") apiKey: String = "c032b935ba10aa6539852ef83b07b730",
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean = false
    ): Call<getMovieResponse>
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val favouritesViewModel: FavouritesViewModel = viewModel(factory = FavouritesViewModelFactory(applicationContext))
            MobileAppTestTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "movieList") {
                    composable("movieList") {
                        MovieListScreen(
                            navController = navController,
                            favouriteMovies = favouritesViewModel.favouriteMovies.value,
                            onAddToFavourites = { movie ->
                                favouritesViewModel.addToFavourites(movie)
                            },
                            onRemoveFromFavourites = { movie ->
                                favouritesViewModel.removeFromFavourites(movie)
                            }
                        )
                    }
                    composable("movieFavourites") {
                        MovieFavourites(
                            navController = navController,
                            favouriteMovies = favouritesViewModel.favouriteMovies.value,
                            onRemoveFromFavourites = { movie ->
                                favouritesViewModel.removeFromFavourites(movie)
                            }
                        )
                    }
                }
            }
        }
    }
}

class FavouritesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FavouritesViewModel(context) as T
    }
}

@Composable
fun MovieListScreen(
    navController: NavController,
    favouriteMovies: List<Movie>,
    onAddToFavourites: (Movie) -> Unit,
    onRemoveFromFavourites: (Movie) -> Unit
) {
    val moviesState = remember { mutableStateOf<List<Movie>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val isError = remember { mutableStateOf(false) }
    val currentPage = remember { mutableStateOf(1) }
    var query by remember { mutableStateOf("") }

    // State for sorting
    val sortByRating = remember { mutableStateOf(true) }

    LaunchedEffect(query, sortByRating.value) {
        moviesState.value = emptyList()
        fetchMovies(query, currentPage.value, sortByRating.value, moviesState, isLoading, isError)
    }

    Column {
        TopBar(
            query = query,
            onQueryChange = { query = it },
            navController = navController,
            sortByRating = sortByRating.value,
            onSortToggle = { sortByRating.value = !sortByRating.value } // Toggle sorting
        )

        MovieContent(
            moviesState = moviesState.value,
            isError = isError.value,
            isLoading = isLoading.value,
            loadNextPage = {
                currentPage.value++
                fetchMovies(query, currentPage.value, sortByRating.value, moviesState, isLoading, isError)
            },
            query = query,
            favouriteMovies = favouriteMovies,
            onAddToFavourites = onAddToFavourites,
            onRemoveFromFavourites = onRemoveFromFavourites
        )
    }
}

private fun fetchMovies(
    query: String,
    page: Int,
    sortByRating: Boolean,
    moviesState: MutableState<List<Movie>>,
    isLoading: MutableState<Boolean>,
    isError: MutableState<Boolean>
) {
    if (isLoading.value) return

    isLoading.value = true
    isError.value = false

    val sortOrder = if (sortByRating) {
        Comparator.comparingDouble<Movie> { it.voteAverage.toDouble() }.reversed()
    } else {
        Comparator.comparingDouble<Movie> { it.voteAverage.toDouble() }
    }

    if (query.isEmpty()) {
        MoviesRepository.getPopularMovies(
            page = page,
            onSuccess = { fetchedMovies ->
                val sortedMovies = fetchedMovies.sortedWith(sortOrder)
                moviesState.value = moviesState.value + sortedMovies
                isLoading.value = false
            },
            onError = {
                Log.e("MoviesApp", "Error fetching popular movies")
                isError.value = true
                isLoading.value = false
            }
        )
    } else {
        MoviesRepository.searchMovies(
            query = query,
            page = page,
            onSuccess = { fetchedMovies ->
                val sortedMovies = fetchedMovies.sortedWith(sortOrder)
                if (page == 1) {
                    moviesState.value = sortedMovies
                } else {
                    moviesState.value = moviesState.value + sortedMovies
                }
                isLoading.value = false
            },
            onError = {
                Log.e("MoviesApp", "Error fetching search results")
                isError.value = true
                isLoading.value = false
            }
        )
    }
}

// The nav bar which has the search, sorting and the menu button to switch pages
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    navController: NavController,
    sortByRating: Boolean,
    onSortToggle: () -> Unit
) {
    var isSearchVisible by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Favourites") },
                    onClick = {
                        navController.navigate("movieFavourites") // Switches to the favourites
                        isMenuExpanded = false
                    }
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSearchVisible) {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Movie App")
                }
            }
        },
        actions = {
            IconButton(onClick = onSortToggle) {
                val sortIcon = if (sortByRating) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.Filled.KeyboardArrowUp
                }
                Icon(imageVector = sortIcon, contentDescription = "Sort by rating")
            }
            IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
            }
        }
    )
}

