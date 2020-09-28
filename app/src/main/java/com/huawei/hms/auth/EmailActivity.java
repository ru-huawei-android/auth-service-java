package com.huawei.hms.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.huawei.agconnect.auth.AGCAuthException;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.EmailAuthProvider;
import com.huawei.agconnect.auth.EmailUser;
import com.huawei.agconnect.auth.VerifyCodeSettings;

import java.util.Locale;

public class EmailActivity extends BaseActivity {

    private static final String TAG = EmailActivity.class.getSimpleName();

    /**
     * Переменная для внутренней логики - для демо
     * Если True - AGConnectAuthCredential будет сформирован с паролем
     * Если False - AGConnectAuthCredential будет сформирован с кодом верификации, пароль не требуется
     */
    private Boolean credentialType = false;
    private String verifyCode = null;
    private String email = null;

    private EditText editTextEmail;
    private EditText editTextVerificationCode;
    private LinearLayout llCodeInput;
    private Button btnEmailLogout;
    private Button btnEmailCode;
    private Button btnCreateUserInAg;
    private Button btnEmailOk;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()
        initView();

        btnEmailCode.setOnClickListener(v -> {
            email = editTextEmail.getText().toString();
            if (email.isEmpty()) {
                showToast("Please put the phone number");
                return;
            }
            requestVerificationCode();
        });

        btnEmailOk.setOnClickListener(v -> {
            verifyCode = editTextVerificationCode.getText().toString();
            if (verifyCode.isEmpty()) {
                showToast("Please put the verification code");
                return;
            }
            //После создания учетной записи пользователь входит в систему со сформированным credential.
            signInToAppGalleryConnect();
        });

        btnCreateUserInAg.setOnClickListener(v -> createUserInAppGallery());
        btnEmailLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        /** Проверяем наличие текущего уже авторизированного пользователя*/
        AGConnectUser currentUser = AGConnectAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            tvResults.setText(getUserInfo(currentUser));
            editTextEmail.setVisibility(View.GONE);
            btnEmailCode.setVisibility(View.GONE);
            llCodeInput.setVisibility(View.GONE);
            btnCreateUserInAg.setVisibility(View.GONE);
            btnEmailLogout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_email_login;
    }

    @Override
    public void logout() {
        super.logout();

        editTextEmail.setEnabled(true);
        editTextEmail.setVisibility(View.VISIBLE);

        btnEmailCode.setVisibility(View.VISIBLE);
        llCodeInput.setVisibility(View.GONE);
        btnCreateUserInAg.setVisibility(View.GONE);
        btnEmailLogout.setVisibility(View.GONE);
    }

    private void initView() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextVerificationCode = findViewById(R.id.editTextVerificationCode);
        llCodeInput = findViewById(R.id.llCodeInput);
        btnEmailLogout = findViewById(R.id.btnEmailLogout);
        btnEmailCode = findViewById(R.id.btnEmailCode);
        btnCreateUserInAg = findViewById(R.id.btnCreateUserInAg);
        btnEmailOk = findViewById(R.id.btnEmailOk);
    }

    private void requestVerificationCode() {
        editTextEmail.setEnabled(false);

        VerifyCodeSettings settings = VerifyCodeSettings.newBuilder()
                .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN) //ACTION_REGISTER_LOGIN o ACTION_RESET_PASSWORD
                // Минимальный интервал отправки, значения от 30 с до 120 с.
                .sendInterval(30)
                //  Необязательный параметр. Указывает язык для отправки кода подтверждения.
                //  Значение должно содержать информацию о языке и стране / регионе.
                //  Значением по умолчанию является Locale.getDefault.
                .locale(new Locale("ru", "RU"))
                .build();

        // Запрос на код подтверждения.
        // Код подтверждения будет отправлен на указанный электронный почтовый яшик

        EmailAuthProvider.requestVerifyCode(email, settings)
                .addOnSuccessListener(verifyCodeResult -> {
                    // Запрос на код подтверждения отправлен успешно.
                    llCodeInput.setVisibility(View.VISIBLE);
                    showToast("Please wait verification code, and then type it and press OK");
                })
                .addOnFailureListener(e -> handleError(e, TAG));
    }

    /**
     * Регистрирация аккаунта в AppGallery Connect, используя e-mail.
     */
    private void createUserInAppGallery() {
        EmailUser emailUser;
        if (credentialType) {
            emailUser = new EmailUser.Builder()
                    .setEmail(email)
                    .setVerifyCode(verifyCode)
                    // Обязательно.
                    // Если этот параметр установлен, по умолчанию для текущего пользователя должен быть создан пароль,
                    // и в дальнейшем пользователь может войти в систему с помощью пароля.
                    // В противном случае пользователь может войти в систему только с помощью кода подтверждения.

                    .setPassword("password")//TODO() we need request password from user...
                    .build();
        } else {
            emailUser = new EmailUser.Builder()
                    .setEmail(email)
                    .setVerifyCode(verifyCode)
                    .build();
        }
        AGConnectAuth.getInstance().createUser(emailUser)
                .addOnSuccessListener(signInResult -> signInToAppGalleryConnect())
                .addOnFailureListener(e -> handleError(e, TAG));
    }


    //TODO error in documentation credentialWithPassword
    private void signInToAppGalleryConnect() {
        // Формируем AGConnectAuthCredential
        AGConnectAuthCredential credential;
        if (credentialType) {
            //С паролем
            credential = EmailAuthProvider.credentialWithPassword(
                    email,
                    "password"//TODO() we need request password from user...
            );
        } else {
            // с кодом верификации, пароль опционален
            credential = EmailAuthProvider.credentialWithVerifyCode(
                    email,
                    "",
                    verifyCode
            );
        }
        // Осуществляем вход.
        AGConnectAuth.getInstance().signIn(credential)
                .addOnSuccessListener(signInResult -> {
                    handleSignInResult(signInResult, AGConnectAuthCredential.Email_Provider);

                    llCodeInput.setVisibility(View.GONE);
                    btnEmailCode.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    handleError(e, TAG);
                    // Если получаем ошибку AGCAuthException.USER_NOT_REGISTERED, то начинаем регистрацию пользователя в AGC
                    if (e instanceof AGCAuthException && ((AGCAuthException) e).getCode() == AGCAuthException.USER_NOT_REGISTERED)
                        btnCreateUserInAg.setVisibility(View.VISIBLE);
                });
    }
}
