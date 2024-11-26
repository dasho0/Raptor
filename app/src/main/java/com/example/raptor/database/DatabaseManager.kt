package com.example.raptor.database

import android.content.Context
import android.util.Log
import androidx.compose.ui.util.fastForEach
import androidx.room.Room
import com.example.raptor.TagExtractor
import com.example.raptor.database.entities.Album
import com.example.raptor.database.entities.AlbumAuthorCrossRef
import com.example.raptor.database.entities.Author
import com.example.raptor.database.entities.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext context: Context
) {
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

    fun collectAuthorsFlow(): Flow<List<Author>> = database.uiDao().getAllAuthorsFlow()

    fun collectAlbumsByAuthorFlow(authorName: String): Flow<List<Album>> {
        return database.uiDao().getAuthorWithAlbums(authorName)
            .map { it.albums }
    }

    fun collectSongsByAlbumFlow(albumId: Long): Flow<List<Song>> {
        return database.uiDao().getAlbumWithSongs(albumId)
            .map { it.songs }
    }

    fun collectSong(songId: Long): Flow<Song> {
        return database.uiDao().collectSongFromId(songId)
    }

    fun getSong(songId: Long): Song {
        return database.logicDao().getSongfromId(songId)
    }

    fun collectAuthorsOfSong(song: Song?): Flow<List<Author>?> {
        Log.d(javaClass.simpleName, "Collecting authors of song: $song")
        return database.uiDao().getAlbumWithAuthors(song?.albumId)
                .map { it?.authors }
    }

    fun collectAlbum(albumId: Long?): Flow<Album> {
        return database.uiDao().getAlbumById(albumId)
    }

    fun populateDatabase(songs: List<TagExtractor.SongInfo>) {
        assert(Thread.currentThread().name != "main")

        val dao = database.logicDao()

        fun addAuthors() {
            songs.fastForEach { song ->
                //TODO: there should be a distinction between albumartists and regular artists
                song.albumArtists?.fastForEach { name ->
                    if(dao.getAuthor(name) == null) {
                        dao.insertAuthor(Author(name = name))
                    }

                }
            }
        }

        fun addAlbumsAndRelations() {
            val distinctAlbumArtistsList = songs
                .map { Pair(it.album, it.albumArtists) }
                .distinct()
            Log.d(javaClass.simpleName, "Distinct artists set: $distinctAlbumArtistsList")

            distinctAlbumArtistsList.fastForEach {
                val albumTitle = it.first.toString()
                val artists = it.second

                val albumId = dao.insertAlbum(Album(title = albumTitle))

                artists?.fastForEach {
                    dao.insertAlbumAuthorCrossRef(AlbumAuthorCrossRef(
                        albumId = albumId,
                        name = it.toString()
                    ))
                }
            }
        }

        fun addSongs() {
            songs.fastForEach { song ->
                Log.d(javaClass.simpleName, "NEW SONG\n")
                Log.d(javaClass.simpleName, "Album artists: ${song.albumArtists}")

                val albumWithAuthorCandidates = dao
                    .getAlbumsByTitle(song.album.toString())
                    .map { it.albumId }
                    .map { dao.getAlbumWithAuthors(it) }
                Log.d(javaClass.simpleName, "$albumWithAuthorCandidates")

                var correctAlbum: Album? = null
                albumWithAuthorCandidates.fastForEach {
                    Log.d(javaClass.simpleName, "${song.albumArtists}, ${it.authors}")
                    //FIXME: theese guys shouldn't be ordered, will have to refactor a bunch of
                    // stuff with sets instead of lists
                    if(song.albumArtists?.sorted() == it.authors.map { it.name }.sorted()) {
                        correctAlbum = it.album
                    }
                }

                dao.insertSong(Song(
                    title = song.title,
                    albumId = correctAlbum?.albumId,
                    fileUri = song.fileUri.toString(),
                ))
            }
        }

        addAuthors()
        addAlbumsAndRelations()
        addSongs()
    }
}
