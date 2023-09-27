package com.md.rtcvoicedemo;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * @author jack
 * @Description
 * @Date 2018/4/19
 */
public class ColorDecoration extends RecyclerView.ItemDecoration {
    private int mHorizontalSpacing;
    private int mVerticalSpacing;
    private boolean mIncludeEdge;
    private int mColor;
    private int ignoreBottomCount = 0;

    private int mMarginStart;
    private int mMarginEnd;
    private boolean isCusStartAndEnd;

    public ColorDecoration(int color, int hSpacing, int vSpacing, boolean includeEdge) {
        mHorizontalSpacing = hSpacing;
        mVerticalSpacing = vSpacing;
        mIncludeEdge = includeEdge;
        this.mColor = color;
    }

    /**
     *
     * @param marginStart
     * px
     * @param marginEnd
     * px
     */
    public ColorDecoration(int color, int hSpacing, int vSpacing, boolean includeEdge, int marginStart, int marginEnd) {
        mHorizontalSpacing = hSpacing;
        mVerticalSpacing = vSpacing;
        mIncludeEdge = includeEdge;
        this.mColor = color;

        isCusStartAndEnd = true;
        mMarginStart = marginStart;
        mMarginEnd = marginEnd;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        // Only handle the vertical situation
        int position = parent.getChildAdapterPosition(view);
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            int spanCount = layoutManager.getSpanCount();
            int column = position % spanCount;
            getGridItemOffsets(outRect, position, column, spanCount);
        } else if (parent.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) parent.getLayoutManager();
            int spanCount = layoutManager.getSpanCount();
            StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
            int column = lp.getSpanIndex();
            getGridItemOffsets(outRect, position, column, spanCount);
        } else if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            if (((LinearLayoutManager) parent.getLayoutManager()).getOrientation() == LinearLayoutManager.VERTICAL) {
                if (mIncludeEdge) {
                    if (position == 0) {
                        outRect.top = mVerticalSpacing;
                    }
                    outRect.bottom = mVerticalSpacing;
                } else {
                    if (position > 0) {
                        outRect.top = mVerticalSpacing;
                    }
                }
            } else {
                if (mIncludeEdge) {
                    if (position == 0) {
                        outRect.left = mHorizontalSpacing;
                    }
                    outRect.right = mHorizontalSpacing;
                } else {
                    if (position > 0) {
                        outRect.left = mHorizontalSpacing;
                    }
                }
            }
        }

    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            if (((LinearLayoutManager) parent.getLayoutManager()).getOrientation() == LinearLayoutManager.VERTICAL) {
                drawVertical(c, parent);
            } else {
                drawHorizontal(c, parent);
            }
        }else{
            c.drawColor(mColor);
        }

    }



    private void getGridItemOffsets(Rect outRect, int position, int column, int spanCount) {
        if (mIncludeEdge) {
            outRect.left = mHorizontalSpacing * (spanCount - column) / spanCount;
            outRect.right = mHorizontalSpacing * (column + 1) / spanCount;
            if (position < spanCount) {
                outRect.top = mVerticalSpacing;
            }
            outRect.bottom = mVerticalSpacing;
        } else {
            outRect.left = mHorizontalSpacing * column / spanCount;
            outRect.right = mHorizontalSpacing * (spanCount - 1 - column) / spanCount;
            if (position >= spanCount) {
                outRect.top = mVerticalSpacing;
            }
        }
    }

    private final Rect mBounds = new Rect();

    private void drawVertical(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int left;
        final int right;

        if (parent.getClipToPadding()) {

            left = isCusStartAndEnd ? mMarginStart : parent.getPaddingLeft();
            right = parent.getWidth() - (isCusStartAndEnd ? mMarginEnd : parent.getPaddingRight());
            canvas.clipRect(left, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        } else {
            left = 0;
            right = parent.getWidth();
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            if (mIncludeEdge){
                if (i == 0){
                    int top = mBounds.top;
                    int bottom =  mVerticalSpacing;
                    canvas.save();
                    canvas.clipRect(left, top, right, bottom);
                    canvas.drawColor(mColor);
                    canvas.restore();
                }
                int bottom = mBounds.bottom + Math.round(child.getTranslationY());
                int top = bottom - mVerticalSpacing;
                canvas.save();
                canvas.clipRect(left, top, right, bottom);
                canvas.drawColor(mColor);
                canvas.restore();
            }else{
                if (i > 0) {
                    int top = mBounds.top + Math.round(child.getTranslationY());
                    int bottom = top + mVerticalSpacing;
                    canvas.save();
                    canvas.clipRect(left, top, right, bottom);
                    canvas.drawColor(mColor);
                    canvas.restore();
                }
            }

        }
        canvas.restore();
    }

    private void drawHorizontal(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int top;
        final int bottom;
        if (parent.getClipToPadding()) {
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
            canvas.clipRect(parent.getPaddingLeft(), top,
                    parent.getWidth() - parent.getPaddingRight(), bottom);
        } else {
            top = 0;
            bottom = parent.getHeight();
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            parent.getLayoutManager().getDecoratedBoundsWithMargins(child, mBounds);
            if(mIncludeEdge){
                if (i == 0){
                    int left = mBounds.left;
                    int right =  mHorizontalSpacing;
                    canvas.save();
                    canvas.clipRect(left, top, right, bottom);
                    canvas.drawColor(mColor);
                    canvas.restore();
                }
                int right = mBounds.right + Math.round(child.getTranslationX());
                int left = right - mHorizontalSpacing;
                canvas.save();
                canvas.clipRect(left, top, right, bottom);
                canvas.drawColor(mColor);
                canvas.restore();
            }else {
                if (i > 0) {
                    int left = mBounds.left + Math.round(child.getTranslationX());
                    int right = left + mHorizontalSpacing;
                    canvas.save();
                    canvas.clipRect(left, top, right, bottom);
                    canvas.drawColor(mColor);
                    canvas.restore();
                }
            }
        }
        canvas.restore();
    }
}
