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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.raptor.database.entities.Song
import com.example.raptor.screens.SongPlayUI
import com.example.raptor.ui.theme.RaptorTheme
import com.example.raptor.viewmodels.AlbumTileViewModel
import com.example.raptor.viewmodels.AlbumsScreenViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.raptor.viewmodels.LibraryViewModel
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import com.example.raptor.database.entities.Album

lateinit var sensorManager: SensorManager
var lightSensor: Sensor? = null
val _isDarkTheme = mutableStateOf(false)
val isDarkTheme: State<Boolean> = _isDarkTheme

val _isAuthenticated = mutableStateOf(false)
val isAuthenticated: State<Boolean> = _isAuthenticated

@AndroidEntryPoint
class MainActivity : FragmentActivity(), SensorEventListener {
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
                ){
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
    }
}

@Composable
fun MainScreen(libraryViewModel: LibraryViewModel = hiltViewModel()) {
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
            AlbumsScreen(navController, author)
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

    libraryViewModel.PrepareFilePicker()

    val authors by libraryViewModel.authors.collectAsState(initial = emptyList())
    val configuration = LocalConfiguration.current

    Log.d("UI", "Authors in db: $authors")
    Text("TEST: $authors", color = Color.White)

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

        // Determine the number of columns based on orientation
        val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            4 // 4 tiles in a row for horizontal mode
        } else {
            3 // 3 tiles in a row for vertical mode
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns), // Dynamically set columns based on orientation
            modifier = Modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(id = R.drawable.tans_background_a),
                    contentScale = ContentScale.Crop, // Rozciąga obraz w sposób zachowujący proporcje
                    alignment = Alignment.Center     // Wyśrodkowanie obrazu
                ),
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
) {
    val viewModel: AlbumsScreenViewModel = hiltViewModel<AlbumsScreenViewModel, AlbumsScreenViewModel.Factory>(
        creationCallback = { it.create(author) }
    )

    val albumsAndCovers by viewModel.albumsAndCovers.collectAsState(emptyList())
    var selectedAlbum by rememberSaveable { mutableStateOf<Pair<Album, ImageBitmap>?>(null) }

    if (selectedAlbum == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(id = R.drawable.tans_background_a),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                ),
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
                            selectedAlbum = pair
                        },
                        modifier = Modifier,
                    )
                }
            }
        }
    } else {
        if (isPortrait()) {
            PortraitView(selectedAlbum = selectedAlbum!!, navController = navController)
        } else {
            LandscapeView(selectedAlbum = selectedAlbum!!, navController = navController)
        }
    }
}

@Composable
fun PortraitView(selectedAlbum: Pair<Album, ImageBitmap>, navController: NavHostController) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().paint(
            painter = painterResource(id = R.drawable.tans_background_a),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        ),

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Image(
                bitmap = selectedAlbum.second,
                contentDescription = "${selectedAlbum.first.title} cover",
                modifier = Modifier
                    .size(200.dp)
                    .weight(1f)
            )
            Text(
                text = selectedAlbum.first.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            )
        }
        SongsScreen(
            navController = navController,
            libraryViewModel = hiltViewModel(),
            albumId = selectedAlbum.first.albumId
        )
    }
}

@Composable
fun LandscapeView(selectedAlbum: Pair<Album, ImageBitmap>, navController: NavHostController) {
    Row(
        modifier = Modifier.fillMaxSize().paint(
            painter = painterResource(id = R.drawable.tans_background_a),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
                .weight(1f)
        ) {
            Image(
                bitmap = selectedAlbum.second,
                contentDescription = "${selectedAlbum.first.title} cover",
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = selectedAlbum.first.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        SongsScreen(
            navController = navController,
            libraryViewModel = hiltViewModel(),
            albumId = selectedAlbum.first.albumId,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
fun SongsScreen(
    navController: NavHostController,
    libraryViewModel: LibraryViewModel,
    albumId: Long?,
    modifier: Modifier = Modifier
) {
    assert(albumId != null)

    val songs by libraryViewModel.getSongsByAlbum(albumId!!).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
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
fun isPortrait(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_PORTRAIT
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
