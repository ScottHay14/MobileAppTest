package com.example.mobileapptest

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavouritesViewModel(private val context: Context) : ViewModel() {

    private val sharedPrefsHelper = SharedPrefsHelper(context)

    // Mutable state for the favourite movies list
    private val _favouriteMovies = mutableStateOf<List<Movie>>(emptyList())
    val favouriteMovies: State<List<Movie>> get() = _favouriteMovies

    init {
        // Load favourite movies from SharedPreferences when the ViewModel is created
        _favouriteMovies.value = sharedPrefsHelper.getFavouriteMovies()
    }

    // Adds movie to favourites
    fun addToFavourites(movie: Movie) {
        if (!_favouriteMovies.value.contains(movie)) {
            val updatedFavourites = _favouriteMovies.value + movie
            _favouriteMovies.value = updatedFavourites
            sharedPrefsHelper.saveFavouriteMovies(updatedFavourites)
        }
    }

    // Updates removed favourites
    fun removeFromFavourites(movie: Movie) {
        val updatedFavourites = _favouriteMovies.value.filterNot { it.id == movie.id }
        _favouriteMovies.value = updatedFavourites
        sharedPrefsHelper.saveFavouriteMovies(updatedFavourites)
    }
}

class SharedPrefsHelper(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Saves the favourite movies
    fun saveFavouriteMovies(favourites: List<Movie>) {
        val json = gson.toJson(favourites)
        sharedPreferences.edit().putString("favourite_movies", json).apply()
    }

    // Get the list of favourite movies from SharedPreferences
    fun getFavouriteMovies(): List<Movie> {
        val json = sharedPreferences.getString("favourite_movies", null)
        return if (json != null) {
            val type = object : TypeToken<List<Movie>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}