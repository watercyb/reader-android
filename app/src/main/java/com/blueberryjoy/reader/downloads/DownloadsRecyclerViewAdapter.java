package com.blueberryjoy.reader.downloads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.reader.R;

public class DownloadsRecyclerViewAdapter extends RecyclerView.Adapter<DownloadsRecyclerViewAdapter.ViewHolder> {

    private final DownloadsRecyclerViewItems downloadsRecyclerViewItems;
    private final LayoutInflater layoutInflater;
    private ItemClickListener itemclickListener;

    // data is passed into the constructor
    public DownloadsRecyclerViewAdapter(Context context, DownloadsRecyclerViewItems downloadsRecyclerViewItems) {
        this.layoutInflater = LayoutInflater.from(context);
        this.downloadsRecyclerViewItems = downloadsRecyclerViewItems;
    }

    // inflates the row layout from XML when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(downloadsRecyclerViewItems.get(position).getItem());
        holder.imageView.setImageBitmap(downloadsRecyclerViewItems.get(position).getIcon());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return downloadsRecyclerViewItems.size();
    }


    // stores and recycles views as they are scrolled off-screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {
        final TextView textView;
        final ImageView imageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            imageView = itemView.findViewById(R.id.imageView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.setOnTouchListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemclickListener != null)
                itemclickListener.onItemClick(getAbsoluteAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            if (itemclickListener != null) {
                float x = (float) view.getTag(R.id.touch_x);
                float y = (float) view.getTag(R.id.touch_y);
                itemclickListener.onItemLongClick(view, getAbsoluteAdapterPosition(), x, y);
            }
            return true;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.setTag(R.id.touch_x, event.getRawX());
                view.setTag(R.id.touch_y, event.getRawY());
            }
            return false;
        }
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemclickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);

        void onItemLongClick(View view, int position, float x, float y);
    }
}