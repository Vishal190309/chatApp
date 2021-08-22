package com.mechat.app.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mechat.app.R;
import com.mechat.app.databinding.UserListRowBinding;
import com.mechat.app.model.Message;
import com.mechat.app.model.User;
import com.mechat.app.model.UserState;
import com.mechat.app.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<User> userList;
    private final Context context;
    private final OnUserClick onUserClick;

    public UserAdapter(List<User> userList, Context context, OnUserClick onUserClick) {
        this.userList = userList;
        this.context = context;
        this.onUserClick = onUserClick;
    }

    @NonNull
    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_list_row, parent, false);
        return new ViewHolder(view, onUserClick);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.binding.username.setText(user.getUserName());
        Glide.with(context).load(user.getProfilePic()).placeholder(R.drawable.user_placeholder).circleCrop().into(holder.binding.profile);
        FirebaseDatabase.getInstance().getReference().child("chats").child(FirebaseAuth.getInstance().getUid() + user.getUserId()).child("messages")
                .orderByChild("timestamp")
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                if (dataSnapshot.exists()) {
                                    Message message = dataSnapshot.getValue(Message.class);
                                    assert message != null;
                                    holder.binding.lastMessage.setText(message.getMessage());
                                    if (DateUtils.isToday(message.getTimestamp())) {
                                        holder.binding.lastMessageTime.setText(Utils.format(new Date(message.getTimestamp())));

                                    } else if (DateUtils.isToday(message.getTimestamp() + DateUtils.DAY_IN_MILLIS)) {
                                        holder.binding.lastMessageTime.setText("Yesterday");
                                    } else if (Calendar.getInstance().get(Calendar.YEAR) == Utils.getYear(new Date(message.getTimestamp()))) {
                                        holder.binding.lastMessageTime.setText(Utils.LastTimeformatToDay(new Date(message.getTimestamp())));
                                    } else {
                                        holder.binding.lastMessageTime.setText(Utils.LastTiemformatToDate(new Date(message.getTimestamp())));
                                    }

                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        FirebaseDatabase.getInstance().getReference().child("presence").child(user.getUserId()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        UserState userState = snapshot.getValue(UserState.class);


                        long date = userState.getDate();
                        String time = userState.getTime();
                        String status = userState.getStatus();
                        assert status != null;
                        if (!status.isEmpty()) {
                            if (status.equals("Typing...")) {
                                holder.binding.typing.setVisibility(View.VISIBLE);
                                holder.binding.typing.setText(status);
                                holder.binding.lastMessage.setVisibility(View.GONE);

                            } else {
                                holder.binding.typing.setVisibility(View.GONE);
                                holder.binding.lastMessage.setVisibility(View.VISIBLE);

                            }
                        } else {
                            holder.binding.typing.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        FirebaseDatabase.getInstance().getReference().child("chats").child(user.getUserId() + FirebaseAuth.getInstance().getUid()).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChildren()) {
                    int unseen = 0;
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        if (snapshot1.getValue() != null) {
                            if (Objects.requireNonNull(snapshot1.getValue(Message.class)).getSenderId().equals(user.getUserId()) && snapshot1.getValue(Message.class).getIsSeen().equals("delivered")) {
                                unseen++;
                            }
                        }


                    }
                    if (unseen == 0) {
                        holder.binding.unseenCount.setVisibility(View.GONE);
                        holder.binding.lastMessageTime.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
                        Log.d("TAG", "onDataChange: " + unseen);
                    } else {
                        Log.d("TAG", "onDataChange: " + unseen);
                        holder.binding.unseenCount.setVisibility(View.VISIBLE);
                        holder.binding.unseenCount.setText(String.valueOf(unseen));
                        holder.binding.lastMessageTime.setTextColor(Color.GREEN);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void searchList(List<User> users) {
        userList = users;
        Collections.sort(userList, new Comparator<User>() {
            @Override
            public int compare(final User object1, final User object2) {
                return object1.getUserName().toLowerCase().compareTo(object2.getUserName().toLowerCase());
            }
        });
        notifyDataSetChanged();


    }

    public void allUsers(ArrayList<User> userArrayList) {
        userList = userArrayList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        UserListRowBinding binding;
        OnUserClick onUserClick;

        public ViewHolder(@NonNull View itemView, OnUserClick onUserClick) {
            super(itemView);
            this.onUserClick = onUserClick;
            binding = UserListRowBinding.bind(itemView);
            itemView.setOnClickListener(this);
            binding.profile.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.profile) {

                onUserClick.OnImageClick(getAdapterPosition());
            } else if (id == itemView.getId()) {
                onUserClick.OnItemClick(getAdapterPosition());
            }
        }
    }
}
