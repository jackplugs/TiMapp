package com.timappweb.timapp.activities;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import com.timappweb.timapp.MyApplication;
import com.timappweb.timapp.config.IntentsUtils;
import com.timappweb.timapp.utils.EventActionButtons;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Created by Stephane on 07/09/2016.
 */
public class ProfileActivityTest {

    private static final int PROFILE_ID = 1;
    @Rule
    public ActivityTestRule<ProfileActivity> mActivityRule = new ActivityTestRule<>(
            ProfileActivity.class, false, false);

    @Before
    public void startActivity(){
        Intent intent = IntentsUtils.buildIntentViewPlace(MyApplication.getApplicationBaseContext(), PROFILE_ID);
        mActivityRule.launchActivity(intent);
    }

    @Test
    public void testEditProfile() {
        // TODO
    }


}
