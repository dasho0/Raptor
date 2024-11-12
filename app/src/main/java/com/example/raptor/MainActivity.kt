package com.example.raptor

import android.content.Context
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.raptor.ui.theme.RaptorTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val _isDarkTheme = mutableStateOf(false)
    private val isDarkTheme: State<Boolean> = _isDarkTheme



    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun SwipeControl() {
        val pagerState = rememberPagerState(initialPage = 1)

        HorizontalPager(
            count = 3,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> RecorderView()
                1 -> AuthorsView()
                2 -> AlbumView()
            }
        }
    }

    @Composable
    fun AuthorsView() {
        val context = LocalContext.current
        libraryViewModel.PrepareFilePicker()
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Button(
                onClick = {
                    libraryViewModel.pickFiles()
                    Log.d("MusicFilePicker", "-TEST-")
                },
                modifier = Modifier.align(Alignment.Center),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Select Folder", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    @Composable
    fun RecorderView() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dyktafon",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 42.sp
            )
        }
    }

    @Composable
    fun AlbumView() {
        val songTags = libraryViewModel.libraryState.collectAsState(initial = emptyList())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        text = "Lista Utworów",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 42.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (songTags.value.isEmpty()) {
                    item {
                        Text(
                            text = "Brak danych do wyświetlenia",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    items(songTags.value.size) { index ->
                        val song = songTags.value[index]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = buildString {
                                    append("Album: ${song.album ?: "Unknown"}\n")
                                    append("Wykonawca: ${song.artist ?: "Unknown"}\n")
                                    append("Tytuł: ${song.title ?: "Unknown"}\n")
                                    append("Rok Wydania: ${null ?: "Unknown"}")
                                },
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        RaptorTheme {
            Greeting("Android")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        libraryViewModel = LibraryViewModel(application)
        super.onCreate(savedInstanceState)

        // Initialize sensor manager and light sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        enableEdgeToEdge()

        setContent {
            // Observe _isDarkTheme state and update theme dynamically
            val darkTheme by isDarkTheme
            RaptorTheme(darkTheme = darkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SwipeControl()
                    }
                }
            }
        }
    }
    // Sensor while in the foreground(app is on)
    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    // Sensor while not in the foreground(pauses the sensor)
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // Sensor controls(light intensity)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lightLevel = event.values[0]
            // Max light detection range, optimized for general use(outdoor + indoor)
            val maxLightLevel = lightSensor?.maximumRange ?: 10000f

            // Updates theme based on light level being below or above 50% of maximum
            // The level needs to be adjusted
            _isDarkTheme.value = lightLevel < 0.5 * maxLightLevel
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Here be nothing
    }
}
