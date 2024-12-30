package com.example.raptor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Dataclass corresponding to the `Album` entity in the database.
 *
 * @param albumId Primary key
 * @param title Title of the album
 * @param coverUri Uri of the album cover from app storage
 */
@Entity
data class Album(
    @PrimaryKey(autoGenerate = true) val albumId: Long = 0,
    val title: String,
    val coverUri: String?,
)