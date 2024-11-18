package com.example.raptor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.raptor.database.entities.Song
import com.example.raptor.ui.theme.RaptorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaptorTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(libraryViewModel: LibraryViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreenContent(navController, libraryViewModel)
        }
        composable("albums/{author}") { backStackEntry ->
            val author = backStackEntry.arguments?.getString("author") ?: ""
            AlbumsScreen(navController, libraryViewModel, author)
        }
        composable("songs/{album}") { backStackEntry ->
            val album = backStackEntry.arguments?.getString("album") ?: ""
            SongsScreen(navController, libraryViewModel, album)
        }
    }
}

@Composable
fun MainScreenContent(navController: NavHostController, libraryViewModel: LibraryViewModel) {
    libraryViewModel.PrepareFilePicker() // Ensures launcher is ready

    val folderSelected by libraryViewModel.folderSelected
    val authors by libraryViewModel.authors.collectAsState(initial = emptyList())
    val configuration = LocalConfiguration.current

    // Determine the number of columns based on orientation
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        4 // 4 tiles in a row for horizontal mode
    } else {
        3 // 3 tiles in a row for vertical mode
    }

    if (!folderSelected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = {
                libraryViewModel.pickFiles()
            }) {
                Text("Select Folder")
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns), // Dynamically set columns based on orientation
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(authors) { author ->
                AuthorTile(author = author.name, onClick = {
                    navController.navigate("albums/${author.name}")
                })
            }
        }
    }
}

@Composable
fun AuthorTile(author: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp) // Adjust the width of the tile
            .height(140.dp), // Adjust the height of the tile
        shape = RectangleShape // Keeps sharp, square edges
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val nameParts = author.split(" ", limit = 2) // Split name into parts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = nameParts.getOrNull(0) ?: "", // First name or empty if not available
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = nameParts.getOrNull(1) ?: "", // Surname or empty if not available
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AlbumsScreen(navController: NavHostController, libraryViewModel: LibraryViewModel, author: String) {
    val albums by libraryViewModel.getAlbumsByAuthor(author).collectAsState(initial = emptyList())

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums) { album ->
            AlbumTile(album = album.title, onClick = {
                navController.navigate("songs/${album.title}")
            })
        }
    }
}

@Composable
fun AlbumTile(album: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(140.dp), // Increased height for full names
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val nameParts = album.split(" ", limit = 2) // Split album name into parts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = nameParts.getOrNull(0) ?: "", // First line or empty if not available
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = nameParts.getOrNull(1) ?: "", // Second line or empty if not available
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SongsScreen(navController: NavHostController, libraryViewModel: LibraryViewModel, album: String) {
    val songs by libraryViewModel.getSongsByAlbum(album).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs) { song ->
            SongItem(song = song)
        }
    }
}

@Composable
fun SongItem(song: Song) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray, RectangleShape)
            .padding(16.dp)
    ) {
        Text(
            text = song.title ?: "Unknown Song",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
