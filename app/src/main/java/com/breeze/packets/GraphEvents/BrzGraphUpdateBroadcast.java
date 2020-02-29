package com.breeze.packets.GraphEvents;

import android.util.Log;

import com.breeze.datatypes.BrzNode;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzGraphUpdateBroadcast implements BrzSerializable {

    public BrzGraph diff = null;
    public BrzGraphUpdateBroadcast(String json) {
        this.fromJSON(json);
    }
    public BrzGraphUpdateBroadcast(BrzGraph diff) {
        this.diff = diff;
    }

    @Override
    public void fromJSON(String json) {
        try {
            this.diff = new BrzGraph(json);
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "err", e);
        }
    }

    @Override
    public String toJSON() {
        return diff.toJSON();
    }
}


