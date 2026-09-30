package com.blueberryjoy.reader.sqlite.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueberryjoy.reader.App.Companion.getAppContext
import com.blueberryjoy.reader.R
import com.blueberryjoy.reader.settings.ReaderSettings
import com.blueberryjoy.reader.sqlite.SQLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object SQLitePage : ViewModel() {

    @JvmStatic
    fun getPages(callback: (pages: List<Page>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pages = SQLite.getDb().pageDao().getAllPages()
                if (pages.isEmpty()) {
                    val page: Page?
                    if (ReaderSettings.getIsShowWelcome()) {
                        page = create(
                            "file:///android_asset/" + getAppContext().getString(R.string.welcome_filename) + ".html",
                            0,
                            isCurrent = true,
                            isPrivate = false,
                            0
                        )
                        ReaderSettings.saveIsShowWelcome(false)
                    } else {
                        page = create("about:newtab", 0, isCurrent = true, isPrivate = false, 0)
                    }
                    pages.add(page)
                }
                pages.sortBy { it.position }
                for (i in pages.indices) {
                    pages[i].position = i
                    SQLite.getDb().pageDao().updatePosition(pages[i].id, i)
                }

                withContext(Dispatchers.Main) {
                    callback(pages)
                }
            } catch (_: Exception) {
            }

        }
    }

    @JvmStatic
    fun saveUrl(id: Int, url: String, state: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SQLite.getDb().pageDao().updateUrl(id, url, state)
        }
    }

    @JvmStatic
    fun saveIdx(id: Int, idx: Int) {
        viewModelScope.launch(Dispatchers.IO) { SQLite.getDb().pageDao().updateIdx(id, idx) }
    }

    @JvmStatic
    fun saveIsCurrent(id: Int, isCurrent: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            SQLite.getDb().pageDao().updateIsCurrent(id, isCurrent)
        }
    }

    @JvmStatic
    suspend fun create(
        url: String, idx: Int, isCurrent: Boolean, isPrivate: Boolean, position: Int
    ): Page {
        val page = Page(url, idx, isCurrent, "", isPrivate, position)
        val id: Long = SQLite.getDb().pageDao().insert(page)
        page.id = id.toInt()
        return page
    }

    @JvmStatic
    fun createPage(
        url: String, idx: Int, isCurrent: Boolean, isPrivate: Boolean, position: Int
    ): Page {
        val pages = runBlocking {
            val page = Page(url, idx, isCurrent, "", isPrivate, position)
            val id: Long = SQLite.getDb().pageDao().insert(page)
            page.id = id.toInt()
            return@runBlocking page
        }
        return pages
    }

    @JvmStatic
    fun delete(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { SQLite.getDb().pageDao().deletePage(id) }
    }

    @JvmStatic
    fun savePosition(id: Int, position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            SQLite.getDb().pageDao().updatePosition(id, position)
        }
    }
}
