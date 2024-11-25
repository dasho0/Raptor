package com.example.raptor.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author

data class AuthorWithAlbums(
    @Embedded val author: Author,
    @Relation(
        parentColumn = "name",
        entityColumn = "albumId",
        associateBy = Junction(AlbumAuthorCrossRef::class)
    )
    val albums: List<Album>
)
