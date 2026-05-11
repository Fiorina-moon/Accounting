package com.example.accounting;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.accounting.data.local.entity.Bill;
import com.example.accounting.viewmodel.BillViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class AddBillSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_BILL_ID = "arg_bill_id";
    private static final String ARG_AMOUNT = "arg_amount";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_DATE_MILLIS = "arg_date_millis";
    private static final String ARG_NOTE = "arg_note";

    private BillViewModel billViewModel;
    private TextInputLayout layoutAmount;
    private TextInputEditText inputAmount;
    private TextInputEditText inputNote;
    private MaterialButtonToggleGroup toggleType;
    private ChipGroup chipGroupCategory;
    private View btnPickDate;
    private View btnSave;

    private long selectedDateMillis;
    private long editingBillId;

    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

    private final InputFilter amountInputFilter = new InputFilter() {
        @Override
        public CharSequence filter(
                CharSequence source,
                int start,
                int end,
                Spanned dest,
                int dstart,
                int dend
        ) {
            StringBuilder builder = new StringBuilder();
            builder.append(dest.subSequence(0, dstart));
            builder.append(source.subSequence(start, end));
            builder.append(dest.subSequence(dend, dest.length()));
            String result = builder.toString();
            if (result.isEmpty()) {
                return null;
            }
            if (result.matches("^\\d*\\.?\\d*$")) {
                return null;
            }
            return "";
        }
    };

    public static AddBillSheetFragment newInstanceForAdd() {
        return new AddBillSheetFragment();
    }

    public static AddBillSheetFragment newInstanceForEdit(@NonNull Bill bill) {
        AddBillSheetFragment fragment = new AddBillSheetFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BILL_ID, bill.getId());
        args.putDouble(ARG_AMOUNT, bill.getAmount());
        args.putInt(ARG_TYPE, bill.getType());
        args.putString(ARG_CATEGORY, bill.getCategory());
        args.putLong(ARG_DATE_MILLIS, bill.getDateMillis());
        args.putString(ARG_NOTE, bill.getNote() != null ? bill.getNote() : "");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_Accounting_BottomSheetDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (!(d instanceof BottomSheetDialog)) {
            return;
        }
        BottomSheetDialog dialog = (BottomSheetDialog) d;
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) {
            return;
        }
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        // 按内容高度升高，顶部仍在屏幕中部以下，是典型的底部弹层，而不是贴顶全屏
        behavior.setFitToContents(true);
        behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
        sheet.post(() -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.layout_add_bill, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        billViewModel = new ViewModelProvider(requireActivity()).get(BillViewModel.class);

        layoutAmount = view.findViewById(R.id.layout_amount);
        inputAmount = view.findViewById(R.id.input_amount);
        inputNote = view.findViewById(R.id.input_note);
        toggleType = view.findViewById(R.id.toggle_type);
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        btnPickDate = view.findViewById(R.id.btn_pick_date);
        btnSave = view.findViewById(R.id.btn_save);

        inputAmount.setFilters(new InputFilter[]{amountInputFilter});
        inputAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                layoutAmount.setError(null);
            }
        });

        editingBillId = 0L;
        Bundle args = getArguments();
        if (args != null && args.getLong(ARG_BILL_ID, 0L) > 0L) {
            editingBillId = args.getLong(ARG_BILL_ID);
            inputAmount.setText(String.format(Locale.getDefault(), "%.2f", args.getDouble(ARG_AMOUNT)));
            int type = args.getInt(ARG_TYPE, Bill.TYPE_EXPENSE);
            toggleType.check(type == Bill.TYPE_INCOME ? R.id.btn_type_income : R.id.btn_type_expense);
            selectChipByCategory(args.getString(ARG_CATEGORY, ""));
            selectedDateMillis = args.getLong(ARG_DATE_MILLIS);
            inputNote.setText(args.getString(ARG_NOTE, ""));
            ((com.google.android.material.button.MaterialButton) btnSave).setText(R.string.add_bill_save_update);
        } else {
            Calendar noon = Calendar.getInstance();
            noon.set(Calendar.HOUR_OF_DAY, 12);
            noon.set(Calendar.MINUTE, 0);
            noon.set(Calendar.SECOND, 0);
            noon.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = noon.getTimeInMillis();
            ((com.google.android.material.button.MaterialButton) btnSave).setText(R.string.add_bill_save);
        }

        updateDateButtonLabel();

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> attemptSave());
    }

    private void selectChipByCategory(@NonNull String category) {
        for (int i = 0; i < chipGroupCategory.getChildCount(); i++) {
            View child = chipGroupCategory.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (category.equals(chip.getText().toString())) {
                    chipGroupCategory.check(chip.getId());
                    return;
                }
            }
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.add_bill_pick_date)
                .setSelection(toUtcStartOfDayMillis(selectedDateMillis))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            Calendar local = Calendar.getInstance();
            local.clear();
            local.set(
                    utc.get(Calendar.YEAR),
                    utc.get(Calendar.MONTH),
                    utc.get(Calendar.DAY_OF_MONTH),
                    12,
                    0,
                    0
            );
            local.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = local.getTimeInMillis();
            updateDateButtonLabel();
        });
        picker.show(getParentFragmentManager(), "material_date_picker");
    }

    private static long toUtcStartOfDayMillis(long localMillis) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(localMillis);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
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
    }

    private void updateDateButtonLabel() {
        ((com.google.android.material.button.MaterialButton) btnPickDate).setText(
                dateFormat.format(selectedDateMillis)
        );
    }

    private void attemptSave() {
        CharSequence amountText = inputAmount.getText();
        if (amountText == null || amountText.toString().trim().isEmpty()) {
            layoutAmount.setError(getString(R.string.error_amount_required));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText.toString().trim());
        } catch (NumberFormatException e) {
            layoutAmount.setError(getString(R.string.error_amount_required));
            return;
        }
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            layoutAmount.setError(getString(R.string.error_amount_required));
            return;
        }

        int checkedToggleId = toggleType.getCheckedButtonId();
        int type = checkedToggleId == R.id.btn_type_income ? Bill.TYPE_INCOME : Bill.TYPE_EXPENSE;

        int chipId = chipGroupCategory.getCheckedChipId();
        if (chipId == View.NO_ID) {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).showFabSnackbar(R.string.error_category_required);
            } else {
                Snackbar.make(requireActivity().findViewById(R.id.main), R.string.error_category_required, Snackbar.LENGTH_SHORT)
                        .show();
            }
            return;
        }
        Chip chip = chipGroupCategory.findViewById(chipId);
        String category = chip.getText().toString();

        String note = "";
        if (inputNote.getText() != null) {
            note = inputNote.getText().toString().trim();
        }

        if (editingBillId > 0L) {
            Bill bill = new Bill(amount, type, category, selectedDateMillis, note);
            bill.setId(editingBillId);
            billViewModel.updateBill(bill, () -> {
                dismiss();
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).showFabSnackbar(R.string.snack_bill_updated);
                }
            });
        } else {
            Bill bill = new Bill(amount, type, category, selectedDateMillis, note);
            billViewModel.insertBill(bill, () -> {
                dismiss();
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).showFabSnackbar(R.string.snack_bill_saved);
                }
            });
        }
    }
}
