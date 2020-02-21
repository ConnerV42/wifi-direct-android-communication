package com.breeze.views.Settings;

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
import android.widget.Toast;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
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
        RecyclerView recyclerView = view.findViewById(R.id.blackList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(this.list);
        this.list.setItemSelectedListener(selectedNode -> {
                Toast.makeText(getContext(), "touch handler goes here for node: " + selectedNode.alias, Toast.LENGTH_SHORT).show();
        });
    }
    @Override
    public void onDetach() {
        super.onDetach();
        this.list.cleanup();
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

}
