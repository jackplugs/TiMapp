package com.timappweb.timapp.map;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.MarkerManager;
import com.timappweb.timapp.entities.Spot;

/**
 * Created by stephane on 9/12/2015.
 */
public class ClusterManager extends  com.google.maps.android.clustering.ClusterManager<Spot>{

    public ClusterManager(Context context, GoogleMap map) {
        super(context, map);
    }

    public ClusterManager(Context context, GoogleMap map, MarkerManager markerManager) {
        super(context, map, markerManager);
    }


}
