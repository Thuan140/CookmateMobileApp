package com.example.cookmate;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OtpRecoverActivity extends AppCompatActivity {

    private EditText etEmail, etOtp;
    private TextView tvResendOtp, tvCountdown;
    private Button btnVerify;
    private RecoverApiService recoverApiService;
    private CountDownTimer countDownTimer;
    private boolean canResend = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_recover);

        etEmail = findViewById(R.id.et_email);
        etOtp = findViewById(R.id.et_otp);
        tvResendOtp = findViewById(R.id.tv_resend_otp);
        tvCountdown = findViewById(R.id.tv_countdown);
        btnVerify = findViewById(R.id.btn_verify);

        recoverApiService = new RecoverApiService(this);

        tvResendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!canResend) {
                Toast.makeText(this, "Please wait before resending OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            recoverApiService.sendOtp(email, new RecoverApiService.RecoverCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(OtpRecoverActivity.this, message, Toast.LENGTH_SHORT).show();
                    startCountdown();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(OtpRecoverActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnVerify.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String otp = etOtp.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(otp)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            recoverApiService.verifyOtp(email, otp, new RecoverApiService.RecoverCallback() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(OtpRecoverActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(OtpRecoverActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void startCountdown() {
        canResend = false;
        tvResendOtp.setEnabled(false);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Resend available in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResendOtp.setEnabled(true);
                tvCountdown.setText("");
            }
        }.start();
    }
}
