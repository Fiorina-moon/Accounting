package com.example.accounting.ui;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
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

        // 视觉修复：圆角裁剪，防止红色溢出
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = view.getResources().getDimension(R.dimen.card_corner_radius);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        view.setClipToOutline(true);

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

        // 状态变量
        private float initialX;
        private float initialTranslationX;
        private boolean isSwiping = false;
        private final int touchSlop;
        private float maxSwipeLimit = -200f; // 初始值，会在 bind 中根据删除按钮宽度动态更新

        BillViewHolder(@NonNull View itemView) {
            super(itemView);
            swipeForeground = itemView.findViewById(R.id.swipe_foreground);
            swipeDelete = itemView.findViewById(R.id.swipe_delete);
            iconCategory = swipeForeground.findViewById(R.id.icon_category);
            textCategory = swipeForeground.findViewById(R.id.text_category);
            textNote = swipeForeground.findViewById(R.id.text_note);
            textDate = swipeForeground.findViewById(R.id.text_date);
            textAmount = swipeForeground.findViewById(R.id.text_amount);
            touchSlop = ViewConfiguration.get(itemView.getContext()).getScaledTouchSlop();
        }

        void recycleSwipe() {
            swipeForeground.animate().cancel();
            swipeForeground.setTranslationX(0f);
            isSwiping = false;
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(@NonNull Bill bill, @Nullable BillActionsListener listener) {
            recycleSwipe();

            // 数据填充（保持不变）
            textCategory.setText(bill.getCategory());
            textNote.setVisibility((bill.getNote() == null || bill.getNote().isEmpty()) ? View.GONE : View.VISIBLE);
            textNote.setText(bill.getNote());
            textDate.setText(DATE_FORMAT.get().format(bill.getDateMillis()));
            String amountStr = String.format(Locale.getDefault(), "%.2f", bill.getAmount());
            if (bill.getType() == Bill.TYPE_EXPENSE) {
                textAmount.setText("-" + amountStr);
                textAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_theme_error));
            } else {
                textAmount.setText("+" + amountStr);
                textAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_success));
            }
            iconCategory.setBackgroundTintList(ColorStateList.valueOf(0xFFFFECF1));

            // --- 关键：动态计算侧滑限位 ---
            swipeDelete.post(() -> {
                int w = swipeDelete.getWidth();
                if (w > 0) maxSwipeLimit = -w;
            });

            // 1. 底层删除按钮的点击监听
            swipeDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBillDeleteClick(bill);
                    recycleSwipe(); // 删除后重置状态
                }
            });

            // 2. 前景卡片的手势处理
            swipeForeground.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float currentTranslationX = v.getTranslationX();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = event.getRawX();
                            initialTranslationX = currentTranslationX;
                            isSwiping = false;

                            // 【核心修复】穿透逻辑
                            // 如果卡片已经滑开，且用户点击的是右侧露出红色的区域
                            if (currentTranslationX < -10) {
                                float clickX = event.getX(); // 相对于卡片的坐标
                                if (clickX > (v.getWidth() + currentTranslationX)) {
                                    // 点击在红色区，不拦截，让事件传到底层的 swipeDelete
                                    return false;
                                }
                            }

                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialX;
                            if (!isSwiping && Math.abs(dx) > touchSlop) {
                                isSwiping = true;
                            }
                            if (isSwiping) {
                                float newT = initialTranslationX + dx;
                                v.setTranslationX(Math.max(maxSwipeLimit, Math.min(0, newT)));
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            float finalTx = v.getTranslationX();

                            if (!isSwiping) {
                                // 点击白色区域收起卡片或触发详情页
                                if (finalTx < -10) {
                                    v.animate().translationX(0).setDuration(200).start();
                                } else {
                                    if (listener != null) listener.onBillClick(bill);
                                }
                                return true;
                            }

                            // 弹性吸附：超过 40% 就锁定打开，否则回弹
                            if (finalTx < maxSwipeLimit * 0.4f) {
                                v.animate().translationX(maxSwipeLimit).setDuration(200).start();
                            } else {
                                v.animate().translationX(0).setDuration(200).start();
                            }
                            return true;
                    }
                    return false;
                }
            });
        }
    }
}