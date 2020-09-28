package com.huawei.hms.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.FacebookAuthProvider;

import java.util.Arrays;
import java.util.List;

public class FacebookActivity extends BaseActivity {
    private static final String TAG = FacebookActivity.class.getSimpleName();

    private CallbackManager callbackManager = CallbackManager.Factory.create();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        btnLinkUnlink.setVisibility(View.GONE);

        btnLogin.setOnClickListener(v -> login());
        btnLogout.setOnClickListener(v -> logout());
        btnLinkUnlink.setOnClickListener(v -> link());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.Facebook_Provider);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    @Override
    public void logout() {
        super.logout();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.Facebook_Provider);
    }

    private void login() {
        loginWithReadPermissions();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AGConnectAuthCredential credential = getCredential(loginResult);
                AGConnectAuthLogin(credential, TAG, AGConnectAuthCredential.Facebook_Provider);
            }

            @Override
            public void onCancel() {
                showToast("Cancel");
            }

            @Override
            public void onError(FacebookException error) {
                handleError(error, TAG);
            }
        });
    }

    private void link() {
        Boolean isLinked = isProviderLinked(getAGConnectUser(), AGConnectAuthCredential.Facebook_Provider);
        if (isLinked) {
            unlink(AGConnectAuthCredential.Facebook_Provider, TAG);
        } else {
            loginWithReadPermissions();
            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    AGConnectAuthCredential credential = getCredential(loginResult);
                    AGConnectAuthLink(credential, TAG, AGConnectAuthCredential.Facebook_Provider);
                }

                @Override
                public void onCancel() {
                    showToast("Cancel");
                }

                @Override
                public void onError(FacebookException error) {
                    handleError(error, TAG);
                }
            });
        }
    }

    private void loginWithReadPermissions() {
        List<String> permissions = Arrays.asList("public_profile", "email");
        LoginManager.getInstance().logInWithReadPermissions(this, permissions);
    }

    private AGConnectAuthCredential getCredential(LoginResult loginResult) {
        String token = loginResult.getAccessToken().getToken();
        return FacebookAuthProvider.credentialWithToken(token);
    }
}
