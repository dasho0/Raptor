package com.example.raptor.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.raptor.TagExtractor
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
        return database.songDao().getAllSongs()
    }

    fun populateDatabase(songs: List<TagExtractor.SongTags>) {
        assert(Thread.currentThread().name != "main")
        songs.forEach { songTags ->
            database.songDao().insert(
                Song(
                    title = songTags.title,
                    artist = songTags.artist,
                    album = songTags.album
                )
            )
        }
    }
}
