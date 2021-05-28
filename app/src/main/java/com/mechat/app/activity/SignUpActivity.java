package com.mechat.app.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.mechat.app.OtpActivity;
import com.mechat.app.R;
import com.mechat.app.model.User;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 60;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    private EditText Username;
    private EditText Email;
    private EditText Password;
    private Button SignUpButton;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageButton facebookButton;
    private ImageButton googleButton;
    DatabaseReference myDatabaseRef;
    private CallbackManager mCallbackManager;


    public SignUpActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.mechat.app.databinding.ActivitySignUpBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        SignUpButton = binding.signupButton;
        Username = binding.username;
        Email = binding.emailText;
        Password = binding.password;
        googleButton = binding.googleButton;
        Username.requestFocus();
        FirebaseDatabase myDatabase = FirebaseDatabase.getInstance();
        myDatabaseRef = myDatabase.getReference();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mCallbackManager = CallbackManager.Factory.create();

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
        binding.buttonFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.facebook.performClick();
            }
        });

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        DynamicLinkHandle();

        googleButton.setOnClickListener(view -> GooglesignIn());


        binding.singin.setOnClickListener(view -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
        SignUpButton.setOnClickListener(v -> {
            if (ValidateFields()) {
                if (TextUtils.isDigitsOnly(binding.emailText.getText())) {
                    Intent intent = new Intent(SignUpActivity.this, OtpActivity.class);
                    intent.putExtra("phoneNumber", "+91" + binding.emailText.getText().toString().trim());
                    intent.putExtra("userName", binding.username.getText().toString().trim());
                    startActivity(intent);
                } else {
                    SignUp(Username.getText().toString(), Email.getText().toString(), Password.getText().toString());
                }

            }
        });


    }

    private void GooglesignIn() {
        mGoogleSignInClient.signOut();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
                Snackbar.make(googleButton, "Google sign in failed", Snackbar.LENGTH_SHORT);
            }
        }
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
                            startActivity(new Intent(SignUpActivity.this, UserActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
                        startActivity(new Intent(SignUpActivity.this, UserActivity.class));
                        finish();

                    } else {
                        // If sign in fails, display a message to the user.
                        Snackbar.make(googleButton, "Google sign in failed", Snackbar.LENGTH_SHORT);
                    }
                });
    }

    private void DynamicLinkHandle() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent());
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    // Get deep link from result (may be null if no link is found)
                    Uri deepLink;
                    if (pendingDynamicLinkData != null) {
                        ProgressDialog progressDialog = new ProgressDialog(this);
                        progressDialog.setTitle("Registering account");
                        progressDialog.setMessage("Please wait while we register and log you in");
                        progressDialog.show();
                        deepLink = pendingDynamicLinkData.getLink();
                        assert deepLink != null;
                        mAuth.applyActionCode(deepLink.getQueryParameter("oobCode")).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    User newUser = new User();
                                    Objects.requireNonNull(mAuth.getCurrentUser()).reload();

                                    newUser.setUserName(Objects.requireNonNull(mAuth.getCurrentUser()).getDisplayName());
                                    newUser.setEmail(mAuth.getCurrentUser().getEmail());
                                    newUser.setUserId(mAuth.getCurrentUser().getUid());
                                    myDatabaseRef.child("Users").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid()).setValue(newUser);
                                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                    Toast.makeText(SignUpActivity.this, "Email Verified,Please Login", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(SignUpActivity.this, "Failed to signup", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                }
                                finish();
                            }
                        });


                    }

                })
                .addOnFailureListener(this, e -> {
                });
    }

    private boolean ValidateFields() {
        if (Username.length() <= 0 || Username.getText() == null) {
            Username.setError("Please Enter Username");
            return false;
        } else if (Email.length() <= 0 || Email.getText() == null) {
            Email.setError("Please Enter Email");
            return false;
        } else if (Password.length() <= 5 || Password.getText() == null) {
            Password.setError("Please Enter Password");
            return false;
        } else {
            return true;
        }
    }


    @Override
    protected void onStart() {
        signIn();

        super.onStart();
    }

    private void signIn() {
        if (mAuth != null) {
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
                startActivity(new Intent(SignUpActivity.this, UserActivity.class));
                finish();
            }
        }
    }

    private void SignUp(final String Username, String Email, String Password) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("Please Wait while we are creating your account...");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(Email, Password)
                .addOnCompleteListener(SignUpActivity.this, task -> {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();
                        Snackbar.make(SignUpButton, "Account Created,Sending Mail", Snackbar.LENGTH_SHORT).show();

                        FirebaseUser user = mAuth.getCurrentUser();

                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(Username).build();

                        assert user != null;
                        user.updateProfile(profileUpdates);


                        ActionCodeSettings actionCodeSettings =
                                ActionCodeSettings.newBuilder()
                                        // URL you want to redirect back to. The domain (www.example.com) for this
                                        // URL must be whitelisted in the Firebase Console.
                                        .setUrl("http://wallet-chat.co.uk/")
                                        // This must be true
                                        .setHandleCodeInApp(true)

                                        .setAndroidPackageName(
                                                "com.mechat.app",
                                                true, /* installIfNotAvailable */
                                                "12"    /* minimumVersion */)
                                        .build();
                        user.sendEmailVerification(actionCodeSettings)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {


                                        progressDialog.setTitle("Verify Mail");
                                        progressDialog.setMessage("Please Verify Your Mail...");
                                        progressDialog.show();


                                    } else {
                                        Toast.makeText(SignUpActivity.this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        // If sign in fails, display a message to the user.
                        Log.w("TAG", "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();

                    }

                });
    }


}