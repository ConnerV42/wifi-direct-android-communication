package com.breeze.views.UserSelection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.breeze.storage.BrzStorage;

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

        public void bind(BrzNode node, int position, Context ctx) {

            TextView user_name = v.findViewById(R.id.user_name);
            user_name.setText(node.name);

            TextView user_alias = v.findViewById(R.id.user_alias);
            user_alias.setText(node.alias);

            ImageView user_image = v.findViewById(R.id.user_image);
            Bitmap b = BrzStorage.getInstance().getProfileImage(node.id, ctx);
            Log.i("STATE", "For " + node.name + " image is " + (b != null));
            user_image.setImageBitmap(b);

            this.position = position;
        }
    }

    private List<BrzNode> filteredNodes = new ArrayList<>();
    private List<BrzNode> allNodes = new ArrayList<>();
    private Consumer<BrzNode> itemSelectedListener = null;

    private Context ctx;

    public UserList(Context ctx) {
        this.ctx = ctx;

        for (BrzNode node : BrzGraph.getInstance()) {
            this.allNodes.add(node);
            this.getFilter().filter("");
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
        holder.bind(filteredNodes.get(position), position, ctx);
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
                if (searchStr.isEmpty()) searchStr = "@";

                List<BrzNode> filteredList = new ArrayList<>();
                for (BrzNode brzNode : allNodes) {
                    if (!brzNode.id.equals(BreezeAPI.getInstance().hostNode.id) && (brzNode.alias.toLowerCase().contains(searchStr) || brzNode.name.toLowerCase().contains(searchStr)))
                        filteredList.add(brzNode);
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
}
