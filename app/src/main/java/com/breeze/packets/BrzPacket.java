package com.breeze.packets;

import android.util.Log;

import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.packets.ChatEvents.BrzChatHandshake;
import com.breeze.packets.ChatEvents.BrzChatResponse;
import com.breeze.packets.GraphEvents.BrzGraphEvent;
import com.breeze.packets.GraphEvents.BrzGraphQuery;
import com.breeze.packets.MessageEvents.BrzMessageReceipt;
import org.json.JSONObject;
import java.util.UUID;

public class BrzPacket implements BrzSerializable {

    public enum BrzPacketType {
        MESSAGE,
        FILE_INFO,
        ACK,

        GRAPH_QUERY,
        GRAPH_EVENT,

        CHAT_HANDSHAKE,
        CHAT_RESPONSE,

        MESSAGE_RECEIPT
    }

    public String id = UUID.randomUUID().toString();
    public String to = "BROADCAST";
    public BrzPacketType type = BrzPacketType.MESSAGE;
    public String body = "";

    public BrzPacket() {
    }

    public BrzPacket(BrzSerializable body) {
        this.body = body.toJSON();
    }

    public BrzPacket(String json) {
        this.fromJSON(json);
    }

    public BrzMessage message() {
        return new BrzMessage(this.body);
    }

    public BrzFileInfo fileInfoPacket() {
        return new BrzFileInfo(this.body);
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

    @Override
    public String toJSON() {

        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("to", this.to);
            json.put("type", this.type);
            json.put("body", this.body);
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
            this.to = jObj.getString("to");
            this.type = BrzPacketType.valueOf(jObj.getString("type"));
            this.body = jObj.getString("body");
        } catch (Exception e) {
            Log.i("DESERIALIZATION ERROR", e.toString());
        }
    }
}
