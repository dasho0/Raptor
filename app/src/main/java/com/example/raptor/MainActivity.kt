package com.example.raptor

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.raptor.database.entities.Song
import com.example.raptor.screens.SongPlayUI
import com.example.raptor.ui.theme.RaptorTheme
import com.example.raptor.viewmodels.AlbumsScreenViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.raptor.viewmodels.LibraryViewModel
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

@AndroidEntryPoint
class MainActivity : FragmentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val _isDarkTheme = mutableStateOf(false)
    private val isDarkTheme: State<Boolean> = _isDarkTheme

    private val _isAuthenticated = mutableStateOf(false)
    private val isAuthenticated: State<Boolean> = _isAuthenticated

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val biometricAuthenticator = BiometricAuthenticator(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        enableEdgeToEdge()

        setContent {
            val darkTheme by isDarkTheme
            val authenticated by isAuthenticated
            RaptorTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (authenticated) {
                        MainScreen()
                    } else {
                        AuthenticationScreen(
                            onAuthenticate = {
                                promptBiometricAuthentication(biometricAuthenticator)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun promptBiometricAuthentication(biometricAuthenticator: BiometricAuthenticator) {
        biometricAuthenticator.PromptBiometricAuth(
            title = "Authentication Required",
            subtitle = "Please authenticate to proceed",
            negativeButtonText = "Cancel",
            fragmentActivity = this,
            onSuccess = {
                runOnUiThread {
                    _isAuthenticated.value = true
                }
            },
            onFailed = {
            },
            onError = { errorCode, errorString ->
            }
        )
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

// MainScreen function included here
@Composable
fun MainScreen(libraryViewModel: LibraryViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backgroundPainter: Painter = painterResource(R.drawable.persona_rings_background)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .paint( painter = painterResource(id = R.drawable.persona_rings_background) )
    ) {
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreenContent(navController, libraryViewModel, backgroundPainter)
            }
            composable("albums/{author}") { backStackEntry ->
                val author = backStackEntry.arguments?.getString("author") ?: ""
                AlbumsScreen(navController, author, backgroundPainter)
            }
            composable(
                route = "songs/{album}",
                arguments = listOf(navArgument("album") { type = NavType.LongType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getLong("album")
                assert(albumId != 0L)

                SongsScreen(navController, libraryViewModel, albumId, backgroundPainter)
            }
            composable(
                route = "player/{songId}",
                arguments = listOf(navArgument("songId") { type = NavType.LongType })
            ) { backStackEntry ->
                val songId = backStackEntry.arguments?.getLong("songId")
                assert(songId != 0L)

                SongPlayUI(songId!!, backgroundPainter) //FIXME: we ball
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    navController: NavHostController,
    libraryViewModel: LibraryViewModel,
    backgroundPainter: Painter
) {
    val expanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint( painter = painterResource(id = R.drawable.persona_rings_background) ) // Set background color here
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
                    .paint( painter = painterResource(id = R.drawable.persona_rings_background) ),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns), // Dynamically set columns based on orientation
                modifier = Modifier
                    .fillMaxSize()
                    .paint( painter = painterResource(id = R.drawable.persona_rings_background) ), // Set background color here
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
fun AlbumsScreen(
    navController: NavHostController,
    author: String,
    backgroundPainter: Painter,
) {
    val viewModel: AlbumsScreenViewModel = hiltViewModel<AlbumsScreenViewModel, AlbumsScreenViewModel.Factory>(
        creationCallback = { it.create(author) }
    )

    val albumsAndCovers by viewModel.albumsAndCovers.collectAsState(emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint( painter = painterResource(id = R.drawable.persona_rings_background) ) // Set background color here
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albumsAndCovers, key = { it.first.albumId }) { pair ->
                val album = pair.first
                val cover = pair.second

                AlbumTile(
                    albumName = album.title,
                    cover = cover,
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
fun AlbumTile(albumName: String, cover: ImageBitmap, onClick: () -> Unit, modifier: Modifier) {
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
    navController: NavHostController,
    libraryViewModel: LibraryViewModel,
    albumId: Long?,
    backgroundPainter: Painter
) {
    assert(albumId != null)

    val songs by libraryViewModel.getSongsByAlbum(albumId!!).collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint( painter = painterResource(id = R.drawable.persona_rings_background) ) // Set background color here
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

@Composable
fun AuthenticationScreen(onAuthenticate: () -> Unit) {
    LaunchedEffect(Unit) {
        onAuthenticate()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Please authenticate to proceed",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onAuthenticate() }) {
                Text("Retry")
            }
        }
    }
}
