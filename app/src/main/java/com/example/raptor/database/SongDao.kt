package com.example.raptor.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.raptor.database.entities.SongTable
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM SongTable")
    fun getAllSongs(): Flow<List<SongTable>>

    @Insert
    fun insert(songTable: SongTable)
}