package com.breeze.application.actions;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.application.BreezeAPI;
import com.breeze.datatypes.BrzNode;
import com.breeze.packets.BrzPacket;

import org.json.JSONObject;

import java.io.File;

public class BreezeActionSendPacket extends BreezeAction {

    private BrzPacket packet;
    private File streamFile;

    BreezeActionSendPacket(@NonNull String json) {
        super(BREEZE_MODULE.GRAPH, ACTION_TYPE.SEND_PACKET, "addVertex");
        this.fromJSON(json);
    }

    BreezeActionSendPacket(@NonNull BrzPacket packet) {
        super(BREEZE_MODULE.GRAPH, ACTION_TYPE.SEND_PACKET, "addVertex");

        if (packet.to == null || packet.to.isEmpty())
            throw new IllegalArgumentException("Packet was not valid");

        this.packet = packet;
        this.streamFile = null;
    }

    BreezeActionSendPacket(@NonNull BrzPacket packet, @NonNull File streamFile) {
        super(BREEZE_MODULE.GRAPH, ACTION_TYPE.SEND_PACKET, "addVertex");

        if (packet.to == null || packet.to.isEmpty() || packet.stream == null)
            throw new IllegalArgumentException("Packet was not valid");

        this.packet = packet;
        this.streamFile = streamFile;
    }

    @Override
    protected boolean doAction() {
        try {
            BreezeAPI api = BreezeAPI.getInstance();
            BrzNode n = api.getGraph().getVertex(this.packet.to);

            // If the target node was not found, return action failure
            if (n == null) throw new RuntimeException("The target node was not found");

            // If the target node was found, send the packet!
            if (streamFile != null)
                api.sendStreamPacket(packet, Uri.fromFile(streamFile));
            else
                api.router.send(packet);

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

            json.put("packet", this.packet.toJSON());
            json.put("streamFile", this.streamFile.getAbsolutePath());

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
            this.packet = new BrzPacket(jsonObj.getString("packet"));
            this.streamFile = new File(jsonObj.getString("streamFile"));
        } catch (Exception e) {
            Log.e("DESERIALIZATION ERROR", "BreezeActionSaveNode", e);
        }
    }
}
