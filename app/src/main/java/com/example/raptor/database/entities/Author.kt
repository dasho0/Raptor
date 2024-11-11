package com.example.raptor.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Author (
    @PrimaryKey val name: String
)