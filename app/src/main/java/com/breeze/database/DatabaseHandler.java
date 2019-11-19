package com.breeze.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.breeze.dbmodels.DBBrzProfile;
import com.breeze.dbmodels.DBBrzMessage;
import com.breeze.dbmodels.DBBrzPreference;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "breezetables";
    private static final int DATABASE_VERSION = 1;

    private static final String PROFILE_TABLE_NAME = "Profiles";
    private static final String MESSAGES_TABLE_NAME = "Messages";
    private static final String PREFERENCES_TABLE_NAME = "Preferences";

    private static final String INIT_PROFILE_TABLE = "CREATE TABLE IF NOT EXISTS Profiles  " +
            "('id' INTEGER PRIMARY KEY, " +
            "'name' TEXT NOT NULL, " +
            "'alias' TEXT UNIQUE NOT NULL, " +
            "'signature' TEXT NOT NULL, " +
            "'lasttalkedto' DATE NOT NULL, " +
            "'blocked' BOOLEAN NOT NULL, " +
            "'friend' BOOLEAN NOT NULL, " +
            "'publickey' TEXT)";
    private static final String INIT_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS Messages " +
            "('id' INTEGER PRIMARY KEY, " +
            "'sender' INTEGER NOT NULL, " +
            "'recipient' INTEGER NOT NULL," +
            "'senderalias' TEXT NOT NULL," +
            "'recipientalias' TEXT NOT NULL," +
            "'body' TEXT NOT NULL, " +
            "'datetime' TEXT NOT NULL, " +
            "'signature' TEXT DEFAULT NULL, " +
            "FOREIGN KEY ('sender') REFERENCES Profiles('id'))";
    private static final String INIT_PREFS_TABLE = "CREATE TABLE IF NOT EXISTS Preferences " +
            "('id' INTEGER PRIMARY KEY, " +
            "'name' TEXT UNIQUE NOT NULL, " +
            "'setting' TEXT NOT NULL)";


    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(DatabaseHandler.INIT_PROFILE_TABLE);
        db.execSQL(INIT_MESSAGE_TABLE);
        db.execSQL(INIT_PREFS_TABLE);
        Log.i("DatabaseInfo", "Table Creations succeeded for Contacts, Messages, Preferences");
    }
    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //migration process if we want to upgrade in future.
        //For now, it'll drop all tables and redo onCreate
        final String DROP_CONTACT_TABLE = "DROP TABLE IF EXISTS Contact";
        db.execSQL(DROP_CONTACT_TABLE);
        final String DROP_MESSAGE_TABLE = "DROP TABLE IF EXISTS Messages";
        db.execSQL(DROP_MESSAGE_TABLE);
        final String DROP_PREFS_TABLE = "DROP TABLE IF EXISTS Preferences";
        db.execSQL(DROP_PREFS_TABLE);
        this.onCreate(db);
    }
    public void addContact( DBBrzProfile DBBrzProfile)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", DBBrzProfile.getId());
        vals.put("name", DBBrzProfile.getName());
        vals.put("alias", DBBrzProfile.getAlias());
        vals.put("signature", DBBrzProfile.getSignature());
        vals.put("lasttalkedto", DBBrzProfile.getLastTalkedTo());
        vals.put("friend", DBBrzProfile.isFriend());
        vals.put("blocked", DBBrzProfile.isBlocked());
        db.insert(PROFILE_TABLE_NAME, null, vals);
        db.close();
    }
    public void addMessage( DBBrzMessage message)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", message.getId());
        vals.put("body", message.getBody());
        vals.put("datetime", message.getDatetime());
        vals.put("encryption", message.getEncryption());
        db.insert(MESSAGES_TABLE_NAME, null, vals);
        db.close();
    }
    public void addPreference( DBBrzPreference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", preference.getId());
        vals.put("name", preference.getName());
        vals.put("setting", preference.getSetting());
        db.insert(PREFERENCES_TABLE_NAME, null, vals);
        db.close();
    }
    public void deleteOneContact( DBBrzProfile contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PROFILE_TABLE_NAME, "id = ?", new String[]{ String.valueOf(contact.getId())});
        db.close();
    }
    public void deleteOneMessage( DBBrzMessage message)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MESSAGES_TABLE_NAME, "id = ?", new String[]{ String.valueOf(message.getId())});
        db.close();
    }
    public void deleteOnePreference( DBBrzPreference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PREFERENCES_TABLE_NAME, "id = ?", new String[]{ String.valueOf(preference.getId())});
        db.close();
    }
    public int updateContact(DBBrzProfile contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        return -1;
    }
    public int updateMessage(DBBrzMessage message)
    {
        return -1;
    }
    public int updatePreference(DBBrzPreference preference)
    {
        return -1;
    }
    public DBBrzProfile getProfileByAlias(String alias)
    {
        return new DBBrzProfile(-1, "","","");
    }
    public DBBrzProfile getProfile(int id)
    {
        return new DBBrzProfile(-1, "","","");
    }
    public List<DBBrzProfile> getAllProfiles()
    {
        return new ArrayList<>();
    }
    public DBBrzMessage getMessageById(int id)
    {
        return null;
    }
    public List<DBBrzMessage> getMessagesFromAlias()
    {
        return null;
    }
    public List<DBBrzMessage> getMessagesFromId()
    {
        return null;
    }
    public List<DBBrzMessage> getMessagesSentToId()
    {
        return null;
    }
    public List<DBBrzMessage> getMessagesSentToAlias()
    {
        return null;
    }
    public List<DBBrzMessage> getAllSentMessages()
    {
        return null;
    }
    public List<DBBrzMessage> getAllReceivedMessages()
    {
        return null;
    }
    public DBBrzPreference getPreference(int id)
    {
        return null;
    }
    public DBBrzPreference getPreferenceByName(String name)
    {
        return null;
    }
}
