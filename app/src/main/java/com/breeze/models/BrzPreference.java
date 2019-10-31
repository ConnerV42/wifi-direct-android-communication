package com.breeze.models;

import android.util.Log;

import com.breeze.packets.BrzSerializable;

import org.json.JSONObject;

public class BrzPreference implements BrzSerializable {
    private int id;
    private String name;
    private String setting;

    public BrzPreference(int id, String name, String setting) {
        this.id = id;
        this.name = name;
        this.setting = setting;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    @Override
    public String toString() {
        return "BrzPreference{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", setting='" + setting + '\'' +
                '}';
    }
    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("name", this.name);
            json.put("setting", this.setting);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }
        return json.toString();
    }
    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);
            this.id = Integer.parseInt(jObj.getString("id"));
            this.name = jObj.getString("name");
            this.setting = jObj.getString("setting");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
