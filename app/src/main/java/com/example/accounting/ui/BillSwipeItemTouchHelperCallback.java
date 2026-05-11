package com.example.accounting.ui;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.accounting.R;

/**
 * 左滑露出删除区（类似微信会话列表），不整行滑出删除。
 */
public class BillSwipeItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final int deleteWidthPx;

    public BillSwipeItemTouchHelperCallback(int deleteWidthPx) {
        this.deleteWidthPx = deleteWidthPx;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target
    ) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // 使用极大阈值避免走系统“滑掉整行”逻辑，仅用手势驱动 translationX
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 100f;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (viewHolder == null) {
            return;
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            RecyclerView rv = (RecyclerView) viewHolder.itemView.getParent();
            if (rv != null) {
                closeOtherRows(rv, viewHolder);
            }
        }
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas c,
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View fg = viewHolder.itemView.findViewById(R.id.swipe_foreground);
            if (fg != null && dX < 0f) {
                float clamped = Math.max(dX, -deleteWidthPx);
                fg.setTranslationX(clamped);
            }
            super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        View fg = viewHolder.itemView.findViewById(R.id.swipe_foreground);
        if (fg != null) {
            float tx = fg.getTranslationX();
            float target = tx <= -deleteWidthPx / 2f ? -deleteWidthPx : 0f;
            fg.animate().cancel();
            fg.animate().translationX(target).setDuration(160).start();
        }
        super.clearView(recyclerView, viewHolder);
    }

    public void closeAllOpenRows(@NonNull RecyclerView recyclerView) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            View fg = child.findViewById(R.id.swipe_foreground);
            if (fg != null && fg.getTranslationX() != 0f) {
                fg.animate().cancel();
                fg.animate().translationX(0f).setDuration(120).start();
            }
        }
    }

    private static void closeOtherRows(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder active) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
            if (vh == active) {
                continue;
            }
            View fg = child.findViewById(R.id.swipe_foreground);
            if (fg != null && fg.getTranslationX() != 0f) {
                fg.animate().cancel();
                fg.animate().translationX(0f).setDuration(120).start();
            }
        }
    }
}
