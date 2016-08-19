package com.timappweb.timapp.views;

import android.content.Context;
import android.support.v4.widget.*;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.timappweb.timapp.listeners.OnScrollListenerRefreshableView;

import java.security.InvalidParameterException;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

/**
 * Created by stephane on 5/14/2016.
 */
public class RefreshableRecyclerView extends RecyclerView {

    private WaveSwipeRefreshLayout mSwipeRefreshLayout;

    public RefreshableRecyclerView(Context context) {
        super(context);
    }

    public RefreshableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RefreshableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.init();
    }

    public void init(){
        this.mSwipeRefreshLayout = (WaveSwipeRefreshLayout) getParent();
        if (this.mSwipeRefreshLayout == null) throw new InvalidParameterException("Refreshable recycler view should be wrap with a WaveSwipeRefreshLayout");
        this.addOnScrollListener(new OnScrollListenerRefreshableView(mSwipeRefreshLayout));
    }


    @Override
    public void setAdapter(Adapter adapter) {
        // Wrap with a new adapter that have another item type which is NO DATA
        super.setAdapter(adapter);
    }
}
