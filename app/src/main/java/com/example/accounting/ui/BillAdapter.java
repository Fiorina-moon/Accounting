package com.example.accounting.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.accounting.R;
import com.example.accounting.data.local.entity.Bill;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class BillAdapter extends ListAdapter<Bill, BillAdapter.BillViewHolder> {

    public interface BillActionsListener {
        void onBillClick(@NonNull Bill bill);

        void onBillDeleteClick(@NonNull Bill bill);
    }

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    );

    @Nullable
    private BillActionsListener billActionsListener;

    public BillAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setBillActionsListener(@Nullable BillActionsListener listener) {
        this.billActionsListener = listener;
    }

    private static final DiffUtil.ItemCallback<Bill> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bill>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return Double.compare(oldItem.getAmount(), newItem.getAmount()) == 0
                    && oldItem.getType() == newItem.getType()
                    && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                    && oldItem.getDateMillis() == newItem.getDateMillis()
                    && Objects.equals(oldItem.getNote(), newItem.getNote());
        }
    };

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        holder.bind(getItem(position), billActionsListener);
    }

    @Override
    public void onViewRecycled(@NonNull BillViewHolder holder) {
        holder.recycleSwipe();
        super.onViewRecycled(holder);
    }

    static final class BillViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView swipeForeground;
        private final View swipeDelete;
        private final ShapeableImageView iconCategory;
        private final TextView textCategory;
        private final TextView textNote;
        private final TextView textDate;
        private final TextView textAmount;

        BillViewHolder(@NonNull View itemView) {
            super(itemView);
            swipeForeground = itemView.findViewById(R.id.swipe_foreground);
            swipeDelete = itemView.findViewById(R.id.swipe_delete);
            iconCategory = swipeForeground.findViewById(R.id.icon_category);
            textCategory = swipeForeground.findViewById(R.id.text_category);
            textNote = swipeForeground.findViewById(R.id.text_note);
            textDate = swipeForeground.findViewById(R.id.text_date);
            textAmount = swipeForeground.findViewById(R.id.text_amount);
        }

        void recycleSwipe() {
            swipeForeground.animate().cancel();
            swipeForeground.setTranslationX(0f);
        }

        void bind(@NonNull Bill bill, @Nullable BillActionsListener listener) {
            recycleSwipe();

            textCategory.setText(bill.getCategory());
            String note = bill.getNote();
            if (note == null || note.isEmpty()) {
                textNote.setVisibility(View.GONE);
            } else {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText(note);
            }
            textDate.setText(DATE_FORMAT.get().format(bill.getDateMillis()));

            Locale locale = Locale.getDefault();
            String amountPart = String.format(locale, "%.2f", bill.getAmount());
            if (bill.getType() == Bill.TYPE_EXPENSE) {
                textAmount.setText(itemView.getContext().getString(R.string.bill_amount_expense_format, amountPart));
                textAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_theme_error));
            } else {
                textAmount.setText(itemView.getContext().getString(R.string.bill_amount_income_format, amountPart));
                textAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_success));
            }

            int bgTint = categoryBackgroundTint(bill.getCategory());
            iconCategory.setImageResource(R.drawable.ic_receipt_24);
            iconCategory.setBackgroundTintList(ColorStateList.valueOf(bgTint));

            swipeForeground.setOnClickListener(v -> {
                if (Math.abs(swipeForeground.getTranslationX()) > 4f) {
                    swipeForeground.animate().translationX(0f).setDuration(120).start();
                    return;
                }
                if (listener != null) {
                    listener.onBillClick(bill);
                }
            });

            swipeDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBillDeleteClick(bill);
                }
            });
        }

        private static int categoryBackgroundTint(String category) {
            if (category == null) {
                return 0xFFE0F2F1;
            }
            switch (category) {
                case "餐饮":
                    return 0xFFE8F5E9;
                case "交通":
                    return 0xFFE3F2FD;
                case "购物":
                    return 0xFFFFF3E0;
                default:
                    return 0xFFE0F2F1;
            }
        }
    }
}
