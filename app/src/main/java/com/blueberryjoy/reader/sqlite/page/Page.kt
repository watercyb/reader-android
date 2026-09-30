package com.blueberryjoy.reader.sqlite.page

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
class Page(
    @JvmField val url: String,
    @JvmField val idx: Int,
    @JvmField val isCurrent: Boolean,
    @JvmField val state: String,
    @JvmField val isPrivate: Boolean = false,
    @JvmField var position: Int
) {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    override fun toString(): String {
        return "Id: $id, Url: $url, Idx: $idx, IsCurrent: $isCurrent, State: ${state.take(10)}, IsPrivate: $isPrivate, Position: $position"
    }
}