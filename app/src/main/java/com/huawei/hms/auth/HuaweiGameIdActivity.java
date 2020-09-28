/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.hms.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.HWGameAuthProvider;
import com.huawei.hms.api.HuaweiMobileServicesUtil;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.PlayersClient;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;

public class HuaweiGameIdActivity extends BaseActivity {
    private final String TAG = HuaweiGameIdActivity.class.getSimpleName();

    private final int HUAWEIGAME_SIGNIN = 7000;
    private final int LINK_CODE = 7002;

    private HuaweiIdAuthService huaweiIdAuthService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        HuaweiMobileServicesUtil.setApplication(getApplication());
        JosAppsClient appsClient = JosApps.getJosAppsClient(this, null);
        appsClient.init();
        HuaweiIdAuthParams authParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM).createParams();
        huaweiIdAuthService = HuaweiIdAuthManager.getService(this, authParams);

        btnLogin.setOnClickListener(v -> login());
        btnLogout.setOnClickListener(v -> logout());
        btnLinkUnlink.setOnClickListener(v -> link());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            showToast("Huawei Game Service Sign in Intent is null");
            return;
        }

        HuaweiIdAuthManager.parseAuthResultFromIntent(data)
                .addOnSuccessListener(authHuaweiId -> getHwGameUserInfo(authHuaweiId, requestCode))
                .addOnFailureListener(e -> handleError(e, TAG));
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    @Override
    public void logout() {
        super.logout();
        getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider);
    }

    private void login() {
        startActivityForResult(huaweiIdAuthService.getSignInIntent(), HUAWEIGAME_SIGNIN);
    }

    private void link() {
        boolean isLinked = isProviderLinked(getAGConnectUser(), AGConnectAuthCredential.HWGame_Provider);
        if (isLinked)
            unlink(AGConnectAuthCredential.HWGame_Provider, TAG);
        else
            startActivityForResult(huaweiIdAuthService.getSignInIntent(), LINK_CODE);
    }

    private void getHwGameUserInfo(AuthHuaweiId signInHuaweiId, int requestCode) {
        PlayersClient client = Games.getPlayersClient(this, signInHuaweiId);
        client.getCurrentPlayer()
                .addOnSuccessListener(player -> {
                    String imageUrl;
                    if (player.hasHiResImage())
                        imageUrl = player.getHiResImageUri().toString();
                    else
                        imageUrl = player.getIconImageUri().toString();

                    AGConnectAuthCredential credential = new HWGameAuthProvider.Builder()
                            .setPlayerSign(player.getPlayerSign())
                            .setPlayerId(player.getPlayerId())
                            .setDisplayName(player.getDisplayName())
                            .setImageUrl(imageUrl)
                            .setPlayerLevel(player.getLevel())
                            .setSignTs(player.getSignTs())
                            .build();

                    if (requestCode == HUAWEIGAME_SIGNIN) {
                        AGConnectAuthLogin(credential, TAG, AGConnectAuthCredential.HWGame_Provider);
                    } else if (requestCode == LINK_CODE) {
                        AGConnectAuthLink(credential, TAG, AGConnectAuthCredential.HWGame_Provider);
                    }
                }).addOnFailureListener(e -> handleError(e, TAG));
    }
}
