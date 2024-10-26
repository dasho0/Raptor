package com.example.raptor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.raptor.ui.theme.RaptorTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState


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
            0 -> Recorder()
            1 -> Aut()
            2 -> Album()
        }
    }
}

@Composable
fun Aut() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Red
    ) {
        // Content for the first screen
    }
}


@Composable
fun Recorder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        // Content for the first screen
    }
}

@Composable
fun Album() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Cyan
    ) {
        // Content for the second screen
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