package com.breeze.views.UserSelection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.function.Consumer;

public class UserSelection extends Fragment {

    private List<String> nodes = new LinkedList<>();
    List<TextView> nodeViews = new LinkedList<>();
    private UserList list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("New chat");

        // Set the done
        FloatingActionButton fab = view.findViewById(R.id.user_selection_fab);
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

            // Navigate back to chats
            NavHostFragment.findNavController(this)
                    .navigate(R.id.chatsView);
        });

        // Set up the list
        RecyclerView recyclerView = view.findViewById(R.id.user_selection_user_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);

        this.list = new UserList(activity, this.nodes);
        recyclerView.setAdapter(this.list);

        // When the user selects a node, add it to the list
        this.list.setItemSelectedListener(node -> {
            int nodeI = nodes.indexOf(node.id);
            if (nodeI == -1) {
                LinearLayout.LayoutParams nodeLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                nodeLayout.leftMargin = 10;
                nodeLayout.bottomMargin = 10;

                TextView nodeView = new TextView(activity);
                nodeView.setText(node.name);
                nodeView.setPadding(20, 15, 20, 15);
                nodeView.setLayoutParams(nodeLayout);
                nodeView.setBackgroundResource(R.drawable.status_bubble);
                nodeView.setId(nodeI);

                FlexboxLayout toList = view.findViewById(R.id.user_selection_to_list);
                toList.addView(nodeView);

                nodes.add(node.id);
                nodeViews.add(nodeView);
            } else {
                FlexboxLayout toList = view.findViewById(R.id.user_selection_to_list);
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
        EditText search = view.findViewById(R.id.user_selection_search);
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

    private Consumer<Object> graphListener;

    @Override
    public void onStart() {
        super.onStart();

        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = BrzGraph.getInstance();
        this.graphListener = newNode -> {
            api.requestProfileImages(graph);
        };

        // Set up event listeners
        this.graphListener.accept(null);
        graph.on("addVertex", this.graphListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        list.cleanup();

        BrzGraph graph = BrzGraph.getInstance();
        graph.off("addVertex", this.graphListener);
    }
}
