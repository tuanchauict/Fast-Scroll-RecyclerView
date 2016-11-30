package com.tuanchauict.fastscrollrv;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by tuanchauict on 11/18/16.
 */

public class FastScrollRecyclerView extends RecyclerView {
    private static final int MSG_NOTIFY_DATA_SET_CHANGED = 1;

    private Scrollbar mScrollbar;

    private int mTouchDeltaY;
    private boolean mFastScrolling = false;

    private Adapter mAdapter;
    private LinearLayoutManager mLinearLayoutManager = null;
    private EstimateScrollerAdapter mEstimateScroller;

    private ScrollerContentHeight mScrollerContentHeight = new ScrollerContentHeight(this);

    private FastScrollListener mFastScrollListener;

    private SectionedAdapter mSectionedAdapter;

    private AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }
    };

    private Handler mControlHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_NOTIFY_DATA_SET_CHANGED)
                onNotifyDataSetChanged();
        }
    };

    public FastScrollRecyclerView(Context context) {
        this(context, null);
    }

    public FastScrollRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Utils.init(context);
        mScrollbar = new Scrollbar(context, this, attrs);

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                LOG.i("State changed: %s", newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    mScrollbar.triggerVisible(true);
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mScrollbar.triggerVisible(false);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mScrollbar.triggerVisible(false);
                updateScrollbar();
            }
        });
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof LinearLayoutManager)
            mLinearLayoutManager = (LinearLayoutManager) layout;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }
        mAdapter = adapter;

        if (mAdapter != null) {
            mAdapter.registerAdapterDataObserver(mAdapterDataObserver);
            if (mAdapter instanceof EstimateScrollerAdapter) {
                mEstimateScroller = (EstimateScrollerAdapter) mAdapter;
            }

            if (mAdapter instanceof SectionedAdapter) {
                mSectionedAdapter = (SectionedAdapter) mAdapter;
            }

        }
        notifyDataSetChanged();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mScrollbar.setBoundingSize(getWidth(), getHeight());
        invalidateScrollbar();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAdapter != null) {
            mAdapter.registerAdapterDataObserver(mAdapterDataObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (mFastScrolling) {
            return true;
        } else {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                mFastScrolling = mScrollbar.canInteract((int) e.getX(), (int) e.getY());
                mScrollbar.setFastScrolling(mFastScrolling);
            }
        }
        return mFastScrolling || super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return handleTouchEvent(e) || super.onTouchEvent(e);
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
        if (!mScrollerContentHeight.isWorking())
            mScrollbar.draw(c);
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        if (mAdapter == null)
            return false;

        int action = ev.getAction();
        int y = (int) ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mFastScrolling && mFastScrollListener != null) {
                    mFastScrollListener.onFastScrollStart();
                }
                mTouchDeltaY = y - mScrollbar.getThumbY();
                break;
            case MotionEvent.ACTION_MOVE:
                y -= mTouchDeltaY;
                if (mFastScrolling) {
                    float percent = mScrollbar.touchPercent(y);

                    int index = mScrollerContentHeight.getAbsoluteIndex(percent);
                    int offset = mScrollerContentHeight.getRowOffset(index, percent);
                    mLinearLayoutManager.scrollToPositionWithOffset(index, offset);
                    if (mSectionedAdapter != null) {
                        mScrollbar.setSectionName(mSectionedAdapter.getSectionName(index));
                    } else {
                        mScrollbar.setSectionName(null);
                    }
                    mScrollbar.triggerVisible(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mFastScrolling && mFastScrollListener != null) {
                    mFastScrollListener.onFastScrollStop();
                }
                mFastScrolling = false;
                mScrollbar.setFastScrolling(false);
                invalidateScrollbar();
                mScrollbar.triggerVisible(false);
                break;
        }

        return mFastScrolling;
    }

    private void updateScrollbar() {
        if (mAdapter == null || mLinearLayoutManager == null || mEstimateScroller == null)
            return;

        if (mScrollerContentHeight.isWorking())
            return;

        int totalItems = mAdapter.getItemCount();

        LinearLayoutManager layoutManager = mLinearLayoutManager;
        int botVisible = layoutManager.findLastVisibleItemPosition();
        if (botVisible == totalItems) {
            mScrollbar.setThumbPercent(1);
        } else {
            int topVisible = layoutManager.findFirstVisibleItemPosition();
            View topVisibleView = layoutManager.findViewByPosition(topVisible);
            int offset = 0;
            if (topVisibleView != null) {
                offset = -topVisibleView.getTop() + mEstimateScroller.getSpaceBetweenRows();// + topVisibleView.getY();
            }
            int topHeight = mScrollerContentHeight.getY(topVisible);

            float percent = (float) (topHeight + offset) / (mScrollerContentHeight.getTotalHeight() - getHeight());
//            LOG.d("t %-3d | tH %-4d | o %-5d | pc %-6.4f, tth %-5d",
//                    topVisible, topHeight, offset, percent, (mScrollerContentHeight.getTotalHeight() - getHeight()));
            percent = percent > 1 ? 1 : percent;
            mScrollbar.setThumbPercent(percent);
        }


        invalidateScrollbar();
    }

    private void onNotifyDataSetChanged() {
        mScrollbar.setAvailable(false);
        mScrollerContentHeight.update(mEstimateScroller);
    }

    void onScrollerContentHeightUpdated(){
        updateScrollbar();
        mScrollbar.setAvailable(true);
        if (getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING)
            mScrollbar.triggerVisible(true);
    }

    private void notifyDataSetChanged() {
        mControlHandler.removeMessages(MSG_NOTIFY_DATA_SET_CHANGED);
        mControlHandler.sendEmptyMessageDelayed(MSG_NOTIFY_DATA_SET_CHANGED, 400);
    }

    void invalidateScrollbar() {
        invalidate(mScrollbar.getInvalidateRect());
        if (mScrollbar.shouldInvalidatePopup())
            invalidate(mScrollbar.getPopupInvalidateRect());
    }

    public void setFastScrollListener(FastScrollListener fastScrollListener) {
        mFastScrollListener = fastScrollListener;
    }

    public interface FastScrollListener {
        void onFastScrollStart();

        void onFastScrollStop();
    }

    public interface SectionedAdapter {
        String getSectionName(int position);
    }
}
