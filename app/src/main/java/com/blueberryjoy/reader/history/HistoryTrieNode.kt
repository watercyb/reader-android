package com.blueberryjoy.reader.history

class HistoryTrieNode {
    private val list = ArrayList<List<HistoryUrl>>()
    private val children = HashMap<Char, HistoryTrieNode>()

    fun next(chr: Char): HistoryTrieNode? {
        return children[chr]
    }

    fun nextOrCreate(chr: Char): HistoryTrieNode {
        if (children[chr] == null)
            children[chr] = HistoryTrieNode()
        return children[chr]!!
    }

    fun add(list: List<HistoryUrl>) {
        this.list.add(list)
    }

    fun getList(): List<List<HistoryUrl>> {
        return list
    }

    fun clear() {
        list.clear()
        children.clear()
    }
}