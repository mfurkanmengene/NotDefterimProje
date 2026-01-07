package com.example.mobilsistemlerproje;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "notes.db";
    public static final int DB_VERSION = 2; // ← ARTIRILDI

    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "_id";
    public static final String COL_TEXT = "text";
    public static final String COL_DATE = "date"; // yyyy-MM-dd şeklinde TEXT
    public static final String COL_CATEGORY = "category"; // Yeni sütun

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create =
                "CREATE TABLE " + TABLE_NOTES + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_TEXT + " TEXT NOT NULL, " +
                        COL_DATE + " TEXT NOT NULL, " +
                        COL_CATEGORY + " TEXT DEFAULT 'Genel'" +
                        ");";
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Basit migration: Eksikse category sütununu eklemeyi dene
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES +
                        " ADD COLUMN " + COL_CATEGORY + " TEXT DEFAULT 'Genel'");
            } catch (Exception ignored) {}
        }
        // Daha ileri migrationlar için yeni if blokları eklenebilir.
    }

    // EKLE
    public long insertNote(String text, String date, String category) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TEXT, text);
        cv.put(COL_DATE, date);
        cv.put(COL_CATEGORY, category);
        return db.insert(TABLE_NOTES, null, cv);
    }

    // GÜNCELLE
    public int updateNote(long id, String text, String date, String category) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TEXT, text);
        cv.put(COL_DATE, date);
        cv.put(COL_CATEGORY, category);
        return db.update(TABLE_NOTES, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // SİL
    public int deleteNote(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NOTES, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // LİSTELE
    public Cursor getAllNotes() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(
                TABLE_NOTES,
                null,
                null,
                null,
                null,
                null,
                COL_DATE + " DESC, " + COL_ID + " DESC"
        );
    }

    // ARAMA (text veya category içinde)
    public Cursor searchNotes(String query) {
        SQLiteDatabase db = getReadableDatabase();
        String like = "%" + query + "%";
        return db.query(
                TABLE_NOTES,
                null,
                COL_TEXT + " LIKE ? OR " + COL_CATEGORY + " LIKE ?",
                new String[]{like, like},
                null, null,
                COL_DATE + " DESC, " + COL_ID + " DESC"
        );
    }
}
