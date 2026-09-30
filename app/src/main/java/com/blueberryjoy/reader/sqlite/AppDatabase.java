package com.blueberryjoy.reader.sqlite;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.blueberryjoy.reader.sqlite.history.History;
import com.blueberryjoy.reader.sqlite.history.HistoryDao;
import com.blueberryjoy.reader.sqlite.page.Page;
import com.blueberryjoy.reader.sqlite.page.PageDao;

@Database(entities = {Page.class, History.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PageDao pageDao();

    public abstract HistoryDao historyDao();
}