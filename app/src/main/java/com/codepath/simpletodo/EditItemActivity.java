package com.codepath.simpletodo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EditItemActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);


        String oldText = getIntent().getStringExtra("text");
        EditText editItem = (EditText) findViewById(R.id.itemBody);
        editItem.setText(oldText);
        editItem.requestFocus();
    }


    public void onSubmit(View v) {
        String oldText = getIntent().getStringExtra("text");
        EditText etItem = (EditText) findViewById(R.id.itemBody);
        int index = getIntent().getIntExtra("index", 0);
        // Prepare data intent
        Intent data = new Intent();
        // Pass relevant data back as a result
        data.putExtra("text", etItem.getText().toString());
        data.putExtra("index", index);
        data.putExtra("oldText", oldText);
        //data.putExtra("editedItem", oldText);
        // Activity finished ok, return the data
        setResult(RESULT_OK, data); // set result code and bundle data for response
        finish(); // closes the activity, pass data to parent
    }
}
