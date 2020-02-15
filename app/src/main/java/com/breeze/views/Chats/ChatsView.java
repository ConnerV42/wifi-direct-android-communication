package com.breeze.views.Chats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.state.BrzStateStore;
import com.breeze.views.Messages.MessagesView;
import com.breeze.views.UserSelection.UserList;
import com.breeze.views.UserSelection.UserSelection;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static androidx.navigation.Navigation.findNavController;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 */
public class ChatsView extends Fragment {
    public ChatsView() {
        // Required empty public constructor
    }

    private ChatList list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Breeze Chats");

        this.list = new ChatList(activity);

        // Set up the list
        RecyclerView recyclerView = view.findViewById(R.id.contactList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(this.list);

        this.list.setItemSelectedListener((selectedChat) -> {
            if (selectedChat.acceptedByHost) {
                startActivity(MessagesView.getIntent(getContext(), selectedChat.id));
            } else {
                startActivity(ChatHandshakeView.getIntent(getContext(), selectedChat.id));
            }
        });
    }



    @Override
    public void onDetach() {
        super.onDetach();
        this.list.cleanup();
    }
}
