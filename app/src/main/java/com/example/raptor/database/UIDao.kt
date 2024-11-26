package com.example.raptor.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song
import com.example.raptor.database.relations.AlbumWithAuthors
import com.example.raptor.database.relations.AlbumWithSongs
import com.example.raptor.database.relations.AuthorWithAlbums
import kotlinx.coroutines.flow.Flow

@Dao
interface UIDao {
    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM Song WHERE songId = :songId")
    fun collectSongFromId(songId: Long): Flow<Song> // does this really need to be a flow?

    // Get an album with its songs
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<AlbumWithSongs>

    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumById(albumId: Long?): Flow<Album>

    // Get an album with its authors
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithAuthors(albumId: Long?): Flow<AlbumWithAuthors?>

    @Query("SELECT * FROM Author")
    fun getAllAuthorsFlow(): Flow<List<Author>>

    // Get an author with their albums
    @Transaction
    @Query("SELECT * FROM author WHERE name = :name")
    fun getAuthorWithAlbums(name: String): Flow<AuthorWithAlbums>
}