package com.breeze.views.UserSelection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.state.BrzStateStore;

public class UserSelection extends AppCompatActivity {

    UserList adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up our content
        setContentView(R.layout.activity_user_selection);

        RecyclerView recyclerView = findViewById(R.id.user_selection_user_list);

        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        this.adapter = new UserList(this);
        recyclerView.setAdapter(adapter);

        // When the user selects a node, add a chat for it
        adapter.setItemSelectedListener(node -> {
            BrzChat newChat = new BrzChat(node.name, node.id);
            BreezeAPI.getInstance().sendChatHandshakes(newChat);
            finish();
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set up our action bar
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;

        // Set up the activity as not "backoutable"
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        // Add our search field
        ab.setCustomView(R.layout.search_view);
        View actionBarView = ab.getCustomView();

        // When the user types a search query, filter the list
        EditText search = actionBarView.findViewById(R.id.user_selection_search);
        search.requestFocus();
        search.setTextColor(Color.parseColor("#ffffff"));
        search.setHintTextColor(Color.parseColor("#dddddd"));

        search.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                adapter.getFilter().filter(s);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return true;
    }
}
