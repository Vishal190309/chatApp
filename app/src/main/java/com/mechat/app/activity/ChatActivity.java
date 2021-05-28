package com.mechat.app.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mechat.app.R;
import com.mechat.app.adapter.MessageAdapter;
import com.mechat.app.adapter.OnMessageClick;
import com.mechat.app.databinding.ActivityChatBinding;
import com.mechat.app.databinding.IncomingMessageBinding;
import com.mechat.app.databinding.OutgoingMessageBinding;
import com.mechat.app.model.ApiService;
import com.mechat.app.model.Message;
import com.mechat.app.model.User;
import com.mechat.app.model.UserState;
import com.mechat.app.notifications.Client;
import com.mechat.app.notifications.Data;
import com.mechat.app.notifications.MyResponse;
import com.mechat.app.notifications.Sender;
import com.mechat.app.notifications.Token;
import com.mechat.app.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity implements OnMessageClick {
    ActivityChatBinding binding;
    private MessageAdapter messageAdapter;
    private ArrayList<Message> messageArrayList;
    private FirebaseDatabase database;
    private String senderRoom;
    private String receiverRoom;
    private ApiService apiService;
    private String receiverUid;
    private String senderUid;
    private boolean isSeen;
    private List<String> msg = new ArrayList<>();
    DatabaseReference reference;
    private ValueEventListener statusListner;
    private ValueEventListener showChatsListner;
    private ValueEventListener seenMessageListner;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);
        setSupportActionBar(binding.chatToolbar);
        Intent intent = getIntent();
        database = FirebaseDatabase.getInstance();
        apiService = Client.getClient("https://fcm.googleapis.com/").create(ApiService.class);
        receiverUid = intent.getStringExtra("id");
        senderUid = FirebaseAuth.getInstance().getUid();
        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;
        messageArrayList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageArrayList, this, this, receiverUid);
        binding.chatRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        binding.chatRecyclerView.setLayoutManager(linearLayoutManager);
        binding.chatRecyclerView.setAdapter(messageAdapter);
        reference = database.getReference().child("chats").child(receiverRoom).child("messages");
        reference.keepSynced(true);
        seenMessageListner = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    dataSnapshot.getRef().child("isSeen").setValue("read");
                }
                messageAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        final Handler handler = new Handler();
        binding.message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                userState("Typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping, 1000);
            }

            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    userState("online");
                }
            };
        });

        showChats(senderRoom);
        database.getReference().child("Users").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user1 = snapshot.getValue(User.class);
                    binding.usernameText.setText(user1.getUserName());
                    Glide.with(ChatActivity.this).load(user1.getProfilePic()).placeholder(R.drawable.user_placeholder).circleCrop().into(binding.chatProfile);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        binding.back.setOnClickListener(view -> finish());
        ScrollListner();
        Handler handler1 = new Handler();
        binding.message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).child("status").setValue("Typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping, 500);

            }

            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).child("status").setValue("Online");
                }
            };
        });

        statusListner = database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserState userState = snapshot.getValue(UserState.class);


                    long date = userState.getDate();
                    String time = userState.getTime();
                    String status = userState.getStatus();
                    assert status != null;
                    if (!status.isEmpty()) {
                        if (status.equals("offline")) {
                            TextView statusText = binding.status;
                            if (DateUtils.isToday(date)) {
                                statusText.setText("Last seen today at " + time);

                            } else if (DateUtils.isToday(date + DateUtils.DAY_IN_MILLIS)) {
                                statusText.setText("Last seen yesterday at " + time);
                            } else if (Calendar.getInstance().get(Calendar.YEAR) == Utils.getYear(new Date(date))) {
                                statusText.setText("Last seen " + Utils.formatToDay(new Date(date)) + " at " + time);
                            } else {
                                statusText.setText("Last seen " + Utils.formatToDate(new Date(date)) + " at " + time);
                            }
                            binding.status.setVisibility(View.VISIBLE);
                        } else {
                            binding.status.setText(status);
                            binding.status.setVisibility(View.VISIBLE);

                        }
                    } else {
                        binding.status.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        SendMessage(intent, senderUid, senderRoom, receiverRoom);


    }

    private void SendMessage(Intent intent, String senderUid, String senderRoom, String receiverRoom) {
        Handler handler = new Handler();
        binding.send.setOnClickListener(view -> {

            SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
            String sound = preferences.getString("sound", "on");
            if (sound.equals("on")) {
                final MediaPlayer mp = MediaPlayer.create(ChatActivity.this, R.raw.messagesound);
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                int originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

                mp.start();
                Runnable setSoundToOriginal = new Runnable() {
                    @Override
                    public void run() {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                    }
                };
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(setSoundToOriginal, 500);


            }

            notify = true;
            if (binding.message.length() > 0) {
                if (intent.getStringExtra("allUser") != null) {
                    if (intent.getStringExtra("allUser").equals("allUserActivity")) {

                        String message = binding.message.getText().toString().trim();
                        Date date = new Date();
                        Message sendMessage = new Message(message, senderUid, date.getTime(), "sent");

                        database.getReference().child("chats").child(senderRoom).child("messages").push().setValue(sendMessage).addOnSuccessListener(aVoid -> {
                            database.getReference().child("chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .push().setValue(sendMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {


                                    FirebaseDatabase.getInstance().getReference().child("chats").child(FirebaseAuth.getInstance().getUid() + receiverUid).child("messages")
                                            .orderByChild("timestamp")
                                            .limitToLast(1)
                                            .addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.hasChildren()) {
                                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                            if (dataSnapshot.exists()) {
                                                                String lastMessage = String.valueOf(dataSnapshot.getValue(Message.class).getTimestamp());
                                                                HashMap<String, Object> receiverHashMap = new HashMap<>();
                                                                receiverHashMap.put("userId", receiverUid);
                                                                receiverHashMap.put("lastMessage", lastMessage);
                                                                database.getReference().child("chatlist")
                                                                        .child(Objects.requireNonNull(senderUid))
                                                                        .child(receiverUid).setValue(receiverHashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        HashMap<String, Object> senderHashMap = new HashMap<>();
                                                                        senderHashMap.put("userId", senderUid);
                                                                        senderHashMap.put("lastMessage", lastMessage);
                                                                        database.getReference().child("chatlist")
                                                                                .child(Objects.requireNonNull(receiverUid))
                                                                                .child(senderUid).setValue(senderHashMap);
                                                                    }
                                                                });

                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });


                                }
                            });
                            binding.chatRecyclerView.smoothScrollToPosition(Objects.requireNonNull(binding.chatRecyclerView.getLayoutManager()).getItemCount() - 1);
                        });

                    }
                } else {

                    String message = binding.message.getText().toString().trim();
                    Date date = new Date();
                    Message sendMessage = new Message(message, senderUid, date.getTime(), "sent");


                    database.getReference().child("chats").child(senderRoom).child("messages").push().setValue(sendMessage).addOnSuccessListener(aVoid -> {
                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .push().setValue(sendMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                FirebaseDatabase.getInstance().getReference().child("chats").child(FirebaseAuth.getInstance().getUid() + receiverUid).child("messages")
                                        .orderByChild("timestamp")
                                        .limitToLast(1)
                                        .addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.hasChildren()) {
                                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                        if (dataSnapshot.exists()) {
                                                            database.getReference().child("chatlist").child(senderUid).child(receiverUid).child("lastMessage").setValue(String.valueOf(dataSnapshot.getValue(Message.class).getTimestamp()));
                                                            database.getReference().child("chatlist").child(receiverUid).child(senderUid).child("lastMessage").setValue(String.valueOf(dataSnapshot.getValue(Message.class).getTimestamp()));
                                                        }
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                            }
                        });
                        binding.chatRecyclerView.smoothScrollToPosition(Objects.requireNonNull(binding.chatRecyclerView.getLayoutManager()).getItemCount() - 1);
                    });
                }
                FirebaseDatabase.getInstance().getReference().child("chats").child(senderRoom).child("messages")
                        .orderByChild("isSeen").limitToLast(5).equalTo("sent")
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                msg = new ArrayList<>();
                                if (snapshot.exists()) {
                                    int i = 0;
                                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                        if (snapshot1.exists()) {
                                            msg.add(snapshot1.getValue(Message.class).getMessage());

                                        }
                                    }

                                    Log.d("Log", "onDataChange: " + msg);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                DatabaseReference referenceNotification = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getUid());
                referenceNotification.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (notify) {
                            assert user != null;
                            sendNotifiaction(receiverUid, user.getUserName(), msg);
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                binding.message.setText("");

            }

        });


    }

    private void sendNotifiaction(String receiver, final String username, final List<String> message) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);
                    Log.d("TAG", "onDataChange: " + "notification");
                    Data data = new Data(senderUid, R.drawable.ic_stat_name, message, username,
                            receiverUid);

                    assert token != null;
                    Sender sender = new Sender(token.getToken(), data);

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(@NonNull Call<MyResponse> call, @NonNull Response<MyResponse> response) {
                                    Log.d("Response", "Response " + response);
                                    if (response.code() == 200) {
                                        if (Objects.requireNonNull(response.body()).success == 1) {
                                            Log.d("Error", "Success!");
                                        } else {
                                            Log.d("Error", "Success!");
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                }


                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void currentUser(String userid) {
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        editor.putString("currentuser", userid);
        editor.apply();
    }

    private void userState(String status) {

        UserState userState = new UserState(Utils.formatTime(new Date()), status, new Date().getTime());

        database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue(userState);
    }


    private void ScrollListner() {
        binding.chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.dayText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);


                binding.dayText.setVisibility(View.VISIBLE);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (Objects.requireNonNull(linearLayoutManager).getItemCount() > 0) {
                    if (DateUtils.isToday(messageArrayList.get(linearLayoutManager.findFirstVisibleItemPosition()).getTimestamp())) {
                        binding.dayText.setText("Today");

                        Log.d("TAG", "onScrolled: " + DateUtils.DAY_IN_MILLIS);
                    } else if (DateUtils.isToday(messageArrayList.get(linearLayoutManager.findFirstVisibleItemPosition()).getTimestamp() + DateUtils.DAY_IN_MILLIS)) {
                        binding.dayText.setText("Yesterday");
                    } else {
                        binding.dayText.setText(Utils.formatToDate(new Date(messageArrayList.get(linearLayoutManager.findFirstVisibleItemPosition()).getTimestamp())));
                    }
                    if (linearLayoutManager.findLastCompletelyVisibleItemPosition() >= linearLayoutManager.getItemCount() - 1) {

                        binding.scrollToLast.setVisibility(View.INVISIBLE);
                    } else {
                        binding.scrollToLast.setVisibility(View.VISIBLE);
                        binding.scrollToLast.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                binding.chatRecyclerView.smoothScrollToPosition(Objects.requireNonNull(binding.chatRecyclerView.getLayoutManager()).getItemCount() - 1);
                            }
                        });

                    }

                }

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.chat_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.clear_chat) {
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            firebaseDatabase.getReference().child("chats").child(senderRoom).child("messages").setValue(null);
        }

        return true;
    }

    private void showChats(String senderRoom) {
        database.getReference().child("chats").child(senderRoom).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    Message message = dataSnapshot.getValue(Message.class);
                    Objects.requireNonNull(message).setMessageId(dataSnapshot.getKey());
                    messageArrayList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) binding.chatRecyclerView.getLayoutManager();
                assert linearLayoutManager != null;
                if (linearLayoutManager.getItemCount() > 0) {
                    if (linearLayoutManager.findLastVisibleItemPosition() == linearLayoutManager.getItemCount() - 2) {
                        binding.chatRecyclerView.smoothScrollToPosition(linearLayoutManager.getItemCount() - 1);
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        reference.addValueEventListener(seenMessageListner);
        userState("online");
        currentUser(receiverUid);
        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        String currentUser = preferences.getString("currentuser", "none");
        Log.d("TAG", "onResume: " + currentUser);


    }

    @Override
    protected void onPause() {
        super.onPause();
        userState("offline");
        reference.removeEventListener(seenMessageListner);
        currentUser("none");
        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        String currentUser = preferences.getString("currentuser", "none");
        Log.d("TAG", "onResume: " + currentUser);


        ;
    }

    @Override
    protected void onStop() {
        super.onStop();
        reference.removeEventListener(seenMessageListner);
    }

    @Override
    public void OnReadMoreClick(OutgoingMessageBinding binding) {
        if (binding.messageLong.getMaxLines() == Integer.MAX_VALUE) {
            binding.readMore.setText("Read more");

            binding.messageLong.setEllipsize(TextUtils.TruncateAt.END);
            binding.messageLong.setMaxLines(15);
        } else {

            binding.readMore.setText("Read less");
            binding.messageLong.setMaxLines(Integer.MAX_VALUE);
            binding.messageLong.setEllipsize(null);
        }
    }

    @Override
    public void OnReadMoreClick(IncomingMessageBinding binding) {
        if (binding.messageLong.getMaxLines() == Integer.MAX_VALUE) {
            binding.readMore.setText("Read more");

            binding.messageLong.setEllipsize(TextUtils.TruncateAt.END);
            binding.messageLong.setMaxLines(15);
        } else {

            binding.readMore.setText("Read less");
            binding.messageLong.setMaxLines(Integer.MAX_VALUE);
            binding.messageLong.setEllipsize(null);
        }
    }
}