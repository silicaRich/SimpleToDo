package com.codepath.simpletodo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by floko_000 on 6/30/2016.
 */
public class ToDoDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "ToDoDatabaseHelper";

    // Database Metadata
    private static final String DATABASE_NAME = "toDoDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_ITEMS = "items";
    private static final String TABLE_USERS = "users";

    // Item Table Columns
    private static final String KEY_ITEM_ID = "id";
    private static final String KEY_ITEM_USER_ID_FK = "userId";
    private static final String KEY_ITEM_TEXT = "text";
    private static final String KEY_ITEM_PRIORITY = "priority";

    // User Table Columns
    private static final String KEY_USER_ID = "id";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_COMPLETED_ITEMS = "completedItems";

    private static ToDoDatabaseHelper sInstance;

    public static synchronized ToDoDatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new ToDoDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }


    public ToDoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_ITEMS +
                "(" +
                KEY_ITEM_ID + " INTEGER PRIMARY KEY," + // Define a primary key
                KEY_ITEM_USER_ID_FK + " INTEGER REFERENCES " + TABLE_USERS + "," + // Define a foreign key
                KEY_ITEM_TEXT + " TEXT" +
                ")";

        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS +
                "(" +
                KEY_USER_ID + " INTEGER PRIMARY KEY," +
                KEY_USER_NAME + " TEXT)";// +
        //  KEY_USER_COMPLETED_ITEMS + "(SELECT * FROM " + TABLE_ITEMS  +
        //" WHERE " + KEY_ITEM_USER_ID_FK + " = "+  KEY_USER_ID + "))";

        db.execSQL(CREATE_ITEMS_TABLE);
        db.execSQL(CREATE_USERS_TABLE);

        User admin = new User();
        admin.userName = "Admin";
        addOrUpdateUser(admin);
    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }


    // Insert a item into the database
    public void addItem(Item item) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();

        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();

        try {
            // The user might already exist in the database (i.e. the same user created multiple ITEMS).
            long userId = addOrUpdateUser(item.user);

            ContentValues values = new ContentValues();
            values.put(KEY_ITEM_USER_ID_FK, userId);
            values.put(KEY_ITEM_TEXT, item.text);

            // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
            db.insertOrThrow(TABLE_ITEMS, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }
    }


    // Insert or update a user in the database
    // Since SQLite doesn't support "upsert" we need to fall back on an attempt to UPDATE (in case the
    // user already exists) optionally followed by an INSERT (in case the user does not already exist).
    // Unfortunately, there is a bug with the insertOnConflict method
    // (https://code.google.com/p/android/issues/detail?id=13045) so we need to fall back to the more
    // verbose option of querying for the user's primary key if we did an update.
    public long addOrUpdateUser(User user) {
        // The database connection is cached so it's not expensive to call getWriteableDatabase() multiple times.
        SQLiteDatabase db = getWritableDatabase();
        long userId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_USER_NAME, user.userName);

            // First try to update the user in case the user already exists in the database
            // This assumes userNames are unique
            int rows = db.update(TABLE_USERS, values, KEY_USER_NAME + "= ?", new String[]{user.userName});

            // Check if update succeeded
            if (rows == 1) {
                // Get the primary key of the user we just updated
                String usersSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        KEY_USER_ID, TABLE_USERS, KEY_USER_NAME);
                Cursor cursor = db.rawQuery(usersSelectQuery, new String[]{String.valueOf(user.userName)});
                try {
                    if (cursor.moveToFirst()) {
                        userId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else {
                // user with this userName did not already exist, so insert new user
                userId = db.insertOrThrow(TABLE_USERS, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add or update user");
        } finally {
            db.endTransaction();
        }
        return userId;
    }


    // Get all ITEMS in the database
    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();

        // SELECT * FROM ITEMS
        // LEFT OUTER JOIN USERS
        // ON ITEMS.KEY_POST_USER_ID_FK = USERS.KEY_USER_ID
        String ITEMS_SELECT_QUERY =
                String.format("SELECT * FROM %s LEFT OUTER JOIN %s ON %s.%s = %s.%s",
                        TABLE_ITEMS,
                        TABLE_USERS,
                        TABLE_ITEMS, KEY_ITEM_USER_ID_FK,
                        TABLE_USERS, KEY_USER_ID);

        // "getReadableDatabase()" and "getWriteableDatabase()" return the same object (except under low
        // disk space scenarios)
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(ITEMS_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    User newUser = new User();
                    newUser.userName = cursor.getString(cursor.getColumnIndex(KEY_USER_NAME));

                    Item newItem = new Item();
                    newItem.text = cursor.getString(cursor.getColumnIndex(KEY_ITEM_TEXT));
                    newItem.user = newUser;
                    items.add(newItem);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get ITEMS from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return items;
    }


    // Update the user's profile picture url
    public int updateUserProfilePicture(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        // Updating profile picture url for user with that userName
        return db.update(TABLE_USERS, values, KEY_USER_NAME + " = ?",
                new String[]{String.valueOf(user.userName)});
    }


    // Delete all ITEMS and users in the database
    public void deleteAllItemssAndUsers() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Order of deletions is important when foreign key relationships exist.
            db.delete(TABLE_ITEMS, null, null);
            db.delete(TABLE_USERS, null, null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to delete all ITEMS and users");
        } finally {
            db.endTransaction();
        }
    }


    // Delete ITEM in the database
    public void deleteItem(Item itemText) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        if (itemText != null) {
            try {
                db.delete(TABLE_ITEMS, KEY_ITEM_TEXT + "=?", new String[]{itemText.text});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.d(TAG, "Error while trying to delete ITEM");
            } finally {
                db.endTransaction();
            }
        } else {
            Log.d(TAG, "Can't delete, item is undefined.");
            db.endTransaction();
        }
    }


    // Update ITEM in the database
    public void updateItem(Intent data) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        if (data != null) {
            try {
                String newText = data.getExtras().getString("text");
                String oldText = data.getExtras().getString("oldText");
                ContentValues newValues = new ContentValues();
                newValues.put(KEY_ITEM_TEXT, newText);
                String[] args = new String[]{oldText};
                db.update(TABLE_ITEMS, newValues, KEY_ITEM_TEXT + "=?", args);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.d(TAG, "Error while trying to update ITEM");
            } finally {
                db.endTransaction();
            }
        } else {
            Log.d(TAG, "Can't update, item is undefined.");
            db.endTransaction();
        }
    }

}




