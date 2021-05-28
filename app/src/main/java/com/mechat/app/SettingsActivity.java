package com.mechat.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mechat.app.databinding.ActivitySettingsBinding;
import com.mechat.app.model.User;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private Uri selectedImage;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        String message_sound = preferences.getString("sound", "on");
        if (message_sound.equals("on")) {
            binding.switchSound.setChecked(true);
        } else {
            binding.switchSound.setChecked(false);
        }
        binding.switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    setSound("on");
                } else {
                    setSound("off");
                }
            }
        });


        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    userName = user.getUserName();
                    binding.nameBox.setText(userName);
                    Glide.with(SettingsActivity.this).load(user.getProfilePic()).placeholder(R.drawable.user_placeholder).circleCrop().into(binding.imageView);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);

            }
        });
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog(SettingsActivity.this);
                progressDialog.setTitle("Updating Profile");
                progressDialog.setMessage("Please wait while we update your profile");
                progressDialog.show();
                String name = binding.nameBox.getText().toString().trim();
                if (name.isEmpty()) {
                    binding.nameBox.setError("Please type your name");
                    return;
                }
                if (selectedImage != null) {
                    StorageReference storageReference = storage.getReference().child("profiles").child(Objects.requireNonNull(auth.getUid()));
                    storageReference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(binding.nameBox.getText().toString().trim()).build();
                                        auth.getCurrentUser().updateProfile(profileUpdates);
                                        database.getReference().child("Users").child(Objects.requireNonNull(auth.getCurrentUser()).getUid()).child("profilePic").setValue(uri.toString());
                                        database.getReference().child("Users").child(Objects.requireNonNull(auth.getCurrentUser()).getUid()).child("userName").setValue(binding.nameBox.getText().toString().trim());
                                        progressDialog.dismiss();
                                        Snackbar.make(binding.continueBtn, "Profile Updated", Snackbar.LENGTH_LONG).show();
//


                                    }
                                });
                            } else {
                                progressDialog.dismiss();
                                Snackbar.make(binding.continueBtn, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()), Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });

                } else {

//
                    database.getReference().child("Users").child(Objects.requireNonNull(auth.getCurrentUser()).getUid()).child("userName").setValue(binding.nameBox.getText().toString().trim());
                    progressDialog.dismiss();
                    Snackbar.make(binding.continueBtn, "Profile Updated", Snackbar.LENGTH_LONG).show();
                }
            }
        });


    }

    private void setSound(String sound) {
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("sound", sound);
        editor.apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (data.getData() != null) {
                Glide.with(this).load(data.getData()).circleCrop().into(binding.imageView);
                selectedImage = data.getData();
            }
        }
    }
}