package com.eitangrimblat.nutriscan.firebase;

import android.app.Activity;
import android.util.Log;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.eitangrimblat.nutriscan.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Utility class for managing user authentication via Firebase Auth and Google Sign-In.
 */
public class Auth {
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * Signs in a user with email and password.
     */
    public static void signIn(Activity activity, String email, String password, OnCompleteRunnable<Void> onComplete) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                Log.d("Eitan Debug Auth", "Signed in");
                onComplete.onComplete(null, true, null);
            }
            else {
                Log.d("Eitan Debug Auth", "Sign in failed: " + task.getException().getMessage());
                onComplete.onComplete(null, false, task.getException().getMessage());
            }
        });
    }

    /**
     * Signs out the currently authenticated user.
     */
    public static void signOut() {
        auth.signOut();
    }

    /**
     * Registers a new user with email and password.
     */
    public static void signUp(Activity activity, String email, String password, OnCompleteRunnable<Void> onComplete) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                Log.d("Eitan Debug Auth", "Signed up");
                onComplete.onComplete(null, true, null);
            }
            else {
                Log.d("Eitan Debug Auth", "Sign up failed");
                onComplete.onComplete(null, false, task.getException().getMessage());
            }
        });
    }

    /**
     * Returns the currently signed-in {@link FirebaseUser}.
     */
    public static FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Updates the current user's profile information.
     */
    public static void updateProfile(UserProfileChangeRequest request, OnCompleteRunnable<Void> onComplete) {
        if (getCurrentUser() == null)
            onComplete.onComplete(null, false, "User not signed in");

        getCurrentUser().updateProfile(request).addOnCompleteListener(task -> {
            if (task.isSuccessful())
                onComplete.onComplete(null, true, null);
            else
                onComplete.onComplete(null, false, task.getException().getMessage());
        });
    }

    /**
     * Sends a password reset email to the current user's email address.
     */
    public static void sendPasswordResetEmail(OnCompleteRunnable<Void> onComplete) {
        if (getCurrentUser() == null) {
            onComplete.onComplete(null, false, "no user connected");
            return;
        }
        if (getCurrentUser().getEmail() == null || getCurrentUser().getEmail().isEmpty()) {
            onComplete.onComplete(null, false, "user does not have an email");
            return;
        }

        auth.sendPasswordResetEmail(getCurrentUser().getEmail()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onComplete.onComplete(null, true, null);
            } else {
                onComplete.onComplete(null, false, task.getException().getMessage());
            }
        });
    }

    /**
     * Initiates Google Sign-In using Credential Manager.
     */
    public static void signInWithGoogle(Activity activity, OnCompleteRunnable<Void> onCompleteRunnable) {
        CredentialManager credentialManager = CredentialManager.create(activity);

        // 1. Configure the Google ID Token request using your Firebase Web Client ID
        GetSignInWithGoogleOption googleIdOption = new GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id))
            .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build();

        // 2. Launch the Credential Manager system UI sheet
        credentialManager.getCredentialAsync(activity, request, null, Runnable::run,
            new androidx.credentials.CredentialManagerCallback<>() {
                @Override
                public void onResult(GetCredentialResponse response) {
                    androidx.credentials.Credential credential = response.getCredential();

                    // 1. Check if the credential is a CustomCredential wrapper
                    if (credential instanceof androidx.credentials.CustomCredential &&
                        (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) ||
                            credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL))) {

                        // 2. Unpack the custom credential bundle using the static helper method
                        androidx.credentials.CustomCredential customCredential = (androidx.credentials.CustomCredential) credential;
                        GoogleIdTokenCredential tokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());

                        String idToken = tokenCredential.getIdToken();

                        // 3. Link the token with Firebase Auth
                        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                        auth.signInWithCredential(firebaseCredential).addOnCompleteListener(activity, task -> {
                            if (task.isSuccessful()) {
                                Log.d("Eitan Debug Auth", "Google Sign-In successful");
                                onCompleteRunnable.onComplete(null, true, null);
                            } else {
                                Log.d("Eitan Debug Auth", "Firebase Google Auth failed");
                                onCompleteRunnable.onComplete(null, false, task.getException() != null ? task.getException().getMessage() : "Firebase Auth error");
                            }
                        });
                    } else {
                        // Log the actual type string so you can see exactly what came back if this fails
                        Log.w("Eitan Debug Auth", "Received type: " + credential.getType());
                        onCompleteRunnable.onComplete(null, false, "Unexpected credential type returned.");
                    }
                }

                @Override
                public void onError(GetCredentialException e) {
                    Log.e("Eitan Debug Auth", "Credential Manager failed: " + e.getMessage());
                    onCompleteRunnable.onComplete(null, false, e.getMessage());
                }
            });
    }

    /**
     * Re-authenticates a Google user to allow sensitive operations.
     */
    public static void reauthenticateGoogleUser(Activity activity, OnCompleteRunnable<Void> onComplete) {
        FirebaseUser user = getCurrentUser();

        if (user == null) {
            onComplete.onComplete(null, false, "No user is currently signed in.");
            return;
        }

        CredentialManager credentialManager = CredentialManager.create(activity);

        // 1. Configure the Google ID Token request
        GetSignInWithGoogleOption googleIdOption = new GetSignInWithGoogleOption.Builder(
            activity.getString(R.string.default_web_client_id)
        ).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build();

        // 2. Launch the Credential Manager system UI sheet
        credentialManager.getCredentialAsync(activity, request, null, Runnable::run,
            new androidx.credentials.CredentialManagerCallback<>() {
                @Override
                public void onResult(GetCredentialResponse response) {
                    androidx.credentials.Credential credential = response.getCredential();

                    if (credential instanceof androidx.credentials.CustomCredential &&
                        (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) ||
                            credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL))) {

                        androidx.credentials.CustomCredential customCredential = (androidx.credentials.CustomCredential) credential;
                        GoogleIdTokenCredential tokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());

                        String idToken = tokenCredential.getIdToken();

                        // 3. Generate the AuthCredential mapping
                        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

                        // 4. CRITICAL CHANGE: Reauthenticate the CURRENT active user instead of standard signing-in
                        user.reauthenticate(firebaseCredential)
                        .addOnCompleteListener(activity, task -> {
                            if (task.isSuccessful()) {
                                Log.d("Eitan Debug Auth", "Google Re-auth successful!");
                                onComplete.onComplete(null, true, null);
                            } else {
                                Log.e("Eitan Debug Auth", "Firebase Google Re-auth failed");
                                onComplete.onComplete(null, false, task.getException() != null ? task.getException().getMessage() : "Firebase Re-auth error");
                            }
                        });
                    } else {
                        onComplete.onComplete(null, false, "Unexpected credential type.");
                    }
                }

                @Override
                public void onError(GetCredentialException e) {
                    Log.e("Eitan Debug Auth", "Credential Manager failed: " + e.getMessage());
                    onComplete.onComplete(null, false, e.getMessage());
                }
            });
    }
}
