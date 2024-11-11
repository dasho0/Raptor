package com.example.raptor.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author

data class AlbumWithAuthors(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "albumId",
        entityColumn = "name",
        associateBy = Junction(AlbumAuthorCrossRef::class)
    )
    val authors: List<Author>
)
