package com.breeze.views.UserSelection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.LinkedList;
import java.util.List;

public class UserSelection extends AppCompatActivity {
    List<String> nodes = new LinkedList<>();
    List<TextView> nodeViews = new LinkedList<>();
    private UserList list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_selection);
        this.list = new UserList(this, this.nodes);
        Toolbar userToolbar = findViewById(R.id.user_selection_toolbar);
        setSupportActionBar(userToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;

        // Set up the activity as not "backoutable"
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        ab.setTitle("New chat");

        // Set the done
        FloatingActionButton fab = findViewById(R.id.user_selection_fab);
        fab.hide();
        fab.setOnClickListener(e -> {
            if (nodes.size() == 1) {
                String nodeId = nodes.get(0);
                BrzNode n = BrzGraph.getInstance().getVertex(nodeId);
                BrzChat newChat = new BrzChat(n.name, nodeId);
                BreezeAPI.getInstance().sendChatHandshakes(newChat);
            } else if (nodes.size() != 0) {
                BrzChat newChat = new BrzChat("New chat", nodes);
                BreezeAPI.getInstance().sendChatHandshakes(newChat);
            }

            finish();
        });

        // Set up the list
        RecyclerView recyclerView = findViewById(R.id.user_selection_user_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);


        recyclerView.setAdapter(this.list);

        // When the user selects a node, add it to the list
        this.list.setItemSelectedListener(node -> {
            int nodeI = nodes.indexOf(node.id);
            if (nodeI == -1) {
                LinearLayout.LayoutParams nodeLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                nodeLayout.leftMargin = 10;
                nodeLayout.bottomMargin = 10;

                TextView nodeView = new TextView(this);
                nodeView.setText(node.name);
                nodeView.setPadding(20, 15, 20, 15);
                nodeView.setLayoutParams(nodeLayout);
                nodeView.setBackgroundResource(R.drawable.status_bubble);
                nodeView.setId(nodeI);

                FlexboxLayout toList = findViewById(R.id.user_selection_to_list);
                toList.addView(nodeView);

                nodes.add(node.id);
                nodeViews.add(nodeView);
            } else {
                FlexboxLayout toList = findViewById(R.id.user_selection_to_list);
                toList.removeView(nodeViews.get(nodeI));

                nodes.remove(nodeI);
                nodeViews.remove(nodeI);
            }

            if (nodes.size() == 0) {
                fab.hide();
            } else {
                fab.show();
            }
        });

        // When the user types a search query, filter the list
        EditText search = findViewById(R.id.user_selection_search);
        search.requestFocus();
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                list.getFilter().filter(s);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        list.cleanup();

        finish();
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        list.cleanup();
    }
}
