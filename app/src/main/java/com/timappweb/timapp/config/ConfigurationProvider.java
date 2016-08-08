package com.timappweb.timapp.config;

import android.content.Context;
import android.util.Log;

import com.activeandroid.query.Select;
import com.timappweb.timapp.data.entities.ApplicationRules;
import com.timappweb.timapp.data.models.EventCategory;
import com.timappweb.timapp.data.models.SpotCategory;
import com.timappweb.timapp.rest.callbacks.RemoteMasterSyncCallback;
import com.timappweb.timapp.rest.managers.MultipleHttpCallManager;
import com.timappweb.timapp.rest.RestClient;
import com.timappweb.timapp.rest.callbacks.HttpCallback;
import com.timappweb.timapp.rest.callbacks.HttpCallbackBase;
import com.timappweb.timapp.rest.callbacks.RequestFailureCallback;
import com.timappweb.timapp.utils.KeyValueStorage;

import java.util.List;

import retrofit2.Response;

/**
 * Created by stephane on 1/24/2016.
 *
 * Manage application configuration coming from the server.
 * Request configuration via HTTP request if not present or if not up to date
 *
 */
public class ConfigurationProvider {

    private static final String TAG = "ConfigurationProvider";
    private static final String KEY_LAST_UPDATE = "configuration_last_update_time";
    private static final String KEY_APP_RULES = "configuration_rules";

    public static final String CALL_ID_APPLICATION_RULES = "app_rule";
    public static final String CALL_ID_SPOT_CATEGORIES = "spot_categories";
    public static final String CALL_ID_EVENT_CATEGORIES = "event_categories";
    private static final long UPDATE_CONF_MAX_DELAY = 6 * 3600 * 1000;

    private static HttpCallback listener;
    private static ApplicationRules applicationRules;
    private static List<EventCategory> eventCategories = null;
    private static List<SpotCategory> spotCategories = null;
    private static HttpCallback callback;
    private static RequestFailureCallback errorCallback;


    public static List<EventCategory> eventCategories(){
        if (eventCategories == null){
            eventCategories = new Select().from(EventCategory.class).orderBy("Position ASC").execute();
        }
        if (eventCategories == null || eventCategories.size() == 0){
            throw new IncompleteConfigurationException("Missing spot categories");
        }
        return eventCategories;
    }

    /**
     *
     * @return
     */
    public static List<SpotCategory> spotCategories(){
        if (spotCategories == null){
            spotCategories = new Select().from(SpotCategory.class).orderBy("Position ASC").execute();
        }
        if (spotCategories == null || spotCategories.size() == 0){
            throw new IncompleteConfigurationException("Missing spot categories");
        }
        return spotCategories;
    }

    /**
     *
     * @return
     */
    public static ApplicationRules rules(){
        if (applicationRules == null){
            applicationRules = KeyValueStorage.instance.get(KEY_APP_RULES, ApplicationRules.class);
            if (applicationRules == null){
                throw new IncompleteConfigurationException("Missing application rules");
            }
        }
        return applicationRules;
    }

    /**
     * load configuration:
     *      - if configuration is not up to date or not present it will start a SYNC immediately
     *      - Otherwise call the callback directly
     */
    public static MultipleHttpCallManager load(Context context){

        MultipleHttpCallManager callManager = RestClient.mulipleCallsManager();

        if (!hasFullConfiguration() || !ConfigurationProvider.dataUpToDate()){
            callManager.addCall(CALL_ID_APPLICATION_RULES, RestClient.service().applicationRules())
                    .onResponse(new HttpCallback<ApplicationRules>() {
                        @Override
                        public void successful(ApplicationRules feedback) {
                            ConfigurationProvider.setApplicationRules(feedback);
                        }
                    });
            callManager.addCall(CALL_ID_SPOT_CATEGORIES, RestClient.service().spotCategories())
                    .onResponse(new RemoteMasterSyncCallback(SpotCategory.class, new Select().from(SpotCategory.class)));
            callManager.addCall(CALL_ID_EVENT_CATEGORIES, RestClient.service().eventCategories())
                    .onResponse(new RemoteMasterSyncCallback(EventCategory.class, new Select().from(EventCategory.class)));
        }
        else{
            Log.i(TAG, "Configuration exists and is up to date");
        }
        return callManager;
    }

    private static boolean dataUpToDate() {
        long lastUpdate = KeyValueStorage.instance.getSafeLong(KEY_LAST_UPDATE, 0);
        return System.currentTimeMillis() - lastUpdate <= UPDATE_CONF_MAX_DELAY;
    }

    public static void updateLastUpdateTime() {
        Log.d(TAG, "Updating last configuration time");
        KeyValueStorage.in().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).commit();
    }

    /**
     *
     * @return
     */
    public static boolean hasFullConfiguration(){
        try{
            eventCategories();
            spotCategories();
            return rules().places_max_name_length > 0;
        }
        catch (IncompleteConfigurationException ex){
            return false;
        }
    }

    public static void setApplicationRules(ApplicationRules applicationRules) {
        ConfigurationProvider.applicationRules = applicationRules;
        KeyValueStorage.instance.set(KEY_APP_RULES, applicationRules);
    }



    @Override
    public String toString() {
        return "ServerConfiguration{" +
                ", rules=" + rules() +
                //", event categories= " + eventCatagoriesManager.toString() +
                //", spot categories= " + spotCatagoriesManager.toString() +
                '}';
    }

    public static SpotCategory getSpotCategoryByRemoteId(long remoteId) {
        for (SpotCategory category: spotCategories()){
            if (category.remote_id == remoteId){
                return category;
            }
        }
        return null;
    }

    public static EventCategory getEventCategoryByRemoteId(long id) {
        for (EventCategory category: eventCategories()){
            if (category.remote_id == id){
                return category;
            }
        }
        return null;
    }

    public static void onSyncError(Throwable e) {
        HttpCallbackBase.dispatchError(e, ConfigurationProvider.errorCallback);
    }

    public static void onSyncResponse(Response response) {
        HttpCallbackBase.dispatchResponse(response, ConfigurationProvider.callback);
    }

    private static class IncompleteConfigurationException extends Error {
        public IncompleteConfigurationException(String s) {
            super(s);
        }
    }

}
