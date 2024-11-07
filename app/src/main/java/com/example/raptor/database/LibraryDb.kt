package com.example.raptor.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.raptor.database.entities.SongTable

@Database(entities = [SongTable::class], version = 1)
abstract class LibraryDb : RoomDatabase() {
    abstract fun songDao(): SongDao
}