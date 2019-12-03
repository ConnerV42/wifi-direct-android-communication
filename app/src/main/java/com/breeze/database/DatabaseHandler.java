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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private static final String INIT_BRZCHAT_TABLE = "CREATE TABLE IF NOT EXISTS BrzChat ('id' TEXT PRIMARY KEY, " +
            "'name' TEXT NOT NULL, " +
            "'nodes' TEXT NOT NULL, " +
            "'isGroup' BOOLEAN NOT NULL " +
//            "'publicKey' TEXT NOT NULL," +
//            "'privateKeyTag' TEXT NOT NULL," +
//            "'createdAt' DATETIME DEFAULT CURRENT_TIMESTAMP, " +
//            "'updatedAt' DATETIME" +
            ")";

    private static final String INIT_BRZMESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS BrzMessage ('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "'from' TEXT NOT NULL, " +
            "'body' TEXT NOT NULL, " +
            "'chatId' TEXT NOT NULL, " +
            "'isStatus' BOOLEAN NOT NULL, " +
            "'createdAt' DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "'updatedAt' DATETIME, " +
            "'read' BOOLEAN NOT NULL DEFAULT 0," +
            "FOREIGN KEY ('chatId') REFERENCES BrzChat(id)," +
            "FOREIGN KEY ('from') REFERENCES BrzNode(id))";

    private static final String INIT_BRZNODE_TABLE = "CREATE TABLE IF NOT EXISTS BrzNode ('id' TEXT PRIMARY KEY, " +
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

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        this.onUpgrade(getWritableDatabase(), 0, 0);
        this.onCreate(getWritableDatabase());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(INIT_BRZCHAT_TABLE);
        db.execSQL(INIT_BRZMESSAGE_TABLE);
        db.execSQL(INIT_BRZNODE_TABLE);
        Log.i("DatabaseInfo", "Table Creations succeeded for BrzChat, BrzMessage, BrzNode, and BrzProfile.");
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        for(String tableName : DatabaseHandler.tableNames)
        {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }
        this.onCreate(db);
    }

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


    public void setChat(@NonNull BrzChat chat) {
        SQLiteDatabase db = this.getWritableDatabase();
        Object[] args = {chat.id, chat.name, new JSONArray(chat.nodes).toString(), chat.isGroup ? 1 : 0};
        try {
            db.execSQL("INSERT OR REPLACE INTO " + BRZCHAT_TABLE_NAME + " VALUES (?,?,?,?)", args);
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

        for(int i = 0; i < c.getCount(); i++) {
            BrzChat n = new BrzChat();

            n.id = c.getString(c.getColumnIndex("id"));
            n.name = c.getString(c.getColumnIndex("name"));

            String nodes = c.getString(c.getColumnIndex("nodes"));
            n.nodesFromJson(nodes);

            int isGroup = c.getInt(c.getColumnIndex("isGroup"));
            n.isGroup = isGroup == 1;

            chats.add(n);

            c.moveToNext();
        }

        c.close();
        db.close();
        return chats;
    }

    public BrzChat getChat(@NonNull String chatId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = { chatId };

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

        c.close();
        db.close();
        return n;
    }

    public void addMessage(@NonNull BrzMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("[from]", message.from);

        vals.put("body", message.body);
        vals.put("isStatus", message.isStatus);
        vals.put("chatId", message.chatId);
        db.insert(BRZMESSAGE_TABLE_NAME, null, vals);
        db.close();
    }
    public void deleteMessage(@NonNull int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(BRZMESSAGE_TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }
    public BrzMessage getMessage(@NonNull int id){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + BRZMESSAGE_TABLE_NAME + " WHERE id = ?;", new String[]{String.valueOf(id)});
        if (c == null) {
            db.close();
            return null;
        } else if (c.getCount() < 1) {
            c.close();
            db.close();
            return null;
        }
        BrzMessage message = new BrzMessage();
        c.moveToFirst();
        message.from = c.getString(c.getColumnIndex("from"));
        message.body = c.getString(c.getColumnIndex("body"));
        message.chatId = c.getString(c.getColumnIndex("chatId"));
        message.isStatus = c.getString(c.getColumnIndex("isStatus")).equals("1");
        long milliseconds = 0;
        SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
        try {
            Date d = f.parse(c.getString(c.getColumnIndex("createdAt")));
            milliseconds = d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        message.datestamp = milliseconds;
        c.close();
        db.close();
        return message;
    }

    public List<BrzMessage> getInbox(@NonNull int currentUserId){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + BRZMESSAGE_TABLE_NAME + " WHERE 'from' != ?;", new String[]{String.valueOf(currentUserId)});
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
                message.from = c.getString(c.getColumnIndex("from"));
                message.body = c.getString(c.getColumnIndex("body"));
                message.chatId = c.getString(c.getColumnIndex("chatId"));
                message.isStatus = c.getString(c.getColumnIndex("isStatus")).equals("1");
                long milliseconds = 0;
                SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
                try {
                    Date d = f.parse(c.getString(c.getColumnIndex("createdAt")));
                    milliseconds = d.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                message.datestamp = milliseconds;
                list.add(message);
                c.moveToNext();
            }
        }
        return list;
    }

    public List<BrzMessage> getSentMessages(@NonNull int currentUserId){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + BRZMESSAGE_TABLE_NAME + " WHERE 'from' = ?;", new String[]{String.valueOf(currentUserId)});
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
                message.from = c.getString(c.getColumnIndex("from"));
                message.body = c.getString(c.getColumnIndex("body"));
                message.chatId = c.getString(c.getColumnIndex("chatId"));
                message.isStatus = c.getString(c.getColumnIndex("isStatus")).equals("1");
                long milliseconds = 0;
                SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
                try {
                    Date d = f.parse(c.getString(c.getColumnIndex("createdAt")));
                    milliseconds = d.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                message.datestamp = milliseconds;
                list.add(message);
                c.moveToNext();
            }
        }
        return list;
    }

    public List<BrzMessage> getChatMessages(@NonNull String chatId){
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = {chatId};
        Cursor c = db.rawQuery("SELECT * FROM " + BRZMESSAGE_TABLE_NAME + " WHERE [chatId] = ? ORDER BY createdAt asc;", args);
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
                message.from = c.getString(c.getColumnIndex("from"));
                message.body = c.getString(c.getColumnIndex("body"));
                message.chatId = c.getString(c.getColumnIndex("chatId"));
                message.isStatus = c.getString(c.getColumnIndex("isStatus")).equals("1");
                long milliseconds = 0;
                SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
                try {
                    Date d = f.parse(c.getString(c.getColumnIndex("createdAt")));
                    milliseconds = d.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                message.datestamp = milliseconds;
                list.add(message);
                c.moveToNext();
            }
        }
        return list;
    }




}
