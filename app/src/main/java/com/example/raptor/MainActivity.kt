package com.example.raptor

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.raptor.ui.theme.RaptorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaptorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StartScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun StartScreen(modifier: Modifier = Modifier) {
    val picker : FilePicker = FilePicker()
    picker.PrepareFilePicker()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) {
        Button(
            onClick = {
                picker.launch()
                Log.d("MusicFilePicker", "-TEST-")
            },
            modifier = modifier
                .align(Alignment.Center)
        ) {
            Text("Button")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    RaptorTheme {
        StartScreen()
    }
}