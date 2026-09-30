package com.blueberryjoy.reader.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blueberryjoy.reader.R
import com.blueberryjoy.reader.history.BrowsingHistory.deleteHistory

class HistorySuggestionAdapter(
    private var items: ArrayList<HistoryUrl> = ArrayList(),
    private val onClick: (HistoryUrl) -> Unit = {},
    private val onLongClick: (HistoryUrl) -> Boolean = { true }
) : RecyclerView.Adapter<HistorySuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.iconView)
        val titleView: TextView = view.findViewById(R.id.titleView)
        val urlView: TextView = view.findViewById(R.id.urlView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_main_history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleView.text = item.title
        holder.urlView.text = item.url
        holder.iconView.setImageBitmap(item.icon)
        holder.itemView.setOnClickListener {
            onClick(item)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun addList(list: ArrayList<HistoryUrl>) {
        items = list
        notifyDataSetChanged()
    }

    fun remove(position: Int) {
        val historyURL = items[position]
        deleteHistory(historyURL)
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}