package com.example.accounting.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.accounting.data.local.entity.Bill;
import com.example.accounting.data.local.model.MonthSummary;
import com.example.accounting.data.local.model.MonthlySummary;
import com.example.accounting.data.repository.BillRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BillViewModel extends AndroidViewModel {

    public static final int FILTER_TYPE_ALL = -1;

    private final BillRepository repository;
    private final MutableLiveData<String> selectedYearMonth = new MutableLiveData<>();
    private final LiveData<List<Bill>> billsForSelectedMonth;
    private final MutableLiveData<Integer> filterBillType = new MutableLiveData<>(FILTER_TYPE_ALL);
    private final MutableLiveData<String> filterCategory = new MutableLiveData<>("");

    private final MediatorLiveData<List<Bill>> displayedBills = new MediatorLiveData<>();
    private final LiveData<MonthSummary> monthOverview;
    private final LiveData<String> selectedMonthLabel;

    public BillViewModel(@NonNull Application application) {
        super(application);
        repository = new BillRepository(application);
        billsForSelectedMonth =
                Transformations.switchMap(selectedYearMonth, repository::observeBillsForYearMonth);
        // 顶部统计为当月全量，不受收支/分类列表筛选影响
        monthOverview = Transformations.map(billsForSelectedMonth, BillViewModel::summarize);
        selectedMonthLabel = Transformations.map(selectedYearMonth, BillViewModel::formatMonthLabel);

        displayedBills.addSource(billsForSelectedMonth, this::recomputeDisplayedBills);
        displayedBills.addSource(filterBillType, v -> recomputeDisplayedBills(billsForSelectedMonth.getValue()));
        displayedBills.addSource(filterCategory, v -> recomputeDisplayedBills(billsForSelectedMonth.getValue()));

        selectedYearMonth.setValue(currentYearMonthString());
    }

    private void recomputeDisplayedBills(@Nullable List<Bill> source) {
        if (source == null) {
            source = Collections.emptyList();
        }
        Integer typeFilter = filterBillType.getValue();
        if (typeFilter == null) {
            typeFilter = FILTER_TYPE_ALL;
        }
        String catFilter = filterCategory.getValue();
        if (catFilter == null) {
            catFilter = "";
        }

        List<Bill> out = new ArrayList<>();
        for (Bill b : source) {
            if (typeFilter != FILTER_TYPE_ALL && b.getType() != typeFilter) {
                continue;
            }
            if (!catFilter.isEmpty() && !catFilter.equals(b.getCategory())) {
                continue;
            }
            out.add(b);
        }
        displayedBills.setValue(out);
    }

    public LiveData<List<Bill>> getDisplayedBills() {
        return displayedBills;
    }

    public LiveData<List<Bill>> getBillsForSelectedMonth() {
        return billsForSelectedMonth;
    }

    public LiveData<MonthSummary> getMonthOverview() {
        return monthOverview;
    }

    public LiveData<String> getSelectedMonthLabel() {
        return selectedMonthLabel;
    }

    public LiveData<String> getFilterCategory() {
        return filterCategory;
    }

    public LiveData<Integer> getFilterBillType() {
        return filterBillType;
    }

    public LiveData<String> getSelectedYearMonth() {
        return selectedYearMonth;
    }

    public void setSelectedYearMonth(@NonNull String yearMonth) {
        selectedYearMonth.setValue(yearMonth);
    }

    public void setFilterBillType(int typeOrAll) {
        filterBillType.setValue(typeOrAll);
    }

    public void setFilterCategory(@Nullable String categoryOrEmptyForAll) {
        filterCategory.setValue(categoryOrEmptyForAll != null ? categoryOrEmptyForAll : "");
    }

    public void setSelectedYearMonthFromPicker(long utcSelectionMillis) {
        Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcSelectionMillis);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        selectedYearMonth.setValue(formatYm(local));
    }

    public long getSelectedMonthPickerUtcMillis() {
        String ym = selectedYearMonth.getValue();
        if (ym == null) {
            ym = currentYearMonthString();
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM", Locale.US);
            Calendar local = Calendar.getInstance();
            local.setTime(in.parse(ym));
            local.set(Calendar.DAY_OF_MONTH, 1);
            local.set(Calendar.HOUR_OF_DAY, 0);
            local.set(Calendar.MINUTE, 0);
            local.set(Calendar.SECOND, 0);
            local.set(Calendar.MILLISECOND, 0);
            Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utc.clear();
            utc.set(
                    local.get(Calendar.YEAR),
                    local.get(Calendar.MONTH),
                    local.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0
            );
            return utc.getTimeInMillis();
        } catch (ParseException e) {
            Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            return utc.getTimeInMillis();
        }
    }

    public void selectPreviousMonth() {
        shiftSelectedMonth(-1);
    }

    public void selectNextMonth() {
        shiftSelectedMonth(1);
    }

    private void shiftSelectedMonth(int deltaMonths) {
        String ym = selectedYearMonth.getValue();
        if (ym == null) {
            ym = currentYearMonthString();
        }
        try {
            selectedYearMonth.setValue(addMonthsToYearMonth(ym, deltaMonths));
        } catch (ParseException ignored) {
            selectedYearMonth.setValue(currentYearMonthString());
        }
    }

    public LiveData<List<Bill>> getAllBills() {
        return repository.observeAllBills();
    }

    public LiveData<List<MonthlySummary>> getMonthlySummaries() {
        return repository.observeMonthlySummaries();
    }

    public void insert(@NonNull Bill bill) {
        insertBill(bill);
    }

    public void insertBill(@NonNull Bill bill) {
        repository.insert(bill);
    }

    public void insertBill(@NonNull Bill bill, @Nullable Runnable onInserted) {
        repository.insert(bill, onInserted);
    }

    public void updateBill(@NonNull Bill bill) {
        repository.update(bill);
    }

    public void updateBill(@NonNull Bill bill, @Nullable Runnable onComplete) {
        repository.update(bill, onComplete);
    }

    public void deleteBill(@NonNull Bill bill) {
        repository.delete(bill);
    }

    public void deleteBill(@NonNull Bill bill, @Nullable Runnable onComplete) {
        repository.delete(bill, onComplete);
    }

    private static MonthSummary summarize(@Nullable List<Bill> bills) {
        if (bills == null || bills.isEmpty()) {
            return new MonthSummary(0.0, 0.0);
        }
        double expense = 0.0;
        double income = 0.0;
        for (Bill b : bills) {
            if (b.getType() == Bill.TYPE_EXPENSE) {
                expense += b.getAmount();
            } else {
                income += b.getAmount();
            }
        }
        return new MonthSummary(expense, income);
    }

    private static String currentYearMonthString() {
        return formatYm(Calendar.getInstance());
    }

    private static String formatYm(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);
        return sdf.format(calendar.getTime());
    }

    private static String addMonthsToYearMonth(String yearMonth, int deltaMonths) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdf.parse(yearMonth));
        cal.add(Calendar.MONTH, deltaMonths);
        return sdf.format(cal.getTime());
    }

    private static String formatMonthLabel(String yearMonth) {
        if (yearMonth == null) {
            return "";
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.setTime(in.parse(yearMonth));
            SimpleDateFormat out = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
            return out.format(cal.getTime());
        } catch (ParseException e) {
            return yearMonth;
        }
    }
}
