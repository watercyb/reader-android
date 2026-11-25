package com.blueberryjoy.reader.history

class HistoryTrie {
    private val root = HistoryTrieNode()

    fun insert(token: String, list: List<HistoryUrl>) {
        var node = root
        for (chr in token) {
            val chr = if (chr.isUpperCase()) chr.lowercase()[0] else chr
            node.add(list)
            node = node.nextOrCreate(chr)
        }
        node.add(list)
    }

    fun get(token: String): List<List<HistoryUrl>> {
        var node = root
        for (chr in token) {
            val chr = if (chr.isUpperCase()) chr.lowercase()[0] else chr
            val next = node.next(chr) ?: break
            node = next
        }
        return node.getList()
    }

    fun addAll(historyTokenMap: HashMap<String, ArrayList<HistoryUrl>>) {
        for ((token, list) in historyTokenMap.entries) {
            insert(token, list)
        }
    }
}