package com.breeze.views.Chats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.breeze.R;
import com.breeze.state.BrzStateStore;
import com.breeze.views.Messages.MessagesView;
import com.breeze.views.UserSelection.UserSelection;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static androidx.navigation.Navigation.findNavController;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link ChatsView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatsView extends Fragment {
    public ChatsView() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ChatsView.
     */
    public static ChatsView newInstance() {
        ChatsView fragment = new ChatsView();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chats_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ChatList chatList = new ChatList(getActivity());
        ListView msgView = view.findViewById(R.id.contactList);
        msgView.setAdapter(chatList);
        msgView.setOnItemClickListener((parentView, childView, position, id) -> {
            startActivity(MessagesView.getIntent(getContext(), chatList.getChatId(position)));
        });

        FloatingActionButton fab = view.findViewById(R.id.chat_view_fab);
        fab.setOnClickListener(e -> {
            Intent i = new Intent(this.getContext(), UserSelection.class);
            this.startActivity(i);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        BrzStateStore.getStore().setTitle("Breeze");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
