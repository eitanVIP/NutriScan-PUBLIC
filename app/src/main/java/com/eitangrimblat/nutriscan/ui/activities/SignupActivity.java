package com.eitangrimblat.nutriscan.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupActivity extends AppCompatActivity {
    private EditText edEmail;
    private EditText edPassword;
    private EditText edName;
    private TextInputLayout ilName;
    private TextInputLayout ilEmail;
    private TextInputLayout ilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Auth.getCurrentUser() != null) {
            Intent intent = new Intent(SignupActivity.this, MainAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutSignup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edEmail = findViewById(R.id.edSignupEmail);
        edPassword = findViewById(R.id.edSignupPassword);
        edName = findViewById(R.id.edSignupName);
        ilName = findViewById(R.id.ilSignupName);
        ilEmail = findViewById(R.id.ilSignupEmail);
        ilPassword = findViewById(R.id.ilSignupPassword);

        findViewById(R.id.bcmSignupSignup).setOnClickListener(v -> {
            ilName.setError(null);
            ilEmail.setError(null);
            ilPassword.setError(null);

            if(edEmail.getText().toString().isEmpty() || edPassword.getText().toString().isEmpty() || edName.getText().toString().isEmpty()) {
                Snackbar.make(findViewById(R.id.layoutSignup), "Signup Failed: Please input all required fields", Snackbar.LENGTH_LONG).show();

                if (edName.getText().toString().isEmpty())
                    ilName.setError("Please input all required fields");
                if (edEmail.getText().toString().isEmpty())
                    ilEmail.setError("Please input all required fields");
                if (edPassword.getText().toString().isEmpty())
                    ilPassword.setError("Please input all required fields");

                return;
            }

            Auth.signUp(SignupActivity.this, edEmail.getText().toString(), edPassword.getText().toString(), (unused, success, exception) -> {
                if (success) {
                    UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                            .setDisplayName(edName.getText().toString())
                            .build();

                    Auth.updateProfile(request, (profileUnused, profileSuccess, profileException) -> {
                        if (profileSuccess) {
                            Snackbar.make(findViewById(R.id.layoutSignup), "Signup Successful", Snackbar.LENGTH_LONG).show();

                            Intent intent = new Intent(SignupActivity.this, MainAppActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Snackbar.make(findViewById(R.id.layoutSignup), "Signup failed: " + profileException, Snackbar.LENGTH_LONG).show();
                            ilName.setError(profileException);
                        }
                    });
                } else {
                    Snackbar.make(findViewById(R.id.layoutSignup), "Signup Failed: " + exception, Snackbar.LENGTH_LONG).show();

                    if (exception.contains("email") && exception.contains("already in use")) {
                        ilEmail.setError("This email is already registered.");
                    } else if (exception.contains("email") || exception.contains("badly formatted")) {
                        ilEmail.setError("Please enter a valid email address.");
                    } else if (exception.contains("password")) {
                        ilPassword.setError("Password must be at least 6 characters.");
                    }
                }
            });
        });
    }
}