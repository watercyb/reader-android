package com.blueberryjoy.reader.main.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.R;

import java.util.List;

public class ListViewAdapter extends ArrayAdapter<ListViewItem> {

    public ListViewAdapter(Context context, List<ListViewItem> data) {
        super(context, 0, data);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.listview_main_item, parent, false);
        }

        ListViewItem item = getItem(position);

        TextView text = convertView.findViewById(R.id.textView);

        assert item != null;
        text.setText(item.getTitle());

        text.setAlpha(!item.getEnable() ? 0.4f : 1.0f);

        return convertView;
    }
}