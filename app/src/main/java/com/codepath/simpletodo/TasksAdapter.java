package com.codepath.simpletodo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by floko_000 on 7/7/2016.
 */
public class TasksAdapter extends ArrayAdapter<Item> {


    public TasksAdapter(Context context, ArrayList<Item> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Item task = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_task, parent, false);
        }
        // Lookup view for data population
        TextView tvText = (TextView) convertView.findViewById(R.id.tvText);
        TextView tvUserName = (TextView) convertView.findViewById(R.id.tvUserName);
        // Populate the data into the template view using the data object
        tvText.setText(task.text);
        tvUserName.setText(task.user.userName);
        // Return the completed view to render on screen
        return convertView;
    }


}
