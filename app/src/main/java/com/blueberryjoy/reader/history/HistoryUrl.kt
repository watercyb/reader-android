package com.blueberryjoy.reader.history

import android.graphics.Bitmap
import com.blueberryjoy.reader.utils.Favicon
import java.net.URI
import java.time.Instant
import java.time.ZoneId

class HistoryUrl(var id: Int, val url: String, var title: String, var timestamp: Long) {
    val icon: Bitmap? = Favicon.getIconFromUrl(url)
    val domainPath = extractDomainPath()
    var isRemoved: Boolean = false

    fun updateTitle(title: String) {
        this.title = title
    }

    fun updateTimestamp(timestamp: Long) {
        this.timestamp = timestamp
    }

    private fun extractDomainPath(): String {
        val uri = URI(url)
        val host = uri.host ?: return url
        val path = uri.path.substringBeforeLast(".")
        return host + path
    }

    override fun toString(): String {
        return "id=$id, url='$url', timestamp=${
            Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }"
    }
}