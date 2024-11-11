package com.example.raptor.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.Song

data class AlbumWithSongs(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "albumId",
        entityColumn = "albumId"
    )
    val songs: List<Song>
)
