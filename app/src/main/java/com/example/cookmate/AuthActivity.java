package com.example.cookmate;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout btnGoogleLogin, btnEmailLogin;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100; // request code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        btnGoogleLogin = findViewById(R.id.btn_google_login);
        btnEmailLogin = findViewById(R.id.btn_email_login);

        // 🔹 1. Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // lấy token xác thực từ Google
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Mở Activity Email Login
        btnEmailLogin.setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, LoginEmailActivity.class);
            startActivity(intent);
        });

        // Xử lý Google Login
        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);  
        });
//        btnGoogleLogin.setOnClickListener(v -> {
//            // Mock dữ liệu Google trả về
//            String googleUserId = "123456789";
//            String email = "user@example.com";
//            String name = "John Doe";
//            String avatar = "https://example.com/avatar.jpg";
//
//            // 🔹 Gọi API Auth để test
//            AuthApiService authApi = new AuthApiService(AuthActivity.this);
//            authApi.loginWithGoogle(googleUserId, email, name, avatar, new AuthApiService.AuthCallback() {
//                @Override
//                public void onSuccess(AuthResponse response) {
//                    // Lưu token và user
//                    SessionManager session = new SessionManager(AuthActivity.this);
//                    session.saveAuthData(response.getToken(), response.getUser());
//
//                    Toast.makeText(AuthActivity.this,
//                            "Welcome " + response.getUser().getName(),
//                            Toast.LENGTH_LONG).show();
//
//                    // Chuyển sang ProfileActivity
//                    Intent intent = new Intent(AuthActivity.this, ProfileActivity.class);
//                    startActivity(intent);
//                    finish();
//                }
//
//                @Override
//                public void onError(String errorMessage) {
//                    Toast.makeText(AuthActivity.this,
//                            "Error: " + errorMessage,
//                            Toast.LENGTH_LONG).show();
//                }
//            });
//        });
    }

    // 🔹 2. Nhận kết quả đăng nhập Google
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    // 🔹 3. Xử lý kết quả Google Sign-In
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                String googleUserId = account.getId();
                String email = account.getEmail();
                String name = account.getDisplayName();
                String avatar = (account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : "";

                // Gọi API Auth để xác thực với server Cookmate
                AuthApiService authApi = new AuthApiService(AuthActivity.this);
                authApi.loginWithGoogle(googleUserId, email, name, avatar, new AuthApiService.AuthCallback() {
                    @Override
                    public void onSuccess(AuthResponse response) {
                        // Lưu token và user
                        SessionManager session = new SessionManager(AuthActivity.this);
                        session.saveAuthData(response.getToken(), response.getUser());

                        Toast.makeText(AuthActivity.this,
                                "Welcome " + response.getUser().getName(),
                                Toast.LENGTH_LONG).show();

                        // Chuyển sang ProfileActivity
                        Intent intent = new Intent(AuthActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(AuthActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }

//        } catch (ApiException e) {
//            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
//            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
//        }
        } catch (ApiException e) {
            Log.e("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode() + " message=" + e.getMessage());
            Toast.makeText(this, "Google Sign-In failed: code=" + e.getStatusCode(), Toast.LENGTH_LONG).show();
        }
    }
}
