package com.breeze.application.actions;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

import java.util.UUID;

public abstract class BreezeAction implements BrzSerializable {
    enum BREEZE_MODULE {ROUTER, STATE, GRAPH}
    enum ACTION_TYPE {SAVE_NODE}

    private String id;
    private BREEZE_MODULE module;
    private ACTION_TYPE actionType;
    private String eventName;

    BreezeAction(BREEZE_MODULE module, ACTION_TYPE actionType, String eventName) {
        this.module = module;
        this.actionType = actionType;
        this.eventName = eventName;
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public ACTION_TYPE getActionType() {
        return actionType;
    }

    public BREEZE_MODULE getModule() {
        return module;
    }

    public String getEventName() {
        return eventName;
    }

    /**
     * Attempts to perform an action
     *
     * @return A boolean that indicates weather the action was completed or not
     */
    protected boolean doAction() {
        return false;
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("module", this.module);
            json.put("actionType", this.actionType);
            json.put("eventName", this.eventName);
        } catch (Exception e) {
            Log.e("SERIALIZATION ERROR", "BreezeAction", e);
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.module = BREEZE_MODULE.valueOf(jObj.getString("module"));
            this.actionType = ACTION_TYPE.valueOf(jObj.getString("actionType"));
            this.eventName = jObj.getString("eventName");
            this.id = jObj.getString("id");
        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BreezeAction", e);
        }
    }

}
