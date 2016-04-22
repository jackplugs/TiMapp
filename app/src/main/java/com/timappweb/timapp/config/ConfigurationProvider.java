package com.timappweb.timapp.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.timappweb.timapp.entities.Category;
import com.timappweb.timapp.entities.SpotCategory;
import com.timappweb.timapp.rest.RestClient;
import com.timappweb.timapp.rest.services.ConfigInterface;
import com.timappweb.timapp.serversync.RESTRemoteSync;
import com.timappweb.timapp.serversync.RemotePersistenceManager;
import com.timappweb.timapp.serversync.SharedPrefSync;
import com.timappweb.timapp.serversync.SyncConfigManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by stephane on 1/24/2016.
 */
public class ConfigurationProvider {

    private static final String TAG = "ConfigurationProvider";
    private static final String PREF_NAME = "com.timapp.pref";

    private static final int CONFIG_ID_RULES = 1;
    private static final int CONFIG_ID_EVENT_CATEGORIES= 2;
    private static final int CONFIG_ID_SPOT_CATEGORIES = 3;

    private final SharedPreferences sharedPref;
    private final Context context;
    private final Listener listener;
    private SyncConfigManager<List<Category>> eventCatagoriesManager;
    private SyncConfigManager<List<SpotCategory>> spotCatagoriesManager;
    private SyncConfigManager<Rules> rulesManager;

    int SHARED_PREF_PRIVATE_MODE = 0;

    public List<Category> eventCategories(){
        return this.eventCatagoriesManager.getData();
    }

    public List<SpotCategory> spotCategories(){
        return this.spotCatagoriesManager.getData();
    }

    public Rules rules(){
        try {
            return this.rulesManager.getData();
        }
        catch (Exception ex){
            throw new InvalidConfigurationException();
        }
    }

    public ConfigurationProvider(Context context, Listener listener) {
        this.context = context;
        this.sharedPref = context.getSharedPreferences(PREF_NAME, SHARED_PREF_PRIVATE_MODE);
        this.listener = listener;

        this.init();
    }

    private <T> SyncConfigManager buildConfManager(int id, String path, Class<T> dataClass){
        return new SyncConfigManager<>(
                id,
                new RESTRemoteSync(path, RestClient.instance().createService(ConfigInterface.class)),
                new SharedPrefSync("config_" + id, sharedPref, dataClass));
    }

    private void init(){
        eventCatagoriesManager = buildConfManager(CONFIG_ID_EVENT_CATEGORIES, "event_categories", List.class);
        spotCatagoriesManager = buildConfManager(CONFIG_ID_SPOT_CATEGORIES, "spot_categories", List.class);
        rulesManager = buildConfManager(CONFIG_ID_RULES, "rules", Rules.class);
    }

    public AsyncTask<Integer, Integer, Boolean> load() {
        Log.d(TAG, "Start sync configuration from server...");
        AsyncTask<Integer, Integer, Boolean> loadTask = new AsyncTask<Integer, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Integer... params) {
                try {
                    eventCatagoriesManager.sync();
                    spotCatagoriesManager.sync();
                    rulesManager.sync();
                } catch (RemotePersistenceManager.CannotLoadException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                if (!success){
                    listener.onFail();
                }
                else{
                    listener.onLoaded();
                }
            }
        };
        return loadTask.execute();
    }

    public void clear() {
        eventCatagoriesManager.clear();
        spotCatagoriesManager.clear();
        rulesManager.clear();
    }

    public interface Listener{
        void onLoaded();
        void onFail();
    }

    @Override
    public String toString() {
        return "ServerConfiguration{" +
                ", rules=" + rulesManager +
                ", event categories= " + eventCatagoriesManager.toString() +
                ", spot categories= " + spotCatagoriesManager.toString() +
                '}';
    }


    public class Rules {
        public Rules() {
            this.places_points_levels = new LinkedList<>();
        }

        public int max_invite_per_request = 20;
        public int picture_max_size;
        public int picture_max_width;
        public int picture_max_height;
        public List<Integer> places_points_levels;
        public int place_max_reachable = 500;
        public int tags_suggest_limit = 40;
        public int places_populars_limit = 20;
        public int places_min_delay_add = 60;
        public int places_users_min_delay_add  = 60;
        public int posts_min_tag_number = 3;
        public int posts_max_tags_number = 3;
        public int tags_min_name_length = 2;
        public int tags_max_name_length = 30;
        public String tags_name_regex = "";
        public int gps_min_time_delay = 60000;
        public int gps_min_accuracy_add_place = 3500;
        public int gps_min_accuracy = 3500;
        public int places_min_name_length = 3;
        public int places_max_name_length;
        public int tags_min_search_length = 0;

        public String toString(){
            return "Rules{" +
                    ", places_points_levels=" + places_points_levels +
                    ", place_max_reachable=" + place_max_reachable +
                    ", tags_suggest_limit=" + tags_suggest_limit +
                    ", places_populars_limit=" + places_populars_limit +
                    ", places_min_delay_add=" + places_min_delay_add +
                    ", places_users_min_delay_add=" + places_users_min_delay_add +
                    ", posts_min_tag_number=" + posts_min_tag_number +
                    ", posts_max_tags_number=" + posts_max_tags_number +
                    ", tags_min_name_length=" + tags_min_name_length +
                    ", tags_max_name_length=" + tags_max_name_length +
                    ", tags_name_regex='" + tags_name_regex + '\'' +
                    ", gps_min_time_delay=" + gps_min_time_delay +
                    ", gps_min_accuracy_add_place=" + gps_min_accuracy_add_place +
                    ", gps_min_accuracy=" + gps_min_accuracy +
                    ", places_min_name_length=" + places_min_name_length +
                    ", tags_min_search_length=" + tags_min_search_length +
                    '}';
        }
    }

    private class InvalidConfigurationException extends Error {

        public InvalidConfigurationException() {
            // TODO flush error
        }
    }
}
