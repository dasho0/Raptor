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

    @Transaction
    @Query("""
        SELECT s.* FROM Song s
        INNER JOIN Album a ON s.albumId = a.albumId
        WHERE a.title = :album
    """)
    fun getSongsByAlbum(album: String): Flow<List<Song>>

    @Query("SELECT * FROM Author")
    fun getAllAuthorsFlow(): Flow<List<Author>>

    @Transaction
    @Query("""
        SELECT a.* FROM Album a
        INNER JOIN AlbumAuthorCrossRef c ON a.albumId = c.albumId
        WHERE c.name = :author
    """)
    fun getAlbumsByAuthor(author: String): Flow<List<Album>>
}