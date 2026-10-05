package com.blueberryjoy.reader.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blueberryjoy.reader.R
import java.util.Locale

class HistoryAdapter(
    private val onClick: (HistoryUrl) -> Unit = {},
    private val onLongClick: (View, HistoryUrl, Float, Float) -> Boolean = { _, _, _, _ -> true }
) : RecyclerView.Adapter<BaseViewHolder>(), Filterable {

    var items = ArrayList<HistoryItem>()
    val header = 0
    val history = 1
    val displayIndexes = ArrayList<Int>()

    init {
        items = BrowsingHistory.getList()
        for (i in items.indices) {
            displayIndexes.add(i)
        }
    }

    class HistoryViewHolder(view: View) : BaseViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.iconView)
        val titleView: TextView = view.findViewById(R.id.titleView)
        val urlView: TextView = view.findViewById(R.id.urlView)
    }

    class HeaderViewHolder(view: View) : BaseViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            header -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.activity_history_header, parent, false)
                HeaderViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_history_item, parent, false)
                HistoryViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[displayIndexes[position]]) {
            is HistoryItem.Header -> header
            is HistoryItem.Entry -> history
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val actualIndex = displayIndexes[position]

        when (val item = items[actualIndex]) {
            is HistoryItem.Header -> {
                val headerHolder = holder as HeaderViewHolder
                headerHolder.textView.text = item.text
            }

            is HistoryItem.Entry -> {
                val historyHolder = holder as HistoryViewHolder
                historyHolder.titleView.text = item.history.title
                historyHolder.urlView.text = item.history.url
                historyHolder.iconView.setImageBitmap(item.history.icon)

                historyHolder.itemView.setOnClickListener {
                    onClick(item.history)
                }
                historyHolder.itemView.setOnLongClickListener { view ->
                    val x = view.getTag(R.id.touch_x) as Float
                    val y = view.getTag(R.id.touch_y) as Float
                    onLongClick(view, item.history, x, y)
                    true
                }
                @SuppressLint("ClickableViewAccessibility")
                historyHolder.itemView.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        view.setTag(R.id.touch_x, event.rawX)
                        view.setTag(R.id.touch_y, event.rawY)
                    }
                    false
                }
            }
        }
    }

    override fun getItemCount(): Int = displayIndexes.size

    fun remove(position: Int): Pair<HistoryUrl, Int> {
        val actualIndex = displayIndexes[position]
        val item = items[actualIndex] as HistoryItem.Entry
        BrowsingHistory.deleteHistory(item.history)
        items.removeAt(actualIndex)
        for (i in position until displayIndexes.size) {
            displayIndexes[i]--
        }
        displayIndexes.removeAt(position)
        notifyItemRemoved(position)
        return Pair(item.history, actualIndex)
    }

    fun recover(historyURL: HistoryUrl, position: Int, actualIndex: Int) {
        historyURL.isRemoved = false
        BrowsingHistory.restoreHistory(historyURL)
        items.add(actualIndex, HistoryItem.Entry(historyURL))
        displayIndexes.add(position, actualIndex)
        for (i in position + 1 until displayIndexes.size) {
            displayIndexes[i]++
        }
        notifyItemInserted(position)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredIndexes = ArrayList<Int>()

                if (constraint.isNullOrEmpty()) {
                    for (i in items.indices) {
                        filteredIndexes.add(i)
                    }
                } else {
                    val filterPattern =
                        constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }

                    for (i in items.indices) {
                        when (val item = items[i]) {
                            is HistoryItem.Header -> {
                                if (filteredIndexes.isNotEmpty() && items[filteredIndexes[filteredIndexes.size - 1]] is HistoryItem.Header) filteredIndexes.removeAt(
                                    filteredIndexes.size - 1
                                )
                                filteredIndexes.add(i)
                            }

                            is HistoryItem.Entry -> {
                                val title = item.history.title
                                val url = item.history.url
                                if (title.lowercase(Locale.getDefault())
                                        .contains(filterPattern) || url.lowercase(Locale.getDefault())
                                        .contains(filterPattern)
                                ) {
                                    filteredIndexes.add(i)
                                }
                            }
                        }
                    }
                }

                val results = FilterResults()
                results.values = filteredIndexes
                return results
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                displayIndexes.clear()
                for (idx in results.values as ArrayList<*>) {
                    displayIndexes.add(idx as Int)
                }
                notifyDataSetChanged()
            }
        }
    }
}

sealed class HistoryItem {
    data class Header(val text: String) : HistoryItem()
    data class Entry(val history: HistoryUrl) : HistoryItem()
}

sealed class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view)