package com.tuanchauict.fastscrollrv;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;

/**
 * Created by tuanchauict on 11/18/16.
 */

public class ScrollPopup {

    private float mTextSize = 60f; //attr
    private int mHPadding = Utils.toPixels(8); //attr
    private int mVPadding = Utils.toPixels(8); //attr
    private int mRadius = Utils.toPixels(4); //attr
    private int mTextColor = 0xFFFFFFFF; //attr
    private int mBgColor = 0xAA000000; //attr
    private int mMarginRight = Utils.toPixels(40); //attr

    private Rect mInvalidateRect = new Rect();
    private int mTargetY;

    private int mThumbWidth;

    private int mX;
    private int mRvWidth;
    private int mRvHeight;
    private int mPopupWidth;
    private int mPopupHeight;

    private String mSectionName;
    private Rect mTextBound;
    private RectF mPopupBound;

    private boolean mAvailable = true;


    private TextPaint mPaint;

    public ScrollPopup(Context context, AttributeSet attrs) {
        readProperties(context, attrs);
        mPaint = new TextPaint();
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setTextSize(mTextSize);
        mPaint.setAntiAlias(true);

        mTextBound = new Rect();
        mPopupBound = new RectF();
    }

    private void readProperties(Context context, AttributeSet attrs) {
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView);

        mHPadding = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPopupHorizontalPadding, Utils.toPixels(8));
        mVPadding = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPopupVerticalPadding, Utils.toPixels(8));
        mRadius = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPopupRadius, Utils.toPixels(4));
        mTextColor = arr.getColor(R.styleable.FastScrollRecyclerView_fastScrollPopupTextColor, 0xFFFFFFFF);
        mTextSize = arr.getDimension(R.styleable.FastScrollRecyclerView_fastScrollPopupTextSize, 60f);
        mBgColor = arr.getColor(R.styleable.FastScrollRecyclerView_fastScrollPopupBgColor, 0xAA000000);

        mMarginRight = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPopupMargin, Utils.toPixels(40));

        arr.recycle();
    }

    public void draw(Canvas canvas) {
        if (!mAvailable) {
            return;
        }
        mPaint.setColor(mBgColor);
        int y = mTargetY - mPopupHeight / 2;
        mPopupBound.set(
                mX,
                y,
                mX + mPopupWidth,
                y + mPopupHeight);
        canvas.drawRoundRect(mPopupBound, mRadius, mRadius, mPaint);
        mPaint.setColor(mTextColor);
        canvas.drawText(mSectionName, mX + mHPadding, y + mVPadding + mTextBound.height(), mPaint);
    }

    public Rect getInvalidateRect() {
        return mInvalidateRect;
    }

    public void setBoundingSize(int width, int height) {
        mRvWidth = width;
        mRvHeight = height;

        calculatePopupRect();
    }

    public void setThumbWidth(int width) {
        mThumbWidth = width;
        calculatePopupRect();
    }

    public void setTargetY(int targetY) {
        mTargetY = targetY;
    }

    public void setSectionName(String sectionName) {
        if (sectionName == null || sectionName.isEmpty()) {
            mAvailable = false;
        }
        if (sectionName.equals(mSectionName)) {
            return;
        }
        mAvailable = true;
        mSectionName = sectionName;

        calculatePopupRect();
    }

    private void calculatePopupRect() {
        mPaint.getTextBounds(mSectionName, 0, mSectionName.length(), mTextBound);
        mPopupWidth = mHPadding * 2 + mTextBound.width();
        mPopupHeight = mVPadding * 2 + mTextBound.height();
        mX = mRvWidth - mThumbWidth - mMarginRight - mPopupWidth;
    }

}
