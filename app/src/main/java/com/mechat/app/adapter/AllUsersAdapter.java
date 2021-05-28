package com.mechat.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mechat.app.R;
import com.mechat.app.databinding.AllUserListRowBinding;
import com.mechat.app.model.User;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AllUsersAdapter extends RecyclerView.Adapter<AllUsersAdapter.UserViewHolder> {
    private List<User> userList;
    private final Context context;
    private OnUserClick onUserClick;

    public AllUsersAdapter(Context context, List<User> userList, OnUserClick onUserClick) {
        this.userList = userList;
        this.context = context;
        this.onUserClick = onUserClick;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.all_user_list_row, parent, false);
        return new UserViewHolder(view, onUserClick);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.binding.username.setText(user.getUserName());
        Glide.with(context).load(user.getProfilePic()).placeholder(R.drawable.user_placeholder).circleCrop().into(holder.binding.profile);
//        holder.binding.lastMessageTime.setText(user.get(position).ge);
    }

    @Override
    public int getItemCount() {
        Collections.sort(userList, new Comparator<User>() {
            @Override
            public int compare(final User object1, final User object2) {
                return object1.getUserName().toLowerCase().compareTo(object2.getUserName().toLowerCase());
            }
        });
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


    public class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        AllUserListRowBinding binding;

        OnUserClick onUserClick;

        public UserViewHolder(@NonNull View itemView, OnUserClick onUserClick) {
            super(itemView);
            binding = AllUserListRowBinding.bind(itemView);
            itemView.setOnClickListener(this);
            this.onUserClick = onUserClick;
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
