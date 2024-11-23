package com.example.raptor.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.Song
import com.example.raptor.database.relations.AlbumWithAuthors
import com.example.raptor.database.relations.AlbumWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
interface UIDao {
    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    // Get an album with its songs
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<List<AlbumWithSongs>>

    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumById(albumId: Long): Flow<Album>

    // Get an album with its authors
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithAuthors(albumId: Long): Flow<List<AlbumWithAuthors>>
}