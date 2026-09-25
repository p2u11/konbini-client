package io.github.konbini.market.ui;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import io.github.konbini.market.R;

public class CategoryListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<String> items;

    public CategoryListAdapter(Context context, List<String> items) {
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    public int getCount() { return items == null ? 0 : items.size(); }
    public Object getItem(int position) { return items.get(position); }
    public long getItemId(int position) { return position; }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) v = inflater.inflate(R.layout.list_item_category, parent, false);
        TextView text1 = (TextView) v.findViewById(R.id.text1);
        text1.setText(items.get(position));
        return v;
    }
}
