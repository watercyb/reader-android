package com.blueberryjoy.reader.bookmark;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blueberryjoy.reader.R;

public class RecyclerViewAdapterWithFilter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    @NonNull
    private final BookmarkList bookmarkList;
    private final LayoutInflater layoutInflater;
    private ItemClickListener itemclickListener;
    public static final int TYPE_DIVIDER = 0;
    public static final int TYPE_ITEM = 1;

    // This list holds the positions from the original list that should be displayed
    @NonNull
    final List<Integer> displayIndexes;

    RecyclerViewAdapterWithFilter(Context context, @NonNull BookmarkList bookmarkList) {
        this.layoutInflater = LayoutInflater.from(context);
        this.bookmarkList = bookmarkList;

        // Initially, display all indexes
        this.displayIndexes = new ArrayList<>();
        for (int i = 0; i < bookmarkList.length(); i++) {
            if (bookmarkList.isShown(i)) displayIndexes.add(i);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ITEM) {
            view = layoutInflater.inflate(R.layout.recyclerview_bookmark_item, parent, false);
            return new ViewHolder(view);
        } else {
            view = layoutInflater.inflate(R.layout.recyclerview_bookmark_divider, parent, false);
            return new ViewHolderDivider(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return bookmarkList.get(displayIndexes.get(position)).getItem() == null ? TYPE_DIVIDER : TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int actualIndex = displayIndexes.get(position);
        BookmarkItem bookmarkItem = bookmarkList.get(actualIndex);

        if (holder instanceof ViewHolder) {
            if (bookmarkItem != null) {
                ((ViewHolder) holder).textView.setText(bookmarkItem.getItem());
                ((ViewHolder) holder).imageView.setImageBitmap(bookmarkItem.getIcon());
                if (bookmarkItem.getType() != null) {
                    ((ViewHolder) holder).category.setVisibility(View.VISIBLE);
                } else {
                    ((ViewHolder) holder).category.setVisibility(View.GONE);
                }
            }
            holder.itemView.setOnClickListener(view -> {
                if (itemclickListener != null) itemclickListener.onItemClick(actualIndex, true);
            });
            holder.itemView.setOnLongClickListener(view -> {
                if (itemclickListener != null) {
                    float x = (float) view.getTag(R.id.touch_x);
                    float y = (float) view.getTag(R.id.touch_y);
                    itemclickListener.onItemLongClick(view, actualIndex, x, y, true);
                }
                return true;
            });
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setTag(R.id.touch_x, event.getRawX());
                        view.setTag(R.id.touch_y, event.getRawY());
                    }
                    return false;
                }
            });
        } else {
            if (bookmarkItem != null) {
                ((ViewHolderDivider) holder).type.setText(bookmarkItem.getType());
                ((ViewHolderDivider) holder).count.setText(String.valueOf(bookmarkList.getFilteredCount(bookmarkItem.getType())));
                ((ViewHolderDivider) holder).image.setImageResource(bookmarkList.isShown(bookmarkItem.getType()) ? R.mipmap.ic_bookmark_open : R.mipmap.ic_bookmark_close);
            }
            holder.itemView.setOnClickListener(view -> {
                if (itemclickListener != null) itemclickListener.onItemClick(actualIndex, false);
            });
            holder.itemView.setOnLongClickListener(view -> {
                if (itemclickListener != null) {
                    float x = (float) view.getTag(R.id.touch_x);
                    float y = (float) view.getTag(R.id.touch_y);
                    itemclickListener.onItemLongClick(view, actualIndex, x, y, false);
                }
                return true;
            });
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setTag(R.id.touch_x, event.getRawX());
                        view.setTag(R.id.touch_y, event.getRawY());
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayIndexes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final ImageView imageView;
        final View category;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            imageView = itemView.findViewById(R.id.imageView);
            category = itemView.findViewById(R.id.category);
        }
    }

    public static class ViewHolderDivider extends RecyclerView.ViewHolder {
        final TextView type;
        final TextView count;
        final ImageView image;

        ViewHolderDivider(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.type);
            count = itemView.findViewById(R.id.count);
            image = itemView.findViewById(R.id.image);
        }
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemclickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(int originalIndex, boolean isItem);

        void onItemLongClick(View view, int originalIndex, float x, float y, boolean isItem);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @NonNull
            @Override
            protected FilterResults performFiltering(@Nullable CharSequence constraint) {
                List<Integer> filteredIndexes = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    for (int i = 0; i < bookmarkList.length(); i++) {
                        if (bookmarkList.isShown(i))
                            filteredIndexes.add(i);
                    }
                    bookmarkList.resetFilteredCount();
                } else {
                    HashMap<String, Integer> filteredCounts = new HashMap<>();
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    List<BookmarkItem> items = bookmarkList.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        BookmarkItem bookmarkItem = items.get(i);
                        if (bookmarkItem.getItem() == null) {
                            filteredIndexes.add(i);
                        } else {
                            String title = bookmarkItem.getItem();
                            String url = bookmarkItem.getLink();
                            String type = bookmarkItem.getType();
                            if (title.toLowerCase().contains(filterPattern) || url.toLowerCase().contains(filterPattern)) {
                                if (bookmarkList.isShown(bookmarkItem.getType()))
                                    filteredIndexes.add(i);
                                if (type != null) {
                                    Integer count = filteredCounts.getOrDefault(type, 0);
                                    if (count == null) count = 0;
                                    filteredCounts.put(type, count + 1);
                                }
                            }
                        }
                    }
                    bookmarkList.updateFilteredCount(filteredCounts);
                }

                FilterResults results = new FilterResults();
                results.values = filteredIndexes;
                return results;
            }

            @SuppressLint("NotifyDataSetChanged")
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
                displayIndexes.clear();
                displayIndexes.addAll((List<Integer>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public void remove(int filteredIdx) {
        bookmarkList.remove(displayIndexes.get(filteredIdx));
        displayIndexes.remove(filteredIdx);
        while (filteredIdx < displayIndexes.size()) {
            displayIndexes.set(filteredIdx, displayIndexes.get(filteredIdx) - 1);
            filteredIdx++;
        }
    }
}
