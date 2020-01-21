package com.breeze.application;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.breeze.App;
import com.breeze.MainActivity;
import com.breeze.R;
import com.breeze.database.DatabaseHandler;
import com.breeze.datatypes.BrzFileInfo;
import com.breeze.datatypes.BrzNode;
import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.graph.BrzGraph;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.ChatEvents.BrzChatHandshake;
import com.breeze.packets.ChatEvents.BrzChatResponse;
import com.breeze.packets.ProfileEvents.BrzProfileRequest;
import com.breeze.packets.ProfileEvents.BrzProfileResponse;
import com.breeze.router.BrzRouter;
import com.breeze.state.BrzStateStore;
import com.breeze.storage.BrzStorage;
import com.breeze.views.ProfileActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class BreezeAPI extends Service {

    // Singleton

    private static BreezeAPI instance = new BreezeAPI();

    public static BreezeAPI getInstance() {
        return instance;
    }

    // Behavioral Modules

    final private String ACTION_STOP_SERVICE = "STOP THIS NOW";
    public SharedPreferences preferences;

    public BrzRouter router = null;
    public BrzStorage storage = null;
    public BrzStateStore state = null;
    public DatabaseHandler db = null;

    // Api modules

    public BreezeMetastateModule meta = null;
    public BreezeEncryptionModule encryption = null;

    // Data members

    public BrzNode hostNode = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        this.initialize(this);

        // Upgrade to foreground process
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent stopSelf = new Intent(this, BreezeAPI.class);
        stopSelf.setAction(this.ACTION_STOP_SERVICE);
        PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelf, 0);

        Notification notification = new NotificationCompat.Builder(this, App.SERVICE_CHANNEL_ID)
                .setContentTitle("Breeze Service")
                .setContentText("Breeze is running in the background")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pending)
                .addAction(R.drawable.ic_launcher, "Stop", pStopSelf)
                .build();

        startForeground(1, notification);

        Intent shellIntent = new Intent(this, MainActivity.class);
        shellIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(shellIntent);

        // There isn't a saved host node yet
        if (!meta.getCachedHostNode()) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            profileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(profileIntent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
            router.stop();
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        router.stop();
        super.onDestroy();
    }

    public void initialize(Context ctx) {
        // Initialize api modules
        if (this.router == null)
            this.router = BrzRouter.initialize(ctx, "BREEZE_MESSENGER");
        if (this.storage == null)
            this.storage = BrzStorage.initialize(ctx);
        if (this.state == null)
            this.state = BrzStateStore.getStore();
        if (this.db == null)
            this.db = new DatabaseHandler(ctx);

        // Initialize api modules
        if (this.encryption == null)
            this.encryption = new BreezeEncryptionModule(this);
        if (this.meta == null)
            this.meta = new BreezeMetastateModule(this);

        // Initialize preferences
        if (this.preferences == null)
            this.preferences = ctx.getSharedPreferences("Breeze", Context.MODE_PRIVATE);
    }

    //
    //
    // Host Node
    //
    //

    public void setHostNode(BrzNode hostNode) {
        if (hostNode == null)
            return;

        // Set up encryption
        this.encryption.setHostNode(hostNode);

        this.hostNode = hostNode;

        // Set our id in preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(App.PREF_HOST_NODE_ID, hostNode.id);
        editor.apply();

        // Push the change elsewhere
        this.router.start(hostNode);
        this.state.setHostNode(hostNode);
        this.db.setNode(hostNode);
    }

    //
    //
    // Chat handshakes
    //
    //

    public void sendChatHandshakes(BrzChat chat) {
        chat.acceptedByHost = true;
        chat.acceptedByRecipient = false;

        if (!chat.nodes.contains(this.hostNode.id))
            chat.nodes.add(this.hostNode.id);

        BrzChatHandshake handshake = new BrzChatHandshake(this.router.hostNode.id, chat);
        encryption.makeSecretKey(handshake);

        BrzPacket p = new BrzPacket(handshake, BrzPacket.BrzPacketType.CHAT_HANDSHAKE, "", false);

        for (String nodeId : chat.nodes) {
            if (nodeId.equals(hostNode.id))
                continue;
            p.to = nodeId;
            this.router.send(p);
        }

        this.updateChat(chat);
    }

    public void updateChat(BrzChat chat) {
        this.state.addChat(chat);
        this.db.setChat(chat);
    }

    public void leaveChat(String chatId) {
        this.rejectHandshake(chatId);
    }

    public void deleteChat(String chatId) {
        this.db.deleteChat(chatId);
        this.db.deleteChatMessages(chatId);
        this.state.removeChat(chatId);
        this.encryption.deleteSecretKey(chatId);
    }

    public void incomingChatResponse(BrzChatResponse response) {
        BrzChat c = this.state.getChat(response.chatId);
        BrzNode n = BrzGraph.getInstance().getVertex(response.from);
        if (c == null || n == null)
            return;

        // Chat accepted!
        if (response.accepted) {
            c.acceptedByRecipient = true;

            BrzMessage statusMessage = new BrzMessage(this.hostNode.id, response.chatId, n.name + " joined the chat", System.currentTimeMillis(), true);
            this.addMessage(statusMessage);
        }

        // Rejected
        else {

            // The chat is a group
            if (c.isGroup)
                c.nodes.remove(response.from);

            BrzMessage statusMessage = new BrzMessage(this.hostNode.id, response.chatId, n.name + " left the chat.", System.currentTimeMillis(), true);
            this.addMessage(statusMessage);
        }

        this.state.addChat(c);
        this.db.setChat(c);
    }

    public void incomingHandshake(BrzChatHandshake handshake) {
        BrzChat chat = handshake.chat;
        if (!chat.isGroup) {
            BrzNode n = BrzGraph.getInstance().getVertex(handshake.from);
            chat.name = n.name;
        }

        chat.acceptedByHost = false;
        chat.acceptedByRecipient = false;

        this.state.addChat(handshake.chat);
        this.db.setChat(handshake.chat);

        // Store the encryption key temporarially
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("HANDSHAKE_KEY_" + chat.id, handshake.secretKey);
        editor.apply();
    }

    public void acceptHandshake(String chatId) {
        BrzChatResponse response = new BrzChatResponse(this.hostNode.id, chatId, true);
        BrzChat c = this.state.getChat(chatId);

        // Send acceptance responses to all participants
        BrzPacket p = new BrzPacket(response, BrzPacket.BrzPacketType.CHAT_RESPONSE, "", false);
        for (String nodeId : c.nodes) {
            if (!nodeId.equals(this.hostNode.id)) {
                p.to = nodeId;
                this.router.send(p);
            }
        }

        // Get the stored secret key from temp
        String secretKey = this.preferences.getString("HANDSHAKE_KEY_" + chatId, "");
        if (secretKey.isEmpty())
            throw new RuntimeException("Handshake's encryption was not stored somehow");

        // Save the encryption key to the keystore and remove it from temp
        encryption.saveSecretKey(chatId, secretKey);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("HANDSHAKE_KEY_" + chatId).apply();

        // Set state to have the chat accepted
        c.acceptedByHost = true;
        c.acceptedByRecipient = true;
        this.state.addChat(c);
        this.db.setChat(c);
    }

    public void rejectHandshake(String chatId) {
        BrzChatResponse response = new BrzChatResponse(this.hostNode.id, chatId, false);
        BrzChat c = this.state.getChat(chatId);

        // Send rejection responses to all participants
        BrzPacket p = new BrzPacket(response, BrzPacket.BrzPacketType.CHAT_RESPONSE, "", false);
        for (String nodeId : c.nodes) {
            if (!nodeId.equals(this.hostNode.id)) {
                p.to = nodeId;
                this.router.send(p);
            }
        }

        // Remove the encryption key from temp
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("HANDSHAKE_KEY_" + chatId).apply();

        this.deleteChat(chatId);
    }

    //
    //
    // Messaging
    //
    //

    public void sendMessage(BrzMessage message) {
        BrzMessage clone = new BrzMessage(message.toJSON());

        // Encrypt the message
        this.encryption.encryptMessage(clone);

        // Build a packet
        BrzPacket p = new BrzPacket(clone, BrzPacket.BrzPacketType.MESSAGE, "", false);

        // Send message to each recipient
        BrzChat chat = this.state.getChat(clone.chatId);
        for (String nodeId : chat.nodes) {
            if (nodeId.equals(hostNode.id))
                continue;
            p.to = nodeId;
            this.router.send(p);
        }

        this.addMessage(message);
    }

    public void openFile(File privateFile, String name) {
        ContentResolver res = getContentResolver();
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File publicFile = new File(downloadsDir, name);
        Log.i("API", publicFile.getAbsolutePath());

        Uri publicFileUri = FileProvider.getUriForFile(this, "com.breeze.fileprovider", publicFile);

        try {
            this.storage.saveMessageFile(publicFile, res.openInputStream(Uri.fromFile(privateFile)));
        } catch (Exception e) {
            Log.e("API", "Error saving public access file", e);
        }

        grantUriPermission(getPackageName(), publicFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(publicFileUri, res.getType(publicFileUri));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Log.i("API", "" + publicFileUri.toString() + " " + res.getType(publicFileUri));

        try {
            startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

    public void sendFileMessage(BrzMessage message, Uri fileUri) {
        ContentResolver res = getContentResolver();
        BrzFileInfo info = new BrzFileInfo();
        info.fileName = new File(fileUri.getPath()).getName();

        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getExtensionFromMimeType(res.getType(fileUri));

        if (message.body.startsWith("File")) {
            message.body = "File: " + info.fileName;
        }

        try {
            storage.saveMessageFile(message, res.openInputStream(fileUri));
            BrzMessage clone = new BrzMessage(message.toJSON());

            // Encrypt the message
            this.encryption.encryptMessage(clone);

            // Build a packet
            BrzPacket p = new BrzPacket(clone, BrzPacket.BrzPacketType.MESSAGE, "", false);
            p.addStream(info);

            // Send message to each recipient
            BrzChat chat = this.state.getChat(clone.chatId);
            for (String nodeId : chat.nodes) {
                if (nodeId.equals(hostNode.id))
                    continue;
                p.to = nodeId;
                InputStream stream = res.openInputStream(fileUri);
                this.router.sendStream(p, stream);

                // Save outgoing file message
                storage.saveMessageFile(p.message(), stream);
            }

            this.addMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addMessage(BrzMessage message) {
        this.state.addMessage(message);
        this.db.addMessage(message);
    }

    //
    //
    // Profile Images
    //
    //

    public void requestProfileImages(List<String> ids) {
        BrzProfileRequest profileRequest = new BrzProfileRequest(this.hostNode.id, false);
        BrzPacket p = new BrzPacket(profileRequest, BrzPacket.BrzPacketType.PROFILE_REQUEST, "", true);

        for(String id : ids) {
            BrzNode node = router.graph.getVertex(id);
            if (node == null || !storage.hasProfileImage(node.id))
                continue;

            p.to = node.id;
            router.send(p);
        }
    }

    public void requestProfileImages(BrzGraph newNodes) {
        if (newNodes == null)
            return;

        BrzProfileRequest profileRequest = new BrzProfileRequest(this.hostNode.id, false);

        // send to newly merged nodes only
        for (BrzNode node : newNodes.getNodeCollection()) {
            BrzPacket p = new BrzPacket(profileRequest, BrzPacket.BrzPacketType.PROFILE_REQUEST, node.id, false);

            router.send(p);
        }
    }

    public void sendProfileResponse(BrzPacket packet, Bitmap bm) {
        BrzProfileResponse response = new BrzProfileResponse(router.hostNode.id, false);

        // Fuck the quality :)
        InputStream stream = BrzStorage.bitmapToInputStream(bm, 45);

        router.sendStream(packet, stream);
    }
}
