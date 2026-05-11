package com.example.accounting;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.accounting.data.local.entity.Bill;
import com.example.accounting.data.local.model.MonthSummary;
import com.example.accounting.ui.BillAdapter;
import com.example.accounting.ui.BillSwipeItemTouchHelperCallback;
import com.example.accounting.viewmodel.BillViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private BillViewModel billViewModel;
    private BillAdapter billAdapter;
    private View emptyState;
    private RecyclerView recyclerBills;
    private View mainContent;
    private BillSwipeItemTouchHelperCallback swipeCallback;

    private MaterialButton btnMonthLabel;

    private MaterialAutoCompleteTextView typeDropdown;
    private final List<String> typeDisplayItems = new ArrayList<>();
    private final List<Integer> typeFilterValues = new ArrayList<>();
    private ArrayAdapter<String> typeAdapter;

    private MaterialAutoCompleteTextView categoryDropdown;
    private final List<String> categoryDisplayItems = new ArrayList<>();
    private final List<String> categoryFilterValues = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mainContent = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerBills = findViewById(R.id.recycler_bills);
        emptyState = findViewById(R.id.empty_state);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        MaterialButton btnMonthPrev = findViewById(R.id.btn_month_prev);
        btnMonthLabel = findViewById(R.id.btn_month_label);
        MaterialButton btnMonthNext = findViewById(R.id.btn_month_next);
        typeDropdown = findViewById(R.id.type_dropdown);
        categoryDropdown = findViewById(R.id.category_dropdown);

        billAdapter = new BillAdapter();
        recyclerBills.setLayoutManager(new LinearLayoutManager(this));
        recyclerBills.setAdapter(billAdapter);
        RecyclerView.ItemAnimator animator = new DefaultItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        recyclerBills.setItemAnimator(animator);

        int deleteWidthPx = (int) (80f * getResources().getDisplayMetrics().density + 0.5f);
        swipeCallback = new BillSwipeItemTouchHelperCallback(deleteWidthPx);
        new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerBills);

        recyclerBills.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy != 0) {
                    swipeCallback.closeAllOpenRows(recyclerView);
                }
            }
        });

        billViewModel = new ViewModelProvider(this).get(BillViewModel.class);

        btnMonthPrev.setOnClickListener(v -> {
            swipeCallback.closeAllOpenRows(recyclerBills);
            billViewModel.selectPreviousMonth();
        });
        btnMonthNext.setOnClickListener(v -> {
            swipeCallback.closeAllOpenRows(recyclerBills);
            billViewModel.selectNextMonth();
        });
        btnMonthLabel.setOnClickListener(v -> {
            swipeCallback.closeAllOpenRows(recyclerBills);
            showMonthPicker();
        });

        buildTypeDropdownData();
        typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, typeDisplayItems);
        typeDropdown.setAdapter(typeAdapter);
        typeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= typeFilterValues.size()) {
                return;
            }
            swipeCallback.closeAllOpenRows(recyclerBills);
            billViewModel.setFilterBillType(typeFilterValues.get(position));
        });

        buildCategoryDropdownData();
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categoryDisplayItems);
        categoryDropdown.setAdapter(categoryAdapter);
        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= categoryFilterValues.size()) {
                return;
            }
            swipeCallback.closeAllOpenRows(recyclerBills);
            billViewModel.setFilterCategory(categoryFilterValues.get(position));
        });

        billViewModel.getDisplayedBills().observe(this, this::onBillsChanged);
        billViewModel.getMonthOverview().observe(this, this::onMonthOverviewChanged);
        billViewModel.getSelectedMonthLabel().observe(this, this::onMonthLabelChanged);
        billViewModel.getFilterCategory().observe(this, this::onFilterCategoryChanged);
        billViewModel.getFilterBillType().observe(this, this::onFilterBillTypeChanged);

        billAdapter.setBillActionsListener(new BillAdapter.BillActionsListener() {
            @Override
            public void onBillClick(@NonNull Bill bill) {
                AddBillSheetFragment.newInstanceForEdit(bill)
                        .show(getSupportFragmentManager(), "edit_bill");
            }

            @Override
            public void onBillDeleteClick(@NonNull Bill bill) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.delete_bill_title)
                        .setMessage(R.string.delete_bill_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete_bill_confirm, (dialog, which) -> {
                            swipeCallback.closeAllOpenRows(recyclerBills);
                            billViewModel.deleteBill(bill, () ->
                                    Snackbar.make(mainContent, R.string.snack_bill_deleted, Snackbar.LENGTH_SHORT)
                                            .show()
                            );
                        })
                        .show();
            }
        });

        fabAdd.setOnClickListener(v ->
                AddBillSheetFragment.newInstanceForAdd()
                        .show(getSupportFragmentManager(), "add_bill")
        );
    }

    private void showMonthPicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.month_picker_title)
                .setSelection(billViewModel.getSelectedMonthPickerUtcMillis())
                .build();
        picker.addOnPositiveButtonClickListener(selection ->
                billViewModel.setSelectedYearMonthFromPicker(selection)
        );
        picker.show(getSupportFragmentManager(), "month_picker");
    }

    private void buildTypeDropdownData() {
        typeDisplayItems.clear();
        typeFilterValues.clear();
        typeDisplayItems.add(getString(R.string.filter_all));
        typeFilterValues.add(BillViewModel.FILTER_TYPE_ALL);
        typeDisplayItems.add(getString(R.string.filter_expense_only));
        typeFilterValues.add(Bill.TYPE_EXPENSE);
        typeDisplayItems.add(getString(R.string.filter_income_only));
        typeFilterValues.add(Bill.TYPE_INCOME);
    }

    private void buildCategoryDropdownData() {
        categoryDisplayItems.clear();
        categoryFilterValues.clear();
        categoryDisplayItems.add(getString(R.string.filter_all));
        categoryFilterValues.add("");
        categoryDisplayItems.add(getString(R.string.category_dining));
        categoryFilterValues.add(getString(R.string.category_dining));
        categoryDisplayItems.add(getString(R.string.category_transport));
        categoryFilterValues.add(getString(R.string.category_transport));
        categoryDisplayItems.add(getString(R.string.category_shopping));
        categoryFilterValues.add(getString(R.string.category_shopping));
        categoryDisplayItems.add(getString(R.string.category_other));
        categoryFilterValues.add(getString(R.string.category_other));
    }

    private void onMonthLabelChanged(String label) {
        if (label == null) {
            return;
        }
        btnMonthLabel.setText(label);
    }

    private void onFilterBillTypeChanged(Integer typeOrAll) {
        int type = typeOrAll != null ? typeOrAll : BillViewModel.FILTER_TYPE_ALL;
        int index = typeFilterValues.indexOf(type);
        if (index < 0) {
            index = 0;
        }
        typeDropdown.setText(typeDisplayItems.get(index), false);
    }

    private void onFilterCategoryChanged(String categoryOrEmpty) {
        String key = categoryOrEmpty != null ? categoryOrEmpty : "";
        int index = categoryFilterValues.indexOf(key);
        if (index < 0) {
            index = 0;
        }
        categoryDropdown.setText(categoryDisplayItems.get(index), false);
    }

    private void onBillsChanged(List<Bill> bills) {
        swipeCallback.closeAllOpenRows(recyclerBills);
        List<Bill> list = bills != null ? bills : Collections.emptyList();
        boolean empty = list.isEmpty();
        billAdapter.submitList(list, () -> {
            if (empty) {
                recyclerBills.setVisibility(View.INVISIBLE);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                recyclerBills.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
            }
        });
    }

    private void onMonthOverviewChanged(MonthSummary summary) {
        if (summary == null) {
            return;
        }
        Locale locale = Locale.getDefault();
        ((android.widget.TextView) findViewById(R.id.text_summary_expense_value)).setText(
                String.format(locale, "%.2f", summary.getTotalExpense())
        );
        ((android.widget.TextView) findViewById(R.id.text_summary_income_value)).setText(
                String.format(locale, "%.2f", summary.getTotalIncome())
        );
        ((android.widget.TextView) findViewById(R.id.text_summary_balance_value)).setText(
                String.format(locale, "%.2f", summary.getBalance())
        );
    }
}
