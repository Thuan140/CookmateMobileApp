package com.example.cookmate;

import java.util.Random;

public class EmailOtpHelper {

    // 🔹 Giả lập việc gửi OTP về email (nếu có backend thật, thay đoạn này bằng API gửi email)
    public static String sendOtpToEmail(String email) {
        String otp = generateOtp();
        System.out.println("Sending OTP " + otp + " to " + email); 
        return otp;
    }

    private static String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
