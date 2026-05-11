package com.example.accounting.ui;

import android.annotation.SuppressLint;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class BillAdapter extends ListAdapter<Bill, BillAdapter.BillViewHolder> {

    public interface BillActionsListener {
        void onBillClick(@NonNull Bill bill);
        void onBillDeleteClick(@NonNull Bill bill);
    }

    public interface SelectionStateListener {
        void onSelectionChanged(boolean inSelectionMode, int selectedCount);
    }

    private interface SelectionCallbacks {
        void onToggleSelection(@NonNull Bill target);
        void onEnterSelectionMode(@NonNull Bill target);
    }

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    );

    @Nullable
    private BillActionsListener billActionsListener;
    @Nullable
    private SelectionStateListener selectionStateListener;
    private boolean selectionMode = false;
    private final LinkedHashSet<Long> selectedBillIds = new LinkedHashSet<>();

    public BillAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setBillActionsListener(@Nullable BillActionsListener listener) {
        this.billActionsListener = listener;
    }

    public void setSelectionStateListener(@Nullable SelectionStateListener listener) {
        this.selectionStateListener = listener;
        notifySelectionStateChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getSelectedCount() {
        return selectedBillIds.size();
    }

    @NonNull
    public List<Bill> getSelectedBills() {
        List<Bill> selected = new ArrayList<>();
        for (Bill bill : getCurrentList()) {
            if (selectedBillIds.contains(bill.getId())) {
                selected.add(bill);
            }
        }
        return selected;
    }

    public void clearSelectionMode() {
        if (!selectionMode && selectedBillIds.isEmpty()) {
            return;
        }
        selectionMode = false;
        selectedBillIds.clear();
        notifyDataSetChanged();
        notifySelectionStateChanged();
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
        Bill bill = getItem(position);
        holder.bind(
                bill,
                billActionsListener,
                selectionMode,
                selectedBillIds.contains(bill.getId()),
                new SelectionCallbacks() {
                    @Override
                    public void onToggleSelection(@NonNull Bill target) {
                        toggleSelection(target);
                    }

                    @Override
                    public void onEnterSelectionMode(@NonNull Bill target) {
                        if (!selectionMode) {
                            selectionMode = true;
                        }
                        toggleSelection(target);
                    }
                }
        );
    }

    @Override
    public void onCurrentListChanged(@NonNull List<Bill> previousList, @NonNull List<Bill> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        if (selectedBillIds.isEmpty()) {
            return;
        }
        Set<Long> validIds = new LinkedHashSet<>();
        for (Bill bill : currentList) {
            validIds.add(bill.getId());
        }
        if (selectedBillIds.retainAll(validIds) && selectedBillIds.isEmpty() && selectionMode) {
            selectionMode = false;
        }
        notifySelectionStateChanged();
    }

    private void toggleSelection(@NonNull Bill target) {
        long id = target.getId();
        if (selectedBillIds.contains(id)) {
            selectedBillIds.remove(id);
        } else {
            selectedBillIds.add(id);
        }
        if (selectedBillIds.isEmpty()) {
            selectionMode = false;
        }
        notifyDataSetChanged();
        notifySelectionStateChanged();
    }

    private void notifySelectionStateChanged() {
        if (selectionStateListener != null) {
            selectionStateListener.onSelectionChanged(selectionMode, selectedBillIds.size());
        }
    }

    /** 收起RecyclerView中所有侧滑展开的行（替换原 ItemTouchHelper 的关闭逻辑）。 */
    public static void closeAllSwipeRows(@Nullable RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View row = recyclerView.getChildAt(i);
            View fg = row.findViewById(R.id.swipe_foreground);
            if (fg != null && fg.getTranslationX() != 0f) {
                fg.animate().cancel();
                fg.animate().translationX(0f).setDuration(120).start();
                fg.bringToFront();
            }
        }
    }

    static void closeOtherSwipeRows(@Nullable RecyclerView recyclerView, @NonNull View exceptRow) {
        if (recyclerView == null) {
            return;
        }
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View row = recyclerView.getChildAt(i);
            if (row == exceptRow) {
                continue;
            }
            View fg = row.findViewById(R.id.swipe_foreground);
            if (fg != null && fg.getTranslationX() != 0f) {
                fg.animate().cancel();
                fg.animate().translationX(0f).setDuration(120).start();
                fg.bringToFront();
            }
        }
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
        private float initialY;
        private long initialDownTimeMs;
        private float initialTranslationX;
        private boolean isSwiping = false;
        private boolean isVerticalDragging = false;
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
            swipeForeground.bringToFront();
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(
                @NonNull Bill bill,
                @Nullable BillActionsListener listener,
                boolean inSelectionMode,
                boolean isSelected,
                @NonNull SelectionCallbacks selectionCallbacks
        ) {
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
            iconCategory.setImageResource(resolveCategoryIconRes(bill.getCategory()));

            if (inSelectionMode) {
                swipeDelete.setVisibility(View.GONE);
                swipeForeground.setTranslationX(0f);
                swipeForeground.setStrokeWidth(isSelected ? 2 : 0);
                swipeForeground.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.md_theme_primary));
                swipeForeground.setOnTouchListener(null);
                swipeForeground.setOnClickListener(v -> selectionCallbacks.onToggleSelection(bill));
                swipeForeground.setOnLongClickListener(v -> {
                    selectionCallbacks.onToggleSelection(bill);
                    return true;
                });
                swipeDelete.setOnClickListener(null);
                return;
            }

            swipeDelete.setVisibility(View.VISIBLE);
            swipeForeground.setStrokeWidth(0);
            swipeForeground.setOnClickListener(null);
            swipeForeground.setOnLongClickListener(null);

            final RecyclerView hostRv = itemView.getParent() instanceof RecyclerView
                    ? (RecyclerView) itemView.getParent()
                    : null;

            // --- 关键：动态计算侧滑限位 ---
            swipeDelete.post(() -> {
                int w = swipeDelete.getWidth();
                if (w > 0) maxSwipeLimit = -w;
            });

            // 1. 底层删除按钮的点击监听
            swipeDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBillDeleteClick(bill);
                    recycleSwipe();
                }
            });

            // 2. 前景卡片的手势处理
            swipeForeground.setOnTouchListener(new View.OnTouchListener() {
                private Runnable longPressRunnable;
                private boolean longPressTriggered;
                /**
                 * 红色删除条在 swipe_foreground 下层，但前景仍为 match_parent，触点命中在前景上；
                 * return false 不会把事件交给 swipe_delete，必须在这里识别删除区并在 UP 时触发删除。
                 */
                private boolean pendingDeleteZoneTap;

                private void cancelLongPress(View v) {
                    if (longPressRunnable != null) {
                        v.removeCallbacks(longPressRunnable);
                        longPressRunnable = null;
                    }
                }

                private boolean isInDeleteZone(MotionEvent event, float translationX) {
                    if (translationX >= -10f) {
                        return false;
                    }
                    int[] loc = new int[2];
                    itemView.getLocationOnScreen(loc);
                    float xInRow = event.getRawX() - loc[0];
                    float threshold = itemView.getWidth() + translationX;
                    return xInRow > threshold - 1f;
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float currentTranslationX = v.getTranslationX();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = event.getRawX();
                            initialY = event.getRawY();
                            initialDownTimeMs = event.getEventTime();
                            initialTranslationX = currentTranslationX;
                            isSwiping = false;
                            isVerticalDragging = false;
                            longPressTriggered = false;
                            pendingDeleteZoneTap = false;

                            if (isInDeleteZone(event, currentTranslationX)) {
                                cancelLongPress(v);
                                pendingDeleteZoneTap = true;
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                                return true;
                            }

                            longPressRunnable = () -> {
                                longPressTriggered = true;
                                selectionCallbacks.onEnterSelectionMode(bill);
                            };
                            v.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());

                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialX;
                            float dy = event.getRawY() - initialY;
                            float absDx = Math.abs(dx);
                            float absDy = Math.abs(dy);

                            if (pendingDeleteZoneTap && (absDx > touchSlop || absDy > touchSlop)) {
                                pendingDeleteZoneTap = false;
                            }

                            if (!isSwiping && !isVerticalDragging) {
                                if (absDy > touchSlop && absDy > absDx) {
                                    isVerticalDragging = true;
                                    cancelLongPress(v);
                                    return false;
                                }
                                if (absDx > touchSlop && absDx > absDy) {
                                    isSwiping = true;
                                    cancelLongPress(v);
                                    v.getParent().requestDisallowInterceptTouchEvent(true);
                                    closeOtherSwipeRows(hostRv, itemView);
                                }
                            }

                            if (isSwiping) {
                                float newT = initialTranslationX + dx;
                                v.setTranslationX(Math.max(maxSwipeLimit, Math.min(0, newT)));
                                return true;
                            }
                            return false;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            cancelLongPress(v);
                            v.getParent().requestDisallowInterceptTouchEvent(false);

                            if (isVerticalDragging || longPressTriggered) {
                                pendingDeleteZoneTap = false;
                                return false;
                            }

                            if (pendingDeleteZoneTap && event.getAction() == MotionEvent.ACTION_UP) {
                                float upDx = Math.abs(event.getRawX() - initialX);
                                float upDy = Math.abs(event.getRawY() - initialY);
                                pendingDeleteZoneTap = false;
                                if (!isSwiping && upDx < touchSlop && upDy < touchSlop) {
                                    if (listener != null) {
                                        listener.onBillDeleteClick(bill);
                                    }
                                    recycleSwipe();
                                }
                                return true;
                            }
                            pendingDeleteZoneTap = false;

                            float finalTx = v.getTranslationX();
                            if (!isSwiping) {
                                float upDx = Math.abs(event.getRawX() - initialX);
                                float upDy = Math.abs(event.getRawY() - initialY);
                                if (upDx < touchSlop && upDy < touchSlop) {
                                    if (finalTx < -10) {
                                        swipeForeground.bringToFront();
                                        v.animate().translationX(0).setDuration(200).start();
                                    } else if (listener != null) {
                                        listener.onBillClick(bill);
                                    }
                                }
                                return true;
                            }

                            if (finalTx < maxSwipeLimit * 0.4f) {
                                swipeDelete.bringToFront();
                                v.animate().translationX(maxSwipeLimit).setDuration(200).start();
                            } else {
                                swipeForeground.bringToFront();
                                v.animate().translationX(0).setDuration(200).start();
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        private int resolveCategoryIconRes(@Nullable String category) {
            if (category == null) {
                return R.drawable.ic_receipt_24;
            }
            if (category.equals(itemView.getContext().getString(R.string.category_dining))) {
                return R.drawable.ic_restaurant_24;
            }
            if (category.equals(itemView.getContext().getString(R.string.category_transport))) {
                return R.drawable.ic_directions_bus_24;
            }
            if (category.equals(itemView.getContext().getString(R.string.category_shopping))) {
                return R.drawable.ic_shopping_bag_24;
            }
            return R.drawable.ic_receipt_24;
        }
    }
}