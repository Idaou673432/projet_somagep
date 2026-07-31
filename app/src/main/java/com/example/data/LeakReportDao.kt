package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LeakReportDao {
    @Query("SELECT * FROM leak_reports ORDER BY timestamp DESC")
    fun getAllLeaks(): Flow<List<LeakReport>>

    @Query("SELECT * FROM leak_reports WHERE id = :id")
    suspend fun getLeakById(id: Int): LeakReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeak(leak: LeakReport): Long

    @Update
    suspend fun updateLeak(leak: LeakReport)

    @Delete
    suspend fun deleteLeak(leak: LeakReport)

    @Query("DELETE FROM leak_reports")
    suspend fun deleteAll()
}
