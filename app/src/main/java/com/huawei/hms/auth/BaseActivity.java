package com.huawei.hms.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.agconnect.auth.AGCAuthException;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.SignInResult;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    protected TextView tvResults;
    protected ImageView ivProfile;
    protected Button btnLogin;
    protected Button btnLogout;
    protected Button btnLinkUnlink;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        tvResults = findViewById(R.id.tvResults);
        ivProfile = findViewById(R.id.ivProfile);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogout = findViewById(R.id.btnLogout);
        btnLinkUnlink = findViewById(R.id.btnLinkUnlink);
    }

    protected abstract int getLayoutResourceId();

    public String getUserInfo(AGConnectUser user) {
        String info = getString(
                R.string.user_info_formatted_string,
                user.getDisplayName(),
                user.getUid(),
                user.getEmail(),
                user.getEmailVerified(),
                user.isAnonymous(),
                user.getPasswordSetted(),
                user.getPhone(),
                providersMap().get(Integer.valueOf(user.getProviderId())),
                user.getProviderId(),
                user.getProviderInfo().toString());

        //TODO iv_profile.load(user.photoUrl) { crossfade(true) }
        return info;
    }

    public Boolean isProviderLinked(AGConnectUser user, int providerId) {
        for (Map<String, String> provider : user.getProviderInfo()) {
            if (provider.containsKey("provider")
                    && Integer.parseInt(provider.get("provider")) == providerId)
                return true;
        }
        return false;
    }

    public AGConnectUser getAGConnectUser() {
        return AGConnectAuth.getInstance().getCurrentUser();
    }

    public String checkError(Exception exception) {
        String message = null;
        if (exception instanceof AGCAuthException) {
            message = exception.getLocalizedMessage();
            switch (((AGCAuthException) exception).getCode()) {
                case AGCAuthException.INVALID_PHONE:
                    message = getString(R.string.invalid_phone);
                    break;
                case AGCAuthException.PASSWORD_VERIFICATION_CODE_OVER_LIMIT:
                    message = getString(R.string.password_verification_code_over_limit);
                    break;
                case AGCAuthException.PASSWORD_VERIFY_CODE_ERROR:
                    message = getString(R.string.password_verify_code_error);
                    break;
                case AGCAuthException.VERIFY_CODE_ERROR:
                    message = getString(R.string.verify_code_error);
                    break;
                case AGCAuthException.VERIFY_CODE_FORMAT_ERROR:
                    message = getString(R.string.verify_code_format_error);
                    break;
                case AGCAuthException.VERIFY_CODE_AND_PASSWORD_BOTH_NULL:
                    message = getString(R.string.verify_code_and_password_both_null);
                    break;
                case AGCAuthException.VERIFY_CODE_EMPTY:
                    message = getString(R.string.verify_code_empty);
                    break;
                case AGCAuthException.VERIFY_CODE_LANGUAGE_EMPTY:
                    message = getString(R.string.verify_code_language_empty);
                    break;
                case AGCAuthException.VERIFY_CODE_RECEIVER_EMPTY:
                    message = getString(R.string.verify_code_receiver_empty);
                    break;
                case AGCAuthException.VERIFY_CODE_ACTION_ERROR:
                    message = getString(R.string.verify_code_action_error);
                    break;
                case AGCAuthException.VERIFY_CODE_TIME_LIMIT:
                    message = getString(R.string.verify_code_time_limit);
                    break;
                case AGCAuthException.ACCOUNT_PASSWORD_SAME:
                    message = getString(R.string.account_password_same);
                    break;
                case AGCAuthException.USER_NOT_REGISTERED:
                    message = getString(R.string.user_have_been_registered);
                    break;
                case AGCAuthException.USER_HAVE_BEEN_REGISTERED:
                    message = getString(R.string.provider_user_have_been_linked);
                    break;
                case AGCAuthException.PROVIDER_USER_HAVE_BEEN_LINKED:
                    message = getString(R.string.user_not_registered);
                    break;
                case AGCAuthException.PROVIDER_HAVE_LINKED_ONE_USER:
                    message = getString(R.string.provider_have_linked_one_user);
                    break;
                case AGCAuthException.CANNOT_UNLINK_ONE_PROVIDER_USER:
                    message = getString(R.string.cannot_unlink_one_provider_user);
                    break;
                case AGCAuthException.AUTH_METHOD_IS_DISABLED:
                    message = getString(R.string.auth_method_is_disabled);
                    break;
                case AGCAuthException.FAIL_TO_GET_THIRD_USER_INFO:
                    message = getString(R.string.fail_to_get_third_user_info);
                    break;
            }
        } else {
            message = exception.getLocalizedMessage();
        }
        return message;
    }

    public void handleError(Exception exception, String tag) {
        String message = checkError(exception);
        Log.e(tag, message);
        showToast(message);
        tvResults.setText(message);
    }

    public void handleSignInResult(SignInResult signInResult, int provider) {
        AGConnectUser user = signInResult.getUser();
        showToast(user.getUid());
        getUserInfoAndSwitchUI(provider);
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void logout() {
        if (AGConnectAuth.getInstance().getCurrentUser() != null) {
            AGConnectAuth.getInstance().signOut();
        }
        tvResults.setText("");
        //TODO ivProfile.clear
    }

    public void unlink(int provider, String tag) {
        AGConnectUser currentUser = AGConnectAuth.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        currentUser
                .unlink(provider)
                .addOnSuccessListener(signInResult -> handleSignInResult(signInResult, provider))
                .addOnFailureListener(e -> handleError(e, tag));
    }

    public void AGConnectAuthLogin(AGConnectAuthCredential credential, String tag, int provider) {
        AGConnectAuth.getInstance()
                .signIn(credential)
                .addOnSuccessListener(signInResult -> handleSignInResult(signInResult, provider))
                .addOnFailureListener(e -> handleError(e, tag));
    }

    public void AGConnectAuthLink(AGConnectAuthCredential credential, String tag, int provider) {
        getAGConnectUser()
                .link(credential)
                .addOnSuccessListener(signInResult -> handleSignInResult(signInResult, provider))
                .addOnFailureListener(e -> handleError(e, tag));
    }

    public void getUserInfoAndSwitchUI(int providerId) {
        // Проверяем наличие текущего уже авторизированного пользователя
        if (getAGConnectUser() != null) {
            //Выводим инфу о пользователе*/
            tvResults.setText(getUserInfo(AGConnectAuth.getInstance().getCurrentUser()));
            // проверяем кол-во привязанных провайдеров*/
            if (getAGConnectUser().getProviderInfo() != null && getAGConnectUser().getProviderInfo().size() > 1
                    /** Если один из них = providerId*/
                    && isProviderLinked(getAGConnectUser(), providerId)) {
                // то меняем текст кнопки*/
                btnLogin.setVisibility(View.GONE);
                btnLogout.setVisibility(View.VISIBLE);
                btnLinkUnlink.setText(getString(R.string.unlink));
                btnLinkUnlink.setVisibility(View.VISIBLE);
            }
            // Если у нас всего один провайдер и он = providerId
            else if (getAGConnectUser().getProviderInfo() != null && getAGConnectUser().getProviderInfo().size() == 1
                    && isProviderLinked(getAGConnectUser(), providerId)) {
                // Скрываем кнопку Login & LinkUnlink
                btnLogin.setVisibility(View.GONE);
                btnLogout.setVisibility(View.VISIBLE);
                btnLogout.setText(getString(R.string.logout));
                btnLinkUnlink.setVisibility(View.GONE);
            } else {
                // Стандартный режим для Link/Unlink
                btnLogin.setVisibility(View.GONE);
                btnLogout.setVisibility(View.VISIBLE);
                btnLinkUnlink.setText(getString(R.string.link));
                btnLinkUnlink.setVisibility(View.VISIBLE);
            }
        } else {
            // Стандартный режим для Login
            btnLogin.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
            btnLinkUnlink.setVisibility(View.GONE);
            btnLinkUnlink.setText(getString(R.string.link));
        }
    }

    private Map<Integer, String> providersMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "Anonymous_Provider");
        map.put(1, "Huawei_Provider");
        map.put(2, "Facebook_Provider");
        map.put(3, "Twitter_Provider");
        map.put(4, "WeiXin_Provider");
        map.put(5, "Huawei_Game_Provider");
        map.put(6, "QQ_Provider");
        map.put(7, "WeiBo_Provider");
        map.put(8, "Google_Provider");
        map.put(9, "GoogleGame_Provider");
        map.put(10, "SelfBuild_Provider");
        map.put(11, "Phone_Provider");
        map.put(12, "Email_Provider");
        return map;
    }
}
