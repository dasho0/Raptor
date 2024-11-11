package com.example.raptor.database

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.room.Room
import com.example.raptor.TagExtractor
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song
import kotlinx.coroutines.flow.Flow
import java.io.File

class DatabaseManager(context: Context) {
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

    fun fetchAllSongs(): Flow<List<Song>> { //TODO: should change this to something
        // more universal
        Log.d(javaClass.simpleName, "Collecting songs on thread ${Thread.currentThread().name}")
        return database.libraryDao().getAllSongs()
    }

    fun populateDatabase(songs: List<TagExtractor.SongTags>) {
        assert(Thread.currentThread().name != "main")

        val dao = database.libraryDao()

        fun getAlbumIdOfAuthor(authorName: String, albumName: String, dao: LibraryDao): Long {
            dao.getAuthorWithAlbums(authorName).albums.fastForEach { album ->
                if(albumName == album.title) {
                    return album.albumId
                }
            }
            return -1L
        }

        songs.fastForEach { song ->

            //TODO: there should be a distinction between albumartists and regular artists
            song.albumArtists?.fastForEach { name ->
                if(dao.getAuthor(name) == null) {
                    dao.insertAuthor(Author(name = name))
                }

            }
        }

        songs.fastForEach { song ->
            val albumName = song.album
            val albumsInDb = dao.getAlbumsByName(albumName.toString())

            if(albumName in albumsInDb.map { it.title }  ) {

            } else {
                val albumId = dao.insertAlbum(Album(title = albumName.toString()))
                // dao.insertAlbumAuthorCrossRef(AlbumAuthorCrossRef())
            }
        }
    }
}
