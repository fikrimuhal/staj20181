package com.hivecdn.androidsqlitedatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ravi on 15/03/18.
 */

public class DatabaseHelper{

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "notes_db";

    private SQLiteDatabase db;


    public DatabaseHelper(Context context) {
        String DATABASE_FILE_PATH = context.getExternalFilesDir(null).getPath() + '/' + DATABASE_NAME;
        db = SQLiteDatabase.openDatabase(DATABASE_FILE_PATH, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                + Note.TABLE_NAME + "'", null);
        if (cursor.getCount() == 0)
            create();
    }

    // Creating Tables
    public void create() {

        // create notes table
        db.execSQL(Note.CREATE_TABLE);
    }

    public long insertNote(byte[] note) {

        ContentValues values = new ContentValues();
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(Note.COLUMN_NOTE, note);

        // insert row
        long id = db.insert(Note.TABLE_NAME, null, values);

        // close db connection
        //db.close();

        // return newly inserted row id
        return id;
    }

    public Note getNote(long id) {
        Cursor cursor = db.query(Note.TABLE_NAME,
                new String[]{Note.COLUMN_ID, Note.COLUMN_NOTE, Note.COLUMN_TIMESTAMP},
                Note.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        // prepare note object
        Note note = new Note(
                cursor.getInt(cursor.getColumnIndex(Note.COLUMN_ID)),
                cursor.getBlob(cursor.getColumnIndex(Note.COLUMN_NOTE)),
                cursor.getString(cursor.getColumnIndex(Note.COLUMN_TIMESTAMP)));

        // close the db connection
        cursor.close();

        return note;
    }
}