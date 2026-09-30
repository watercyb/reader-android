package com.blueberryjoy.reader.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class DebouncedInserter : ViewModel() {
    private val jobs = ConcurrentHashMap<String, Job>()
    private val delayMs: Long = 3000L

    fun insert(url: String, title: String) {
        jobs[url]?.cancel()
        val job = viewModelScope.launch(Dispatchers.Default) {
            delay(delayMs.milliseconds)
            BrowsingHistory.insert(url, title)
            jobs.remove(url)
        }
        jobs[url] = job
    }
}