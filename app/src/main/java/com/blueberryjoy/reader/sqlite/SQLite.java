package com.blueberryjoy.reader.sqlite;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.blueberryjoy.reader.App;

public class SQLite {
    private static AppDatabase db;

    public static void initialize() {
        db = Room.databaseBuilder(App.Companion.getAppContext(), AppDatabase.class, "app_db")
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .build();
    }

    public static AppDatabase getDb() {
        if (db == null) initialize();
        return db;
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 1️⃣ Add the state column (nullable for now)
            database.execSQL("ALTER TABLE pages ADD COLUMN state TEXT");

            // 2️⃣ Create a new table with proper NOT NULL state column
            database.execSQL(
                    "CREATE TABLE pages_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "idx INTEGER NOT NULL, " +
                            "isCurrent INTEGER NOT NULL, " +
                            "state TEXT NOT NULL, " +
                            "url TEXT" +
                            ")"
            );

            // 3️⃣ Copy old data into new table, filling state with '' if null
            database.execSQL(
                    "INSERT INTO pages_new (id, idx, isCurrent, state, url) " +
                            "SELECT id, idx, isCurrent, COALESCE(state, ''), url FROM pages"
            );

            // 4️⃣ Drop old table
            database.execSQL("DROP TABLE pages");

            // 5️⃣ Rename new table to original name
            database.execSQL("ALTER TABLE pages_new RENAME TO pages");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS histories (" +
                            "id INTEGER PRIMARY KEY NOT NULL, " +
                            "timestamp INTEGER NOT NULL, " +
                            "url TEXT NOT NULL)"
            );
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "DROP TABLE IF EXISTS histories"
            );

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS histories (" +
                            "id INTEGER PRIMARY KEY NOT NULL, " +
                            "timestamp INTEGER NOT NULL, " +
                            "url TEXT NOT NULL," +
                            "title TEXT NOT NULL)"
            );
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            // Rename the old table
            database.execSQL(
                    "ALTER TABLE pages RENAME TO pages_old"
            );

            // Create the new table
            database.execSQL(
                    "CREATE TABLE pages (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "idx INTEGER NOT NULL, " +
                            "isCurrent INTEGER NOT NULL, " +
                            "state TEXT NOT NULL, " +
                            "url TEXT NOT NULL" +
                            ")"
            );

            // Copy the data, replacing NULL URLs with an empty string
            database.execSQL(
                    "INSERT INTO pages (id, idx, isCurrent, state, url) " +
                            "SELECT id, idx, isCurrent, state, " +
                            "COALESCE(url, '') " +
                            "FROM pages_old"
            );

            // Drop the old table
            database.execSQL(
                    "DROP TABLE pages_old"
            );
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE pages ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL(
                    "ALTER TABLE pages ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0"
            );

            database.execSQL(
                    "UPDATE pages SET `position` = id"
            );
        }
    };
}

