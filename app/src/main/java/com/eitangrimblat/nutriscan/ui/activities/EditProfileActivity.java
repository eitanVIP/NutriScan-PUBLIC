package com.eitangrimblat.nutriscan.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.firebase.Database;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Activity that allows users to update their profile information, including display name and avatar.
 */
public class EditProfileActivity extends AppCompatActivity {
    private ShapeableImageView imgAvatar;
    private FloatingActionButton fabPicture;
    private EditText edName;
    private EditText edEmail;
    private EditText edPassword;
    private TextInputLayout ilPassword;
    private Button bcmSave;
    private Button bcmSaveGoogle;
    private ImageButton bcmBack;
    private SwipeRefreshLayout srlEditProfile;

    private Bitmap newImage;
    private Uri tempImageUri;
    private boolean loading = false;

    // Launcher for Gallery
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    imgAvatar.setImageURI(uri);
                    newImage = getBitmapFromUri(uri);
                }
            });

    // Launcher for Camera
    private final ActivityResultLauncher<Uri> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
            if (isSuccess) {
                newImage = getBitmapFromUri(tempImageUri);

                if (newImage != null) {
                    saveBitmapToFile(newImage);
                    imgAvatar.setImageBitmap(newImage);
                }
            }
        });

    /**
     * Saves a Bitmap to a temporary file for camera processing.
     */
    private void saveBitmapToFile(Bitmap bitmap) {
        File tempFile = new File(getExternalCacheDir(), "temp_image.jpg");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Decodes a Bitmap from a given Uri.
     */
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } catch (IOException e) {
            Snackbar.make(findViewById(R.id.layoutEditProfile), "Failed to load image", Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a Bitmap from the temporary camera file.
     */
    private Bitmap getBitmapFromCamera() {
        File tempFile = new File(getExternalCacheDir(), "temp_image.jpg");
        if (tempFile.exists()) {
            return BitmapFactory.decodeFile(tempFile.getAbsolutePath());
        }
        return null;
    }

    /**
     * Launches the system photo picker.
     */
    private void openGallery() {
        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Launches the camera app to capture a new profile picture.
     */
    private void openCamera() {
        File tempFile = new File(getExternalCacheDir(), "temp_image.jpg");
        tempImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
        cameraLauncher.launch(tempImageUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutEditProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imgAvatar = findViewById(R.id.imgEditProfileAvatar);
        fabPicture = findViewById(R.id.fabEditProfilePicture);
        edName = findViewById(R.id.edEditProfileName);
        edEmail = findViewById(R.id.edEditProfileEmail);
        edPassword = findViewById(R.id.edEditProfileConfirmPassword);
        ilPassword = findViewById(R.id.ilEditProfilePassword);
        bcmSave = findViewById(R.id.bcmEditProfileSave);
        bcmSaveGoogle = findViewById(R.id.bcmEditProfileSaveGoogle);
        bcmBack = findViewById(R.id.bcmEditProfileBack);
        srlEditProfile = findViewById(R.id.srlEditProfile);

        if (Auth.getCurrentUser() == null) {
            Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        srlEditProfile.setEnabled(false);

        if (Auth.getCurrentUser().getPhotoUrl() != null) {
            Glide.with(this)
                .load(Auth.getCurrentUser().getPhotoUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into((ImageView) findViewById(R.id.imgEditProfileAvatar));
        }

        edEmail.setText(Auth.getCurrentUser().getEmail());
        edEmail.setEnabled(false);
        edName.setText(Auth.getCurrentUser().getDisplayName());

        bcmBack.setOnClickListener(v -> {
            startActivity(new Intent(EditProfileActivity.this, SettingsActivity.class));
        });

        fabPicture.setOnClickListener(v -> {
            String[] options = { "Take Photo", "Choose from Gallery" };
            new MaterialAlertDialogBuilder(this)
            .setTitle("Change Profile Picture")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openCamera();
                } else {
                    openGallery();
                }
            }).show();
        });

        bcmSave.setOnClickListener(v -> {
            if (loading)
                return;
            loading = true;
            srlEditProfile.setRefreshing(true);

            if (edPassword.getText().toString().isEmpty()) {
                Snackbar.make(findViewById(R.id.layoutEditProfile), "Incorrect password", Snackbar.LENGTH_LONG).show();
                ilPassword.setError("Incorrect password");
                loading = false;
                srlEditProfile.setRefreshing(false);
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(Auth.getCurrentUser().getEmail(), edPassword.getText().toString());

            Auth.getCurrentUser().reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    buildUpdateProfileRequest();
                } else {
                    Snackbar.make(findViewById(R.id.layoutEditProfile), "Incorrect password", Snackbar.LENGTH_LONG).show();
                    ilPassword.setError("Incorrect password");
                    loading = false;
                    srlEditProfile.setRefreshing(false);
                }
            });
        });

        bcmSaveGoogle.setOnClickListener(v -> {
            if (loading)
                return;
            loading = true;
            srlEditProfile.setRefreshing(true);

            Auth.reauthenticateGoogleUser(EditProfileActivity.this, (unused, success, exception) -> {
                if (success) {
                    buildUpdateProfileRequest();
                } else {
                    Snackbar.make(findViewById(R.id.layoutEditProfile), "Failed to authenticate google user", Snackbar.LENGTH_LONG).show();
                    loading = false;
                    srlEditProfile.setRefreshing(false);
                }
            });
        });
    }

    /**
     * Constructs and executes a profile update request based on user input.
     */
    private void buildUpdateProfileRequest() {
        UserProfileChangeRequest.Builder request = new UserProfileChangeRequest.Builder();
        boolean changedSomething = false;

        if (!edName.getText().toString().isEmpty()) {
            request.setDisplayName(edName.getText().toString());
            changedSomething = true;
        }

        if (newImage != null) {
            if (Auth.getCurrentUser().getPhotoUrl() != null) {
                Database.deleteFile(Auth.getCurrentUser().getPhotoUrl(), (unused, deleteSuccess, deleteException) -> {
                    if (!deleteSuccess)
                        Log.d("Eitan Debug Settings", "Failed to delete old profile image: " + deleteException);
                });
            }
            Database.storeImage(newImage, "profile_" + System.currentTimeMillis() + ".jpg", 40, (pictureUri, pictureSuccess, pictureException) -> {
                if (pictureSuccess) {
                    request.setPhotoUri(pictureUri);
                } else {
                    Snackbar.make(findViewById(R.id.layoutEditProfile), "Failed to update profile", Snackbar.LENGTH_LONG).show();
                    Log.d("Eitan Debug Settings", "Failed to update profile (image): " + pictureException);
                }

                updateProfile(request.build());
            });
            return;
        }

        if (!changedSomething) {
            Snackbar.make(findViewById(R.id.layoutEditProfile), "Nothing changed", Snackbar.LENGTH_LONG).show();
            loading = false;
            srlEditProfile.setRefreshing(false);
            return;
        }

        updateProfile(request.build());
    }

    /**
     * Applies the profile changes via Firebase Auth.
     */
    private void updateProfile(UserProfileChangeRequest request) {
        Auth.updateProfile(request, (profileUnused, profileSuccess, profileException) -> {
            if (profileSuccess) {
                Snackbar.make(findViewById(R.id.layoutEditProfile), "Edit profile Successful", Snackbar.LENGTH_LONG).show();

                Intent intent = new Intent(EditProfileActivity.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Snackbar.make(findViewById(R.id.layoutSignup), "Edit profile failed: " + profileException, Snackbar.LENGTH_LONG).show();
            }
            loading = false;
            srlEditProfile.setRefreshing(false);
        });
    }
}
