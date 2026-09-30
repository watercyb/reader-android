package com.blueberryjoy.reader.sqlite.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: History): Long

    @Query("SELECT * FROM histories ORDER BY timestamp DESC")
    suspend fun allHistories(): List<History>

    @Query("DELETE FROM histories WHERE id = :historyId")
    suspend fun deleteHistory(historyId: Int)

    @Query("UPDATE histories SET timestamp = :timestamp, title = :title WHERE id = :historyId")
    suspend fun updateHistory(historyId: Int, title: String, timestamp: Long)

    @Query("DELETE FROM histories")
    suspend fun deleteAll()
}
