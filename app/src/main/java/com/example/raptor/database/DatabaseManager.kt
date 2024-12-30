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

/**
 * This class serves as an intermediary between the application database and the rest of the
 * application
 *
 * This class is a singleton
 */
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

    /**
     * Collects a flow of all authors from the database.
     *
     * @return A [Flow] emitting a list of [Author] objects.
     */
    fun collectAuthorsFlow(): Flow<List<Author>> = database.uiDao().getAllAuthorsFlow()

    /**
     * Collects a flow of albums by a specific author from the database.
     *
     * @param authorName The name of the author whose albums are to be collected.
     * @return A [Flow] emitting a list of [Album] objects.
     */
    fun collectAlbumsByAuthorFlow(authorName: String): Flow<List<Album>> {
        return database.uiDao().getAuthorWithAlbums(authorName)
            .map { it.albums }
    }

    /**
     * Collects a flow of songs by a specific album from the database.
     *
     * @param albumId The ID of the album whose songs are to be collected.
     * @return A [Flow] emitting a list of [Song] objects.
     */
    fun collectSongsByAlbumFlow(albumId: Long): Flow<List<Song>> {
        return database.uiDao().getAlbumWithSongs(albumId)
            .map { it.songs }
    }

    /**
     * Collects a flow of a specific song from the database.
     *
     * @param songId The ID of the song to be collected.
     * @return A [Flow] emitting a [Song] object.
     */
    fun collectSong(songId: Long): Flow<Song> {
        return database.uiDao().collectSongFromId(songId)
    }

    /**
     * Retrieves a specific song from the database.
     *
     * @param songId The ID of the song to be retrieved.
     * @return A [Song] object.
     */
    fun getSong(songId: Long): Song {
        return database.logicDao().getSongfromId(songId)
    }

    /**
     * Collects a flow of authors of a specific song from the database.
     *
     * @param song The [Song] object whose authors are to be collected.
     * @return A [Flow] emitting a list of [Author] objects or null.
     */
    fun collectAuthorsOfSong(song: Song?): Flow<List<Author>?> {
        Log.d(javaClass.simpleName, "Collecting authors of song: $song")
        return database.uiDao().getAlbumWithAuthors(song?.albumId)
            .map { it?.authors }
    }

    /**
     * Collects a flow of a specific album from the database.
     *
     * @param albumId The ID of the album to be collected.
     * @return A [Flow] emitting an [Album] object.
     */
    fun collectAlbum(albumId: Long?): Flow<Album> {
        return database.uiDao().getAlbumById(albumId)
    }

    /**
     * Populate the database with a list of [TagExtractor.SongInfo]
     */

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
            // FIXME: xdddddddd
            val distinctAlbumArtistsList = songs
                .map { Triple(it.album, it.albumArtists, it.coverUri) }
                .distinct()
            Log.d(javaClass.simpleName, "Distinct artists set: $distinctAlbumArtistsList")

            distinctAlbumArtistsList.fastForEach {
                val albumTitle = it.first.toString()
                val artists = it.second
                val coverUri = it.third

                val albumId = dao.insertAlbum(Album(
                    title = albumTitle,
                    coverUri = coverUri.toString(),
                ))

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
