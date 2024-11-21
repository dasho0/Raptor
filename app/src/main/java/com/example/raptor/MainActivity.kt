package com.example.raptor

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raptor.database.entities.Song
import com.example.raptor.ui.theme.RaptorTheme
import com.example.raptor.viewmodels.LibraryViewModel
import com.example.raptor.viewmodels.PlayerViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var libraryViewModel : LibraryViewModel

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun SwipeControl() {
        val pagerState = rememberPagerState(initialPage = 1)

        HorizontalPager(
            count = 4, // Number of screens
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> RecorderView()
                1 -> AuthorsView()
                2 -> AlbumView()
                3 -> SongPlayUI(application)
            }
        }
    }

    @Composable
    fun AuthorsView() {
        val context = LocalContext.current
        libraryViewModel.PrepareFilePicker()

        val playerViewModel = PlayerViewModel(application)

        val isPlaying by remember { playerViewModel.isPlayingUI }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            Button(
                onClick = {
                    libraryViewModel.pickFiles()
                    // coroutineScope.launch {
                    //     libraryViewModel.processFiles(context)
                    // }
                    Log.d("MusicFilePicker", "-TEST-")
                },
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Text("Select Folder")
            }
        }
    }


    @Composable
    fun RecorderView() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dyktafon",
                color = Color.White,
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
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        text = "Lista Utworów",
                        color = Color.Black,
                        fontSize = 42.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (songTags.value.isEmpty()) {
                    item {
                        Text(
                            text = "Brak danych do wyświetlenia",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    items(songTags.value.size) { index ->
                        val song = songTags.value[index]
                        songTags.value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                .background(Color(0xff3aa8c1))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = buildString {
                                    // append("Album: ${song.album ?: "Unknown"}\n")
                                    // append("Wykonawca: ${song.artists ?: "Unknown"}\n")
                                    // append("Tytuł: ${song.title ?: "Unknown"}\n")
                                    // append("Rok Wydania: ${null ?: "Unknown"}")
                                },
                                fontSize = 18.sp,
                                color = Color.DarkGray
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
            modifier = modifier
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
        enableEdgeToEdge()
        setContent {
            RaptorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
                        SwipeControl()
                    }
                }
            }
        }
    }
}

@Composable
fun SongPlayUI(application: Application) {
    val playerViewModel = hiltViewModel<PlayerViewModel>()
    val isPlaying by playerViewModel.isPlayingUI

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        Button(
            modifier = Modifier.align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(
             if(isPlaying) Color.Blue else Color.Red
            ),
            onClick = {
                playerViewModel.playPauseSong(Song(0, null, null, null))
            }
        ) {
            Text(text = if(isPlaying) "Zapauzuj piosenkę" else "Zagraj piosenkę")
        }

        Text(text = isPlaying.toString())
    }
}