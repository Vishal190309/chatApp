package com.mechat.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mechat.app.R;
import com.mechat.app.adapter.AllUsersAdapter;
import com.mechat.app.adapter.OnUserClick;
import com.mechat.app.databinding.ActivityAllUsersBinding;
import com.mechat.app.model.User;
import com.mechat.app.model.UserState;
import com.mechat.app.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AllUsersActivity extends AppCompatActivity implements OnUserClick {
    private ActivityAllUsersBinding binding;
    private ArrayList<User> userList;
    private AllUsersAdapter allUsersAdapter;
    private FirebaseDatabase database;
    private ValueEventListener showUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_users);
        userList = new ArrayList<>();
        database = FirebaseDatabase.getInstance();
        allUsersAdapter = new AllUsersAdapter(AllUsersActivity.this, userList, this);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setAdapter(allUsersAdapter);
        binding.recyclerView.showShimmerAdapter();
        DatabaseReference reference = database.getReference().child("Users");
        reference.keepSynced(true);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        showUsers();


    }

    private void showUsers() {
        showUser = database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (!Objects.requireNonNull(user).getUserId().equals(FirebaseAuth.getInstance().getUid())) {

                                userList.add(user);

                            }
                            binding.recyclerView.hideShimmerAdapter();

                        }
                    }
                    allUsersAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.all_user_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                searchUsers(s);
                return true;

            }
        });
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            showUsers();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        userState("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        database.getReference().removeEventListener(showUser);
        userState("offline");

    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    private void searchUsers(String s) {
        List<User> users = new ArrayList<>();
        if (s.isEmpty()) {
            allUsersAdapter.searchList(userList);
        }
        for (User user : userList) {
            if (user.getUserName().toLowerCase().contains(s.toLowerCase())) {
                users.add(user);
            }
            allUsersAdapter.searchList(users);
        }
    }

    private void userState(String status) {

        UserState userState = new UserState(Utils.formatTime(new Date()), status, new Date().getTime());

        database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue(userState);
    }

    @Override
    public void OnItemClick(int position) {
        Intent intent = new Intent(AllUsersActivity.this, ChatActivity.class);
        intent.putExtra("name", userList.get(position).getUserName());
        intent.putExtra("id", userList.get(position).getUserId());
        intent.putExtra("profileImage", userList.get(position).getProfilePic());
        intent.putExtra("allUser", "allUserActivity");
        startActivity(intent);
        finish();
    }

    @Override
    public void OnImageClick(int position) {

        AlertDialog.Builder imageDialog = new AlertDialog.Builder(AllUsersActivity.this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.small_imgae, null);
        ImageView image = layout.findViewById(R.id.imageView3);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        TextView textView = layout.findViewById(R.id.username_text);
        textView.setText(userList.get(position).getUserName());
        Glide.with(this).load(userList.get(position).getProfilePic()).placeholder(R.drawable.user_placeholder).into(image);
        imageDialog.setView(layout);
        imageDialog.create();
        imageDialog.show();
    }


}