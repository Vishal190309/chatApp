package com.mechat.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mechat.app.OtpActivity;
import com.mechat.app.R;
import com.mechat.app.model.User;

import java.util.Arrays;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 60;
    private EditText Email;
    private EditText Password;
    private FirebaseAuth mAuth;
    private DatabaseReference myDatabaseRef;
    private GoogleSignInClient mGoogleSignInClient;
    private com.mechat.app.databinding.ActivityLoginBinding binding;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        Button loginButton = binding.signinButton;
        Email = binding.emailText;
        Password = binding.password;
        Email.requestFocus();
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase myDatabase = FirebaseDatabase.getInstance();
        myDatabaseRef = myDatabase.getReference();
        binding.singup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mCallbackManager = CallbackManager.Factory.create();
        binding.buttonFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.facebook.performClick();
            }
        });
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
        }

        binding.facebook.setReadPermissions("email", "public_profile");
        binding.facebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("TAG", "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d("TAG", "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d("TAG", "facebook:onError", error);
            }
        });


        binding.forgotPassword.setOnClickListener(view -> {
            if (Email.length() <= 0 || Email.getText() == null) {
                Email.setError("Please Enter Email to reset Password ");


            } else {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                mAuth.sendPasswordResetEmail(Email.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Snackbar.make(binding.forgotPassword, "Reset mail sent,check your mailbox", Snackbar.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        });

        binding.googleButton.setOnClickListener(view -> GooglesignIn());
        loginButton.setOnClickListener(v -> {

            if (ValidateFields()) {
                if (TextUtils.isDigitsOnly(binding.emailText.getText())) {
                    Intent intent = new Intent(LoginActivity.this, OtpActivity.class);
                    intent.putExtra("phoneNumber", "+91" + binding.emailText.getText().toString().trim());
                    intent.putExtra("userName", "");
                    startActivity(intent);
                } else {
                    Login();
                }

            }

        });

        if (mAuth != null) {
            if (mAuth.getCurrentUser() != null) {
                if (mAuth.getCurrentUser().isEmailVerified() || mAuth.getCurrentUser().getPhoneNumber() != null) {

                    startActivity(new Intent(LoginActivity.this, UserActivity.class));
                    finish();
                }
            }
        }

    }


    private void GooglesignIn() {
        mGoogleSignInClient.signOut();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("TAG", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            User users = new User();
                            assert user != null;
                            users.setEmail(user.getEmail());
                            users.setProfilePic(user.getPhotoUrl().toString());
                            users.setUserName(user.getDisplayName());
                            users.setUserId(user.getUid());
                            myDatabaseRef.child("Users").child(user.getUid()).setValue(users);
                            startActivity(new Intent(LoginActivity.this, UserActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                assert account != null;
                Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());


            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Snackbar.make(binding.googleButton, "Google sign in failed", Snackbar.LENGTH_SHORT);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        User users = new User();
                        assert user != null;
                        users.setEmail(user.getEmail());
                        users.setProfilePic(user.getPhotoUrl().toString());
                        users.setUserName(user.getDisplayName());
                        users.setUserId(user.getUid());
                        myDatabaseRef.child("Users").child(user.getUid()).setValue(users);
                        startActivity(new Intent(LoginActivity.this, UserActivity.class));
                        finish();

                    } else {
                        // If sign in fails, display a message to the user.
                        Snackbar.make(binding.googleButton, "Google sign in failed", Snackbar.LENGTH_SHORT);
                    }
                });
    }

    private boolean ValidateFields() {
        if (Email.length() <= 0 || Email.getText() == null) {
            Email.setError("Please Enter Email");
            return false;
        } else if (Password.length() <= 5 || Password.getText() == null) {
            Password.setError("Please Enter Passsword");
            return false;
        } else {
            return true;
        }
    }

    private void Login() {

        mAuth.signInWithEmailAndPassword(Email.getText().toString().trim(), Password.getText().toString().trim())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("TAG", "signInWithEmail:success");

                        final FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        if (user.isEmailVerified()) {


                            Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this, "Please Verify Your Email", Toast.LENGTH_SHORT).show();
                        }


                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("TAG", "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();

                    }

                });


    }
}