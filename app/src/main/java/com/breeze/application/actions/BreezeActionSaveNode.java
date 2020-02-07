package com.breeze.application.actions;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;

import org.json.JSONObject;

public class BreezeActionSaveNode extends BreezeAction {
    private String nodeId;

    BreezeActionSaveNode(String json, boolean isJson) {
        super(BREEZE_MODULE.GRAPH, ACTION_TYPE.SAVE_NODE, "addVertex");
        this.fromJSON(json);
    }

    BreezeActionSaveNode(String nodeId) {
        super(BREEZE_MODULE.GRAPH, ACTION_TYPE.SAVE_NODE, "addVertex");
        this.nodeId = nodeId;
    }

    @Override
    protected boolean doAction() {
        try {
            BreezeAPI api = BreezeAPI.getInstance();
            BrzNode n = api.getGraph().getVertex(this.nodeId);

            // If the target node was not found, return action failure
            if (n == null) throw new RuntimeException("The target node was not found");

            // If the target node was found, save it!
            api.db.setNode(n);
            api.state.setNode(n);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public String toJSON() {
        String jsonStr = super.toJSON();

        try {
            JSONObject json = new JSONObject(jsonStr);
            json.put("nodeId", this.nodeId);
            return json.toString();
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BreezeActionSaveNode", e);
        }

        return null;
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            this.nodeId = jsonObj.getString("nodeId");
        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BreezeActionSaveNode", e);
        }
    }
}
