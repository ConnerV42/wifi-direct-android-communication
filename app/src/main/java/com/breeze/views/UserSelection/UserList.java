package com.breeze.views.UserSelection;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.breeze.R;
import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzUser;
import com.breeze.state.BrzStateStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserList extends RecyclerView.Adapter<UserList.UserItemHolder>
        implements Filterable {

    public static class UserItemHolder extends RecyclerView.ViewHolder {
        View v;
        int position = 0;

        public UserItemHolder(View v) {
            super(v);
            this.v = v;
        }

        public void bind(BrzUser user, int position) {

            TextView user_name = v.findViewById(R.id.user_name);
            user_name.setText(user.name);

            TextView user_alias = v.findViewById(R.id.user_alias);
            user_alias.setText(user.alias);

            ImageView user_image = v.findViewById(R.id.user_image);
            user_image.setImageBitmap(user.getProfileImage());

            this.position = position;
        }
    }

    private List<BrzNode> filteredNodes = new ArrayList<>();
    private List<BrzNode> allNodes = new ArrayList<>();
    private Consumer<BrzNode> itemSelectedListener = null;

    public UserList(Context ctx) {
        for (BrzNode node : BrzGraph.getInstance()) {
            this.allNodes.add(node);
            this.filteredNodes.add(node);
        }
    }


    @NonNull
    @Override
    public UserList.UserItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Get inflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate a new li_user
        View user_list_item = inflater.inflate(R.layout.li_user, parent, false);

        // Make our holder
        UserItemHolder holder = new UserItemHolder(user_list_item);

        // Make the item clickable!
        user_list_item.setOnClickListener(e -> {
            if (this.itemSelectedListener != null)
                this.itemSelectedListener.accept(this.filteredNodes.get(holder.position));
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(UserItemHolder holder, int position) {
        holder.bind(filteredNodes.get(position).user, position);
    }

    @Override
    public int getItemCount() {
        return this.filteredNodes.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence searchSequence) {
                String searchStr = searchSequence.toString().toLowerCase();
                if (searchStr.isEmpty()) {
                    filteredNodes = allNodes;
                } else {
                    List<BrzNode> filteredList = new ArrayList<>();
                    for (BrzNode brzNode : allNodes) {
                        if (brzNode.user.alias.toLowerCase().contains(searchStr) || brzNode.user.name.toLowerCase().contains(searchStr))
                            filteredList.add(brzNode);
                    }

                    filteredNodes = filteredList;
                }

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
}
