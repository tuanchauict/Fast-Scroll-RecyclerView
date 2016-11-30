package com.tuanchauict.fastscrollrv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

/**
 * Created by tuanchauict on 11/18/16.
 */

class Scrollbar {
    private static final int MSG_SHOW_SCROLLER = 2;
    private static final int MSG_HIDE_SCROLLER = 3;

    private static final long ANIM_DURATION_SHOW = 150;
    private static final long ANIM_DURATION_HIDE = 250;
    private static final int AUTO_HIDE_DELAY_DURATION = 1500;

    private Context mContext;
    private FastScrollRecyclerView mRecyclerView;

    private int mPaddingTop = Utils.toPixels(8); //attr
    private int mPaddingBottom = Utils.toPixels(8); //attr
    private int mPaddingRight = Utils.toPixels(4); //attr

    private int mOffsetX = Utils.toPixels(0);
    private int mOffsetHide;
    private int mOffsetShow;

    private int mTouchWidth = Utils.toPixels(32); //attr

    private int mThumbWidth = Utils.toPixels(8); //attr
    private int mThumbHeight = Utils.toPixels(50); //attr
    private int mThumbRadius = Utils.toPixels(1); //attr
    private int mThumbColor = 0xFFFF0000; //attr
    private int mThumbX;
    private int mThumbY;

    private int mTrackWidth = Utils.toPixels(4); //attr
    private int mTrackColor = 0xFF00FF00; //attr
    private int mTrackRadius = 0; //attr
    private int mTrackHeight;
    private int mTrackX;

    private int mAutoHideDelay;

    private int mRvWidth;
    private int mRvHeight;

    private int mTouchHeight;

    private Rect mInvalidateRect = new Rect();

    private ScrollPopup mPopup;

    private Paint mPaint;

    private boolean mAvailable;
    private boolean mShowing = false;
    private boolean mFastScrolling = false;

    private ValueAnimator mAnimShow;
    private ValueAnimator mAnimHide;

    private RectF mThumbRect = new RectF();
    private RectF mTrackRect = new RectF();

    private Handler mShowHideHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_SCROLLER) {
                onShowingScroller();
            } else if (msg.what == MSG_HIDE_SCROLLER) {
                onHidingScroller();
            }
        }
    };

    Scrollbar(Context context, FastScrollRecyclerView recyclerView, AttributeSet attrs) {
        mContext = context;
        mRecyclerView = recyclerView;

        readProperties(context, attrs);


        mOffsetShow = 0;
        mOffsetHide = mPaddingRight + mThumbWidth;
        mOffsetX = mOffsetHide;

        mPopup = new ScrollPopup(context, attrs);
        mPopup.setThumbWidth(mThumbWidth);

        mPaint = new Paint();

        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mOffsetX = mOffsetShow + (int) ((float) valueAnimator.getAnimatedValue() * (mOffsetHide - mOffsetShow));
//            Logger.d("update offset: %s, animValue = %s", mOffsetX, valueAnimator.getAnimatedValue());
                mRecyclerView.invalidateScrollbar();
            }
        };

        mAnimShow = ValueAnimator.ofFloat(1f, 0f);
        mAnimShow.setDuration(ANIM_DURATION_SHOW);
        mAnimShow.addUpdateListener(updateListener);
        mAnimShow.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mShowing = true;
            }
        });
        mAnimHide = ValueAnimator.ofFloat(0f, 1f);
        mAnimHide.setDuration(ANIM_DURATION_HIDE);
        mAnimHide.addUpdateListener(updateListener);
        mAnimHide.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mShowing = false;
            }
        });
    }

    private void readProperties(Context context, AttributeSet attrs) {
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView);

        mPaddingTop = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPaddingTop, Utils.toPixels(8));
        mPaddingBottom = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPaddingBottom, Utils.toPixels(8));
        mPaddingRight = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollPaddingRight, Utils.toPixels(4));

        mThumbWidth = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollThumbWidth, Utils.toPixels(8));
        mThumbHeight = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollThumbHeight, Utils.toPixels(64));
        mThumbRadius = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollThumbRoundRadius, 0);
        mThumbColor = arr.getColor(R.styleable.FastScrollRecyclerView_fastScrollThumbColor, 0xFF2196F3);

        mTrackWidth = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollTrackWidth, Utils.toPixels(2));
        mTrackColor = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollTrackColor, 0x11000000);
        mTrackRadius = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollTrackRoundRadius, 0);

        mTouchWidth = arr.getDimensionPixelOffset(R.styleable.FastScrollRecyclerView_fastScrollTouchWidth, (int) (mThumbWidth * 1.5));

        mAutoHideDelay = arr.getInt(R.styleable.FastScrollRecyclerView_fastScrollAutoHideDelay, AUTO_HIDE_DELAY_DURATION);

        arr.recycle();
    }


    void setBoundingSize(int width, int height) {
        mRvWidth = width;
        mRvHeight = height;

        mThumbX = width - mThumbWidth - mPaddingRight;
        mTrackX = mThumbX + ((mThumbWidth - mTrackWidth) >> 1);
        mTrackHeight = height - mPaddingTop - mPaddingBottom;

        mTouchHeight = mTrackHeight - mThumbHeight;

        mInvalidateRect.set(mThumbX, mPaddingTop, width, height - mPaddingBottom);

        mPopup.setBoundingSize(width, height);
    }

    void setThumbPercent(float percent) {
        mThumbY = (int) (percent * mTouchHeight) + mPaddingTop;

        if (mFastScrolling) {
            mPopup.setTargetY(mThumbY + mThumbHeight / 2);
        }
    }

    void draw(Canvas canvas) {
        mPaint.setColor(mTrackColor);
        int x = mTrackX + mOffsetX;
        mTrackRect.set(x, mPaddingTop, x + mTrackWidth, mRvHeight - mPaddingBottom);
//        canvas.drawRect(x, mPaddingTop, x + mTrackWidth, mPaddingBottom, mPaint);
        canvas.drawRoundRect(mTrackRect, mTrackRadius, mTrackRadius, mPaint);

        mPaint.setColor(mThumbColor);
        x = mThumbX + mOffsetX;
        mThumbRect.set(x, mThumbY, x + mThumbWidth, mThumbY + mThumbHeight);
//        canvas.drawRect(x, mThumbY, x + mThumbWidth, mThumbY + mThumbHeight, mPaint);
        canvas.drawRoundRect(mThumbRect, mThumbRadius, mThumbRadius, mPaint);

        if (mFastScrolling) {
            mPopup.draw(canvas);
        }
//        Logger.d("Draw: %s", x);
    }

    Rect getInvalidateRect() {
        return mInvalidateRect;
    }

    Rect getPopupInvalidateRect() {
        return mPopup.getInvalidateRect();
    }

    boolean shouldInvalidatePopup() {
        return mFastScrolling;
    }

    void setAvailable(boolean available) {
//        if (available != mAvailable) {
//            if (mShowing && !available) {
//                hideScroller(0);
//            }
//        }
        mAvailable = available;
    }

    void setFastScrolling(boolean fastScrolling) {
        mFastScrolling = fastScrolling;
    }

    void triggerVisible(boolean visible) {
        if (visible) {
            showScroller(0);
        } else {
            hideScroller(mAutoHideDelay);
        }
    }

    private void showScroller(long delay) {
//        mShowHideHandler.removeMessages(MSG_SHOW_SCROLLER);
        mShowHideHandler.removeMessages(MSG_HIDE_SCROLLER);
        mShowHideHandler.sendEmptyMessageDelayed(MSG_SHOW_SCROLLER, delay);
    }

    private void hideScroller(long delay) {
//        mShowHideHandler.removeMessages(MSG_SHOW_SCROLLER);
        mShowHideHandler.removeMessages(MSG_HIDE_SCROLLER);
        mShowHideHandler.sendEmptyMessageDelayed(MSG_HIDE_SCROLLER, delay);
    }

    private void onShowingScroller() {
        if (!mAvailable)
            return;
        if (!mShowing)
            mAnimShow.start();
    }

    private void onHidingScroller() {
        if (!mAvailable)
            return;
        if (mShowing && !mFastScrolling)
            mAnimHide.start();
    }

    boolean canInteract(int x, int y) {
        return mShowing &&
                (mRvWidth - mTouchWidth <= x && mThumbY <= y && y <= mThumbY + mThumbHeight);
    }

    float touchPercent(int y) {
        int yy = y - mPaddingTop;
        return (float) yy / (mTrackHeight - mThumbHeight);
    }

    int getThumbY() {
        return mThumbY;
    }

    void setSectionName(String sectionName) {
        mPopup.setSectionName(sectionName);
    }
}
