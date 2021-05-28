package com.mechat.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mechat.app.R;
import com.mechat.app.SettingsActivity;
import com.mechat.app.adapter.OnUserClick;
import com.mechat.app.adapter.UserAdapter;
import com.mechat.app.databinding.ActivityUserBinding;
import com.mechat.app.model.User;
import com.mechat.app.model.UserState;
import com.mechat.app.notifications.Token;
import com.mechat.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class UserActivity extends AppCompatActivity implements OnUserClick {
    private ActivityUserBinding binding;
    private ArrayList<User> userArrayList;
    private ArrayList<User> chatArrayList;
    private UserAdapter userAdapter;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private ValueEventListener showUserListner;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user);
        mAuth = FirebaseAuth.getInstance();
        userArrayList = new ArrayList<>();
        chatArrayList = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        userAdapter = new UserAdapter(userArrayList, this, this);
        binding.userRecyclerView.setHasFixedSize(true);
        binding.userRecyclerView.setAdapter(userAdapter);
        binding.userRecyclerView.showShimmerAdapter();
        FacebookSdk.sdkInitialize(getApplicationContext());

        binding.floatingActionButton2.setOnClickListener(view -> startActivity(new Intent(UserActivity.this, AllUsersActivity.class)));
        DatabaseReference scoresRef = FirebaseDatabase.getInstance().getReference("chatlist").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
        DatabaseReference UserRef = FirebaseDatabase.getInstance().getReference("Users");
        scoresRef.keepSynced(true);
        UserRef.keepSynced(true);
        userRef = database.getReference().child("Users");
        showUserListner = database.getReference().child("chatlist").child(FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    chatArrayList.clear();
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        User user1 = snapshot1.getValue(User.class);
                        chatArrayList.add(user1);

                    }
                    showUser(chatArrayList);
                } else {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.userRecyclerView.hideShimmerAdapter();
                            Toast.makeText(UserActivity.this, "No Chats", Toast.LENGTH_LONG).show();
                        }
                    }, 500);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {

                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                updateToken(token);

                // Log and toast

            }
        });


    }

    private void showUser(ArrayList<User> chatArrayList) {
        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                userArrayList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        User user1 = snapshot1.getValue(User.class);
                        for (User user2 : chatArrayList) {
                            if (user1.getUserId().equals(user2.getUserId())) {
                                user1.setLastMessage(user2.getLastMessage());
                                userArrayList.add(user1);
                            }
                        }
                    }
                    Comparator<User> comparator = new Comparator<User>() {
                        @Override
                        public int compare(final User object1, final User object2) {
                            return object1.getLastMessage().compareTo(object2.getLastMessage());
                        }
                    };
                    Collections.sort(userArrayList, Collections.reverseOrder(comparator));


                    binding.userRecyclerView.hideShimmerAdapter();
                    userAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void updateToken(String token) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(Objects.requireNonNull(mAuth.getUid())).setValue(token1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        userState("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        userState("offline");


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        MenuItem menuItem = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.isEmpty()) {
                    userAdapter.allUsers(userArrayList);
                } else {

                    searchUsers(s);
                }
                return true;

            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void searchUsers(String s) {
        List<User> users = new ArrayList<>();

        for (User user : userArrayList) {
            if (user.getUserName().toLowerCase().contains(s.toLowerCase())) {
                users.add(user);
            }
            userAdapter.searchList(users);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.settings) {
            startActivity(new Intent(UserActivity.this, SettingsActivity.class));
        } else if (id == R.id.logout) {
            Log.d("TAG", "onOptionsItemSelected: " + "logout");
            userState("offline");
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (isLoggedIn) {
                LoginManager.getInstance().logOut();
                AccessToken.setCurrentAccessToken(null);
            }

            mAuth.signOut();
            startActivity(new Intent(UserActivity.this, LoginActivity.class));
            finish();

        }

        return true;
    }

    private void userState(String status) {

        UserState userState = new UserState(Utils.formatTime(new Date()), status, new Date().getTime());

        if (FirebaseAuth.getInstance().getUid() != null) {
            database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue(userState);
        }
    }

    @Override
    public void OnItemClick(int position) {
        Intent intent = new Intent(UserActivity.this, ChatActivity.class);
        intent.putExtra("id", userArrayList.get(position).getUserId());
        startActivity(intent);
    }

    @Override
    public void OnImageClick(int position) {

        AlertDialog.Builder imageDialog = new AlertDialog.Builder(UserActivity.this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.small_imgae, null);
        ImageView image = layout.findViewById(R.id.imageView3);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        TextView textView = layout.findViewById(R.id.username_text);
        textView.setText(userArrayList.get(position).getUserName());
        Glide.with(this).load(userArrayList.get(position).getProfilePic()).placeholder(R.drawable.user_placeholder).into(image);
        imageDialog.setView(layout);
        imageDialog.create();
        imageDialog.show();
    }

}