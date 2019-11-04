package com.breeze.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.breeze.models.BrzContact;
import com.breeze.models.BrzMessage;
import com.breeze.models.BrzPreference;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DatabaseHandler extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "breezetables";
    private static final int DATABASE_VERSION = 1;

    private static final String CONTACTS_TABLE_NAME = "Contacts";
    private static final String MESSAGES_TABLE_NAME = "BrzMessage";
    private static final String PREFERENCES_TABLE_NAME = "Preferences";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        final String INIT_CONTACT_TABLE = "CREATE TABLE IF NOT EXISTS Contacts ('id' INTEGER PRIMARY KEY, 'name' TEXT NOT NULL, 'alias' TEXT NOT NULL, 'signature' TEXT NOT NULL, 'lasttalkedto' DATE NOT NULL, 'blocked' BOOLEAN NOT NULL, 'friend' BOOLEAN NOT NULL)";
        db.execSQL(INIT_CONTACT_TABLE);
        final String INIT_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS Messages ('id' INTEGER PRIMARY KEY, 'from' INTEGER NOT NULL, 'body' TEXT NOT NULL, 'datetime' TEXT NOT NULL, 'encryption' TEXT DEFAULT NULL, FOREIGN KEY ('from') REFERENCES Contacts('id'))";
        db.execSQL(INIT_MESSAGE_TABLE);
        final String INIT_PREFS_TABLE = "CREATE TABLE IF NOT EXISTS Preferences ('id' INTEGER PRIMARY KEY, 'name' TEXT NOT NULL, 'setting' TEXT NOT NULL)";
        db.execSQL(INIT_PREFS_TABLE);
        Log.i("DatabaseInfo", "Table Creations succeeded for Contacts, Messages, Preferences");
    }
    @Override
    public void onUpgrade(@NotNull SQLiteDatabase db, int oldVersion, int newVersion) {
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
    public void addContact(@org.jetbrains.annotations.NotNull BrzContact brzContact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", brzContact.getId());
        vals.put("name", brzContact.getName());
        vals.put("alias", brzContact.getAlias());
        vals.put("signature", brzContact.getSignature());
        vals.put("lasttalkedto", brzContact.getLastTalkedTo());
        vals.put("friend", brzContact.isFriend());
        vals.put("blocked", brzContact.isBlocked());
        db.insert(CONTACTS_TABLE_NAME, null, vals);
        db.close();
    }
    public void addMessage(@NotNull BrzMessage message)
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
    public void addPreference(@NotNull BrzPreference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues vals = new ContentValues();
        vals.put("id", preference.getId());
        vals.put("name", preference.getName());
        vals.put("setting", preference.getSetting());
        db.insert(PREFERENCES_TABLE_NAME, null, vals);
        db.close();
    }
    public void deleteOneContact(@NotNull BrzContact contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CONTACTS_TABLE_NAME, "id = ?", new String[]{ String.valueOf(contact.getId())});
        db.close();
    }
    public void deleteOneMessage(@NotNull BrzMessage message)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MESSAGES_TABLE_NAME, "id = ?", new String[]{ String.valueOf(message.getId())});
        db.close();
    }
    public void deleteOnePreference(@NotNull BrzPreference preference)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PREFERENCES_TABLE_NAME, "id = ?", new String[]{ String.valueOf(preference.getId())});
        db.close();
    }

    public int updateContact(BrzContact contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        return -1;
    }
    public int updateMessage(BrzMessage message)
    {
        return -1;
    }
    public int updatePreference(BrzPreference preference)
    {
        return -1;
    }
}
