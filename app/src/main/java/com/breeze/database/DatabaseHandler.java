package com.breeze.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "breezetables";
    private static final int DATABASE_VERSION = 1;

    private String databasePath;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        databasePath = context.getDatabasePath("breezetables.db").getPath();
    }

    /**
     * Check if the database exist and can be read.
     *
     * @return true if it exists and can be read, false if it doesn't
     */
    private boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READONLY);
            checkDB.close();
        } catch (SQLiteException e) {
            // database doesn't exist yet.
        }
        return checkDB != null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String INIT_CONTACT_TABLE = "CREATE TABLE IF NOT EXISTS Contacts ('id' INTEGER PRIMARY KEY, 'name' TEXT NOT NULL, 'alias' TEXT NOT NULL, 'signature' TEXT NOT NULL)";
        db.execSQL(INIT_CONTACT_TABLE);
        final String INIT_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS Messages ('id' INTEGER PRIMARY KEY, 'from' TEXT NOT NULL, 'body' TEXT NOT NULL, 'datetime' DATETIME NOT NULL, 'encryption' TEXT DEFAULT NULL)";
        db.execSQL(INIT_MESSAGE_TABLE);
        final String INIT_PREFS_TABLE = "CREATE TABLE IF NOT EXISTS Preferences ('id' INTEGER PRIMARY KEY, 'preferencename' TEXT NOT NULL, 'preferencesetting' TEXT NOT NULL)";
        db.execSQL(INIT_PREFS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
    
}
