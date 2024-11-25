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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.raptor.database.entities.Song
import com.example.raptor.ui.theme.RaptorTheme

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val _isDarkTheme = mutableStateOf(false)
    private val isDarkTheme: State<Boolean> = _isDarkTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize sensor manager and light sensor
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

    // Sensor controls (light intensity)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lightLevel = event.values[0]
            val maxLightLevel = lightSensor?.maximumRange ?: 10000f

            // Updates theme based on light level being below or above 50% of maximum
            _isDarkTheme.value = lightLevel < 0.5 * maxLightLevel
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No implementation needed
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
        composable(
            route = "songs/{album}",
            arguments = listOf(navArgument("album") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("album")
            assert(albumId != 0L)
            SongsScreen(navController, libraryViewModel, albumId!!)
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
            .width(120.dp) // Adjust the width of the tile
            .height(140.dp), // Adjust the height of the tile
        shape = RectangleShape, // Keeps sharp, square edges
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
                    text = nameParts.getOrNull(0) ?: "", // First name or empty if not available
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = nameParts.getOrNull(1) ?: "", // Surname or empty if not available
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
            items(albums) { album ->
                AlbumTile(albumName = album.title, onClick = {
                    Log.d("MainActivity", "Album id passed to navhost: ${album.albumId}")
                    assert(album.albumId != 0L)
                    navController.navigate("songs/${album.albumId}")
                })
            }
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
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
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
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = nameParts.getOrNull(1) ?: "", // Second line or empty if not available
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
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
                SongItem(song = song)
            }
        }
    }
}

@Composable
fun SongItem(song: Song) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, RectangleShape)
            .padding(16.dp)
    ) {
        Text(
            text = song.title ?: "Unknown Song",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
