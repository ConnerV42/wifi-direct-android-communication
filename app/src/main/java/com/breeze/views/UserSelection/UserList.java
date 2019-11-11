package com.breeze.views.UserSelection;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breeze.R;
import com.breeze.graph.BrzGraph;
import com.breeze.graph.BrzNode;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzUser;
import com.breeze.state.BrzStateStore;

import java.util.ArrayList;
import java.util.List;

public class UserList extends BaseAdapter {

    private class UserComponent {
        ImageView image;
        TextView name;
        TextView alias;
    }

    private Context ctx;
    private List<BrzNode> nodes = new ArrayList<>();

    public UserList(Context ctx) {
        this.ctx = ctx;

        for(BrzNode node : BrzGraph.getInstance())
            this.nodes.add(node);
    }

    @Override
    public int getCount() {
        return nodes.size();
    }

    @Override
    public Object getItem(int i) {
        return nodes.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        BrzUser user = nodes.get(i).user;
        if(user == null) return convertView;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        UserComponent userComponent = new UserComponent();

        convertView = inflater.inflate(R.layout.li_user, null);
        convertView.setTag(userComponent);

        userComponent.name = convertView.findViewById(R.id.user_name);
        userComponent.name.setText(user.name);

        userComponent.alias = convertView.findViewById(R.id.user_alias);
        userComponent.alias.setText(user.alias);

        userComponent.image = convertView.findViewById(R.id.user_image);
        userComponent.image.setImageBitmap(user.getProfileImage());

        return convertView;
    }
}
