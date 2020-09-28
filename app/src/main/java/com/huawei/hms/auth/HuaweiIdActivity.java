package com.huawei.hms.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.HwIdAuthProvider;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.api.entity.hwid.HwIDConstant;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;

import java.util.Arrays;
import java.util.List;

public class HuaweiIdActivity extends BaseActivity {
    private final String TAG = HuaweiIdActivity.class.getSimpleName();
    private final int HUAWEI_ID_SIGNIN = 8000;
    private final int LINK_CODE = 8002;

    private HuaweiIdAuthService huaweiIdAuthService;
    private HuaweiIdAuthParams huaweiIdAuthParams;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        List<Scope> scopeList = Arrays.asList(new Scope(HwIDConstant.SCOPE.ACCOUNT_BASEPROFILE));
        HuaweiIdAuthParamsHelper authParamsHelper = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM);
        authParamsHelper.setScopeList(scopeList);

        huaweiIdAuthParams = authParamsHelper.setAccessToken().createParams();
        huaweiIdAuthService = HuaweiIdAuthManager.getService(this, huaweiIdAuthParams);

        btnLogin.setOnClickListener(v -> login());
        btnLogout.setOnClickListener(v -> logout());
        btnLinkUnlink.setOnClickListener(v -> link());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HUAWEI_ID_SIGNIN) {
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                String accessToken = huaweiAccount.getAccessToken();
                AGConnectAuthCredential credential = HwIdAuthProvider.credentialWithToken(accessToken);
                if (getAGConnectUser() == null)
                    AGConnectAuthLogin(credential, TAG, AGConnectAuthCredential.HMS_Provider);
                else {
                    AGConnectUser user = getAGConnectUser();
                    showToast(user.getUid());
                    getUserInfoAndSwitchUI(AGConnectAuthCredential.HMS_Provider);
                }
            } else {
                showToast("HwID signIn failed" + authHuaweiIdTask.getException().getMessage());
            }
        } else if (requestCode == LINK_CODE) {
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                String accessToken = huaweiAccount.getAccessToken();
                AGConnectAuthCredential credential = HwIdAuthProvider.credentialWithToken(accessToken);
                AGConnectAuthLink(credential, TAG, AGConnectAuthCredential.HMS_Provider);
            } else {
                showToast("Link is failed : " + authHuaweiIdTask.getException().getMessage());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.HMS_Provider);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    @Override
    public void logout() {
        super.logout();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.HMS_Provider);
    }

    private void login() {
        startActivityForResult(huaweiIdAuthService.getSignInIntent(), HUAWEI_ID_SIGNIN);
    }

    private void link() {
        boolean isLinked = isProviderLinked(getAGConnectUser(), AGConnectAuthCredential.HMS_Provider);
        if (isLinked)
            unlink(AGConnectAuthCredential.HMS_Provider, TAG);
        else
            startActivityForResult(huaweiIdAuthService.getSignInIntent(), LINK_CODE);
    }
}
