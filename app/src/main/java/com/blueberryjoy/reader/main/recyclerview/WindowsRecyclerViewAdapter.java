package com.blueberryjoy.reader.main.recyclerview;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.reader.R;

public class WindowsRecyclerViewAdapter extends RecyclerView.Adapter<WindowsRecyclerViewAdapter.ViewHolder> {
    private final WindowsRecyclerViewItems windowsRecyclerViewItems;
    private final LayoutInflater layoutInflater;
    private ItemClickListener itemclickListener;

    public WindowsRecyclerViewAdapter(Context context, WindowsRecyclerViewItems windowsRecyclerViewItems) {
        this.layoutInflater = LayoutInflater.from(context);
        this.windowsRecyclerViewItems = windowsRecyclerViewItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recyclerview_main_windows_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(windowsRecyclerViewItems.get(position).item());
        holder.imageView.setImageBitmap(windowsRecyclerViewItems.get(position).icon());
        if (windowsRecyclerViewItems.get(position).isPrivate())
            holder.imageViewBadge.setVisibility(View.VISIBLE);
        if (windowsRecyclerViewItems.get(position).window().getIsCurrentWindow()) {
            holder.linearLayout.setBackgroundResource(R.color.page_current_background);
        } else {
            holder.linearLayout.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return windowsRecyclerViewItems.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;
        final ImageView imageView;
        final ImageView imageViewBadge;
        final LinearLayout linearLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            imageView = itemView.findViewById(R.id.imageView);
            imageViewBadge = itemView.findViewById(R.id.imageViewBadge);
            linearLayout = itemView.findViewById(R.id.linearLayout);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemclickListener != null)
                itemclickListener.onItemClick(getAbsoluteAdapterPosition());
        }
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemclickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(int position);
    }

    public void add(WindowsRecyclerViewNode node) {
        windowsRecyclerViewItems.add(node);
        notifyItemInserted(windowsRecyclerViewItems.size() - 1);
    }

    public void add(int idx, WindowsRecyclerViewNode node) {
        windowsRecyclerViewItems.add(idx, node);
        notifyItemInserted(idx);
    }

    public WindowsRecyclerViewNode get(int idx) {
        return windowsRecyclerViewItems.get(idx);
    }

    public void remove(int idx) {
        windowsRecyclerViewItems.remove(idx);
        notifyItemRemoved(idx);
    }

    public void move(int from, int to) {
        windowsRecyclerViewItems.move(from, to);
        notifyItemMoved(from, to);
    }
}
