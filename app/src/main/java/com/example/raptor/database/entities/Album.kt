package com.example.raptor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Album(
    @PrimaryKey(autoGenerate = true) val albumId: Long = 0,
    val title: String,
    val coverUri: String?,
)