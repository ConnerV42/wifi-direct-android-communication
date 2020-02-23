package com.breeze.views.Settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.breeze.views.UserSelection.UserList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class BlackListView extends Fragment {
    public BlackListView() {
        // Required empty public constructor
    }
    private List<String> nodes = new LinkedList<>();

    private UserList list;

    private BreezeAPI api = BreezeAPI.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blacklist_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;
        ActionBar ab = activity.getSupportActionBar();
        if (ab == null) return;
        ab.setTitle("Connected Nodes");
        this.list = new UserList(activity, this.nodes);
        this.list.blacklist = true;

        RecyclerView recyclerView = view.findViewById(R.id.blackList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(this.list);

        this.list.setItemSelectedListener(selectedNode -> {
            this.triggerBlockedNode(selectedNode);
            list.notifyDataSetChanged();
            recyclerView.setAdapter(list);
        });
    }
    @Override
    public void onDetach() {
        super.onDetach();
    //    this.list.cleanup();
    }
    private Consumer<Object> graphListener;

    @Override
    public void onStart() {
        super.onStart();

        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = api.getGraph();
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

        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = api.getGraph();
        graph.off("addVertex", this.graphListener);
    }

    private void triggerBlockedNode(BrzNode node){
        if(this.api.state.getAllBlockedNodes().contains(node)){
            this.showConfirmUnblockDialog(node);
            this.api.state.unblockNode(node.id);
        }
        else {
            this.showConfirmBlockDialog(node);
            this.api.state.blockNode(node);
        }
    }
    private void showConfirmBlockDialog(BrzNode node) {
        new AlertDialog.Builder(this.getContext())
                .setTitle("Block User")
                .setMessage("Blocking " + node.alias + " from your device will result in ALL data from their device not being processed by your device, and you will not be able to start chats with the user")
                .setPositiveButton("I understand", null)
                .show();
    }
    private void showConfirmUnblockDialog(BrzNode node) {
        new AlertDialog.Builder(this.getContext())
                .setTitle("Unblock User")
                .setMessage("Unblocking " + node.alias + " from your device...")
                .setPositiveButton("I understand", null)
                .show();
    }
}
