package com.example.raptor

import android.content.Context
import androidx.media3.common.AudioAttributes
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.raptor.database.entities.Song
import kotlinx.coroutines.flow.MutableStateFlow

class AudioPlayer(val context: Context) {
    private val player = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
    }

    private var currentSong: Song? = null

    private fun updateListeners(song: Song) {
        if(currentSong == song) {
            return
        }

        currentSong = song

        player.addListener(
            object: Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d(AudioPlayer::class.simpleName, if (isPlaying) "player is playing $song"
                    else "player is not playing")

                    // TODO: this feels hacky... maybe this callback should be remove alltogether
                    if(
                        player.playbackState == ExoPlayer.STATE_ENDED ||
                        player.playbackState == ExoPlayer.STATE_BUFFERING
                    ) {
                        return
                    }

                    playbackState.value = if(isPlaying)
                        PlaybackStates.STATE_PLAYING else PlaybackStates.STATE_PAUSED
                    Log.d(AudioPlayer::class.simpleName, "Playback: ${playbackState
                        .value.name}")
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when(playbackState) {
                        Player.STATE_BUFFERING -> {
                            Log.d(AudioPlayer::class.simpleName, "Playback: ${
                                PlaybackStates
                                .STATE_BUFFERING}")
                            this@AudioPlayer.playbackState.value = PlaybackStates.STATE_BUFFERING
                        }

                        Player.STATE_ENDED -> {
                            Log.d(AudioPlayer::class.simpleName, "Playback: ${
                                PlaybackStates
                                .STATE_ENDED.name}")
                            this@AudioPlayer.playbackState.value = PlaybackStates.STATE_ENDED
                        }

                        Player.STATE_IDLE -> {
                            Log.d(AudioPlayer::class.simpleName, "Playback: ${
                                PlaybackStates
                                .STATE_IDLE.name}")
                            this@AudioPlayer.playbackState.value = PlaybackStates.STATE_IDLE
                        }

                        Player.STATE_READY -> {
                            Log.d(AudioPlayer::class.simpleName, "Playback: ${
                                PlaybackStates
                                .STATE_READY.name}")
                            this@AudioPlayer.playbackState.value = PlaybackStates.STATE_READY
                        }
                    }
                }
            }
        )
    }

    private val isPlayingInternal get() = player.isPlaying

    val currentPosition get() = player.currentPosition
    val currentDuration get() = player.duration

    enum class PlaybackStates {
        STATE_BUFFERING,
        STATE_ENDED,
        STATE_IDLE,
        STATE_READY,
        STATE_PLAYING,
        STATE_PAUSED,
    }
    val playbackState = MutableStateFlow(PlaybackStates.STATE_IDLE)

    fun playSong(song: Song) {
        assert(isPlayingInternal == false)
        assert(!player.isPlaying)
        assert(playbackState.value != PlaybackStates.STATE_PLAYING)
        Log.d(javaClass.simpleName, "calling playSong")

        updateListeners(song)

        if(player.currentMediaItem.hashCode() != MediaItem.fromUri(song.fileUri!!.toUri())
                .hashCode()) {
            player.apply {
                setMediaItem(MediaItem.fromUri(song.fileUri!!.toUri())) //FIXME: we ball
                prepare()
            }

            Log.d(javaClass.simpleName, "playSong(), songs don't match")
        }

        player.play()

    }

    fun restartCurrentPlayback() {
        player.seekTo(0)
        player.play()
    }

    fun pause() {
        assert(playbackState.value == PlaybackStates.STATE_PLAYING)
        assert(isPlayingInternal)

        Log.d(javaClass.simpleName, "calling pause")
        player.pause()

    }

    fun changeCurrentPosition(time: Long) {
        player.seekTo(time)
    }
}