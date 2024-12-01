package com.example.raptor.viewmodels

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raptor.ImageManager
import com.example.raptor.database.DatabaseManager
import com.example.raptor.database.entities.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val databaseManager: DatabaseManager,
    private val imageManager: ImageManager,
): ViewModel() {
    private val currentAlbums: MutableStateFlow<List<Album>?> = MutableStateFlow(null)

    val albumCovers = currentAlbums.map { albums ->
        albums?.map { album ->
            imageManager.collectBitmapFromUri(album.coverUri?.toUri())
        }
    }

    init {
        viewModelScope.launch {
            databaseManager.collectAlbumsByAuthorFlow(savedStateHandle["author"]!!).collect {
                Log.d(javaClass.simpleName, "Collecting albums of author: $it")
                currentAlbums.value = it
            }  // FIXME:we ball
        }

    }
}