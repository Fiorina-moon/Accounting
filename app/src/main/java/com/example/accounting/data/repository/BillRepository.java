package com.example.accounting.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.accounting.data.local.AccountingDatabase;
import com.example.accounting.data.local.dao.BillDao;
import com.example.accounting.data.local.entity.Bill;
import com.example.accounting.data.local.model.MonthlySummary;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BillRepository {

    private final BillDao billDao;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BillRepository(@NonNull Application application) {
        billDao = AccountingDatabase.getInstance(application).billDao();
    }

    public LiveData<List<Bill>> observeAllBills() {
        return billDao.observeAllBills();
    }

    public LiveData<List<Bill>> observeBillsForYearMonth(@NonNull String yearMonth) {
        return billDao.observeBillsForYearMonth(yearMonth);
    }

    public LiveData<List<MonthlySummary>> observeMonthlySummaries() {
        return billDao.observeMonthlySummaries();
    }

    public void insert(@NonNull Bill bill, @Nullable Runnable onComplete) {
        ioExecutor.execute(() -> {
            billDao.insert(bill);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void insert(@NonNull Bill bill) {
        insert(bill, null);
    }

    public void update(@NonNull Bill bill) {
        update(bill, null);
    }

    public void update(@NonNull Bill bill, @Nullable Runnable onComplete) {
        ioExecutor.execute(() -> {
            billDao.update(bill);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void delete(@NonNull Bill bill) {
        delete(bill, null);
    }

    public void delete(@NonNull Bill bill, @Nullable Runnable onComplete) {
        ioExecutor.execute(() -> {
            billDao.delete(bill);
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    public void delete(@NonNull List<Bill> bills, @Nullable Runnable onComplete) {
        ioExecutor.execute(() -> {
            for (Bill bill : bills) {
                billDao.delete(bill);
            }
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }
}
