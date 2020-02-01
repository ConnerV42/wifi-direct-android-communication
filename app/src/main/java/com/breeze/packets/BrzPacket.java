package com.breeze.packets;

import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.ChatEvents.BrzChatHandshake;
import com.breeze.packets.ChatEvents.BrzChatResponse;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;
import com.breeze.packets.MessageEvents.BrzMessageReceipt;
import com.breeze.packets.ProfileEvents.BrzProfileImageEvent;

import org.json.JSONObject;

import java.util.UUID;

public class BrzPacket implements BrzSerializable {

    public enum BrzPacketType {
        MESSAGE, PUBLIC_MESSAGE, MESSAGE_RECEIPT,

        GRAPH_QUERY, GRAPH_EVENT,

        CHAT_HANDSHAKE, CHAT_RESPONSE,

        PROFILE_REQUEST, PROFILE_RESPONSE,
    }

    public String id = UUID.randomUUID().toString();
    public BrzPacketType type = BrzPacketType.MESSAGE;
    public String to = "BROADCAST";
    public boolean broadcast = true;
    public String body = "";

    public BrzFileInfo stream;

    public BrzPacket() {
    }

    public BrzPacket(BrzSerializable body) {
        this.body = body.toJSON();
    }

    public BrzPacket(BrzSerializable body, BrzPacketType type, @NonNull String to, boolean broadcast) {
        this.body = body.toJSON();
        this.type = type;
        this.to = to;
        this.broadcast = broadcast;
    }

    public BrzPacket(String json) {
        this.fromJSON(json);
    }

    // Methods

    public void addStream(BrzFileInfo stream) {
        this.stream = stream;
    }

    // Body access

    public BrzMessage message() {
        return new BrzMessage(this.body);
    }

    public BrzGraphQuery graphQuery() {
        return new BrzGraphQuery(this.body);
    }

    public BrzGraphEvent graphEvent() {
        return new BrzGraphEvent(this.body);
    }

    public BrzChatHandshake chatHandshake() {
        return new BrzChatHandshake(this.body);
    }

    public BrzChatResponse chatResponse() {
        return new BrzChatResponse(this.body);
    }

    public BrzMessageReceipt messageReceipt() {
        return new BrzMessageReceipt(this.body);
    }

    public BrzProfileImageEvent profileImageEvent() { return new BrzProfileImageEvent(this.body); }

    public boolean hasStream() {
        return this.stream != null;
    }

    @Override
    public String toJSON() {

        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("type", this.type);
            json.put("to", this.to);
            json.put("broadcast", this.broadcast);
            json.put("body", this.body);

            if(this.hasStream())
            json.put("stream", this.stream.toJSON());
        } catch (Exception e) {
            Log.i("SERIALIZATION ERROR", e.toString());
        }

        return json.toString();
    }

    @Override
    public void fromJSON(String json) {
        try {
            JSONObject jObj = new JSONObject(json);

            this.id = jObj.getString("id");
            this.type = BrzPacketType.valueOf(jObj.getString("type"));
            this.to = jObj.getString("to");
            this.broadcast = jObj.getBoolean("broadcast");
            this.body = jObj.getString("body");

            if(jObj.has("stream")) {
                this.stream = new BrzFileInfo(jObj.getString("stream"));
            }
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
