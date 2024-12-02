package com.example.raptor.viewmodels

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.raptor.ImageManager
import com.example.raptor.database.DatabaseManager
import com.example.raptor.database.entities.Album
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@Deprecated("this will be removed")
@HiltViewModel(assistedFactory = AlbumTileViewModel.Factory::class)
class AlbumTileViewModel @AssistedInject constructor(
    @Assisted album: Album,
    private val imageManager: ImageManager
): ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(album: Album): AlbumTileViewModel
    }

    val title = album.title

    // FIXME: this happens on the UI thread
    val cover = imageManager.collectBitmapFromUri(album.coverUri?.toUri())

    init {
        Log.d(javaClass.simpleName, "Album passed to viewmodel: $album")
    }
}