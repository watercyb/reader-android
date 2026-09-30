package com.blueberryjoy.reader.history

class HistoryTokens {
    private val map = HashMap<String, ArrayList<HistoryUrl>>()
    private val stopTokens =
        setOf("www", "com", "org", "net", "edu", "gov", "co", "io", "html", "php", "asp", "aspx")

    fun insert(historyURL: HistoryUrl) {
        val tokens = getTokens(historyURL.domainPath)
        for (token in tokens) {
            if (token in stopTokens) continue
            if (!map.containsKey(token)) {
                val list = ArrayList<HistoryUrl>()
                list.add(historyURL)
                map[token] = list
                BrowsingHistory.insertToTrie(token, list)
            } else {
                map[token]!!.add(historyURL)
            }
        }
    }

    fun getTokens(domainPath: String): ArrayList<String> {
        val res = ArrayList<String>()
        val host = domainPath.substringBefore('/')
        val path = domainPath.substringAfter('/', "")
        res.addAll(host.split(".").filter { it.isNotEmpty() })
        res.addAll(path.split("/").filter { it.isNotEmpty() })
        return res
    }

    fun addAll(historyUrlMap: HashMap<String, HistoryUrl>): HashMap<String, ArrayList<HistoryUrl>> {
        for (historyURL in historyUrlMap.values) {
            for (token in getTokens(historyURL.domainPath)) {
                if (token in stopTokens) continue
                if (!map.containsKey(token)) {
                    val list = ArrayList<HistoryUrl>()
                    list.add(historyURL)
                    map[token] = list
                } else {
                    map[token]!!.add(historyURL)
                }
            }
        }
        return map
    }
}