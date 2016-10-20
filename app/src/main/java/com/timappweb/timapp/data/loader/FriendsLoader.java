package com.timappweb.timapp.data.loader;

import android.content.Context;

import com.timappweb.timapp.MyApplication;
import com.timappweb.timapp.adapters.FriendsAdapter;
import com.timappweb.timapp.data.models.User;
import com.timappweb.timapp.data.models.UserFriend;
import com.timappweb.timapp.sync.SyncAdapterOption;
import com.timappweb.timapp.sync.data.DataSyncAdapter;
import com.timappweb.timapp.utils.loaders.AutoModelLoader;
import com.timappweb.timapp.views.SwipeRefreshLayout;

/**
 * Created by Stephane on 18/08/2016.
 */
public class FriendsLoader extends SyncDataLoader<UserFriend, FriendsLoader> {

    private static final String TAG = "FriendsLoader";
    private static final long MIN_AUTO_REFRESH_DELAY = 3600 * 24 * 1000;     // Automatic refresh after one day
    private static final long MIN_FORCE_REFRESH_DELAY = 30 * 1000;            // Must wait 30 sec before reload

    // -----------------------------------------------------------------------------------------

    // TODO use factory
    public FriendsLoader(Context context, final FriendsAdapter adapter, SwipeRefreshLayout swipeAndRefreshLayout) {
        super(context);
        this.setHistoryItemInterface(MyApplication.getCurrentUser());
        this.setMinDelayForceRefresh(MIN_AUTO_REFRESH_DELAY);
        this.setMinDelayForceRefresh(MIN_FORCE_REFRESH_DELAY);
        this.setSwipeAndRefreshLayout(swipeAndRefreshLayout);
        this.setAdapter(adapter);
        this.setSyncOptions(new SyncAdapterOption()
                .setType(DataSyncAdapter.SYNC_TYPE_FRIENDS)
                .setHashId(historyItemInterface));
        this.setModelLoader(new AutoModelLoader(context, UserFriend.class, ((User)historyItemInterface).getFriendsQuery(), false));
    }

}
