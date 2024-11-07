package com.example.raptor.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.raptor.database.entities.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Insert
    fun insert(song: Song)
}