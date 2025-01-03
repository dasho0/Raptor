package com.example.raptor.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import com.example.raptor.viewmodels.PlayerViewModel
import java.nio.file.WatchEvent
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MediaControls(playerViewModel: PlayerViewModel) {
    val mainButtonImage by playerViewModel.currentIconImage.collectAsState(initial = Icons.Filled.PlayArrow)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { playerViewModel.skipTrack(false) }
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Skip Backward",
                modifier = Modifier.size(42.dp)
            )
        }

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

        IconButton(
            onClick = { playerViewModel.skipTrack(true) }
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skip Forward",
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
fun CurrentSongInfo(title: String?, artists: String?, cover: ImageBitmap, modifier: Modifier) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            bitmap = cover,
            contentDescription = "Album Art",
            modifier = Modifier.size(300.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = modifier.height(8.dp))

        Column(
            modifier = modifier.width(300.dp)
        ) {
            Text(
                title ?: "Unknown",
                color = textColor,
                fontSize = 24.sp,
                textAlign = TextAlign.Left
            )

            Text(
                artists ?: "Unknown",
                color = textColor,
                fontSize = 16.sp,
                textAlign = TextAlign.Left
            )
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SongPlayUI(songId: Long) {
    val playerViewModel = hiltViewModel<PlayerViewModel>()
    val context = LocalContext.current

    val progressBarPosition by playerViewModel.progressBarPosition.collectAsState(initial = 0f)
    val title by playerViewModel.currentSongTitle.collectAsState("Unknown")
    val artists by playerViewModel.currentSongArtists.collectAsState("Unknown")
    val cover by playerViewModel.currentCover.collectAsState(ImageBitmap(1,1))
    val waveform by playerViewModel.currentWaveform.collectAsState(emptyList())

    LaunchedEffect(Unit) {
        playerViewModel.toast.collect() {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()

        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Display cover and track info in the center
        CurrentSongInfo(
            title,
            artists,
            cover,
            modifier = Modifier
                .align(Alignment.Center)
        )

        // Waveform and controls at the bottom
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 64.dp)
        ) {
            WaveformSeekBar(
                waveform = waveform,
                progress = progressBarPosition,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(80.dp),
                onProgressChanged = { playerViewModel.onProgressBarMoved(it) }
            )
            MediaControls(playerViewModel)
        }
    }
}