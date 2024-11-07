package com.example.raptor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String?,
    val artist: String?,
    val album: String?
)