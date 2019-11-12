package com.breeze.views.UserSelection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toolbar;

import com.breeze.R;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzUser;
import com.breeze.state.BrzStateStore;

public class UserSelection extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_selection);

        ListView userList = findViewById(R.id.user_selection_user_list);
        UserList adapter = new UserList(this);
        userList.setAdapter(adapter);
        userList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BrzNode chatNode = (BrzNode) adapter.getItem(position);
                BrzStateStore.getStore().addChat(new BrzChat(chatNode.id, chatNode.user.name));
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setTitle("New Chat");
    }
}
