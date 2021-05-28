package com.mechat.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mechat.app.activity.UserActivity;
import com.mechat.app.databinding.ActivityOtpBinding;
import com.mechat.app.model.User;
import com.mukesh.OnOtpCompletionListener;

import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {

    ActivityOtpBinding binding;
    FirebaseAuth auth;

    String verificationId;
    DatabaseReference myDatabaseRef;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_otp);
        FirebaseDatabase myDatabase = FirebaseDatabase.getInstance();
        myDatabaseRef = myDatabase.getReference();
        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending OTP...");
        dialog.setCancelable(false);
        dialog.show();

        auth = FirebaseAuth.getInstance();


        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        String userName = getIntent().getStringExtra("userName");

        binding.phoneLbl.setText("Verify " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(OtpActivity.this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {

                    }

                    @Override
                    public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verifyId, forceResendingToken);
                        dialog.dismiss();
                        verificationId = verifyId;


                        binding.otpView.requestFocus();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);

        binding.otpView.setOtpCompletionListener(new OnOtpCompletionListener() {
            @Override
            public void onOtpCompleted(String otp) {
                dialog = new ProgressDialog(OtpActivity.this);
                dialog.setMessage("Loggin IN...");
                dialog.setCancelable(false);
                dialog.show();
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

                auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();

                            User users = new User();
                            if (!userName.isEmpty()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(userName).build();

                                assert user != null;
                                user.updateProfile(profileUpdates);

                                users.setEmail(user.getPhoneNumber());
                                users.setUserName(userName);
                                users.setUserId(user.getUid());
                                myDatabaseRef.child("Users").child(user.getUid()).setValue(users);
                            }
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(binding.continueBtn.getWindowToken(), 0);
                            Intent intent = new Intent(OtpActivity.this, UserActivity.class);
                            startActivity(intent);
                            finishAffinity();
                        } else {
                            Toast.makeText(OtpActivity.this, "Failed.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });


    }
}