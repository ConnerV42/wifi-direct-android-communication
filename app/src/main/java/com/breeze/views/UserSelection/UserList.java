package com.breeze.views.UserSelection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.breeze.R;
import com.breeze.application.BreezeAPI;
import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;
import com.breeze.router.BrzRouter;
import com.breeze.storage.BrzStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserList extends RecyclerView.Adapter<UserList.UserItemHolder>
        implements Filterable {

    public static class UserItemHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;
        boolean blacklist = false;
        BreezeAPI api = BreezeAPI.getInstance();
        public UserItemHolder(View v) {
            super(v);
            this.v = v;
        }
        public UserItemHolder(View v, boolean blacklist) {
            super(v);
            this.v = v;
            this.blacklist = blacklist;
        }
        public void bind(BrzNode node, int position, Context ctx, List<String> nodes) {
            boolean nodeIsInBlacklist = false;
            if(blacklist){
                if(api.state.getAllBlockedNodes().contains(node)){
                    nodeIsInBlacklist = true;
                }
            }
            TextView user_name = v.findViewById(R.id.user_name);
            user_name.setText(node.name);

            TextView user_alias = v.findViewById(R.id.user_alias);
            user_alias.setVisibility(View.VISIBLE);
            user_alias.setText(node.alias);

            // Set bitmap if it exists in BrzStorage
            ImageView user_image = v.findViewById(R.id.user_image);
            Bitmap bm = api.storage.getProfileImage(api.storage.PROFILE_DIR, node.id);
            if (bm != null)
                user_image.setImageBitmap(bm);

            user_name.setPaintFlags(user_name.getPaintFlags());
            if (nodes.contains(node.id)) {
                user_name.setTextColor(ctx.getColor(R.color.colorAccent));
                user_alias.setTextColor(ctx.getColor(R.color.colorAccent));
            } else if(nodeIsInBlacklist) {
                user_name.setTextColor(ctx.getColor(R.color.design_default_color_error));
                user_alias.setTextColor(ctx.getColor(R.color.design_default_color_error));
                user_name.setPaintFlags(user_name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                user_name.setTextColor(ctx.getColor(android.R.color.black));
                user_alias.setTextColor(ctx.getColor(android.R.color.black));
            }
            this.position = position;
        }

        public void bind(String placeholderId, Context ctx) {
            TextView user_name = v.findViewById(R.id.user_name);
            user_name.setText("Connecting...");

            TextView user_alias = v.findViewById(R.id.user_alias);
            user_alias.setVisibility(View.GONE);

            user_name.setTextColor(ctx.getColor(android.R.color.black));
            user_alias.setTextColor(ctx.getColor(android.R.color.black));

            this.position = -1;
        }
    }

    private List<BrzNode> filteredNodes = new ArrayList<>();
    private List<BrzNode> allNodes = new ArrayList<>();
    private List<String> placeholders = new ArrayList<>();
    private Consumer<BrzNode> itemSelectedListener = null;

    private Context ctx;
    private List<String> nodes;

    private Consumer<String> profileImageListener;
    private Consumer<Object> graphListener;
    private Consumer<String> placeholderListener;
    private Consumer<String> removePlaceholderListener;
    public boolean blacklist = false;


    public UserList(Context ctx, List<String> nodes) {
        this.ctx = ctx;
        this.nodes = nodes;
        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = api.getGraph();
        BrzRouter router = api.router;

        this.graphListener = newNode -> {
            this.allNodes = new ArrayList<>(graph.getNodeCollection());
            notifyDataSetChanged();
            this.getFilter().filter("");
        };

        // Set up event listeners
        this.graphListener.accept(null);
        graph.on("addVertex", this.graphListener);
        graph.on("deleteVertex", this.graphListener);
        graph.on("setVertex", this.graphListener);

        this.placeholderListener = endpointId -> {
            this.placeholders.add(endpointId);
            this.notifyDataSetChanged();
        };
        this.removePlaceholderListener = endpointId -> {
            this.placeholders.remove(endpointId);
            this.notifyDataSetChanged();
        };

        router.on("endpointFound", this.placeholderListener);
        router.on("endpointConnected", this.removePlaceholderListener);
        router.on("endpointDisconnected", this.removePlaceholderListener);

        this.profileImageListener = nodeId -> {
            this.notifyDataSetChanged();
        };

        api.storage.on("downloadDone", this.profileImageListener);
    }


    @NonNull
    @Override
    public UserList.UserItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new li_user
        View user_list_item = inflater.inflate(R.layout.li_user, parent, false);
        UserItemHolder holder = new UserItemHolder(user_list_item, blacklist);
        // Make the item clickable!
        user_list_item.setOnClickListener(e -> {
            try {
                if (this.itemSelectedListener != null)
                    this.itemSelectedListener.accept(this.filteredNodes.get(holder.position));
                holder.bind(filteredNodes.get(holder.position), holder.position, ctx, nodes);
            }
            catch (ArrayIndexOutOfBoundsException error) {
                Log.i("UserList.java - onCreateViewHolder", "User tried to click on a \"Connecting...\" node");
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull UserItemHolder holder, int position) {
        if (position >= filteredNodes.size()) {
            holder.bind(this.placeholders.get(position - filteredNodes.size()), ctx);
        } else {
            holder.bind(filteredNodes.get(position), position, ctx, nodes);
        }
    }

    @Override
    public int getItemCount() {
        return this.filteredNodes.size() + this.placeholders.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence searchSequence) {
                String searchStr = searchSequence.toString().toLowerCase();
                if (searchStr.isEmpty()) searchStr = "@";
                BreezeAPI api = BreezeAPI.getInstance();

                List<BrzNode> filteredList = new ArrayList<>();
                for (BrzNode brzNode : allNodes) {
                    String id = brzNode.id;
                    String alias = brzNode.alias.toLowerCase();
                    String name = brzNode.name.toLowerCase();
//                    if (!id.equals(api.hostNode.id) && (alias.contains(searchStr) || name.contains(searchStr))){
//                        filteredList.add(brzNode);
//                    }
                    if(!blacklist){
                        if (!id.equals(api.hostNode.id) && (alias.contains(searchStr) || name.contains(searchStr))
                                && (!api.state.getAllBlockedNodes().contains(brzNode))
                        )
                            filteredList.add(brzNode);
                    }
                    else {
                        if (!id.equals(api.hostNode.id) && (alias.contains(searchStr) || name.contains(searchStr))){
                            filteredList.add(brzNode);
                        }
                    }

                }

                filteredNodes = filteredList;

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredNodes;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredNodes = (ArrayList<BrzNode>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public void setItemSelectedListener(Consumer<BrzNode> itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }

    public void cleanup() {
        BreezeAPI api = BreezeAPI.getInstance();
        BrzGraph graph = api.getGraph();

        graph.off("addVertex", this.graphListener);
        graph.off("deleteVertex", this.graphListener);
        graph.off("setVertex", this.graphListener);

        api.router.off("endpointFound", this.placeholderListener);
        api.router.off("endpointConnected", this.removePlaceholderListener);
        api.router.off("endpointDisconnected", this.removePlaceholderListener);

        api.storage.off("downloadDone", this.profileImageListener);
    }

}
