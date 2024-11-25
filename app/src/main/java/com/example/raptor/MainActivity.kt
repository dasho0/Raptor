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
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.raptor.database.entities.Song
import com.example.raptor.screens.SongPlayUI
import com.example.raptor.ui.theme.RaptorTheme
import com.example.raptor.viewmodels.LibraryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

// FIXME: libraryViewModel has no place being here and it should be removed. it's usage should be
//  restricted to the corresponding screens. This function should either have it's own viewmodel
//  or simply direct access to the db.
@Composable
fun MainScreen(libraryViewModel: LibraryViewModel = hiltViewModel<LibraryViewModel>()) {
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
        composable(
            route = "songs/{album}",
            arguments = listOf(navArgument("album") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("album")
            assert(albumId != 0L)
            SongsScreen(navController, libraryViewModel, albumId)
        }

        composable(
            route = "player/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId")
            assert(songId != 0L)
            SongPlayUI()
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
                    // FIXME: co jak autor ma 3 różne człony w nazwie albo więcej
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// TODO: make a separate viewmodel for this screen
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
            AlbumTile(albumName = album.title, onClick = {
                Log.d(javaClass.simpleName, "Album id passed to navhost: ${album.albumId}")
                assert(album.albumId != 0L)
                navController.navigate("songs/${album.albumId}")
            })
        }
    }
}

@Composable
fun AlbumTile(albumName: String, onClick: () -> Unit) {
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
            val nameParts = albumName.split(" ", limit = 2) // Split album name into parts
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
                    // FIXME: przy nazwach albumu to juz wgl sensu nie ma jak one maja jeszcze
                    //  wiecej czlonow
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// TODO: make a separate viewmodel for this screen
@Composable
fun SongsScreen(
    navController: NavHostController, libraryViewModel: LibraryViewModel, albumId:
    Long?
) {
   assert(albumId != null)

    val songs by libraryViewModel.getSongsByAlbum(albumId!!).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs) { song ->
            SongItem(song = song, navController)
        }
    }
}

@Composable
fun SongItem(song: Song, navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray, RectangleShape)
            .padding(16.dp)
            .clickable(onClick = {
                Log.d("SongsScreen", "Clicked on song: $song")

                navController.navigate("player/${song.songId}")
            })
    ) {
        Text(
            text = song.title ?: "Unknown Song",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
