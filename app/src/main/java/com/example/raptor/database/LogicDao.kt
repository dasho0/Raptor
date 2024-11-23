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
import com.example.raptor.database.relations.AuthorWithAlbums

@Dao
interface LogicDao {
    @Insert
    fun insertSong(song: Song)

    @Insert
    fun insertAlbum(album: Album): Long

    @Insert
    fun insertAuthor(author: Author )

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insertAlbumAuthorCrossRef(albumAuthorCrossRef: AlbumAuthorCrossRef)

    @Query("SELECT * FROM Author")
    fun getAllAuthors(): List<Author>

    // Get an album with its authors
    @Transaction
    @Query("SELECT * FROM album WHERE albumId = :albumId")
    fun getAlbumWithAuthors(albumId: Long): AlbumWithAuthors

    // Get an author with their albums
    @Transaction
    @Query("SELECT * FROM author WHERE name = :name")
    fun getAuthorWithAlbums(name: String): AuthorWithAlbums

    @Query("SELECT * FROM author WHERE name = :name")
    fun getAuthor(name: String): Author?

    @Query("SELECT * FROM album WHERE title = :name")
    fun getAlbumsByName(name: String): List<Album>?

    @Query("SELECT * FROM AlbumAuthorCrossRef WHERE albumId = :albumId AND name = :authorName " +
            "LIMIT 1")
    fun getCrossRefByAlbumAndAuthor(albumId: Long, authorName: String): AlbumAuthorCrossRef?
}