package com.blueberryjoy.reader.history

class HistoryTrie {
    private val root = HistoryTrieNode()
    var size = 0
    var count = 0

    fun insert(token: String, list: List<HistoryUrl>) {
        var node = root
        for (chr in token) {
            node.add(list)
            node = node.nextOrCreate(chr)
        }
        node.add(list)
    }

    fun insert1(token: String, list: List<HistoryUrl>) {
        var node = root
        for (chr in token) {
            node.add(list)

            //TODO test code
            if (node.next(chr) == null) size++
            count++

            node = node.nextOrCreate(chr)
        }
        node.add(list)
    }

    fun get(token: String): List<List<HistoryUrl>> {
        var node = root
        for (chr in token) {
            val next = node.next(chr) ?: break
            node = next
        }
        return node.getList()
    }

    fun addAll(historyTokenMap: HashMap<String, ArrayList<HistoryUrl>>) {
        for ((token, list) in historyTokenMap.entries) {
            insert1(token, list)
        }
    }
}