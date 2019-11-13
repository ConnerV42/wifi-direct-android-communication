package com.breeze.views.UserSelection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

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
            BrzStateStore.getStore().addChat(new BrzChat(node.id, node.user.name));
            finish();
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set up our action bar
        ActionBar ab = getActionBar();
        if (ab == null) return;

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.search_view, null);
        ab.setCustomView(v);

        // When the user types a search query, filter the list
        EditText search = v.findViewById(R.id.user_selection_search);
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
