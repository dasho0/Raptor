package com.example.raptor

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    fun getAllSongs(): List<SongTable>

    @Insert
    fun insert(songTable: SongTable)
}

@Database(entities = [SongTable::class], version = 1)
abstract class LibraryDb : RoomDatabase() {
    abstract fun songDao(): SongDao
}

class DatabaseManager {
    private lateinit var database: LibraryDb

    fun prepareDatabase(context: Context) {
        database = Room.databaseBuilder(
            context,
            LibraryDb::class.java, "Library"
        ).build()
    }

    fun fetchAllSongs(): List<SongTable> { //TODO: should change this to something
        // more universal
        return database.songDao().getAllSongs()
    }

    fun populateDatabase(songs: List<TagExtractor.SongTags>) {
        songs.forEach { songTags ->
            database.songDao().insert(SongTable(
                title = songTags.title,
                artist = songTags.artist,
                album = songTags.album
            ))
        }
    }
}
