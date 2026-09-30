package com.blueberryjoy.reader.sqlite.page

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PageDao {
    @Insert
    suspend fun insert(page: Page): Long

    @Query("SELECT * FROM pages")
    suspend fun getAllPages(): MutableList<Page>

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePage(pageId: Int)

    @Query("UPDATE pages SET url = :url, state = :state WHERE id = :pageId")
    suspend fun updateUrl(pageId: Int, url: String, state: String)

    @Query("UPDATE pages SET idx = :idx WHERE id = :pageId")
    suspend fun updateIdx(pageId: Int, idx: Int)

    @Query("UPDATE pages SET isCurrent = :isCurrent WHERE id = :pageId")
    suspend fun updateIsCurrent(pageId: Int, isCurrent: Boolean)

    @Query("UPDATE pages SET position = :position WHERE id = :pageId")
    suspend fun updatePosition(pageId: Int, position: Int)

    @Query("DELETE FROM pages")
    suspend fun deleteAllPages()
}
