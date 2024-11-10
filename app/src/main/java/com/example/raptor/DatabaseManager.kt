package com.example.raptor

import android.content.Context
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class SongTable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String?,
    val artist: String?,
    val album: String?
)

@Dao
interface SongDao {
    @Query("SELECT * FROM SongTable")
    fun getAllSongs(): Flow<List<SongTable>>

    @Insert
    fun insert(songTable: SongTable)
}

@Database(entities = [SongTable::class], version = 1)
abstract class LibraryDb : RoomDatabase() {
    abstract fun songDao(): SongDao
}

class DatabaseManager(context: Context) {
    private val database: LibraryDb = Room.databaseBuilder(
            context,
            LibraryDb::class.java, "Library"
        ).build()

    fun fetchAllSongs(): Flow<List<SongTable>> { //TODO: should change this to something
        // more universal
        Log.d(javaClass.simpleName, "Collecting songs on thread ${Thread.currentThread().name}")
        return database.songDao().getAllSongs()
    }

    fun populateDatabase(songs: List<TagExtractor.SongTags>) {
        assert(Thread.currentThread().name != "main")
        songs.forEach { songTags ->
            Log.d(javaClass.simpleName, "populating database with: $songTags")
            database.songDao().insert(SongTable(
                title = songTags.title,
                //artist = songTags.artists,
                artist = null,
                album = songTags.album
            ))
        }
    }
}
