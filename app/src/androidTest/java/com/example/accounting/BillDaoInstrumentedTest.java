package com.example.accounting;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.accounting.data.local.AccountingDatabase;
import com.example.accounting.data.local.dao.BillDao;
import com.example.accounting.data.local.entity.Bill;
import com.example.accounting.data.local.model.MonthlySummary;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BillDaoInstrumentedTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AccountingDatabase database;
    private BillDao billDao;

    @Before
    public void createDb() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        AccountingDatabase.class
                )
                .allowMainThreadQueries()
                .build();
        billDao = database.billDao();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void insertThenObserveAll_containsBill() throws Exception {
        Bill bill = new Bill(12.5, Bill.TYPE_EXPENSE, "餐饮", 1_700_000_000_000L, "午餐");
        long id = billDao.insert(bill);
        assertTrue(id > 0);

        List<Bill> bills = getLiveDataValue(billDao.observeAllBills());
        assertNotNull(bills);
        assertEquals(1, bills.size());
        assertEquals(id, bills.get(0).getId());
        assertEquals(12.5, bills.get(0).getAmount(), 0.001);
        assertEquals(Bill.TYPE_EXPENSE, bills.get(0).getType());
        assertEquals("餐饮", bills.get(0).getCategory());
        assertEquals("午餐", bills.get(0).getNote());
    }

    @Test
    public void updateBill_reflectedInQuery() throws Exception {
        Bill bill = new Bill(10.0, Bill.TYPE_EXPENSE, "购物", 1_700_000_000_100L, "a");
        long id = billDao.insert(bill);
        bill.setId(id);
        bill.setAmount(99.0);
        bill.setNote("b");
        billDao.update(bill);

        List<Bill> bills = getLiveDataValue(billDao.observeAllBills());
        assertEquals(1, bills.size());
        assertEquals(99.0, bills.get(0).getAmount(), 0.001);
        assertEquals("b", bills.get(0).getNote());
    }

    @Test
    public void deleteBill_removedFromQuery() throws Exception {
        Bill bill = new Bill(5.0, Bill.TYPE_INCOME, "工资", 1_700_000_000_200L, "");
        long id = billDao.insert(bill);
        bill.setId(id);

        billDao.delete(bill);

        List<Bill> bills = getLiveDataValue(billDao.observeAllBills());
        assertNotNull(bills);
        assertTrue(bills.isEmpty());
    }

    @Test
    public void observeAllBills_ordersByDateMillisDescThenIdDesc() throws Exception {
        long older = 1_700_000_000_000L;
        long newer = 1_800_000_000_000L;
        Bill firstInserted = new Bill(1.0, Bill.TYPE_EXPENSE, "a", older, "");
        Bill secondInserted = new Bill(2.0, Bill.TYPE_EXPENSE, "b", newer, "");
        long idOld = billDao.insert(firstInserted);
        long idNew = billDao.insert(secondInserted);

        List<Bill> bills = getLiveDataValue(billDao.observeAllBills());
        assertEquals(2, bills.size());
        assertEquals(newer, bills.get(0).getDateMillis());
        assertEquals(idNew, bills.get(0).getId());
        assertEquals(older, bills.get(1).getDateMillis());
        assertEquals(idOld, bills.get(1).getId());
    }

    @Test
    public void observeMonthlySummaries_groupsByLocalCalendarMonth() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2024);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long marchMillis = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, Calendar.APRIL);
        cal.set(Calendar.DAY_OF_MONTH, 10);
        long aprilMillis = cal.getTimeInMillis();

        billDao.insert(new Bill(100.0, Bill.TYPE_EXPENSE, "餐饮", marchMillis, ""));
        billDao.insert(new Bill(200.0, Bill.TYPE_INCOME, "奖金", marchMillis, ""));
        billDao.insert(new Bill(50.0, Bill.TYPE_EXPENSE, "交通", aprilMillis, ""));
        billDao.insert(new Bill(30.0, Bill.TYPE_EXPENSE, "餐饮", marchMillis, ""));

        List<MonthlySummary> summaries = getLiveDataValue(billDao.observeMonthlySummaries());
        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        MonthlySummary april = summaries.get(0);
        assertEquals(2024, april.getYear());
        assertEquals(4, april.getMonth());
        assertEquals(50.0, april.getTotalExpense(), 0.001);
        assertEquals(0.0, april.getTotalIncome(), 0.001);

        MonthlySummary march = summaries.get(1);
        assertEquals(2024, march.getYear());
        assertEquals(3, march.getMonth());
        assertEquals(130.0, march.getTotalExpense(), 0.001);
        assertEquals(200.0, march.getTotalIncome(), 0.001);
    }

    @Test
    public void observeMonthlySummaries_emptyTable() throws Exception {
        List<MonthlySummary> summaries = getLiveDataValue(billDao.observeMonthlySummaries());
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    public void observeBillsForYearMonth_returnsOnlyMatchingMonth() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2024);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long marchMillis = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, Calendar.APRIL);
        cal.set(Calendar.DAY_OF_MONTH, 10);
        long aprilMillis = cal.getTimeInMillis();

        billDao.insert(new Bill(10.0, Bill.TYPE_EXPENSE, "餐饮", marchMillis, "m1"));
        billDao.insert(new Bill(20.0, Bill.TYPE_INCOME, "工资", marchMillis, "m2"));
        billDao.insert(new Bill(30.0, Bill.TYPE_EXPENSE, "交通", aprilMillis, "a1"));

        SimpleDateFormat ym = new SimpleDateFormat("yyyy-MM", Locale.US);
        cal.setTimeInMillis(marchMillis);
        String marchYm = ym.format(cal.getTime());

        List<Bill> marchBills = getLiveDataValue(billDao.observeBillsForYearMonth(marchYm));
        assertNotNull(marchBills);
        assertEquals(2, marchBills.size());
    }

    private static <T> T getLiveDataValue(LiveData<T> liveData) throws InterruptedException {
        final Object[] holder = new Object[1];
        CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = value -> {
            holder[0] = value;
            latch.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.observeForever(observer));
        assertTrue(
                "LiveData did not emit within timeout",
                latch.await(5, TimeUnit.SECONDS)
        );
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.removeObserver(observer));
        @SuppressWarnings("unchecked")
        T result = (T) holder[0];
        return result;
    }
}
