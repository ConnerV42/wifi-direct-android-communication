package com.breeze.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.datatypes.BrzChat;
import com.breeze.datatypes.BrzMessage;
import com.breeze.datatypes.BrzNode;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "breezetables";
    private static final int DATABASE_VERSION = 1;

    private static final String[] tableNames = new String[]{
            "BrzChat",
            "BrzMessage",
            "BrzNode"
    };

    private static final String BRZCHAT_TABLE_NAME = "BrzChat";
    private static final String BRZMESSAGE_TABLE_NAME = "BrzMessage";
    private static final String BRZNODE_TABLE_NAME = "BrzNode";
    private static final String BRZRECEIPT_TABLE_NAME = "BrzMessageReceipt";

    private static final String INIT_BRZCHAT_TABLE = "CREATE TABLE IF NOT EXISTS BrzChat (" +
            "'id' TEXT PRIMARY KEY, " +
            "'name' TEXT NOT NULL, " +
            "'nodes' TEXT NOT NULL, " +
            "'isGroup' BOOLEAN NOT NULL, " +
            "'acceptedByHost' BOOLEAN NOT NULL, " +
            "'acceptedByRecipient' BOOLEAN NOT NULL " +
            ")";

    private static final String INIT_BRZMESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS BrzMessage (" +
            "'id' TEXT PRIMARY KEY, " +
            "'from' TEXT NOT NULL, " +
            "'body' TEXT NOT NULL, " +
            "'chatId' TEXT NOT NULL, " +
            "'isStatus' BOOLEAN NOT NULL, " +
            "'datestamp' INTEGER NOT NULL, " +

            "FOREIGN KEY ('chatId') REFERENCES BrzChat(id)," +
            "FOREIGN KEY ('from') REFERENCES BrzNode(id))";

    private static final String INIT_BRZNODE_TABLE = "CREATE TABLE IF NOT EXISTS BrzNode (" +
            "'id' TEXT PRIMARY KEY, " +
            "'endpointId' TEXT NOT NULL UNIQUE, " +
            "'publicKey' TEXT NOT NULL UNIQUE," +
            "'name' TEXT NOT NULL, " +
            "'alias' TEXT NOT NULL)";


    /**
     * TODO:
     * In order to connect BrzNodes to their respective chats in the database, I'm creating a
     * table that links them (BrzNode and BrzChat) together because it'll be a many to many relationship
     */
    private static final String INIT_CHAT_HAS_BRZNODE_TABLE = "CREATE TABLE IF NOT EXISTS BrzChatHasBrzNode ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "'brzNodeId' TEXT NOT NULL," +
            "'brzChatId' TEXT NOT NULL," +
            "FOREIGN KEY ('brzNodeId') REFERENCES BrzNode(id)," +
            "FOREIGN KEY ('brzChatId') REFERENCES BrzChat(id))";

    private static final String INIT_BRZRECEIPT_TABLE = "CREATE TABLE IF NOT EXISTS " + BRZRECEIPT_TABLE_NAME + " (" +
            "'id' TEXT PRIMARY KEY NOT NULL, " +
            "'delivered' BOOLEAN NOT NULL, " +
            "'read' BOOLEAN NOT NULL," +
            "FOREIGN KEY ('id') REFERENCES BrzMessage(id)" +
            ")";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.onCreate(getWritableDatabase());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(INIT_BRZCHAT_TABLE);
        db.execSQL(INIT_BRZMESSAGE_TABLE);
        db.execSQL(INIT_BRZNODE_TABLE);
        db.execSQL(INIT_BRZRECEIPT_TABLE);
        Log.i("DatabaseInfo", "SQLITE tables created successfully");
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String tableName : DatabaseHandler.tableNames) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }
        this.onCreate(db);
    }


    /*
     *
     *      BrzNode table
     *
     */

    public void setNode(@NonNull BrzNode node) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = {node.id, node.endpointId, node.publicKey, node.name, node.alias};
        try {
            db.execSQL("INSERT OR REPLACE INTO " + BRZNODE_TABLE_NAME + " VALUES (?,?,?,?,?)", args);
        } catch (Exception e) {
            Log.i("Bad SQL Error", "Error with SQL Syntax");
        }
        db.close();
    }

    public BrzNode getNode(@NonNull String nodeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {nodeId};

        Cursor c = db.rawQuery("SELECT * FROM " + BRZNODE_TABLE_NAME + " WHERE id = ?;", args);

        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }


        BrzNode n = new BrzNode();
        c.moveToFirst();

        n.id = c.getString(c.getColumnIndex("id"));
        n.endpointId = c.getString(c.getColumnIndex("endpointId"));
        n.publicKey = c.getString(c.getColumnIndex("publicKey"));
        n.name = c.getString(c.getColumnIndex("name"));
        n.alias = c.getString(c.getColumnIndex("alias"));

        c.close();
        db.close();
        return n;
    }

    public List<BrzNode> getAllNodes() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {};

        Cursor c = db.rawQuery("SELECT * FROM " + BRZNODE_TABLE_NAME + ";", args);

        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }

        c.moveToFirst();


        List<BrzNode> nodes = new LinkedList<>();
        for (int i = 0; i < c.getCount(); i++) {
            BrzNode n = new BrzNode();
            n.id = c.getString(c.getColumnIndex("id"));
            n.endpointId = c.getString(c.getColumnIndex("endpointId"));
            n.publicKey = c.getString(c.getColumnIndex("publicKey"));
            n.name = c.getString(c.getColumnIndex("name"));
            n.alias = c.getString(c.getColumnIndex("alias"));
            nodes.add(n);
            c.moveToNext();
        }

        c.close();
        db.close();
        return nodes;
    }

    /*
     *
     *      BrzChat table
     *
     */

    public void setChat(@NonNull BrzChat chat) {
        SQLiteDatabase db = this.getWritableDatabase();
        Object[] args = {chat.id, chat.name, new JSONArray(chat.nodes).toString(), chat.isGroup ? 1 : 0, chat.acceptedByHost ? 1 : 0, chat.acceptedByRecipient ? 1 : 0};
        try {
            db.execSQL("INSERT OR REPLACE INTO " + BRZCHAT_TABLE_NAME + " VALUES (?,?,?,?,?,?)", args);
        } catch (Exception e) {
            Log.i("Bad SQL Error", "Error with SQL Syntax");
        }
        db.close();
    }

    public void deleteChat(@NonNull String chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Object[] args = {chatId};
        try {
            db.execSQL("DELETE FROM " + BRZCHAT_TABLE_NAME + " WHERE id = ?", args);
        } catch (Exception e) {
            Log.i("Bad SQL Error", "Error with SQL Syntax");
        }
        db.close();
    }

    public List<BrzChat> getAllChats() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {};
        Cursor c = db.rawQuery("SELECT * FROM " + BRZCHAT_TABLE_NAME, args);

        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }

        c.moveToFirst();

        List<BrzChat> chats = new ArrayList<>();

        for (int i = 0; i < c.getCount(); i++) {
            BrzChat n = new BrzChat();

            n.id = c.getString(c.getColumnIndex("id"));
            n.name = c.getString(c.getColumnIndex("name"));

            String nodes = c.getString(c.getColumnIndex("nodes"));
            n.nodesFromJson(nodes);

            int isGroup = c.getInt(c.getColumnIndex("isGroup"));
            n.isGroup = isGroup == 1;

            int acceptedByHost = c.getInt(c.getColumnIndex("acceptedByHost"));
            n.acceptedByHost = acceptedByHost == 1;

            int acceptedByRecipient = c.getInt(c.getColumnIndex("acceptedByRecipient"));
            n.acceptedByRecipient = acceptedByRecipient == 1;

            chats.add(n);

            c.moveToNext();
        }

        c.close();
        db.close();
        return chats;
    }

    public BrzChat getChat(@NonNull String chatId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {chatId};

        Cursor c = db.rawQuery("SELECT * FROM " + BRZCHAT_TABLE_NAME + " WHERE id = ?;", args);

        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }

        BrzChat n = new BrzChat();
        c.moveToFirst();

        n.id = c.getString(c.getColumnIndex("id"));
        n.name = c.getString(c.getColumnIndex("name"));

        String nodes = c.getString(c.getColumnIndex("nodes"));
        n.nodesFromJson(nodes);

        int isGroup = c.getInt(c.getColumnIndex("isGroup"));
        n.isGroup = isGroup == 1;

        int acceptedByHost = c.getInt(c.getColumnIndex("acceptedByHost"));
        n.acceptedByHost = acceptedByHost == 1;

        int acceptedByRecipient = c.getInt(c.getColumnIndex("acceptedByRecipient"));
        n.acceptedByRecipient = acceptedByRecipient == 1;

        c.close();
        db.close();
        return n;
    }

    public List<BrzChat> getAcceptancePendingChats() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {};
        Cursor c = db.rawQuery("SELECT * FROM " + BRZCHAT_TABLE_NAME + " where acceptedByHost = 0", args);

        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }

        c.moveToFirst();

        List<BrzChat> chats = new ArrayList<>();

        for (int i = 0; i < c.getCount(); i++) {
            BrzChat n = new BrzChat();

            n.id = c.getString(c.getColumnIndex("id"));
            n.name = c.getString(c.getColumnIndex("name"));

            String nodes = c.getString(c.getColumnIndex("nodes"));
            n.nodesFromJson(nodes);

            int isGroup = c.getInt(c.getColumnIndex("isGroup"));
            n.isGroup = isGroup == 1;

            int acceptedByHost = c.getInt(c.getColumnIndex("acceptedByHost"));
            n.acceptedByHost = acceptedByHost == 1;

            int acceptedByRecipient = c.getInt(c.getColumnIndex("acceptedByRecipient"));
            n.acceptedByRecipient = acceptedByRecipient == 1;

            chats.add(n);

            c.moveToNext();
        }

        c.close();
        db.close();
        return chats;
    }

    /*
     *
     *      BrzMessage table
     *
     */

    public void addMessage(@NonNull BrzMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("[id]", message.id);
        vals.put("[from]", message.from);
        vals.put("body", message.body);
        vals.put("chatId", message.chatId);
        vals.put("isStatus", message.isStatus);
        vals.put("datestamp", message.datestamp);
        db.insert(BRZMESSAGE_TABLE_NAME, null, vals);

        db.execSQL("INSERT INTO " + BRZRECEIPT_TABLE_NAME + " VALUES (?,?,?)", new Object[]{message.id, 0, 0});

        db.close();
    }

    public void deleteMessage(@NonNull String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(BRZMESSAGE_TABLE_NAME, "id = ?", new String[]{id});
        db.close();
    }

    public List<BrzMessage> getChatMessages(@NonNull String chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = {chatId};
        Cursor c = db.rawQuery("SELECT * FROM " + BRZMESSAGE_TABLE_NAME + " WHERE [chatId] = ? ORDER BY datestamp asc;", args);
        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }
        ArrayList<BrzMessage> list = new ArrayList<>();
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                BrzMessage message = new BrzMessage();
                message.id = c.getString(c.getColumnIndex("id"));
                message.from = c.getString(c.getColumnIndex("from"));
                message.body = c.getString(c.getColumnIndex("body"));
                message.chatId = c.getString(c.getColumnIndex("chatId"));

                int isStatus = c.getInt(c.getColumnIndex("isStatus"));
                message.isStatus = isStatus == 1;

                message.datestamp = c.getLong(c.getColumnIndex("datestamp"));

                list.add(message);
                c.moveToNext();
            }
        }
        return list;
    }

    public void deleteChatMessages(@NonNull String chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(BRZMESSAGE_TABLE_NAME, "chatId = ?", new String[]{chatId});
        db.close();
    }

    public int getUnreadCount(@NonNull String chatId, @NonNull String hostId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {chatId, hostId};
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) as count FROM BrzMessage " +
                        "natural join ( select * from BrzMessageReceipt where read == 0 ) " +
                        "where chatId = ? and [from] != ?;"
                , args);

        if (c == null) {
            db.close();
            return 0;
        }

        c.moveToFirst();
        int unread = c.getInt(c.getColumnIndex("count"));

        c.close();
        db.close();

        return unread;
    }

    /*
     *
     *      BrzMessageReceipt table
     *
     */

    public void setDelivered(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Object[] args = {messageId};
        try {
            db.execSQL("UPDATE " + BRZRECEIPT_TABLE_NAME + " set delivered = 1 where id = ?", args);
        } catch (Exception e) {
            Log.i("Bad SQL Error", "Error with SQL Syntax");
        }
        db.close();
    }

    public void setRead(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Object[] args = {messageId};
        try {
            db.execSQL("UPDATE " + BRZRECEIPT_TABLE_NAME + " set read = 1 where id = ?", args);
        } catch (Exception e) {
            Log.i("Bad SQL Error", "Error with SQL Syntax");
        }
        db.close();
    }

    public boolean isDelivered(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT delivered FROM " + BRZRECEIPT_TABLE_NAME + " WHERE id = ?;", new String[]{messageId});
        if (c == null) {
            db.close();
            return false;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return false;
        }

        c.moveToFirst();
        int deliveredInt = c.getInt(c.getColumnIndex("delivered"));
        boolean delivered = deliveredInt == 1;

        c.close();
        db.close();
        return delivered;
    }

    public boolean isRead(String messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT read FROM " + BRZRECEIPT_TABLE_NAME + " WHERE id = ?;", new String[]{messageId});
        if (c == null) {
            db.close();
            return false;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return false;
        }

        c.moveToFirst();
        int readInt = c.getInt(c.getColumnIndex("read"));
        boolean read = readInt == 1;

        c.close();
        db.close();
        return read;
    }
}
