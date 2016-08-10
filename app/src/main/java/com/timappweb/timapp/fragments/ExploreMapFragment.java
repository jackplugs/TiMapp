package com.timappweb.timapp.fragments;

import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.timappweb.timapp.MyApplication;
import com.timappweb.timapp.R;
import com.timappweb.timapp.activities.DrawerActivity;
import com.timappweb.timapp.config.IntentsUtils;
import com.timappweb.timapp.data.entities.MarkerValueInterface;
import com.timappweb.timapp.data.models.Event;
import com.timappweb.timapp.databinding.FragmentExploreMapBinding;
import com.timappweb.timapp.listeners.OnExploreTabSelectedListener;
import com.timappweb.timapp.listeners.OnItemAdapterClickListener;
import com.timappweb.timapp.map.EventClusterRenderer;
import com.timappweb.timapp.map.MapFactory;
import com.timappweb.timapp.map.RemovableNonHierarchicalDistanceBasedAlgorithm;
import com.timappweb.timapp.utils.AreaDataCaching.AreaRequestHistory;
import com.timappweb.timapp.utils.AreaDataCaching.AreaRequestItem;
import com.timappweb.timapp.utils.AreaDataCaching.AreaRequestItemFactory;
import com.timappweb.timapp.utils.location.LocationManager;
import com.timappweb.timapp.utils.location.MyLocationProvider;
import com.timappweb.timapp.views.HorizontalTagsRecyclerView;
import com.timappweb.timapp.views.SimpleTimerView;

import java.util.List;

public class ExploreMapFragment extends Fragment implements OnExploreTabSelectedListener, LocationManager.LocationListener, OnMapReadyCallback {

    private static final String             TAG                             = "GoogleMapFragment";
    private static final long               TIME_WAIT_MAP_VIEW              = 500;
    private static final int                MARGIN_BUTTON_LOCATE_MAP        = 120;
    private static final int                PADDING__MAP                    = 30;
    private float                           ZOOM_LEVEL_CENTER_MAP           = 12.0f;

    // ---------------------------------------------------------------------------------------------

    enum ZoomType {IN, OUT, NONE};
    private ZoomType                        currentZoomMode                 = ZoomType.NONE;
    private float                           previousZoomLevel = -1;
    private float                           currentZoomLevel = -1;

    // Declare a variable for the cluster manager.
    private ClusterManager<Event>           mClusterManagerPost;
    private GoogleMap                       gMap = null;
    private MapView                         mapView = null;

    private View                            root;
    private View                            eventView;
    private View                            progressView;
    private HorizontalTagsRecyclerView      filterTagsRv;
    private View                            filterTagsContainer;
    private SimpleTimerView                 tvCountPoints;

    private ExploreFragment                 exploreFragment;
    private FragmentExploreMapBinding       mBinding;
    private AreaRequestHistory              history;
    private Bundle                          mapBundle;


    private boolean                         needCenterMap           = true;
    // ---------------------------------------------------------------------------------------------

    public AreaRequestHistory getHistory() {
        return history;
    }

    @Override
    public void onLocationChanged(Location newLocation, Location lastLocation) {
        if (needCenterMap){
            centerMap(newLocation);
        }
    }

    @Override
    public void onTabSelected() {

    }

    // ---------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_explore_map, container, false);
        root = mBinding.getRoot();
        exploreFragment = (ExploreFragment) getParentFragment();
        setHasOptionsMenu(true);
        mapView = (MapView) root.findViewById(R.id.map);
        mapView.onCreate(mapBundle);
        progressView = root.findViewById(R.id.progress_view);
        filterTagsRv = (HorizontalTagsRecyclerView) root.findViewById(R.id.search_tags);
        filterTagsContainer = root.findViewById(R.id.search_tags_container);
        tvCountPoints = (SimpleTimerView) root.findViewById(R.id.white_points_text);

        eventView = root.findViewById(R.id.event_view);
        eventView.setVisibility(View.GONE);
        initListeners();
        return root;
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
        LocationManager.removeLocationListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (gMap == null){
            mapView.getMapAsync(this);
        }
        initLocationButton();
        updateFilterView();
        LocationManager.addOnLocationChangedListener(this);
    }

    private void initLocationButton() {
        // Get the button view
        View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
        rlp.addRule(RelativeLayout.ALIGN_END, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rlp.setMargins(0,0,0,MARGIN_BUTTON_LOCATE_MAP);
        mapView.getMap().setPadding(PADDING__MAP, 0, 0, 0);
    }

    // ---------------------------------------------------------------------------------------------

    public void setLoaderVisibility(boolean bool) {
        if(progressView!=null) {
            if(bool) {
                progressView.setVisibility(View.VISIBLE);
            }
            else {
                progressView.setVisibility(View.GONE);
            }
        }
    }

    private void displayEvent(Event event) {
        // TODO can be removed later when all data are synchronized localy...
        if (!event.hasLocalId()) event.mySave();
        mBinding.setEvent(event);
        final Animation slideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_up);
        eventView.startAnimation(slideIn);
        eventView.setVisibility(View.VISIBLE);
        tvCountPoints.cancelTimer();
        tvCountPoints.initTimer(event.getPoints()*1000);
        //TODO Steph : might be better to initialize the timer through databinding
    }

    public void hideEvent() {
        final Animation slideOut = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_down);
        eventView.startAnimation(slideOut);
        eventView.setVisibility(View.GONE);
    }


    public void updateFilterView() {
        if(isFilterActive()) {
            filterTagsContainer.setVisibility(View.VISIBLE);
            filterTagsRv.getAdapter().setData(MyApplication.searchFilter.tags);
            Log.d(TAG,"Number of tags filtered : " + MyApplication.searchFilter.tags.size());
            //mapView.getMap().setPadding(0, MARGIN_BUTTON_LOCATE_MAP, 0, 0);
        } else {
            filterTagsContainer.setVisibility(View.GONE);
            //mapView.getMap().setPadding(0, 0, 0, 0);
        }
        getActivity().invalidateOptionsMenu();
    }

    public boolean isFilterActive() {
        return MyApplication.searchFilter.tags!=null && MyApplication.searchFilter.tags.size()!=0;
    }

    private void initListeners() {
        eventView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentsUtils.viewSpecifiedEvent(getActivity(), mBinding.getEvent());
            }
        });

        filterTagsRv.getAdapter().setItemAdapterClickListener(new OnItemAdapterClickListener() {
            @Override
            public void onClick(int position) {
                IntentsUtils.filter(getActivity());
            }
        });

        mapView.getMap().setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                hideEvent();
            }
        });
    }


    // ---------------------------------------------------------------------------------------------

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "Map is now ready!");
        gMap = googleMap;
        MapFactory.initMap(gMap);
        setUpClusterer();
        initAreaRequestHistory();
        exploreFragment.getDataLoader().setAreaRequestHistory(this.history);

        if (LocationManager.hasLastLocation()){
            centerMap(LocationManager.getLastLocation());
        }
    }


    private void initAreaRequestHistory(){
        history = new AreaRequestHistory(exploreFragment.getDataLoader());
        history.setAreaRequestItemFactory(new AreaRequestItemFactory() {
            @Override
            public AreaRequestItem build() {
                AreaRequestItem<Event> requestItem = new AreaRequestItem<Event>();
                requestItem.setListener(new AreaRequestItem.OnDataChangeListener() {
                    @Override
                    public void onDataChange() {
                        Log.d(TAG, "AreaRequestItem.onDataChange(): ");
                        // Each time data change we reset everything
                        List<Event> events = history.getInsideBoundsItems(getMapBounds());
                        mClusterManagerPost.clearItems();
                        mClusterManagerPost.addItems(events);
                        mClusterManagerPost.cluster();
                    }
                });
                return requestItem;
            }
        });
    }


    private void centerMap(Location location){
        centerMap(location, null);
    }
    private void centerMap(Location location,  GoogleMap.CancelableCallback  callback){
        needCenterMap = false;
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(MyLocationProvider.convert(location), ZOOM_LEVEL_CENTER_MAP), callback);
    }

    /**
     * TODO: check if we need to send back a copy instead ?
     * @return
     */
    public LatLngBounds getMapBounds(){
        /*
        LatLngBounds cpyBounds;
        LatLngBounds bounds = mapView.getMap().getProjection().getVisibleRegion().latLngBounds;
        synchronized (bounds){
            cpyBounds = new LatLngBounds(bounds.southwest, bounds.northeast);
        }*/
        return  mapView.getMap().getProjection().getVisibleRegion().latLngBounds;
    }

    public void updateMapData(){
        final LatLngBounds bounds = getMapBounds();
        if (bounds == null) return;
        Log.d(TAG, "Map bounds: " + bounds.southwest + " " + bounds.southwest);

        if (!history.isInitialized() || currentZoomMode == ZoomType.OUT){
            // Remove previous cache and all markers
            history.resizeArea(bounds);
        }
        history.update(bounds);
    }

    private void setUpClusterer(){
        Log.i(TAG, "Setting up cluster!");

        final GoogleMap map = mapView.getMap();

        // Initialize the manager with the context and the map.
        mClusterManagerPost = new ClusterManager<>(getActivity(), map);
        mClusterManagerPost.setRenderer(new EventClusterRenderer(getActivity(), map, mClusterManagerPost));
        mClusterManagerPost.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<Event>() {
            @Override
            public boolean onClusterClick(Cluster<Event> cluster) {

                // If zoom level is too big, go to list (TODO global parameter)
                if (currentZoomLevel > map.getMaxZoomLevel() - 2){
                    // TODO JEAN: AFFICHE LA LISTE SINON
                    //((ExploreFragment)getParentFragment()).getViewPager().setCurrentItem(1);
                    return true;
                }

                Log.d(TAG, "You clicked on a cluster");
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                //builder.include(cluster.getPosition());
                for (Event m : cluster.getItems()) {
                    builder.include(m.getPosition());
                }
                LatLngBounds bounds = builder.build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 150);
                map.animateCamera(cameraUpdate);

                hideEvent();
                return true;
            }
        });
        mClusterManagerPost.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<Event>() {
            @Override
            public boolean onClusterItemClick(Event item) {
                Log.d(TAG, "You clicked on a cluster item: " + item);
                showMarkerDetail(item);
                return true;
            }

        });
        map.setOnMarkerClickListener(mClusterManagerPost);
        map.setOnCameraChangeListener(new OnCameraChangeListener());
        mClusterManagerPost.setAlgorithm(new RemovableNonHierarchicalDistanceBasedAlgorithm<Event>());

        this.exploreFragment.getDataLoader().setClusterManager(mClusterManagerPost);
    }

    // ---------------------------------------------------------------------------------------------

    private void showMarkerDetail(MarkerValueInterface markerValue){
        Event event = (Event) markerValue;
        if(isPlaceViewVisible() && mBinding.getEvent() == event) {
            IntentsUtils.viewSpecifiedEvent(getActivity(), event);
        } else {
            MarkerOptions markerOptions = ((Event) markerValue).getMarkerOption();

            ImageView pin = new ImageView(getContext());
            pin.setImageResource(R.drawable.pin);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(getResources().getIdentifier("pin","drawable", getContext().getPackageName())));

            displayEvent(event);
        }
    }

    public boolean isPlaceViewVisible() {
        return eventView.getVisibility()==View.VISIBLE;
    }

    private class OnCameraChangeListener implements GoogleMap.OnCameraChangeListener{

        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            Log.d(TAG, "Zoom level is now: " + cameraPosition.zoom);

            previousZoomLevel = currentZoomLevel;
            currentZoomLevel = cameraPosition.zoom;
            // Register zoom updates
            if (previousZoomLevel == currentZoomLevel) {
                currentZoomMode = ZoomType.NONE;
            } else if (previousZoomLevel > currentZoomLevel) {
                currentZoomMode = ZoomType.OUT;
            } else {
                currentZoomMode = ZoomType.IN;
            }

            previousZoomLevel = cameraPosition.zoom;
            mClusterManagerPost.onCameraChange(cameraPosition);

            // Updating datas
            updateMapData();
        }
    }


}
