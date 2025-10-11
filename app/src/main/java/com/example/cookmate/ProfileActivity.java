package com.example.cookmate;

//import android.os.Bundle;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//public class ProfileActivity extends AppCompatActivity {
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_profile);
//
//        // chọn đúng item tương ứng với activity này
//        NavHelper.setupBottomNav(this, R.id.navigation_profile);

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView profileImage;
    private TextView userName, userEmail, dietValue;
    private MaterialButton logoutButton;
    private Uri selectedImageUri = null;

    private SessionManager session;
    private User user;
    private ProfileApiService profileApiService;

    private static final int REQUEST_STORAGE_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // chọn đúng item tương ứng với activity này
        NavHelper.setupBottomNav(this, R.id.navigation_profile);

        profileImage = findViewById(R.id.profile_image);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        dietValue = findViewById(R.id.diet_value);
        logoutButton = findViewById(R.id.log_out_button);

        session = new SessionManager(this);
        profileApiService = new ProfileApiService(this);
        user = session.getUser();

        if (user != null) {
            userName.setText(user.getName());
            userEmail.setText(user.getEmail());
            if (user.getDietaryPreferences() != null && !user.getDietaryPreferences().isEmpty()) {
                dietValue.setText(String.join(", ", user.getDietaryPreferences()));
            }
            Glide.with(this).load(user.getAvatar()).into(profileImage);
        }

        // Mở thư viện ảnh
        profileImage.setOnClickListener(v -> {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                        REQUEST_STORAGE_PERMISSION);
//            } else {
//                openGallery();
//            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            REQUEST_STORAGE_PERMISSION);
                } else {
                    openGallery();
                }
            } else {
                // Android 12 trở xuống
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_STORAGE_PERMISSION);
                } else {
                    openGallery();
                }
            }

        });

        // Chỉnh sửa tên
        userName.setOnClickListener(v -> showEditDialog("Edit Name", userName.getText().toString(), newName -> {
            userName.setText(newName);
            user.setName(newName);
            updateProfile();
        }));

        // Chỉnh sửa email
        userEmail.setOnClickListener(v -> showEditDialog("Edit Email", userEmail.getText().toString(), newEmail -> {
            userEmail.setText(newEmail);
            user.setEmail(newEmail);
            updateProfile();
        }));

        // Chỉnh sửa chế độ ăn
        dietValue.setOnClickListener(v -> showEditDialog("Edit Diet", dietValue.getText().toString(), newDiet -> {
            dietValue.setText(newDiet);
            user.setDietaryPreferences(Arrays.asList(newDiet.split(",")));
            updateProfile();
        }));

        // Log out
        logoutButton.setOnClickListener(v -> {
            session.clear();
            Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });
    }

    // Mở thư viện ảnh
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // Nhận kết quả chọn ảnh
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(profileImage);
                    user.setAvatar(selectedImageUri.toString());
                    updateProfile();
                }
            }
    );

    // Dialog chỉnh sửa
    private void showEditDialog(String title, String currentValue, OnValueEdited callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setText(currentValue);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) callback.onEdited(newValue);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Interface callback
    private interface OnValueEdited {
        void onEdited(String newValue);
    }

    // Gửi request PUT cập nhật
    private void updateProfile() {
        JSONObject body = new JSONObject();
        try {
            body.put("name", user.getName());
            body.put("avatar", user.getAvatar());
            body.put("dietaryPreferences", new JSONArray(user.getDietaryPreferences()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        profileApiService.updateProfile(body, response -> {
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            try {
                JSONObject userJson = response.getJSONObject("user");
                User updatedUser = new User(
                        userJson.getString("id"),
                        userJson.getString("email"),
                        userJson.getString("name"),
                        userJson.getString("avatar"),
                        jsonArrayToList(userJson.getJSONArray("dietaryPreferences"))
                );
                session.saveAuthData(session.getToken(), updatedUser);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            Toast.makeText(this, "Update failed!", Toast.LENGTH_SHORT).show();
        });
    }

    private List<String> jsonArrayToList(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(array.getString(i));
        return list;
    }

    // Quyền đọc ảnh
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
