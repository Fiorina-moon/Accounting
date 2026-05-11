package com.example.accounting.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "bills")
public class Bill {

    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    @PrimaryKey(autoGenerate = true)
    private long id;
    private double amount;
    private int type;
    private String category;
    private long dateMillis;
    private String note;

    public Bill() {
    }

    @Ignore
    public Bill(double amount, int type, String category, long dateMillis, String note) {
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.dateMillis = dateMillis;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
