package com.breeze.packets.LiveAudio;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzLiveAudioEvent implements BrzSerializable {

    public String from = "";
    public Boolean accepted = false;

    public BrzLiveAudioEvent(String json) {
        this.fromJSON(json);
    }

    public BrzLiveAudioEvent() {
        this.from = BreezeAPI.getInstance().hostNode.id;
    }

    public BrzLiveAudioEvent(Boolean accepted) {
        this.from = BreezeAPI.getInstance().hostNode.id;
        this.accepted = accepted;
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.accepted = jObj.getBoolean("accepted");
            this.from = jObj.getString("from");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", "BrzLiveAudioEvent", e);
        }
    }

    @Override
    public String toJSON() {
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("accepted", this.accepted);
            jObj.put("from", this.from);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BrzLiveAudioEvent", e);
        }

        return jObj.toString();
    }

}
