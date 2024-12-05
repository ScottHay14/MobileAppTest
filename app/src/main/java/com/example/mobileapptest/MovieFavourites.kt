package com.example.mobileapptest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieFavourites(
    navController: NavController,
    favouriteMovies: List<Movie>,
    onRemoveFromFavourites: (Movie) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = { Text("Movie Favourites") },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(favouriteMovies) { movie ->
                MovieCard(
                    movie = movie,
                    isFavourite = true,
                    onFavouriteChange = { isSelected ->
                        if (!isSelected) {
                            onRemoveFromFavourites(movie)
                        }
                    }
                )
            }
        }
    }
}

