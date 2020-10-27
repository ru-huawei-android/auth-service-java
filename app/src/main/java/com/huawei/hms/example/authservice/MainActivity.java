package com.huawei.hms.example.authservice;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.huawei.agconnect.auth.AGConnectAuth;

public class MainActivity extends BaseActivity {
    /**
     * Срок действия access token составляет два дня, а refresh token валиден два месяца.
     * Если пользователь не входит в приложение в течение двух месяцев подряд,
     * срок действия refresh token истекает, и при пользователь получит код ошибки 203817986.
     * */

    /**
     * Если при попытке сделать Link() используя аккаунт YYY в ответ получаем ошибку
     * PROVIDER_USER_HAVE_BEEN_LINKED = 203818038
     * Это значит что данный аккаунт YYY уже слинкован с другим аккунтом (с другим UID)
     * Для того что бы сделать UnLink этого аккаунта YYY нужно (два способа):
     * 1. Найти в списке providerInfo аккоунт который идет самым первым (мастер)
     * <p>
     * а. Осуществить SignIn (логин) с помощью этого мастер аккаунта
     * б. Осуществить UnLink() для аккаунта YYY
     * <p>
     * 2. Осуществить SignIn (логин) с помощью аккаунта YYY
     * а. Удалить данного юзера серез метод deleteUser()
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        findViewById(R.id.btnPhoneLogin).setOnClickListener(v -> navigateToPhoneLogin());
        findViewById(R.id.btnEmailLogin).setOnClickListener(v -> navigateToEmailLogin());
        findViewById(R.id.btnAnonymousLogin).setOnClickListener(v -> navigateToAnonymousLogin());
        findViewById(R.id.btnHwidLogin).setOnClickListener(v -> navigateToHwidLogin());
        findViewById(R.id.btnHwGameLogin).setOnClickListener(v -> navigateToHwGameLogin());
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> navigateToGoogleLogin());
        findViewById(R.id.btnFbLogin).setOnClickListener(v -> navigateToFbLogin());
        findViewById(R.id.btnTwLogin).setOnClickListener(v -> navigateToTwLogin());
        findViewById(R.id.btnDeleteUser).setOnClickListener(v -> {
            AGConnectAuth.getInstance().deleteUser();
            logout();
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        //проверяем наличие текущего уже авторизированного пользователя
        if (AGConnectAuth.getInstance().getCurrentUser() != null)
            tvResults.setText(getUserInfo(AGConnectAuth.getInstance().getCurrentUser()));
        else
            tvResults.setText("");
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    private void navigateToPhoneLogin() {
        startActivity(new Intent(this, PhoneActivity.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToEmailLogin() {
        startActivity(new Intent(this, EmailActivity.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToAnonymousLogin() {
        startActivity(new Intent(this, AnonymousLogin.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToHwidLogin() {
        startActivity(new Intent(this, HuaweiIdActivity.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToHwGameLogin() {
        startActivity(new Intent(this, HuaweiGameIdActivity.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToGoogleLogin() {
        startActivity(new Intent(this, GoogleActivity.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToFbLogin() {
        startActivity(new Intent(this, FacebookActivity.class));
        overridePendingTransition(0, 0);
    }

    private void navigateToTwLogin() {
        startActivity(new Intent(this, TwitterActivity.class));
        overridePendingTransition(0, 0);
    }
}
