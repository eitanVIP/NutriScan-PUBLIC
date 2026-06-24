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

/**
 * The initial landing activity for authentication, supporting email/password and Google Sign-In.
 */
public class MainActivity extends AppCompatActivity {
    private EditText edEmail;
    private EditText edPassword;
    private TextInputLayout ilPassword;
    private TextInputLayout ilEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Auth.getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edEmail = findViewById(R.id.edMainEmail);
        edPassword = findViewById(R.id.edMainPassword);
        ilPassword = findViewById(R.id.ilMainPassword);
        ilEmail = findViewById(R.id.ilMainEmail);

        findViewById(R.id.bcmMainSignin).setOnClickListener(v -> {
            ilEmail.setError(null);
            ilPassword.setError(null);

            if(edEmail.getText().toString().isEmpty() || edPassword.getText().toString().isEmpty()) {
                Snackbar.make(findViewById(R.id.layoutMain), "Sign In Failed: Please input all required fields", Snackbar.LENGTH_LONG).show();

                if (edEmail.getText().toString().isEmpty())
                    ilEmail.setError("Please input all required fields");
                if (edPassword.getText().toString().isEmpty())
                    ilPassword.setError("Please input all required fields");

                return;
            }

            Auth.signIn(MainActivity.this, edEmail.getText().toString(), edPassword.getText().toString(), (unused, success, exception) -> {
                if (success) {
                    Snackbar.make(findViewById(R.id.layoutMain), "Sign In Successful", Snackbar.LENGTH_LONG).show();

                    Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Snackbar.make(findViewById(R.id.layoutMain), "Sign In Failed: " + exception, Snackbar.LENGTH_LONG).show();
                    ilPassword.setError("Incorrect Password");
                }
            });
        });

        findViewById(R.id.bcmMainSignup).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignupActivity.class));
        });

        findViewById(R.id.bcmMainGoogle).setOnClickListener(v -> {
            Auth.signInWithGoogle(MainActivity.this, (unused, success, exception) -> {
                if (success) {
                    Snackbar.make(findViewById(R.id.layoutMain), "Sign In Successful", Snackbar.LENGTH_LONG).show();

                    Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Snackbar.make(findViewById(R.id.layoutMain), "Sign In Failed: " + exception, Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }
}
