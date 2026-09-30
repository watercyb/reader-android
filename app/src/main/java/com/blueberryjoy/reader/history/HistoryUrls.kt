package com.blueberryjoy.reader.history

import com.blueberryjoy.reader.sqlite.history.History

class HistoryUrls {
    private val urlMap = HashMap<String, HistoryUrl>()
    private val historyList = ArrayList<HistoryUrl>()

    suspend fun insertURL(url: String, title: String) {
        if (urlMap.containsKey(url)) {
            val historyUrl = urlMap[url]!!

            if (historyUrl.isRemoved) {
                val restoredHistory = BrowsingHistory.createHistory(url, title)
                historyUrl.id = restoredHistory.id
                historyUrl.title = restoredHistory.title
                historyUrl.timestamp = restoredHistory.timestamp
                historyUrl.isRemoved = false
            } else {
                historyUrl.updateTitle(title)
                historyUrl.updateTimestamp(BrowsingHistory.updateHistory(historyUrl.id, title))
            }

            var index = 0
            while (index < historyList.size && historyList[index] != historyUrl) {
                index++
            }

            if (index == historyList.size) {
                val newHistory = BrowsingHistory.createHistory(url, title)
                urlMap[url] = newHistory
                historyList.add(0, newHistory)
                BrowsingHistory.insertToTokens(newHistory)
            } else {
                for (i in index downTo 1) {
                    historyList[i] = historyList[i - 1]
                }
                historyList[0] = historyUrl
            }
        } else {
            val newHistory = BrowsingHistory.createHistory(url, title)
            urlMap[url] = newHistory
            historyList.add(0, newHistory)
            BrowsingHistory.insertToTokens(newHistory)
        }
    }

    fun addAll(histories: List<History>): HashMap<String, HistoryUrl> {
        for (history in histories) {
            val historyUrl = HistoryUrl(
                history.id,
                history.url,
                history.title,
                history.timestamp
            )

            urlMap[history.url] = historyUrl
            historyList.add(historyUrl)
        }

        historyList.sortByDescending { it.timestamp }

        return urlMap
    }

    fun getList(): ArrayList<HistoryUrl> {
        return historyList
    }
}