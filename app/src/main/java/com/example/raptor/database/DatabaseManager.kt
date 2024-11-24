package com.example.raptor.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.raptor.TagExtractor
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song
import com.example.raptor.database.relations.AuthorWithAlbums
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File

class DatabaseManager(private val context: Context) {
    private val database: LibraryDb = Room.databaseBuilder(
        context,
        LibraryDb::class.java, "Library"
    ).build()

    init {
        val dbFile = File(context.getDatabasePath("Library").absolutePath)
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    fun fetchAllSongs(): Flow<List<Song>> {
        return database.uiDao().getAllSongs()
    }

    fun fetchAuthors(): Flow<List<Author>> = database.uiDao().getAllAuthorsFlow()

    fun fetchAlbumsByAuthor(authorName: String): Flow<List<Album>> = flow {
        val authorWithAlbums: AuthorWithAlbums? = withContext(Dispatchers.IO) {
            database.logicDao().getAuthorWithAlbums(authorName)
        }
        emit(authorWithAlbums?.albums ?: emptyList())
    }.flowOn(Dispatchers.IO)

    fun fetchSongsByAlbum(albumTitle: String): Flow<List<Song>> = flow {
        val album = withContext(Dispatchers.IO) {
            database.logicDao().getAlbumByTitle(albumTitle)
        }
        if (album != null) {
            val songs = database.uiDao().getSongsByAlbumId(album.albumId)
            emitAll(songs)
        } else {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun populateDatabase(songs: List<TagExtractor.SongTags>) {
        withContext(Dispatchers.IO) {
            val dao = database.logicDao()

            songs.forEach { songTags ->
                // Add authors
                songTags.albumArtists?.forEach { name ->
                    if (dao.getAuthor(name) == null) {
                        dao.insertAuthor(Author(name = name))
                    }
                }

                // Add album
                val albumTitle = songTags.album ?: "Unknown Album"
                val albumId = dao.getAlbumByTitle(albumTitle)?.albumId
                    ?: dao.insertAlbum(Album(title = albumTitle))

                // Add album-author relationships
                songTags.albumArtists?.forEach { artistName ->
                    dao.insertAlbumAuthorCrossRef(
                        AlbumAuthorCrossRef(
                            albumId = albumId,
                            name = artistName
                        )
                    )
                }

                // Add song
                dao.insertSong(
                    Song(
                        title = songTags.title ?: "Unknown Title",
                        albumId = albumId
                    )
                )
            }
        }
    }
}
