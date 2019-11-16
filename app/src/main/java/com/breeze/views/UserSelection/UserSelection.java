package com.breeze.views.UserSelection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toolbar;

import com.breeze.R;
import com.breeze.packets.BrzChat;
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
            BrzStateStore.getStore().addChat(new BrzChat(node.id, node.name));
            finish();
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set up our action bar
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);

        // Do some themeing
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);
        ab.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffffff")));

        Window w = getWindow();
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Color.parseColor("#ffffff"));


        // Add our search field
        ab.setCustomView(R.layout.search_view);
        View actionBarView = ab.getCustomView();

        // When the user types a search query, filter the list
        EditText search = actionBarView.findViewById(R.id.user_selection_search);
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
