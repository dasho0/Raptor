package com.example.raptor.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raptor.viewmodels.PlayerViewModel
import java.nio.file.WatchEvent

@Composable
fun MediaControls(playerViewModel: PlayerViewModel) {
    val mainButtonImage by playerViewModel
        .currentIconImage
        .collectAsState(Icons.Filled.PlayArrow)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                playerViewModel.playPauseRestartCurrentSong()
            },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = mainButtonImage,
                contentDescription = "Play Button",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun CurrentSongInfo(title: String?, artists: String?, modifier: Modifier) {
    Column(
        modifier = modifier
    ) {
        Text(
            title?: "Unknown",
            color = Color.White,
            fontSize = 24.sp,
        )
        Text(
            artists ?: "Unknown",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SongPlayUI(songId: Long) {
    val playerViewModel = hiltViewModel<PlayerViewModel>()

    val progressBarPosition by playerViewModel.progressBarPosition
        .collectAsState(initial = 0)
    val title by playerViewModel.currentSongTitle.collectAsState("Unknown")
    val artists by playerViewModel.currentSongArtists.collectAsState("Unknown")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CurrentSongInfo(
            title,
            artists,
            modifier = Modifier
                .align(Alignment.Center)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 64.dp)
        ) {
            Slider(
                value = progressBarPosition.toFloat(),
                onValueChange = { playerViewModel.onProgressBarMoved(it) },
                enabled = true,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )

            MediaControls(playerViewModel)
        }
    }
}