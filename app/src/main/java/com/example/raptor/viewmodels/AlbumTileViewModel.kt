package com.example.raptor.viewmodels

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
    val cover = imageManager.collectBitmapFromUri(album.coverUri?.toUri())
}