package com.example.raptor.database.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [Index(value = ["albumId"])]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val songId: Long = 0,
    val title: String?,
    val albumId: Long?,
    val fileUri: String?,
)