package com.example.accounting.data.local.model;

/**
 * 当前筛选列表汇总的收支与结余（由 UI 层展示）。
 */
public class MonthSummary {

    private final double totalExpense;
    private final double totalIncome;

    public MonthSummary(double totalExpense, double totalIncome) {
        this.totalExpense = totalExpense;
        this.totalIncome = totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getBalance() {
        return totalIncome - totalExpense;
    }
}
