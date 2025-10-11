package com.example.cookmate;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginEmailActivity extends AppCompatActivity {

    private EditText etEmail, etOtp;
    private Button btnVerify;
    private TextView tvResendOtp;

    private String generatedOtp = "";
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_email);

        context = this;
        etEmail = findViewById(R.id.et_email);
        etOtp = findViewById(R.id.et_otp);
        btnVerify = findViewById(R.id.btn_verify);
        tvResendOtp = findViewById(R.id.tv_resend_otp);

        // ⌨️ Khi người dùng nhấn Enter sau khi nhập email
        etEmail.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                String email = etEmail.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show();
                } else {
                    generatedOtp = EmailOtpHelper.sendOtpToEmail(email);
                    Toast.makeText(context, "OTP sent to " + email, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // 🔁 Gửi lại OTP
        tvResendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                generatedOtp = EmailOtpHelper.sendOtpToEmail(email);
                Toast.makeText(context, "OTP resent to " + email, Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Xác minh OTP
        btnVerify.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            if (enteredOtp.equals(generatedOtp)) {
                String name = email.split("@")[0];
                String avatar = "https://example.com/avatar.jpg"; // giả định avatar mặc định

                AuthApiService api = new AuthApiService(context);
                api.loginWithGoogle(email, email, name, avatar, new AuthApiService.AuthCallback() {
                    @Override
                    public void onSuccess(AuthResponse response) {
                        Toast.makeText(context, "Welcome " + response.getUser().getName(), Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(context, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
