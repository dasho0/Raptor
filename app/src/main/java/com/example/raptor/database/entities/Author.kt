package com.example.raptor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Dataclass corresponding to the `Author` entity in the database.
 *
 * @param name Primary key, name of the author, each name must be unique
 */
@Entity
data class Author (
    @PrimaryKey val name: String
)