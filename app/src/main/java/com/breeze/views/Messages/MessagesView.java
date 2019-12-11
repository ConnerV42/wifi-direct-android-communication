package com.breeze.views.Messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.google.android.gms.nearby.connection.Payload;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link MessagesView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessagesView extends Fragment {

    private static final int READ_REQUEST_CODE = 69;
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
        RecyclerView msgView = getView().findViewById(R.id.messageList);
        msgView.setAdapter(msgList);

        LinearLayoutManager msgLayout = new LinearLayoutManager(getActivity());
        msgLayout.setStackFromEnd(true);
        msgView.setLayoutManager(msgLayout);

        Log.i("STATE", "Bound message list to " + this.chat.id);

        Button sendMessage = getView().findViewById(R.id.sendMessage);
        sendMessage.setOnClickListener(view1 -> { // send message

            EditText messageBox = getView().findViewById(R.id.editText);
            String messageBoxText = messageBox.getText().toString();

            // Reset message box
            messageBox.setText("");

            BreezeAPI.getInstance().sendMessage(BrzPacketBuilder.makeMessage(router.hostNode.id, messageBoxText, chat.id, false), chat.id);
        });

        Button sendPhoto = getView().findViewById(R.id.sendPhoto);
        sendPhoto.setOnClickListener(view1 -> {
            // Bring up the option to select media to send from external storage
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, READ_REQUEST_CODE);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();

                // For Payload (FILE)
                ParcelFileDescriptor parcel = this.getContext().getContentResolver().openFileDescriptor(imageUri, "r");
                Payload filePayload = Payload.fromFile(parcel);
                // Payload filePayloadAsStream = Payload.fromStream(parcel);

                // For File Name Payload (BYTES)
                String filePayloadId = "" + filePayload.getId();
                String fileName = imageUri.getLastPathSegment();

                final BrzRouter router = BrzRouter.getInstance();
                BrzPacket packet = BrzPacketBuilder.fileInfoPacket(router.hostNode.id, chat.id, filePayloadId, fileName);
                BrzRouter.getInstance().sendFilePayload(filePayload, packet);
            } catch (Exception e) {
                Log.e("FILE_ACCESS", "Failure ", e);
            }
        }
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
