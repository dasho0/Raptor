package com.example.raptor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.raptor.ui.theme.RaptorTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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


@OptIn(ExperimentalPagerApi::class)
@Composable
fun SwipeControl() {
    val pagerState = rememberPagerState(initialPage = 1)

    HorizontalPager(
        count = 3, // Number of screens
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
    LibraryManager.PrepareFilePicker(context)
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) {
        Button(
            onClick = {
                LibraryManager.pickFiles()
                coroutineScope.launch {
                    LibraryManager.processFiles(context)
                }
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
    val songs = LibraryManager.getAllTags()

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

            if (songs.isEmpty()) {
                item {
                    Text(
                        text = "Brak danych do wyświetlenia",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            } else {
                items(songs) { song ->
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
                                append("Album: ${song.album ?: "Unknown"}\n")
                                append("Wykonawca: ${song.artist ?: "Unknown"}\n")
                                append("Tytuł: ${song.title ?: "Unknown"}\n")
                                append("Rok Wydania: ${song.releaseYear ?: "Unknown"}")
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