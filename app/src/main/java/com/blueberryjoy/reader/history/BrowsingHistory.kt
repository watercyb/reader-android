/*
main -> initialize<viewModelScope><lock>
HistoryAdapter -> restoreHistory<viewModelScope><lock>
HistorySuggestionAdapter, HistoryAdapter -> deleteHistory<viewModelScope><lock>
main -> search<lock>
main -> debouncedInsert -> DebouncedInserter<viewModelScope> -> insert<lock>:
HistoryAdapter -> getList<runBlocking><lock>
HistoryPage -> clear<viewModelScope><lock>

historyURLs -> createHistory
historyURL -> updateHistory
historyURLs -> insertToTokens
*/

package com.blueberryjoy.reader.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueberryjoy.reader.sqlite.SQLite
import com.blueberryjoy.reader.sqlite.history.SQLiteHistory
import com.blueberryjoy.reader.utils.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

object BrowsingHistory : ViewModel() {
    private val repo: SQLiteHistory = SQLiteHistory(SQLite.getDb().historyDao())
    private var lock = Mutex()
    private var historyURLs = HistoryUrls()
    private var historyTokens = HistoryTokens()
    private var historyTrie = HistoryTrie()
    private var debouncedInserter = DebouncedInserter()
    private const val MAX_HISTORY_PER_DOMAIN = 3

    init {
        viewModelScope.launch(Dispatchers.IO) {
            lock.withLock {
                val start = System.currentTimeMillis()
                historyURLs = HistoryUrls()
                historyTokens = HistoryTokens()
                historyTrie = HistoryTrie()
                val histories = repo.loadHistories()
                val historyURLMap = historyURLs.addAll(histories)
                val historyTokenMap = historyTokens.addAll(historyURLMap)
                historyTrie.addAll(historyTokenMap)
                val end = System.currentTimeMillis()
                Message.log("Initialize: ${end - start}, URLs: ${historyURLMap.size}")
            }
        }
    }

    @Suppress("EmptyMethod")
    fun initialize() {
    }

    suspend fun createHistory(url: String, title: String): HistoryUrl {
        val history = repo.create(url, title, System.currentTimeMillis())
        return HistoryUrl(history.id, history.url, history.title, history.timestamp)
    }

    fun restoreHistory(historyURL: HistoryUrl) {
        viewModelScope.launch(Dispatchers.IO) {
            lock.withLock {
                repo.restore(historyURL)
            }
        }
    }

    suspend fun updateHistory(id: Int, title: String): Long {
        val timestamp = System.currentTimeMillis()
        repo.updateHistory(id, title, timestamp)
        return timestamp
    }

    fun deleteHistory(historyURL: HistoryUrl) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteHistory(historyURL.id)
            historyURL.isRemoved = true
        }
    }

    fun insertToTokens(historyURL: HistoryUrl) {
        historyTokens.insert(historyURL)
    }

    fun insertToTrie(token: String, list: List<HistoryUrl>) {
        historyTrie.insert(token, list)
    }

    suspend fun insert(url: String, title: String?) {
        lock.withLock {
            if (title == null) {
                historyURLs.insertURL(url, "<NO TITLE>")
            } else {
                historyURLs.insertURL(url, title)
            }
        }
    }

    fun debouncedInsert(url: String, title: String) {
        debouncedInserter.insert(url, title)
    }

    fun getList(): ArrayList<HistoryItem> {
        val items = runBlocking {
            lock.withLock {
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                val items = ArrayList<HistoryItem>()
                var lastDate: LocalDate? = null
                for (history in historyURLs.getList()) {
                    if (history.isRemoved) continue
                    val date =
                        Instant.ofEpochMilli(history.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    if (date != lastDate) {
                        items.add(
                            HistoryItem.Header(
                                when (date) {
                                    LocalDate.now() -> "Today - ${date.format(formatter)}"
                                    LocalDate.now().minusDays(1) -> "Yesterday - ${
                                        date.format(
                                            formatter
                                        )
                                    }"

                                    else -> date.format(formatter)
                                }
                            )
                        )
                        lastDate = date
                    }
                    items.add(HistoryItem.Entry(history))
                }
                return@runBlocking items
            }
        }
        return items
    }

    fun clear() {
        viewModelScope.launch(Dispatchers.IO) {
            lock.withLock {
                historyURLs = HistoryUrls()
                historyTokens = HistoryTokens()
                historyTrie = HistoryTrie()
                repo.deleteAll()
            }
        }
    }

    fun searchHistory(query: String): ArrayList<HistoryUrl> {
        if (!lock.tryLock()) return ArrayList()
        try {
            var matchedHistory = ArrayList<HistoryUrl>()
            if (query.trim().isEmpty()) {
                matchedHistory = historyURLs.getList().stream().filter { !it.isRemoved }
                    .collect(Collectors.toList()) as ArrayList<HistoryUrl>
            } else {
                val matchScores = runBlocking {
                    calculateMatchScores(query)
                }
                val rankedResults = ArrayList<Pair<HistoryUrl, Int>>()
                for ((key, value) in matchScores) {
                    rankedResults.add(Pair(key, value))
                }
                rankedResults.sortWith(Comparator { o1, o2 ->
                    if (o1.second == o2.second)
                        o2.first.timestamp.compareTo(o1.first.timestamp)
                    o2.second.compareTo(o1.second)
                })
                for ((first) in rankedResults) {
                    matchedHistory.add(first)
                }
            }
            val domainCounts = HashMap<String, Int>()
            val res = ArrayList<HistoryUrl>()
            for (historyURL in matchedHistory) {
                val key = historyURL.domainPath.substringBefore("/")
                val count = domainCounts.getOrDefault(key, 0)
                if (count < MAX_HISTORY_PER_DOMAIN) {
                    res.add(historyURL)
                    domainCounts[key] = count + 1
                }
            }
            return res
        } finally {
            lock.unlock()
        }
    }

    suspend fun calculateMatchScores(searchString: String): Map<HistoryUrl, Int> = coroutineScope {
        val tokens = searchString.split(" ")
        val processedTokens = HashSet<String>()
        val deferredResults = tokens.map { str ->
            async(Dispatchers.Default) {
                if (!processedTokens.add(str)) return@async HashSet()
                searchToken(str)
            }
        }
        val historyScores = HashMap<HistoryUrl, Int>()
        deferredResults.awaitAll().forEach { set ->
            set.forEach { token ->
                historyScores[token] = (historyScores[token] ?: 0) + 1
            }
        }
        historyScores
    }

    fun searchToken(searchString: String): Set<HistoryUrl> {
        if (searchString.trim().isEmpty()) return HashSet()
        val matchedUrls = HashSet<HistoryUrl>()
        for (list in historyTrie.get(
            searchString
        )) {
            for (historyURL in list) {
                if (!historyURL.isRemoved)
                    matchedUrls.add(historyURL)
            }
        }
        return matchedUrls
    }
}