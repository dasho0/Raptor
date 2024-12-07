package com.example.raptor.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.example.raptor.ImageManager
import com.example.raptor.database.DatabaseManager
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.Author
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map

@HiltViewModel(assistedFactory = AlbumsScreenViewModel.Factory::class)
class AlbumsScreenViewModel @AssistedInject constructor(
    @Assisted currentAuthor: String,
    private val imageManager: ImageManager,
    private val database: DatabaseManager
): ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(author: String): AlbumsScreenViewModel
    }

    val albumsAndCovers = database.collectAlbumsByAuthorFlow(currentAuthor)
        .map { it.map { album ->
            Pair<Album, ImageBitmap>(album, imageManager.collectBitmapFromUri(album.coverUri?.toUri()))
        }}
    // val covers = albums.map { it.map { album ->
    //     imageManager.collectBitmapFromUri(album.coverUri?.toUri())
    // }}
}