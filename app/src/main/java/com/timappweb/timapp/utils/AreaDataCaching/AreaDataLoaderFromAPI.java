package com.timappweb.timapp.utils.AreaDataCaching;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.maps.android.clustering.ClusterManager;
import com.timappweb.timapp.R;
import com.timappweb.timapp.activities.DrawerActivity;
import com.timappweb.timapp.adapters.PlacesAdapter;
import com.timappweb.timapp.entities.Place;
import com.timappweb.timapp.fragments.ExploreFragment;
import com.timappweb.timapp.fragments.ExploreMapFragment;
import com.timappweb.timapp.rest.QueryCondition;
import com.timappweb.timapp.rest.RestCallback;
import com.timappweb.timapp.rest.RestClient;
import com.timappweb.timapp.utils.IntPoint;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;


/**
 * Created by stephane on 12/9/2015.
 */
public class AreaDataLoaderFromAPI implements AreaDataLoaderInterface<Place> {

    private static final String TAG = "AreaDataLoaderFromAPI";
    private Context mContext;
    private ExploreFragment exploreFragment;

    private ClusterManager<Place> mClusterManagerPost = null;
    private int requestCounter = 0;
    private int lastClear = -1;
    private PlacesAdapter placesAdapter = null;

    public AreaDataLoaderFromAPI(Context context) {
        this.mContext = context;
    }

    public AreaDataLoaderFromAPI(Context context, ExploreFragment fragment) {
        this.mContext = context;
        this.exploreFragment = fragment;
    }

    public void setClusterManager(ClusterManager<Place> mClusterManagerPost) {
        this.mClusterManagerPost = mClusterManagerPost;
    }

    public void setPlacesAdapter(PlacesAdapter placesAdapter) {
        this.placesAdapter = placesAdapter;
    }

    public AreaDataLoaderFromAPI(Context context, ClusterManager<Place> clusterManagerPost) {
        this.mContext = context;
        this.mClusterManagerPost = clusterManagerPost;
    }

    @Override
    public void load(final IntPoint pCpy, final AreaRequestItem request, QueryCondition conditions) {
        final ExploreMapFragment exploreMapFragment = exploreFragment.getExploreMapFragment();
        exploreMapFragment.setLoaderVisibility(true);

        conditions.setTimeRange(ExploreMapFragment.getDataTimeRange());
        conditions.setMainTags(true);
        final int requestId = this.requestCounter++;

        Call<List<Place>> call = RestClient.service().bestPlaces(conditions.toMap());

        final int itemRequestId = request.setPendingCall(call);
        Log.i(TAG, "Request loading of area " + conditions.toString()+ ". Request id: " + itemRequestId);


        call.enqueue(new RestCallback<List<Place>>(mContext) {

            @Override
            public void onFailure(Throwable t) {
                exploreMapFragment.setLoaderVisibility(false);
                Toast.makeText(mContext, R.string.cannot_load_events, Toast.LENGTH_SHORT).show();
                super.onFailure(t);
            }

            @Override
            public void onResponse(Response<List<Place>> response) {
                super.onResponse(response);

                if (request.isOutdated(itemRequestId)){
                    Log.d(TAG, "Outdated request. Do not load tags");
                    exploreMapFragment.setLoaderVisibility(false);
                    Toast.makeText(mContext, R.string.cannot_load_events, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.isSuccess()){
                    List<Place> places = response.body();
                    exploreMapFragment.setLoaderVisibility(false);
                    Log.i(TAG, "WS loaded tags done. Loaded " + places.size() + " result(s). " + " for point " + pCpy);
                    // Test if request is out dated
                    if (requestId <= lastClear) {
                        Log.d(TAG, "This request is out dated");
                        return;
                    }

                    // Setting the last timestamp retrieved from the server
                    // TODO what happens if two row have the same timestamp for the same area ?
                    request.setDataTimestamp(
                            places.size() > 1
                                    ? places.get(places.size() - 1).created
                                    : 0);

                    request.appendData(places);

                    if (mClusterManagerPost != null){
                        mClusterManagerPost.addItems(places);
                        mClusterManagerPost.cluster();
                    }

                    if (placesAdapter != null){
                        placesAdapter.clear();
                        placesAdapter.addAll(places);
                    }
                }
            }
        });
    }

    @Override
    public void clear(List<Place> data) {
        if (mClusterManagerPost != null){
            for (Place place: data){
                mClusterManagerPost.removeItem(place);
            }
        }
        if (placesAdapter != null){
            placesAdapter.clear();
        }
    }

    @Override
    public void clearAll() {
        lastClear = this.requestCounter;        // Every request id that is less or equal is out dated

        if (mClusterManagerPost != null){
            mClusterManagerPost.clearItems();
        }
        if (placesAdapter != null){
            placesAdapter.clear();
        }
    }
}
