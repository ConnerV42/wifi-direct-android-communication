package com.breeze.packets;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class BrzUser implements BrzSerializable {
    public String id = "";
    public String name = "";
    public String alias = "";
    public String profileImage = "";

    public void setProfileImage(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        this.profileImage = Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public Bitmap getProfileImage() {
        byte[] profImageBytes = Base64.decode(this.profileImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(profImageBytes, 0, profImageBytes.length);
    }

    @Override
    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("name", this.name);
            json.put("alias", this.alias);
            json.put("profileImage", this.profileImage);
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jobj = new JSONObject(json);
            this.id = jobj.getString("id");
            this.name = jobj.getString("name");
            this.alias = jobj.getString("alias");
            this.profileImage = jobj.getString("profileImage");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
