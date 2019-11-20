package com.breeze.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.breeze.dbmodels.DBBrzContact;
import com.breeze.dbmodels.DBBrzMessage;
import com.breeze.dbmodels.DBBrzPreference;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "breezetables";
    private static final int DATABASE_VERSION = 1;

    private static final String NODE_TABLE = "Nodes";

    private static final String CONTACTS_TABLE_NAME = "Contacts";
    private static final String MESSAGES_TABLE_NAME = "DBBrzMessage";
    private static final String PREFERENCES_TABLE_NAME = "Preferences";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String nodeTableSQL = "CREATE TABLE IF NOT EXISTS Nodes ('id' TEXT PRIMARY KEY, 'endpointId' TEXT, 'publicKey' TEXT NOT NULL, 'name' TEXT NOT NULL, 'alias' TEXT NOT NULL)";
        db.execSQL(nodeTableSQL);

        final String INIT_CONTACT_TABLE = "CREATE TABLE IF NOT EXISTS Contacts ('id' INTEGER PRIMARY KEY, 'name' TEXT NOT NULL, 'alias' TEXT NOT NULL, 'signature' TEXT NOT NULL, 'lasttalkedto' DATE NOT NULL, 'blocked' BOOLEAN NOT NULL, 'friend' BOOLEAN NOT NULL)";
        db.execSQL(INIT_CONTACT_TABLE);
        final String INIT_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS Messages ('id' INTEGER PRIMARY KEY, 'from' INTEGER NOT NULL, 'body' TEXT NOT NULL, 'datetime' TEXT NOT NULL, 'encryption' TEXT DEFAULT NULL, FOREIGN KEY ('from') REFERENCES Contacts('id'))";
        db.execSQL(INIT_MESSAGE_TABLE);
        final String INIT_PREFS_TABLE = "CREATE TABLE IF NOT EXISTS Preferences ('id' INTEGER PRIMARY KEY, 'name' TEXT NOT NULL, 'setting' TEXT NOT NULL)";
        db.execSQL(INIT_PREFS_TABLE);
        Log.i("DatabaseInfo", "Table Creations succeeded for Contacts, Messages, Preferences");
    }
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Nodes");

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
    public void addContact(@NonNull DBBrzContact DBBrzContact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", DBBrzContact.getId());
        vals.put("name", DBBrzContact.getName());
        vals.put("alias", DBBrzContact.getAlias());
        vals.put("signature", DBBrzContact.getSignature());
        vals.put("lasttalkedto", DBBrzContact.getLastTalkedTo());
        vals.put("friend", DBBrzContact.isFriend());
        vals.put("blocked", DBBrzContact.isBlocked());
        db.insert(CONTACTS_TABLE_NAME, null, vals);
        db.close();
    }
    public void addMessage(@NonNull DBBrzMessage message)
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
    public void addPreference(@NonNull DBBrzPreference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", preference.getId());
        vals.put("name", preference.getName());
        vals.put("setting", preference.getSetting());
        db.insert(PREFERENCES_TABLE_NAME, null, vals);
        db.close();
    }
    public void deleteOneContact(@NonNull DBBrzContact contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CONTACTS_TABLE_NAME, "id = ?", new String[]{ String.valueOf(contact.getId())});
        db.close();
    }
    public void deleteOneMessage(@NonNull DBBrzMessage message)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MESSAGES_TABLE_NAME, "id = ?", new String[]{ String.valueOf(message.getId())});
        db.close();
    }
    public void deleteOnePreference(@NonNull DBBrzPreference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PREFERENCES_TABLE_NAME, "id = ?", new String[]{ String.valueOf(preference.getId())});
        db.close();
    }

    public int updateContact(DBBrzContact contact)
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
}
