package com.tuanchauict.fastscrollrv;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tuanchauict on 11/21/16.
 */

class ScrollerContentHeight {
    private List<Integer> mScrollerHeights = new ArrayList<>();
    //    private List<Integer> mItemRowYs = new ArrayList<>();
    private List<Integer> mRowHeights = new ArrayList<>();
    private List<Integer> mRows = new ArrayList<>();
    private List<Integer> mRowIndexes = new ArrayList<>();
    private AtomicBoolean mWorking = new AtomicBoolean(false);
    private boolean mInitiated = false;
    private int mTotalHeight;

    private FastScrollRecyclerView mFastScrollRecyclerView;

    private AsyncTask<EstimateScrollerAdapter, Void, Void> updateTask = new AsyncTask<EstimateScrollerAdapter, Void, Void>() {
        @Override
        protected void onPreExecute() {
            mWorking.set(true);
        }

        @Override
        protected Void doInBackground(EstimateScrollerAdapter... estimateScrollerAdapters) {
            EstimateScrollerAdapter scroller = estimateScrollerAdapters[0];
            int rowHeight = 0;
            int scrollHeight = 0;
            mScrollerHeights.clear();
//            mItemRowYs.clear();
            mRowHeights.clear();
            mRowIndexes.clear();
            mRows.clear();
            int row = -1;

            int count = scroller.getItemCount();
            for (int i = 0; i < count; i++) {
                int c = scroller.getColumn(i);

                int h = scroller.getEstimateItemHeight(scroller.getItemViewType(i)) + scroller.getSpaceBetweenRows();
                if (c > 0) {
                    if (h > rowHeight) {
                        rowHeight = h;
                    }
                } else {
                    row += 1;
                    scrollHeight += rowHeight;
                    rowHeight = h;
                    mRowHeights.add(rowHeight);
                    mScrollerHeights.add(scrollHeight);
                    mRowIndexes.add(i);
                }

                mRows.add(row);

            }
            mTotalHeight = mScrollerHeights.get(mScrollerHeights.size() - 1) + rowHeight;

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            mWorking.set(false);
            mInitiated = true;

            mFastScrollRecyclerView.onScrollerContentHeightUpdated();
        }
    };

    ScrollerContentHeight(FastScrollRecyclerView fastScrollRecyclerView) {
        mFastScrollRecyclerView = fastScrollRecyclerView;
    }

    void update(EstimateScrollerAdapter scroller) {
        if (mWorking.get())
            return;
        updateTask.execute(scroller);

//        mWorking.set(true);
//        return Observable.create(subscriber -> {
////            long t = System.currentTimeMillis();
//            int rowHeight = 0;
//            int scrollHeight = 0;
//            mScrollerHeights.clear();
////            mItemRowYs.clear();
//            mRowHeights.clear();
//            mRowIndexes.clear();
//            mRows.clear();
//            int row = -1;
//
//            int count = scroller.getItemCount();
//            for (int i = 0; i < count; i++) {
//                int c = scroller.getColumn(i);
//
//                int h = scroller.getEstimateItemHeight(scroller.getItemViewType(i)) + scroller.getSpaceBetweenRows();
//                if (c > 0) {
//                    if (h > rowHeight) {
//                        rowHeight = h;
//                    }
//                } else {
//                    row += 1;
//                    scrollHeight += rowHeight;
//                    rowHeight = h;
//                    mRowHeights.add(rowHeight);
//                    mScrollerHeights.add(scrollHeight);
//                    mRowIndexes.add(i);
//                }
//
//                mRows.add(row);
//
//            }
//            mTotalHeight = mScrollerHeights.get(mScrollerHeights.size() - 1) + rowHeight;
//
//            mWorking.set(false);
//            subscriber.onNext(Boolean.TRUE);
//            subscriber.onCompleted();
//            mInitiated = true;
//        });
    }

    boolean isWorking() {
        return mInitiated && mWorking.get();
    }

    int getY(int pos) {
        if (pos < 0 || pos >= mRows.size())
            return 0;
        return mScrollerHeights.get(mRows.get(pos));
    }

    public int getRowHeight(int pos) {
        return mRowHeights.get(mRows.get(pos));
    }

    int getTotalHeight() {
        return mTotalHeight;
    }

    int getRow(int pos) {
        if (pos < 0 || pos >= mRows.size())
            return 0;
        return mRows.get(pos);
    }

    int getAbsoluteIndex(float percent) {
        int y = (int) (mTotalHeight * percent);
        int row = binarySearch(y);
        row = row >= mRowIndexes.size() ? mRowIndexes.size() - 1 : row;
        return mRowIndexes.get(row);
    }

    int getRowOffset(int index, float percent) {
        int row = getRow(index);
        int rowY = mScrollerHeights.get(row);
        int y = (int) (mTotalHeight * percent);

        return rowY - y;
    }

    private int binarySearch(int y) {
        List<Integer> ys = mScrollerHeights;
        int lo = 0;
        int hi = ys.size() - 1;
        int mid;
        int midV;
        while (hi >= lo) {
            mid = (lo + hi) >> 1;
            midV = ys.get(mid);
            if (midV < y) {
                lo = mid + 1;
            } else if (midV > y) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return lo;
    }
}
