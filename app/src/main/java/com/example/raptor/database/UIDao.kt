package com.example.raptor.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song
import com.example.raptor.database.relations.AlbumWithAuthors
import com.example.raptor.database.relations.AlbumWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
interface UIDao {
    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<AlbumWithSongs>

    @Query("SELECT * FROM Song WHERE albumId = :albumId")
    fun getSongsByAlbumId(albumId: Long): Flow<List<Song>>

    @Query("SELECT * FROM Author")
    fun getAllAuthorsFlow(): Flow<List<Author>>
}
