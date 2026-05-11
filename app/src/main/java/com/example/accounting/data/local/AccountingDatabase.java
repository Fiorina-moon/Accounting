package com.example.accounting.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.accounting.data.local.dao.BillDao;
import com.example.accounting.data.local.entity.Bill;

@Database(entities = {Bill.class}, version = 1, exportSchema = false)
public abstract class AccountingDatabase extends RoomDatabase {

    private static final String DB_NAME = "accounting.db";
    private static volatile AccountingDatabase instance;

    public abstract BillDao billDao();

    @NonNull
    public static AccountingDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AccountingDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AccountingDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
