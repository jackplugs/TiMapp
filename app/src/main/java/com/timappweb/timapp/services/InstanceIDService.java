package com.timappweb.timapp.services;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;
import com.timappweb.timapp.MyApplication;
import com.timappweb.timapp.config.Constants;
import com.timappweb.timapp.rest.RestCallback;
import com.timappweb.timapp.rest.RestClient;
import com.timappweb.timapp.rest.RestFeedbackCallback;
import com.timappweb.timapp.rest.model.RestFeedback;

import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;

/**
 * Created by stephane on 4/3/2016.
 */
public class InstanceIDService extends InstanceIDListenerService {

    private static final String TAG = "InstanceIDService";
    private final InstanceID iid;
    private ArrayList<TokenItem> tokens;
    
    public void onTokenRefresh() {
        refreshAllTokens();
    }

    public InstanceIDService() {
        this.tokens = new ArrayList<>();
        iid = InstanceID.getInstance(this);
    }

    private void refreshAllTokens() {
        // assuming you have defined TokenList as
        // some generalized store for your tokens
        for(TokenItem tokenItem : this.tokens) {
            try {
                tokenItem.token =
                        iid.getToken(tokenItem.authorizedEntity, tokenItem.scope, tokenItem.options);
                MyApplication.updateGoogleMessagingToken(tokenItem.token);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // send this tokenItem.token to your server
        }
    }
    
    

    private class TokenItem {
        public String token;
        public String authorizedEntity;
        public java.lang.String scope;
        public Bundle options;
    }
}
