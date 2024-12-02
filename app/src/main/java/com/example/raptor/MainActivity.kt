package com.example.raptor

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.Song
import com.example.raptor.screens.SongPlayUI
import com.example.raptor.ui.theme.RaptorTheme
import com.example.raptor.viewmodels.AlbumTileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.coroutines.EmptyCoroutineContext.get
import com.example.raptor.viewmodels.LibraryViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val _isDarkTheme = mutableStateOf(false)
    private val isDarkTheme: State<Boolean> = _isDarkTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        enableEdgeToEdge()

        setContent {
            val darkTheme by isDarkTheme
            RaptorTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lightLevel = event.values[0]
            val maxLightLevel = lightSensor?.maximumRange ?: 10000f

            _isDarkTheme.value = lightLevel < 0.4 * maxLightLevel
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nothing here!
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

            SongPlayUI(songId!!) //FIXME: we ball
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(navController: NavHostController, libraryViewModel: LibraryViewModel) {
    val expanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Set background color here
    ) {
        TopAppBar(
            title = { Text("Raptor") },
            actions = {
                IconButton(onClick = { expanded.value = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select Folder") },
                        onClick = {
                            libraryViewModel.pickFiles()
                            expanded.value = false
                        }
                    )
                }
            },
        )
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
                    .background(MaterialTheme.colorScheme.background),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns), // Dynamically set columns based on orientation
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background), // Set background color here
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
}

@Composable
fun AuthorTile(author: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(140.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
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
                    text = nameParts.getOrNull(0) ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = nameParts.getOrNull(1) ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun AlbumsScreen(navController: NavHostController, libraryViewModel: LibraryViewModel, author: String) {
    val albums by libraryViewModel.getAlbumsByAuthor(author).collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Set background color here
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.albumId }) { album ->
                AlbumTile(
                    album = album,
                    onClick = {
                        Log.d("MainActivity", "Album id passed to navhost: ${album.albumId}")
                        assert(album.albumId != 0L)
                        navController.navigate("songs/${album.albumId}")
                    },

                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
fun AlbumTile(album: Album, onClick: () -> Unit, modifier: Modifier) {
    Log.d("UI", "Album passed to AlbumTile: $album")

    val albumName by remember { mutableStateOf(album.title) }
    // Log.d("UI", "album from viewmodel???: $albumName")
    val cover = ImageBitmap(1,1)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(110.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Image(
                bitmap = cover,
                contentDescription = "$albumName cover",
                Modifier.scale(1.4f)
            )

        }

        Spacer(modifier.height(4.dp))

        val nameParts = albumName.split(" ", limit = 2) // Split album name into parts
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = nameParts.getOrNull(0) ?: "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = nameParts.getOrNull(1) ?: "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SongsScreen(
    navController: NavHostController, libraryViewModel: LibraryViewModel, albumId: Long?
) {
    assert(albumId != null)

    val songs by libraryViewModel.getSongsByAlbum(albumId!!).collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Set background color here
    ) {
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
}

@Composable
fun SongItem(song: Song, navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
            .clickable(onClick = {
                Log.d("SongsScreen", "Clicked on song: $song")

                navController.navigate("player/${song.songId}")
            })
    ) {
        Text(
            text = song.title ?: "Unknown Song",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}