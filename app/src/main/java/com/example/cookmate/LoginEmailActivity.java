package com.example.cookmate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LoginEmailActivity extends AppCompatActivity {

    private EditText etEmail, etOtp;
    private Button btnVerify;
    private TextView tvResendOtp, tvCountdown;
    private SessionManager session;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_email);

        etEmail = findViewById(R.id.et_email);
        etOtp = findViewById(R.id.et_otp);
        btnVerify = findViewById(R.id.btn_verify);
        tvResendOtp = findViewById(R.id.tv_resend_otp);
        tvCountdown = findViewById(R.id.tv_countdown);
        session = new SessionManager(this);

        // Hiện bàn phím khi nhấn vào ô email
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etEmail, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Gửi OTP
        tvResendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            tvResendOtp.setEnabled(false);
            startCountdown();

            OtpApiService.sendOtp(email, response -> {
                runOnUiThread(() -> {
                    if (response.has("message")) {
                        Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, response.optString("error", "Send failed"), Toast.LENGTH_SHORT).show();
                        resetCountdown();
                    }
                });
            });
        });

        // Xác minh OTP
        btnVerify.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String otp = etOtp.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(otp)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            OtpApiService.verifyOtp(email, otp, response -> {
                runOnUiThread(() -> {
                    if (response.has("token")) {
                        try {
                            JSONObject userJson = response.getJSONObject("user");
                            User user = new User(
                                    userJson.optString("_id"),
                                    userJson.optString("email"),
                                    userJson.optString("name"),
                                    userJson.optString("avatar"),
                                    jsonArrayToList(userJson.optJSONArray("dietaryPreferences"))
                            );
                            String token = response.optString("token");
                            session.saveAuthData(token, user);

                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, ProfileActivity.class));
                            finish();

                        } catch (Exception e) {
                            Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, response.optString("error", "Invalid OTP"), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    // Đếm ngược 60 giây
    private void startCountdown() {
        tvCountdown.setText("Resend available in 60s");
        countDownTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Resend available in " + (millisUntilFinished / 1000) + "s");
            }

            public void onFinish() {
                resetCountdown();
            }
        }.start();
    }

    private void resetCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        tvCountdown.setText("");
        tvResendOtp.setEnabled(true);
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(array.optString(i));
        return list;
    }
}
