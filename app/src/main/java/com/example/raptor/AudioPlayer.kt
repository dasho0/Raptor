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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * This class handles audio playback in the media player, by wrapping around an `ExoPlayer` instance
 *
 * It handles the logic of media controls, what song to play, at what time, etc.
 */
class AudioPlayer @Inject constructor(@ApplicationContext context: Context) {
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

    /**
     * Gets the current playback position of the currently playing song in milliseconds
     */
    val currentPosition get() = player.currentPosition
    /**
     * Gets the duration of the currently playing song in milliseconds
     */
    val currentDuration get() = player.duration

    /**
     * Enum describing all states that the player can be in
     */
    enum class PlaybackStates {
        STATE_BUFFERING,
        STATE_ENDED,
        STATE_IDLE,
        STATE_READY,
        STATE_PLAYING,
        STATE_PAUSED,
    }

    /**
     * Gets the current playback state of the player, as described in `PlaybackStates`
     */
    val playbackState = MutableStateFlow(PlaybackStates.STATE_IDLE)

    /**
     * Plays a given `Song` and sets it as the current song
     *
     * Player can't be in the playing state at the time of calling.
     *
     * @param song `Song` to play
     */
    fun playSong(song: Song) {
        assert(isPlayingInternal == false)
        assert(!player.isPlaying)
        assert(playbackState.value != PlaybackStates.STATE_PLAYING)
        Log.d(javaClass.simpleName, "calling playSong")

        Log.d(AudioPlayer::class.simpleName, "Song uri: ${song.fileUri}")

        updateListeners(song)

        if(player.currentMediaItem.hashCode() != MediaItem.fromUri(song.fileUri!!.toUri())
                 .hashCode()) { //FIXME: we ball
            player.apply {
                setMediaItem(MediaItem.fromUri(song.fileUri.toUri()))
                prepare()
            }

            Log.d(javaClass.simpleName, "playSong(), songs don't match")
        }

        player.play()

    }

    /**
     * Restarts playback from the beginning of the current song
     */
    fun restartCurrentPlayback() {
        player.seekTo(0)
        player.play()
    }

    /**
     * Pauses the currently playing song
     *
     * Player must be in the playing state when calling
     */
    fun pause() {
        assert(playbackState.value == PlaybackStates.STATE_PLAYING)
        assert(isPlayingInternal)

        Log.d(javaClass.simpleName, "calling pause")
        player.pause()

    }

    /**
     * Changes the currently playing song
     *
     * @param song song to change to
     */
    fun changeSong(song: Song) {
        player.stop()
        playSong(song)
        player.seekTo(0)
    }

    /**
     * Changes the timeline position of the playback to some value in milliseconds
     *
     * @param time value in milliseconds to change playback to
     */
    fun changeCurrentPosition(time: Long) {
        player.seekTo(time)
    }

    /**
     * Releases the internal `ExoPlayer` object
     *
     * Must be called in order to free memory, when an object of this class is no longer used
     */
    fun releasePlayer() {
        player.release()
    }
}