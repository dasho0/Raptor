package com.example.raptor.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.raptor.viewmodels.PlayerViewModel

@Composable
fun MediaControls(playerViewModel: PlayerViewModel,) {
    val mainButtonImage by playerViewModel
        .currentIconImage
        .collectAsState(Icons.Filled.PlayArrow)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = {
            playerViewModel.playPauseRestartCurrentSong()
        }) {
            Icon(
                imageVector = mainButtonImage,
                contentDescription = "Play Button"
            )
        }
    }
}

@Composable
fun SongPlayUI(songId: Long) {
    val playerViewModel = hiltViewModel<PlayerViewModel>()

    val progressBarPosition by playerViewModel.progressBarPosition
        .collectAsState(initial = 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 16.dp)
        ) {
            Slider(
                value = progressBarPosition.toFloat(),
                onValueChange = { playerViewModel.onProgressBarMoved(it) },
                enabled = true
            )

            MediaControls(playerViewModel)

        }
    }
}