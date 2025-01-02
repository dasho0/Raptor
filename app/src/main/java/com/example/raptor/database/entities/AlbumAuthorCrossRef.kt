package com.example.raptor.database.entities
import androidx.room.Entity

/**
 * Junction table used in the relation between the `Album` and `Author` tables
 */
@Entity(primaryKeys = ["albumId", "name"])
data class AlbumAuthorCrossRef(
    val albumId: Long,
    val name: String
)