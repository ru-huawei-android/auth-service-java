package com.huawei.hms.sample2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.huawei.agconnect.auth.AGCAuthException;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.PhoneAuthProvider;
import com.huawei.agconnect.auth.PhoneUser;
import com.huawei.agconnect.auth.VerifyCodeResult;
import com.huawei.agconnect.auth.VerifyCodeSettings;
import com.huawei.hmf.tasks.Task;

import java.util.Locale;

import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;

public class PhoneActivity extends BaseActivity {
  private final String TAG = PhoneActivity.class.getSimpleName();

  String verifyCode;
  String phoneNumber;
  Boolean hasPhonePermission;

  /**
   * Переменная для внутренней логики - внедрено для демо
   * Если True - AGConnectAuthCredential будет сформирован с паролем
   * Если False - AGConnectAuthCredential будет сформирован с кодом верификации, пароль не требуется
   */
  Boolean credentialType;

  private String[] permissions = new String[]{READ_PHONE_STATE, READ_PHONE_NUMBERS};

  private EditText editTextPhone;
  private EditText editTextVerificationCode;
  private Button btnPhoneCode;
  private LinearLayout llCodeInput;
  private Button btnCreateUserInAg;
  private Button btnPhoneLogout;
  private Button btnPhoneCodeOk;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //Do not call here setContentView()

    initView();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      checkPermissions();

    btnPhoneCode.setOnClickListener(v -> {
      phoneNumber = editTextPhone.getText().toString();
      if (phoneNumber.isEmpty()) {
        showToast("Please put the phone number");
        return;
      }
      requestVerificationCode();
    });

    btnPhoneCodeOk.setOnClickListener(v -> {
      verifyCode = editTextVerificationCode.getText().toString();
      if (verifyCode.isEmpty()) {
        showToast("Please put the verification code");
        return;
      }
      //  После создания учетной записи пользователь входит в систему со сформированным credential.
      signInToAppGalleryConnect();
    });

    btnCreateUserInAg.setOnClickListener(v -> createUserInAppGallery());
    btnPhoneLogout.setOnClickListener(v -> logout());
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Проверяем наличие текущего уже авторизированного пользователя
    AGConnectUser currentUser = AGConnectAuth.getInstance().getCurrentUser();
    if (currentUser != null) {
      tvResults.setText(getUserInfo(currentUser));
      btnPhoneCode.setVisibility(View.VISIBLE);
      llCodeInput.setVisibility(View.GONE);
      btnCreateUserInAg.setVisibility(View.GONE);
      btnPhoneLogout.setVisibility(View.GONE);
    }

    // Делаем попытку достать номер телефона и подставить его в поле ввода
    if (hasPhonePermission) {
      phoneNumber = getPhoneNumberFromTelephony();
      editTextPhone.setText(phoneNumber);
    }
  }

  @Override
  protected int getLayoutResourceId() {
    return R.layout.activity_phone_login;
  }

  @Override
  public void logout() {
    super.logout();
    editTextPhone.setEnabled(true);
    editTextPhone.setVisibility(View.VISIBLE);

    btnPhoneCode.setVisibility(View.VISIBLE);
    llCodeInput.setVisibility(View.GONE);
    btnCreateUserInAg.setVisibility(View.GONE);
    btnPhoneLogout.setVisibility(View.GONE);
  }

  private void initView() {
    btnPhoneCode = findViewById(R.id.btnPhoneCode);
    llCodeInput = findViewById(R.id.llCodeInput);
    btnCreateUserInAg = findViewById(R.id.btnCreateUserInAg);
    btnPhoneLogout = findViewById(R.id.btnPhoneLogout);
    btnPhoneCode = findViewById(R.id.btnPhoneCode);
    btnPhoneCodeOk = findViewById(R.id.btnPhoneCodeOk);

    editTextPhone = findViewById(R.id.editTextPhone);
    editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
  }

  private void requestVerificationCode() {
    editTextPhone.setEnabled(false);
    btnPhoneCode.setEnabled(false);

    VerifyCodeSettings settings = VerifyCodeSettings.newBuilder()
        .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN) //ACTION_REGISTER_LOGIN o ACTION_RESET_PASSWORD
        //Минимальный интервал отправки, значения от 30 с до 120 с.
        .sendInterval(30)
        // Необязательный параметр. Указывает язык для отправки кода подтверждения.
        // Значение должно содержать информацию о языке и стране / регионе.
        // Значением по умолчанию является Locale.getDefault.
        .locale(new Locale("ru", "RU"))
        .build();

    // Запрос на проверку кода для регистрации мобильного номера.
    // Код подтверждения будет отправлен на ваш номер мобильного телефона,
    // поэтому вам нужно убедиться, что номер мобильного телефона правильный.
    Task<VerifyCodeResult> task =
        PhoneAuthProvider.requestVerifyCode(
            phoneNumber.substring(0, 2),
            phoneNumber.substring(2),
            settings);
    task.addOnSuccessListener(verifyCodeResult -> {
      // Запрос на код подтверждения отправлен успешно.
      llCodeInput.setVisibility(View.VISIBLE);
      showToast("Please wait verification code, and then type it and press OK");
    })
        .addOnFailureListener(e -> {
          handleError(e, TAG);
          tvResults.setText(e.getLocalizedMessage());
          editTextPhone.setEnabled(true);
          btnPhoneCode.setEnabled(true);
        });
  }

  private void createUserInAppGallery() {
    // Зарегистрирация аккаунта в AppGallery Connect, используя номер мобильного телефона.
    PhoneUser phoneUser;
    if (credentialType) {
      phoneUser = new PhoneUser.Builder()
          // Код страны (международный), для России это 7, вводится без знака +
          .setCountryCode(phoneNumber.substring(1, 2))
          // Номер телефона без кода страны т.е. 9876543210, вводится без разделителей и доп. символов*/
          .setPhoneNumber(phoneNumber.substring(2))
          .setVerifyCode(verifyCode)
          // Обязательно.
          // Если этот параметр установлен, по умолчанию для текущего пользователя должен быть создан пароль,
          // и в дальнейшем пользователь может войти в систему с помощью пароля.
          // В противном случае пользователь может войти в систему только с помощью кода подтверждения.
          .setPassword("password")//TODO() we need request password from user...
          .build();
    } else {
      phoneUser = new PhoneUser.Builder()
          // Код страны (международный), для России это 7, вводится без знака +
          .setCountryCode(phoneNumber.substring(1, 2))
          // Номер телефона без кода страны т.е. 9876543210, вводится без разделителей и доп. символов
          .setPhoneNumber(phoneNumber.substring(2))
          .setVerifyCode(verifyCode)
          .build();
    }
    AGConnectAuth.getInstance().createUser(phoneUser)
        .addOnSuccessListener(signInResult -> {
          //  После создания учетной записи пользователь входит в систему
          signInToAppGalleryConnect();
        })
        .addOnFailureListener(e -> handleError(e, TAG));
  }

  private void signInToAppGalleryConnect() {
    // Формируем AGConnectAuthCredential
    AGConnectAuthCredential credential;
    if (credentialType) {
      // С паролем
      credential = PhoneAuthProvider.credentialWithPassword(
          // Код страны (международный), для России это 7, вводится без знака +*/
          phoneNumber.substring(1, 2),
          // Номер телефона без кода страны т.е. 9876543210, вводится без разделителей и доп. символов*/
          phoneNumber.substring(2),
          "password"//TODO() we need request password from user...
      );
    } else {
      // с кодом верификации, пароль опционален
      credential = PhoneAuthProvider.credentialWithVerifyCode(
          // Код страны (международный), для России это 7, вводится без знака +
          phoneNumber.substring(1, 2),
          //  Номер телефона без кода страны т.е. 9876543210, вводится без разделителей и доп. символов
          phoneNumber.substring(2),
          // пароль опционален
          "",
          verifyCode);
    }
    // Осуществляем вход.
    AGConnectAuth.getInstance().signIn(credential)
        .addOnSuccessListener(signInResult -> {
          AGConnectUser user = signInResult.getUser();
          showToast(user.getUid());
          tvResults.setText(getUserInfo(user));
          llCodeInput.setVisibility(View.GONE);
          btnPhoneCode.setVisibility(View.GONE);
        })
        .addOnFailureListener(e -> {
          handleError(e, TAG);
          if (e instanceof AGCAuthException && ((AGCAuthException) e).getCode() == AGCAuthException.USER_NOT_REGISTERED) {
            btnCreateUserInAg.setVisibility(View.VISIBLE);
          }
        });
  }

  /**
   * Делаем попытку достать номер телефона
   */
  @SuppressLint(value = "HardwareIds")
  private String getPhoneNumberFromTelephony() {
    try {
      TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
      return telephonyManager.getLine1Number();
    } catch (SecurityException securityException) {
      handleError(securityException, TAG);
    }
    return null;
  }

  /**
   * Проверяем наличие разрешений READ_PHONE_STATE & READ_PHONE_NUMBERS для получения номера
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private void checkPermissions() {
    if (ContextCompat.checkSelfPermission(this, READ_PHONE_STATE)
        != PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, READ_PHONE_NUMBERS)
        != PackageManager.PERMISSION_GRANTED
    ) {
      /* Проверяем, должны ли мы показать дополнительное уведомление*/
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_PHONE_STATE)
          || ActivityCompat.shouldShowRequestPermissionRationale(this, READ_PHONE_NUMBERS)
      ) {
        showPhonePermissionRationale();
      } else {
        requestStoragePermissions();
      }
    } else {
      hasPhonePermission = true;
    }
  }

  /**
   * Запрпшиваем разрешения READ_PHONE_STATE & READ_PHONE_NUMBERS для получения номера
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private void requestStoragePermissions() {
    ActivityCompat.requestPermissions(this, permissions, 1);
  }

  /**
   * Показываем уведомление о том что нам нужны разрешения READ_PHONE_STATE & READ_PHONE_NUMBERS для получения номера
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private void showPhonePermissionRationale() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.permission_request_phone_title)
        .setMessage(R.string.permission_request_phone_description)
        .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
        .setCancelable(false)
        .setOnDismissListener(dialog -> requestStoragePermissions())
        .show();
  }
}