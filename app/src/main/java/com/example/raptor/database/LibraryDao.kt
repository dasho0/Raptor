package com.example.raptor.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song
import com.example.raptor.database.relations.AlbumWithAuthors
import com.example.raptor.database.relations.AlbumWithSongs
import com.example.raptor.database.relations.AuthorWithAlbums
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Insert
    fun insertSong(song: Song)

    @Insert
    fun insertAlbum(album: Album): Long

    @Insert
    fun insertAuthor(author: Author )

    @Insert
    fun insertAlbumAuthorCrossRef(albumAuthorCrossRef: AlbumAuthorCrossRef)

    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    // Get an album with its songs
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<List<AlbumWithSongs>>

    // Get an album with its authors
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithAuthors(albumId: Long): Flow<List<AlbumWithAuthors>>

    // Get an author with their albums
    @Transaction
    @Query("SELECT * FROM author WHERE name = :name")
    fun getAuthorWithAlbums(name: String): AuthorWithAlbums

    @Query("SELECT * FROM author WHERE name = :name")
    fun getAuthor(name: String): Author?

    @Query("SELECT * FROM album WHERE title = :name")
    fun getAlbumsByName(name: String): List<Album>
}