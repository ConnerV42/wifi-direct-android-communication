package com.breeze.views.Messages;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.breeze.R;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link MessagesView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessagesView extends Fragment {

    private static final String ARG_CHAT_ID = "";
    private BrzChat chat;

    public MessagesView() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param chatId The li_chat id to view.
     * @return A new instance of fragment MessagesView.
     */
    public static MessagesView newInstance(String chatId) {
        MessagesView fragment = new MessagesView();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String chatId = getArguments().getString("ARG_CHAT_ID");
            BrzStateStore.getStore().getChat(chatId, chat -> this.chat = chat);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final BrzRouter router = BrzRouter.getInstance();

        MessageList msgList = new MessageList(getActivity(), this.chat.id);
        ListView msgView = (ListView) getView().findViewById(R.id.messageList);
        msgView.setAdapter(msgList);

        Button sendMessage = getView().findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(view1 -> { // send message

            EditText messageBox = getView().findViewById(R.id.editText);
            String messageBoxText = messageBox.getText().toString();

            // Reset message box
            messageBox.setText("");

            BrzPacket packet = BrzPacketBuilder.message(router.hostNode.id, chat.id, messageBoxText);
            router.send(packet);

            BrzStateStore.getStore().addMessage(packet.to, packet.message());
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        BrzStateStore.getStore().setTitle(this.chat.name);
    }
}
