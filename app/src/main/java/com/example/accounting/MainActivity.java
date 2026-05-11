package com.example.accounting;

import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
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
import com.example.accounting.viewmodel.BillViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BillViewModel billViewModel;
    private BillAdapter billAdapter;
    private View emptyState;
    private RecyclerView recyclerBills;
    private View mainContent;
    private View selectionActionsLayout;
    private TextView selectionCountText;
    private View btnSelectionCancel;
    private View btnSelectionDelete;
    private FloatingActionButton fabAdd;

    private MaterialButton btnMonthLabel;

    private MaterialAutoCompleteTextView typeDropdown;
    private final List<String> typeDisplayItems = new ArrayList<>();
    private final List<Integer> typeFilterValues = new ArrayList<>();
    private ArrayAdapter<String> typeAdapter;

    private MaterialAutoCompleteTextView categoryDropdown;
    private final List<String> categoryDisplayItems = new ArrayList<>();
    private boolean[] categoryCheckedStates = new boolean[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        mainContent = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int mask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets bars = insets.getInsets(mask);
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            // 避免子 View（如 AppBar）再各自 fitsSystemWindows 叠一层；同时保证底栏虚拟按键区域被留出
            return WindowInsetsCompat.CONSUMED;
        });

        recyclerBills = findViewById(R.id.recycler_bills);
        emptyState = findViewById(R.id.empty_state);
        fabAdd = findViewById(R.id.fab_add);
        MaterialButton btnMonthPrev = findViewById(R.id.btn_month_prev);
        btnMonthLabel = findViewById(R.id.btn_month_label);
        MaterialButton btnMonthNext = findViewById(R.id.btn_month_next);
        typeDropdown = findViewById(R.id.type_dropdown);
        categoryDropdown = findViewById(R.id.category_dropdown);
        TextInputLayout layoutCategoryDropdown = findViewById(R.id.layout_category_dropdown);
        selectionActionsLayout = findViewById(R.id.layout_selection_actions);
        selectionCountText = findViewById(R.id.text_selection_count);
        btnSelectionCancel = findViewById(R.id.btn_selection_cancel);
        btnSelectionDelete = findViewById(R.id.btn_selection_delete);

        billAdapter = new BillAdapter();
        billAdapter.setSelectionStateListener(this::onSelectionStateChanged);
        recyclerBills.setLayoutManager(new LinearLayoutManager(this));
        recyclerBills.setAdapter(billAdapter);
        RecyclerView.ItemAnimator animator = new DefaultItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        recyclerBills.setItemAnimator(animator);

        recyclerBills.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy != 0) {
                    BillAdapter.closeAllSwipeRows(recyclerView);
                }
            }
        });

        billViewModel = new ViewModelProvider(this).get(BillViewModel.class);

        btnMonthPrev.setOnClickListener(v -> {
            BillAdapter.closeAllSwipeRows(recyclerBills);
            billViewModel.selectPreviousMonth();
        });
        btnMonthNext.setOnClickListener(v -> {
            BillAdapter.closeAllSwipeRows(recyclerBills);
            billViewModel.selectNextMonth();
        });
        btnMonthLabel.setOnClickListener(v -> {
            BillAdapter.closeAllSwipeRows(recyclerBills);
            showMonthPicker();
        });

        buildTypeDropdownData();
        typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, typeDisplayItems);
        typeDropdown.setAdapter(typeAdapter);
        typeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= typeFilterValues.size()) {
                return;
            }
            BillAdapter.closeAllSwipeRows(recyclerBills);
            billViewModel.setFilterBillType(typeFilterValues.get(position));
        });

        buildCategoryDropdownData();
        categoryDropdown.setInputType(InputType.TYPE_NULL);
        categoryDropdown.setKeyListener(null);
        categoryDropdown.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                BillAdapter.closeAllSwipeRows(recyclerBills);
                showCategoryMultiSelectDialog();
                return true;
            }
            return false;
        });
        categoryDropdown.setOnClickListener(v -> {
            BillAdapter.closeAllSwipeRows(recyclerBills);
            showCategoryMultiSelectDialog();
        });
        if (layoutCategoryDropdown != null) {
            layoutCategoryDropdown.setEndIconOnClickListener(v -> {
                BillAdapter.closeAllSwipeRows(recyclerBills);
                showCategoryMultiSelectDialog();
            });
        }

        billViewModel.getDisplayedBills().observe(this, this::onBillsChanged);
        billViewModel.getMonthOverview().observe(this, this::onMonthOverviewChanged);
        billViewModel.getSelectedMonthLabel().observe(this, this::onMonthLabelChanged);
        billViewModel.getFilterCategories().observe(this, this::onFilterCategoriesChanged);
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
                            BillAdapter.closeAllSwipeRows(recyclerBills);
                            billViewModel.deleteBill(bill, () ->
                                    showFabSnackbar(R.string.snack_bill_deleted)
                            );
                        })
                        .show();
            }
        });

        btnSelectionCancel.setOnClickListener(v -> {
            BillAdapter.closeAllSwipeRows(recyclerBills);
            billAdapter.clearSelectionMode();
        });
        btnSelectionDelete.setOnClickListener(v -> onBatchDeleteRequested());

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
        categoryDisplayItems.add(getString(R.string.category_dining));
        categoryDisplayItems.add(getString(R.string.category_transport));
        categoryDisplayItems.add(getString(R.string.category_shopping));
        categoryDisplayItems.add(getString(R.string.category_other));
        categoryCheckedStates = new boolean[categoryDisplayItems.size()];
    }

    private static final float FAB_SNACKBAR_GAP_DP = 4f;
    /** 略大于典型单行 Snackbar，用于 show 前预抬，避免条带滑入过程中与 FAB 区域重合 */
    private static final float SNACKBAR_EST_HEIGHT_DP = 56f;

    /**
     * FAB 在 Coordinator 外：show 前预抬 + 布局后按实际高度瞬间同步（无位移动画，避免与 Snackbar 滑入在时间上重叠）。
     */
    public void showFabSnackbar(int messageResId) {
        showFabSnackbar(getString(messageResId));
    }

    public void showFabSnackbar(@NonNull CharSequence message) {
        mainContent.post(() -> {
            Snackbar snackbar = Snackbar.make(mainContent, message, Snackbar.LENGTH_SHORT);
            snackbar.setGestureInsetBottomIgnored(true);
            final View snackView = snackbar.getView();

            preLiftFabBeforeSnackbarShows();

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onShown(Snackbar sb) {
                    syncFabLiftToSnackbarHeight(snackView);
                }

                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    fabAdd.animate().cancel();
                    fabAdd.setTranslationX(0f);
                    fabAdd.setTranslationY(0f);
                }
            });
            snackbar.show();
        });
    }

    private void preLiftFabBeforeSnackbarShows() {
        if (fabAdd.getVisibility() != View.VISIBLE) {
            return;
        }
        fabAdd.animate().cancel();
        float density = getResources().getDisplayMetrics().density;
        int estH = (int) (SNACKBAR_EST_HEIGHT_DP * density + 0.5f);
        float gapPx = FAB_SNACKBAR_GAP_DP * density;
        fabAdd.setTranslationY(-(estH + gapPx));
    }

    /** 测量到实际高度后瞬时对齐（不播放 translation 动画） */
    private void syncFabLiftToSnackbarHeight(@NonNull final View snackView) {
        if (fabAdd.getVisibility() != View.VISIBLE) {
            return;
        }
        final ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int h = snackbarInnerHeightPx(snackView);
                if (h <= 0) {
                    return;
                }
                if (snackView.getViewTreeObserver().isAlive()) {
                    snackView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                applyFabLiftImmediatePx(h);
            }
        };
        ViewTreeObserver observer = snackView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(listener);
        }
        snackView.post(() -> {
            int h = snackbarInnerHeightPx(snackView);
            if (h > 0) {
                if (snackView.getViewTreeObserver().isAlive()) {
                    snackView.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
                }
                applyFabLiftImmediatePx(h);
            }
        });
    }

    private static int snackbarInnerHeightPx(@NonNull View snackView) {
        return Math.max(snackView.getHeight(), snackView.getMeasuredHeight());
    }

    private void applyFabLiftImmediatePx(int snackbarBarHeightPx) {
        if (fabAdd.getVisibility() != View.VISIBLE || snackbarBarHeightPx <= 0) {
            return;
        }
        float gapPx = FAB_SNACKBAR_GAP_DP * getResources().getDisplayMetrics().density;
        fabAdd.animate().cancel();
        fabAdd.setTranslationY(-(snackbarBarHeightPx + gapPx));
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

    private void onFilterCategoriesChanged(Set<String> selectedCategories) {
        if (selectedCategories == null) {
            selectedCategories = Collections.emptySet();
        }
        if (categoryCheckedStates.length != categoryDisplayItems.size()) {
            categoryCheckedStates = new boolean[categoryDisplayItems.size()];
        }
        for (int i = 0; i < categoryDisplayItems.size(); i++) {
            categoryCheckedStates[i] = selectedCategories.contains(categoryDisplayItems.get(i));
        }
        categoryDropdown.setText(buildCategorySummaryText(selectedCategories), false);
    }

    private void showCategoryMultiSelectDialog() {
        boolean[] workingChecked = categoryCheckedStates.clone();
        CharSequence[] labels = categoryDisplayItems.toArray(new CharSequence[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.filter_category_label)
                .setMultiChoiceItems(labels, workingChecked, (dialog, which, isChecked) -> {
                    if (which >= 0 && which < workingChecked.length) {
                        workingChecked[which] = isChecked;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < workingChecked.length; i++) {
                        if (workingChecked[i]) {
                            selected.add(categoryDisplayItems.get(i));
                        }
                    }
                    billViewModel.setFilterCategories(selected);
                })
                .show();
    }

    private String buildCategorySummaryText(Set<String> selectedCategories) {
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            return getString(R.string.filter_all);
        }
        StringBuilder sb = new StringBuilder();
        for (String label : categoryDisplayItems) {
            if (!selectedCategories.contains(label)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('、');
            }
            sb.append(label);
        }
        return sb.length() == 0 ? getString(R.string.filter_all) : sb.toString();
    }

    private void onSelectionStateChanged(boolean inSelectionMode, int selectedCount) {
        if (selectionActionsLayout == null || selectionCountText == null || fabAdd == null) {
            return;
        }
        if (!inSelectionMode) {
            selectionActionsLayout.setVisibility(View.GONE);
            fabAdd.setVisibility(View.VISIBLE);
            return;
        }
        BillAdapter.closeAllSwipeRows(recyclerBills);
        selectionActionsLayout.setVisibility(View.VISIBLE);
        fabAdd.setVisibility(View.GONE);
        selectionCountText.setText(getString(R.string.selection_count, selectedCount));
    }

    private void onBatchDeleteRequested() {
        List<Bill> selectedBills = billAdapter.getSelectedBills();
        if (selectedBills.isEmpty()) {
            billAdapter.clearSelectionMode();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_bills_title)
                .setMessage(getString(R.string.delete_bills_message, selectedBills.size()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_bill_confirm, (dialog, which) -> {
                    BillAdapter.closeAllSwipeRows(recyclerBills);
                    billViewModel.deleteBills(selectedBills, () -> {
                        billAdapter.clearSelectionMode();
                        showFabSnackbar(getString(R.string.snack_bills_deleted, selectedBills.size()));
                    });
                })
                .show();
    }

    private void onBillsChanged(List<Bill> bills) {
        BillAdapter.closeAllSwipeRows(recyclerBills);
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
