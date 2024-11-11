package com.example.raptor.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song

@Database(entities = [Song::class, Author::class, Album::class, AlbumAuthorCrossRef::class], version
= 1)
abstract class LibraryDb : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}