package com.huawei.hms.example.authservice;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;

public class AnonymousLogin extends BaseActivity {

    private final String TAG = AnonymousLogin.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        btnLinkUnlink.setVisibility(View.GONE);

        btnLogin.setOnClickListener(v -> login());
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUserInfoAndSwitchUI();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    private void login() {
        AGConnectAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(signInResult -> handleSignInResult(signInResult, AGConnectAuthCredential.Anonymous))
                .addOnFailureListener(e -> handleError(e, TAG));
    }

    //TODO
    private void getUserInfoAndSwitchUI() {
        // Проверяем наличие текущего уже авторизированного пользователя
        if (getAGConnectUser() != null) {
            // Выводим инфу о пользователе*/
            tvResults.setText(getUserInfo(AGConnectAuth.getInstance().getCurrentUser()));
            // Скрываем кнопку Login & LinkUnlink
            btnLogin.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            // Стандартный режим
            btnLogin.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }
    }
}
