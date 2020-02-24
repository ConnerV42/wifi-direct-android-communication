package com.breeze.views.PublicMessages;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.breeze.MainActivity;
import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzMessage;
import com.breeze.graph.BrzGraph;

import java.util.function.Consumer;


public class PublicMessagesView extends Fragment {

    public PublicMessageList list;
    private Consumer<Object> onlineListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_messages_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = api.getGraph();
        //setup content
        Switch publicSwitch = view.findViewById(R.id.PublicSwitch);
        publicSwitch.setChecked(false);
        publicSwitch.setOnTouchListener((v, e) -> {
            switch(e.getAction()){
                case MotionEvent.ACTION_UP:
                    if(publicSwitch.isChecked()){
                        api.state.setPublicThreadOn(false);
                        //Toast.makeText(getActivity(), "Thread activity: OFF", Toast.LENGTH_LONG).show();
                        publicSwitch.setChecked(false);
                        return true;

                    }
                    else{

                        //msgView.setAdapter(this.list);

                        RecyclerView msgView = view.findViewById(R.id.publicMessageList);
                        this.list = new PublicMessageList(this.getActivity(), msgView);
                        msgView.setAdapter(this.list);
                        LinearLayoutManager msgLayout = new LinearLayoutManager(this.getActivity());
                        msgLayout.setStackFromEnd(true);
                        msgView.setLayoutManager(msgLayout);
                        EditText messageBox = view.findViewById(R.id.editText);
                        ImageButton sendMessage = view.findViewById(R.id.sendMessage);
                        // Set up message sending listener
                        sendMessage.setOnClickListener(vw -> {
                            if(!publicSwitch.isChecked()) {
                                return;
                            }
                            String messageBoxText = messageBox.getText().toString();

                            // Reset message box
                            messageBox.setText("");

                            BrzMessage msg = new BrzMessage(api.hostNode.id, "", messageBoxText, System.currentTimeMillis(), false);
                            api.sendPublicMessage(msg);
                        });

                        //Toast.makeText(getActivity(), "Thread activity: ON", Toast.LENGTH_LONG).show();
                        publicSwitch.setChecked(true);
                        api.state.setPublicThreadOn(true);
                        return true;
                    }
            }
            return false;
        });


        this.onlineListener = (o) -> {
            TextView numberOnline = view.findViewById(R.id.number_online);
            numberOnline.setTextColor(Color.parseColor("#66E0FF"));
            //numberOnline.setVisibility(View.VISIBLE);
            numberOnline.setText("Online: " + (graph.getSize() - 1));

        };
        onlineListener.accept(null);



    }

    @Override
    public void onStart() {
        super.onStart();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Public Feed");

        BrzGraph graph = BreezeAPI.getInstance().getGraph();
        graph.on("addVertex", this.onlineListener);
        graph.on("deleteVertex", this.onlineListener);
        graph.on("setVertex", this.onlineListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        BrzGraph graph = BreezeAPI.getInstance().getGraph();
        graph.on("addVertex", this.onlineListener);
        graph.on("deleteVertex", this.onlineListener);
        graph.on("setVertex", this.onlineListener);
        try {
            this.list.cleanup();
        } catch(Exception e){
            Log.i("PUBLICMESSAGESVIEW", "Error while cleaning up");
        }
    }

}