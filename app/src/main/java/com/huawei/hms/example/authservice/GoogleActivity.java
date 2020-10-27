package com.huawei.hms.example.authservice;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.GoogleAuthProvider;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

public class GoogleActivity extends BaseActivity {
    private static final String TAG = GoogleActivity.class.getSimpleName();
    private static final String ACTION = "com.example.authservice.HANDLE_AUTHORIZATION_RESPONSE";
    private static final String USED_INTENT = "USED_INTENT";

    private static final int GOOGLE_SIGN_CODE = 9901;
    private static final int LINK_CODE = 9902;

    private GoogleSignInClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not call here setContentView()

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();
        client = GoogleSignIn.getClient(this, options);

        btnLogin.setOnClickListener(v -> login());
        btnLogout.setOnClickListener(v -> logout());
        btnLinkUnlink.setOnClickListener(v -> link());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_CODE) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(googleSignInAccount -> {
                        AGConnectAuthCredential credential = GoogleAuthProvider.credentialWithToken(googleSignInAccount.getIdToken());
                        AGConnectAuthLogin(credential, TAG, AGConnectAuthCredential.Google_Provider);
                    })
                    .addOnFailureListener(e -> handleError(e, TAG));
        } else if (requestCode == LINK_CODE) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(googleSignInAccount -> {
                        AGConnectAuthCredential credential = GoogleAuthProvider.credentialWithToken(googleSignInAccount.getIdToken());
                        AGConnectAuthLink(credential, TAG, AGConnectAuthCredential.Google_Provider);
                    })
                    .addOnFailureListener(e -> handleError(e, TAG));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    private void login() {
        if (isGmsAvailable())
            startActivityForResult(client.getSignInIntent(), GOOGLE_SIGN_CODE);
        else
            loginLinkWithOpenid();
    }

    private void link() {
        boolean isLinked = isProviderLinked(getAGConnectUser(), AGConnectAuthCredential.Google_Provider);
        if (isLinked)
            unlink(AGConnectAuthCredential.Google_Provider, TAG);
        else {
            if (isGmsAvailable())
                startActivityForResult(client.getSignInIntent(), LINK_CODE);
            else
                loginLinkWithOpenid();
        }
    }

    private boolean isGmsAvailable() {
        return ConnectionResult.SUCCESS ==
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
    }


    private void loginLinkWithOpenid() {
        AuthorizationServiceConfiguration configuration = new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth") /* auth endpoint */,
                Uri.parse("https://www.googleapis.com/oauth2/v4/token") /* token endpoint */);
        Uri redirectUri = Uri.parse("com.example.authservice:/oauth2callback");
        String clientId = getString(R.string.google_app_id);
        AuthorizationRequest.Builder builder = new AuthorizationRequest
                .Builder(configuration, clientId, AuthorizationRequest.RESPONSE_TYPE_CODE, redirectUri);
        builder.setScopes(AuthorizationService.SCOPE_PROFILE);
        AuthorizationRequest request = builder.build();

        Intent postAuthorizationIntent = new Intent(ACTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, request.hashCode(), postAuthorizationIntent, 0);

        AuthorizationService authorizationService = new AuthorizationService(this);
        authorizationService.performAuthorizationRequest(request, pendingIntent);
        finish();
    }

    private void checkIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ACTION) && intent.hasExtra(USED_INTENT)) {
            handleAuthorizationResponse(intent);
            intent.putExtra(USED_INTENT, true);
        }
    }

    private void handleAuthorizationResponse(Intent intent) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        AuthState authState = new AuthState(response, error);
        if (response == null)
            return;

        Log.i(TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
        AuthorizationService service = new AuthorizationService(this);
        service.performTokenRequest(response.createTokenExchangeRequest(), (tokenResponse, exception) -> {
            if (exception != null)
                Log.w(TAG, "Token Exchange failed" + exception);
            else {
                Log.i(TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]",
                        tokenResponse.accessToken,
                        tokenResponse.idToken));
                //AGC will come in screen after getting token
                //Obtain the **idToken** after login authorization.
                AGConnectAuthCredential credential = GoogleAuthProvider.credentialWithToken(tokenResponse.idToken);
                if (getAGConnectUser() == null)
                    AGConnectAuthLogin(credential, TAG, AGConnectAuthCredential.Google_Provider);
                else
                    AGConnectAuthLink(credential, TAG, AGConnectAuthCredential.Google_Provider);
            }
        });
    }
}
