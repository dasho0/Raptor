package com.example.raptor.database.entities
import androidx.room.Entity

@Entity(primaryKeys = ["albumId", "name"])
data class AlbumAuthorCrossRef(
    val albumId: Long,
    val name: String
)