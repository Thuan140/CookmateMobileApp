package com.example.cookmate;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiService {

    public static JSONObject postJson(String urlString, JSONObject body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            OutputStream os = new BufferedOutputStream(conn.getOutputStream());
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream()
            ));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            return new JSONObject(sb.toString());

        } catch (Exception e) {
            Log.e("ApiService", "Error: " + e.getMessage());
            JSONObject err = new JSONObject();
            try { err.put("error", e.getMessage()); } catch (Exception ignored) {}
            return err;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
