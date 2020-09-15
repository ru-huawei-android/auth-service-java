package com.huawei.hms.sample2;

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
    String info = "displayName: " + user.getDisplayName() + "\n";
    info += "UID: " + user.getUid() + "\n";
    info += "email: " + user.getEmail() + "\n";
    info += "emailVerified: " + user.getEmailVerified() + "\n";
    info += "isAnonymous: " + user.isAnonymous() + "\n";
    info += "passwordSetted: " + user.getPasswordSetted() + "\n";
    info += "phone: " + user.getPhone() + "\n";
    info += "providerId: " + providersMap().get(Integer.parseInt(user.getProviderId()))
        + " [id: " + user.getProviderId() + "]" + "\n";
    if (user.getProviderInfo() != null)
      info += "providerInfo:\n" + user.getProviderInfo().toString();

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
          message = "Invalid mobile number.";
          break;
        case AGCAuthException.PASSWORD_VERIFICATION_CODE_OVER_LIMIT:
          message = "The number of verification code inputs for password-based sign-in exceeds the upper limit.";
          break;
        case AGCAuthException.PASSWORD_VERIFY_CODE_ERROR:
          message = "Incorrect password or verification code.";
          break;
        case AGCAuthException.VERIFY_CODE_ERROR:
          message = "Incorrect verification code.";
          break;
        case AGCAuthException.VERIFY_CODE_FORMAT_ERROR:
          message = "Incorrect verification code format.";
          break;
        case AGCAuthException.VERIFY_CODE_AND_PASSWORD_BOTH_NULL:
          message = "The verification code or password cannot be empty.";
          break;
        case AGCAuthException.VERIFY_CODE_EMPTY:
          message = "The verification code is empty.";
          break;
        case AGCAuthException.VERIFY_CODE_LANGUAGE_EMPTY:
          message = "The language for sending a verification code is empty.";
          break;
        case AGCAuthException.VERIFY_CODE_RECEIVER_EMPTY:
          message = "The verification code receiver is empty.";
          break;
        case AGCAuthException.VERIFY_CODE_ACTION_ERROR:
          message = "The verification code type is empty.";
          break;
        case AGCAuthException.VERIFY_CODE_TIME_LIMIT:
          message = "The number of times for sending verification codes exceeds the upper limit.";
          break;
        case AGCAuthException.ACCOUNT_PASSWORD_SAME:
          message = "The password cannot be the same as the user name.";
          break;
        case AGCAuthException.USER_NOT_REGISTERED:
          message = "The user has not been registered.";
          break;
        case AGCAuthException.USER_HAVE_BEEN_REGISTERED:
          message = "The user already exists.";
          break;
        case AGCAuthException.PROVIDER_USER_HAVE_BEEN_LINKED:
          message = "The authentication mode has been associated with another user.";
          break;
        case AGCAuthException.PROVIDER_HAVE_LINKED_ONE_USER:
          message = "The authentication mode has already been associated with the user.";
          break;
        case AGCAuthException.CANNOT_UNLINK_ONE_PROVIDER_USER:
          message = "Cannot disassociate a single authentication mode.";
          break;
        case AGCAuthException.AUTH_METHOD_IS_DISABLED:
          message = "The authentication mode is not supported.";
          break;
        case AGCAuthException.FAIL_TO_GET_THIRD_USER_INFO:
          message = "Failed to obtain the third-party user information.";
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
