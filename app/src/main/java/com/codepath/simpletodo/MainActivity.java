package com.codepath.simpletodo;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.codepath.simpletodo.ToDoDatabaseHelper.*;
import com.codepath.simpletodo.Item;
import com.codepath.simpletodo.User;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> items;
    ArrayAdapter<String> itemsAdapter;
    ListView lvItems;
    private final int REQUEST_CODE = 20;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lvItems = (ListView)findViewById(R.id.lvItems);
        readItems();
        itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        lvItems.setAdapter(itemsAdapter);
        setupListViewListener();

    }

    private void setupListViewListener(){
        lvItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

            @Override
            public boolean onItemLongClick(AdapterView<?> adapter, View item, int pos, long id){
                ToDoDatabaseHelper toDoDatabaseHelper = ToDoDatabaseHelper.getInstance(MainActivity.this);
                String itemText = itemsAdapter.getItem(pos).toString();
                itemsAdapter.remove(itemText);
                toDoDatabaseHelper.deleteItem(itemText);
                readItems();
                itemsAdapter.notifyDataSetChanged();
                return true;
            }

        });
        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View item, int pos, long id) {
                // first parameter is the context, second is the class of the activity to launch
                Intent i = new Intent(MainActivity.this, EditItemActivity.class);
               // itemsAdapter.getItem(pos).toString();
                // passing data to new activity
                i.putExtra("text", itemsAdapter.getItem(pos).toString());
                i.putExtra("index", pos);
                startActivityForResult(i, REQUEST_CODE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public void onAddItem(View v){
        ToDoDatabaseHelper toDoDatabaseHelper = ToDoDatabaseHelper.getInstance(MainActivity.this);
        EditText etNewItem = (EditText) findViewById(R.id.etNewItem);
        String itemText = etNewItem.getText().toString();
        itemsAdapter.add(itemText);
        etNewItem.setText("");
        Item newItem = new Item ();
        User admin = new User();
        admin.userName = "Admin";
        newItem.text = itemText;
        newItem.user = admin;

        toDoDatabaseHelper.addItem(newItem);

    }

    private void readItems(){
        ToDoDatabaseHelper toDoDatabaseHelper = ToDoDatabaseHelper.getInstance(MainActivity.this);
        // Get all Items from database
        List<Item> DBitems = toDoDatabaseHelper.getAllItems();
        items = new ArrayList<String>();
        for (Item item : DBitems) {
           items.add(item.text);
        }
    }


    // Edit item results from EditItemActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ToDoDatabaseHelper toDoDatabaseHelper = ToDoDatabaseHelper.getInstance(MainActivity.this);
        // REQUEST_CODE is defined above
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {

            toDoDatabaseHelper.updateItem(data);
            String text = data.getExtras().getString("text");
            int position = data.getExtras().getInt("index");
            itemsAdapter.remove(itemsAdapter.getItem(position).toString());
            itemsAdapter.insert(text, position);
        }
    }

}
