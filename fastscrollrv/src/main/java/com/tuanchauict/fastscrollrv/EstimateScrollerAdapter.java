package com.tuanchauict.fastscrollrv;

/**
 * Created by tuanchauict on 11/21/16.
 * Apply to the Adapter
 */

public interface EstimateScrollerAdapter {
    int getItemCount();
    int getItemViewType(int position);
    int getColumn(int position);
    int getEstimateItemHeight(int type);
    int getSpaceBetweenRows();
}
