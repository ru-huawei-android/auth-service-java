package com.huawei.hms.example.authservice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.TwitterAuthProvider;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

public class TwitterActivity extends BaseActivity {
    private final String TAG = TwitterActivity.class.getSimpleName();

    private TwitterAuthClient twitterAuthClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                getString(R.string.twitter_app_id),
                getString(R.string.twitter_app_secret)
        );

        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
                .twitterAuthConfig(authConfig)
                .build();
        Twitter.initialize(twitterConfig);
        twitterAuthClient = new TwitterAuthClient();

        btnLinkUnlink.setVisibility(View.GONE);
        btnLogin.setOnClickListener(v -> login());
        btnLogout.setOnClickListener(v -> logout());
        btnLinkUnlink.setOnClickListener(v -> link());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        twitterAuthClient.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.Twitter_Provider);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    @Override
    public void logout() {
        super.logout();
    }

    private void login() {
        twitterAuthClient.authorize(this, new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                String token = result.data.getAuthToken().token;
                String secret = result.data.getAuthToken().secret;
                AGConnectAuthCredential credential = TwitterAuthProvider.credentialWithToken(token, secret);
                AGConnectAuthLogin(credential, TAG, AGConnectAuthCredential.Twitter_Provider);
            }

            @Override
            public void failure(TwitterException exception) {
                handleError(exception, TAG);
            }
        });
    }

    private void link() {
        boolean isLinked = isProviderLinked(getAGConnectUser(), AGConnectAuthCredential.Twitter_Provider);
        if (isLinked)
            unlink(AGConnectAuthCredential.Twitter_Provider, TAG);
        else {
            twitterAuthClient.authorize(this, new Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {
                    String token = result.data.getAuthToken().token;
                    String secret = result.data.getAuthToken().secret;
                    AGConnectAuthCredential credential = TwitterAuthProvider.credentialWithToken(token, secret);
                    AGConnectAuthLink(credential, TAG, AGConnectAuthCredential.Twitter_Provider);
                }

                @Override
                public void failure(TwitterException exception) {
                    handleError(exception, TAG);
                }
            });
        }
    }
}
