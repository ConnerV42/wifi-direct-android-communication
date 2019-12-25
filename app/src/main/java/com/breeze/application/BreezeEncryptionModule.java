package com.breeze.application;

import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;
import com.breeze.encryption.BrzEncryption;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ChatEvents.BrzChatHandshake;

import android.util.Base64;
import android.util.Log;

import java.security.PublicKey;

import javax.crypto.SecretKey;

public class BreezeEncryptionModule extends BreezeModule {
    private BrzEncryption encryption = new BrzEncryption();

    public BreezeEncryptionModule(BreezeAPI api) {
        super(api);
    }

    // Management stuff
    public void setHostNode(BrzNode n) {
        try {

            if (!encryption.storeContainsKey(n.id))
                encryption.generateAndSaveKeyPair(n.id);

            // Set up public key
            PublicKey key = encryption.getPublicKeyFromKeyStore(n.id);
            String keyStr = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
            n.publicKey = keyStr;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Generating host node keys failed");
        }
    }

    // Handshake stuff
    public void makeSecretKey(BrzChatHandshake handshake) {
        SecretKey key = encryption.generateAndSaveSymKey(handshake.chat.id);
        handshake.secretKey = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    public void saveSecretKey(BrzChatHandshake handshake) {
        encryption.saveSymKey(handshake.chat.id, handshake.secretKey);
    }

    // Message stuff
    public void encryptMessage(BrzMessage msg) {
        String body = encryption.symmetricEncrypt(msg.chatId, msg.body);
        if (body == null) throw new RuntimeException("Message did not encrypt successfully");
        msg.body = body;
    }

    public void decryptMessage(BrzMessage msg) {
        String body = encryption.symmetricDecrypt(msg.chatId, msg.body);
        if (body == null) throw new RuntimeException("Message did not decrypt successfully");
        msg.body = body;
    }

    // Packet stuff
    public void encryptPacket(BrzPacket p) {
        BrzNode destNode = BrzGraph.getInstance().getVertex(p.to);
        if (destNode == null)
            throw new IllegalArgumentException("Could not find the packet's destination node");

        String body = encryption.asymmetricEncrypt(destNode.publicKey, p.body);
        if (body == null) throw new RuntimeException("Could not encrypt packet");
        p.body = body;
    }

    public void decryptPacket(BrzPacket p) {
        String body = encryption.asymmetricDecrypt(api.hostNode.id, p.body);
        if (body == null) throw new RuntimeException("Could not encrypt packet");
        p.body = body;
    }
}
