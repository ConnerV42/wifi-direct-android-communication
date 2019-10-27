package com.breeze;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.breeze.packets.BrzChat;
import com.breeze.state.BrzStateStore;
import com.breeze.views.ChatList;
import com.breeze.views.MessageList;

import static androidx.navigation.Navigation.findNavController;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link ContactView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactView extends Fragment {
    public ContactView() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContactView.
     */
    public static ContactView newInstance() {
        ContactView fragment = new ContactView();
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
        return inflater.inflate(R.layout.fragment_contact_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ChatList chatList = new ChatList(getActivity());
        ListView msgView = (ListView) view.findViewById(R.id.contactList);
        msgView.setAdapter(chatList);
        msgView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parentView, View childView, int position, long id) {
                NavController nav = findNavController(getView());
                //Bundle args = new Bundle();
                //args.putString("ARG_CHAT_ID", ((BrzChat) chatList.getItem(position)).id);
                //nav.navigate(R.id.chatView, args);
                nav.navigate(R.id.chatView);
            }
        });

        BrzStateStore store = BrzStateStore.getStore();
        store.addChat(new BrzChat("0", "Zach"));
        store.addChat(new BrzChat("1", "Conner"));
        store.addChat(new BrzChat("2", "Paul"));
        store.addChat(new BrzChat("3", "Jake"));
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
