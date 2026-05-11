package com.example.accounting.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.accounting.data.local.entity.Bill;
import com.example.accounting.data.local.model.MonthlySummary;

import java.util.List;

@Dao
public interface BillDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Bill bill);

    @Delete
    void delete(Bill bill);

    @Update
    void update(Bill bill);

    @Query("SELECT * FROM bills ORDER BY dateMillis DESC, id DESC")
    LiveData<List<Bill>> observeAllBills();

    @Query(
            "SELECT * FROM bills "
                    + "WHERE strftime('%Y-%m', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) = :yearMonth "
                    + "ORDER BY dateMillis DESC, id DESC"
    )
    LiveData<List<Bill>> observeBillsForYearMonth(String yearMonth);

    @Query(
            "SELECT "
                    + "CAST(strftime('%Y', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS year, "
                    + "CAST(strftime('%m', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS month, "
                    + "SUM(CASE WHEN type = 0 THEN amount ELSE 0 END) AS totalExpense, "
                    + "SUM(CASE WHEN type = 1 THEN amount ELSE 0 END) AS totalIncome "
                    + "FROM bills "
                    + "GROUP BY strftime('%Y-%m', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) "
                    + "ORDER BY year DESC, month DESC"
    )
    LiveData<List<MonthlySummary>> observeMonthlySummaries();
}
