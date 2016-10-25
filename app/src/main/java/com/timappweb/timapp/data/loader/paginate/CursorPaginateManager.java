package com.timappweb.timapp.data.loader.paginate;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.timappweb.timapp.MyApplication;
import com.timappweb.timapp.R;
import com.timappweb.timapp.adapters.flexibleadataper.ExpandableHeaderItem;
import com.timappweb.timapp.adapters.flexibleadataper.MyFlexibleAdapter;
import com.timappweb.timapp.data.loader.RecyclerViewManager;
import com.timappweb.timapp.data.models.MyModel;

import java.util.List;

import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.ISectionable;

/**
 * Created by Stephane on 06/09/2016.
 */
public class CursorPaginateManager<DataType extends MyModel>
        extends RecyclerViewManager<CursorPaginateManager<DataType>>
        implements CursorPaginateDataLoader.Callback<DataType> {

    private static final String TAG = "CursorPaginateManager";
    private final CursorPaginateDataLoader<DataType, ?> mDataLoader;
    private long minDelayForceRefresh   = -1;
    private long lastRefresh = -1;
    private ExpandableHeaderItem expandableHeaderItem;
    private boolean clearOnRefresh = false;
    private CursorPaginateDataLoader.Callback<DataType> callback;

    //private long minDelayAutoRefresh    = -1;

    public CursorPaginateManager(Context context, MyFlexibleAdapter adapter, CursorPaginateDataLoader<DataType, ?> dataLoader) {
        super(context, adapter);
        this.mDataLoader = dataLoader;
        this.mDataLoader.setCallback(this);
    }

    public CursorPaginateManager<DataType> setSubSection(ExpandableHeaderItem expandableHeaderItem){
        this.expandableHeaderItem = expandableHeaderItem;
        return this;
    }

    public CursorPaginateManager<DataType> setMinDelayForceRefresh(long minDelayForceRefresh) {
        this.minDelayForceRefresh = minDelayForceRefresh;
        return this;
    }

    /*
    public CursorPaginateManager<DataType> setMinDelayAutoRefresh(long minDelayAutoRefresh) {
        this.minDelayAutoRefresh = minDelayAutoRefresh;
        return this;
    }*/


    // ---------------------------------------------------------------------------------------------

    @Override
    public void onRefresh() {
        if ( this.minDelayForceRefresh == -1 || this.lastRefresh == -1 ||
                ((this.lastRefresh + this.minDelayForceRefresh) > System.currentTimeMillis())){
            if (clearOnRefresh){
                this.mDataLoader.deleteCache();
                this.clearItems();
                this.mDataLoader.loadNext();
            }
            else{
                this.mDataLoader.update();
            }
        }
        else{
            Log.d(TAG, "Data up to date. Last update was: " + (this.lastRefresh !=  -1 ? ((System.currentTimeMillis() - this.lastRefresh)/1000) + " seconds ago" : " NEVER"));
            Toast.makeText(MyApplication.getApplicationBaseContext(), R.string.data_already_refresh, Toast.LENGTH_SHORT).show();
            setRefreshing(false);
        }
    }

    private void clearItems() {
        if (expandableHeaderItem != null){
            this.mAdapter.removeItems(expandableHeaderItem);
        }
        else{
            mAdapter.removeAll();
        }
    }

    @Override
    public void onLoadMore() {
        this.mDataLoader.loadNext();
    }

    @Override
    public void onLoadStart(CursorPaginateDataLoader.LoadType loadType) {
        switch (loadType){
            case NEXT:
                break;
            case UPDATE:
                this.lastRefresh = System.currentTimeMillis();
                setRefreshing(true);
                break;
            case PREV:
                setRefreshing(true);
                break;
        }
        if (this.callback != null) this.callback.onLoadStart(loadType);
    }

    // ---------------------------------------------------------------------------------------------


    @Override
    public void onLoadEnd(List<DataType> data, CursorPaginateDataLoader.LoadType type, boolean overwrite) {
        List<AbstractFlexibleItem> items = null;
        if (data != null){
            items = mItemTransformer.transform(data);
        }

        if (overwrite){
            Log.i(TAG, "Removing already loaded data");
            this.clearItems();
        }

        setRefreshing(false);
        switch (type){
            case NEXT:
                mAdapter.onLoadMoreComplete(items);
                break;
            case UPDATE:
                if (items != null) {
                    int i = 0;
                    for (AbstractFlexibleItem item : items) {
                        if (mAdapter.contains(item)) {
                            Log.d(TAG, "Updating existing item");
                            mAdapter.updateItem(item, null);
                        } else {
                            if (expandableHeaderItem != null){
                                mAdapter.addSubItem(expandableHeaderItem, (ISectionable) item);
                            }
                            else{
                                mAdapter.addItem(i++, item);
                            }
                        }
                    }
                }
                break;
            case PREV:
                if (items != null) {
                    if (expandableHeaderItem != null){
                        for (AbstractFlexibleItem item: items){
                            mAdapter.addSubItem(expandableHeaderItem, (ISectionable) item);
                        }
                    }
                    else{
                        mAdapter.addBeginning(items);
                    }
                }
                break;
        }

        if (!mAdapter.hasData()){
            if (this.noDataCallback != null) this.noDataCallback.run();
        }

        if (this.callback != null) this.callback.onLoadEnd(data, type, overwrite);
    }

    @Override
    public void onLoadError(Throwable error, CursorPaginateDataLoader.LoadType loadType) {
        setRefreshing(false);
        switch (loadType){
            case NEXT:
                mAdapter.onLoadMoreComplete(null);
                break;
            /*
            case UPDATE:
                setRefreshing(false);
                break;
            case PREV:
                break;*/
        }
        if (this.callback != null) this.callback.onLoadError(error, loadType);
    }


    public CursorPaginateManager<DataType> load() {
        //if (this.mDataLoader.isFirstLoad()){
        setRefreshing(true);
        //}
        this.mDataLoader.loadNext();
        return this;
    }

    public CursorPaginateManager<DataType> setClearOnRefresh(boolean clearOnRefresh) {
        this.clearOnRefresh = clearOnRefresh;
        return this;
    }

    public CursorPaginateManager<DataType> setCallback(CursorPaginateDataLoader.Callback<DataType> callback) {
        this.callback = callback;
        return this;
    }
}
