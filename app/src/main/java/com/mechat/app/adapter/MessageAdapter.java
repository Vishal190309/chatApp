package com.mechat.app.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.mechat.app.R;
import com.mechat.app.databinding.IncomingMessageBinding;
import com.mechat.app.databinding.OutgoingMessageBinding;
import com.mechat.app.model.Message;
import com.mechat.app.utils.Utils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter {


    private final List<Message> messageList;
    private final Context context;
    private final OnMessageClick onMessageClick;
    private String recieverId;

    public MessageAdapter(List<Message> messageList, Context context, OnMessageClick onMessageClick, String recieverId) {
        this.messageList = messageList;
        this.context = context;
        this.onMessageClick = onMessageClick;
        this.recieverId = recieverId;
    }

    public MessageAdapter(List<Message> messageList, Context context, OnMessageClick onMessageClick) {
        this.messageList = messageList;
        this.context = context;
        this.onMessageClick = onMessageClick;
    }

    private final int ITEM_SENT = 1;
    private final int ITEM_RECEIVED = 2;


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.outgoing_message, parent, false);
            return new SendViewHolder(view, onMessageClick);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.incoming_message, parent, false);
            return new ReceiveViewHolder(view, onMessageClick);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (message.getMessage() != null && message.getMessageId() != null && message.getSenderId() != null) {

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setMessage("Are you sure you want to delete this message")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                                    String senderRoom = FirebaseAuth.getInstance().getUid() + recieverId;
                                    firebaseDatabase.getReference().child("chats").child(senderRoom).child("messages").child(message.getMessageId()).setValue(null);
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
                    return false;
                }
            });
            if (holder.getClass() == SendViewHolder.class) {

                SendViewHolder sendViewHolder = (SendViewHolder) holder;
                if (DateUtils.isToday(messageList.get(position).getTimestamp())) {
                    sendViewHolder.binding.dayText.setText("Today");
                } else if (DateUtils.isToday(messageList.get(position).getTimestamp() + DateUtils.DAY_IN_MILLIS)) {
                    sendViewHolder.binding.dayText.setText("Yesterday");
                } else {
                    sendViewHolder.binding.dayText.setText(Utils.formatToDate(new Date(messageList.get(position).getTimestamp())));
                }

                if (position > 0) {
                    if (Utils.getDay(new Date(message.getTimestamp())).equals(Utils.getDay(new Date((messageList.get(position - 1).getTimestamp()))))) {
                        sendViewHolder.binding.dayText.setVisibility(View.GONE);
                    } else {
                        sendViewHolder.binding.dayText.setVisibility(View.VISIBLE);
                    }
                } else {
                    sendViewHolder.binding.dayText.setVisibility(View.VISIBLE);
                }
                if (message.getMessage().length() > 12) {

                    sendViewHolder.binding.messageLong.setVisibility(View.VISIBLE);
                    sendViewHolder.binding.messageLong.setText(message.getMessage());
                    if (message.getMessage().length() > 500) {

                        sendViewHolder.binding.messageLong.setEllipsize(TextUtils.TruncateAt.END);
                        sendViewHolder.binding.readMore.setVisibility(View.VISIBLE);

                    } else {
                        sendViewHolder.binding.readMore.setVisibility(View.GONE);
                    }
                } else {

                    sendViewHolder.binding.messageLong.setVisibility(View.GONE);
                    sendViewHolder.binding.readMore.setVisibility(View.GONE);
                    sendViewHolder.binding.message.setText(message.getMessage());
                }
                sendViewHolder.binding.msgTime.setText(Utils.format(new Date(message.getTimestamp())));
                if (message.getIsSeen().trim().equals("sent")) {
                    sendViewHolder.binding.isRead.setVisibility(View.VISIBLE);

                } else if (message.getIsSeen().trim().equals("delivered")) {
                    sendViewHolder.binding.isRead.setVisibility(View.VISIBLE);
                    sendViewHolder.binding.isRead2.setVisibility(View.VISIBLE);

                } else if (message.getIsSeen().trim().equals("read")) {

                    sendViewHolder.binding.isRead.setVisibility(View.VISIBLE);
                    sendViewHolder.binding.isRead2.setVisibility(View.VISIBLE);
                    sendViewHolder.binding.isRead.setColorFilter(ContextCompat.getColor(context, R.color.green_blue), android.graphics.PorterDuff.Mode.SRC_IN);
                    sendViewHolder.binding.isRead2.setColorFilter(ContextCompat.getColor(context, R.color.green_blue), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            } else if (holder.getClass() == ReceiveViewHolder.class) {
                ReceiveViewHolder receiveViewHolder = (ReceiveViewHolder) holder;
                if (DateUtils.isToday(message.getTimestamp())) {
                    receiveViewHolder.binding.dayText.setText("Today");
                } else if (DateUtils.isToday(message.getTimestamp() + DateUtils.DAY_IN_MILLIS)) {
                    receiveViewHolder.binding.dayText.setText("Yesterday");
                } else {
                    receiveViewHolder.binding.dayText.setText(Utils.formatToDate(new Date(message.getTimestamp())));
                }

                if (position > 0) {
                    if (Utils.getDay(new Date(message.getTimestamp())).equals(Utils.getDay(new Date((messageList.get(position - 1).getTimestamp()))))) {
                        receiveViewHolder.binding.dayText.setVisibility(View.GONE);
                    } else {
                        receiveViewHolder.binding.dayText.setVisibility(View.VISIBLE);
                    }
                } else {
                    receiveViewHolder.binding.dayText.setVisibility(View.VISIBLE);
                }
                if (message.getMessage().length() > 12) {
                    receiveViewHolder.binding.messageLong.setVisibility(View.VISIBLE);
                    receiveViewHolder.binding.messageLong.setText(message.getMessage());
                    if (message.getMessage().length() > 500) {

                        receiveViewHolder.binding.messageLong.setEllipsize(TextUtils.TruncateAt.END);
                        receiveViewHolder.binding.readMore.setVisibility(View.VISIBLE);

                    } else {
                        receiveViewHolder.binding.readMore.setVisibility(View.GONE);
                    }
                } else {
                    receiveViewHolder.binding.readMore.setVisibility(View.GONE);
                    receiveViewHolder.binding.messageLong.setVisibility(View.GONE);
                    receiveViewHolder.binding.message.setText(message.getMessage());
                }
                receiveViewHolder.binding.msgTime.setText(Utils.format(new Date(message.getTimestamp())));
            }
        }
    }


    @Override
    public int getItemViewType(int position) {
        if (Objects.equals(FirebaseAuth.getInstance().getUid(), messageList.get(position).getSenderId())) {
            return ITEM_SENT;
        } else {
            return ITEM_RECEIVED;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class SendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        OutgoingMessageBinding binding;
        OnMessageClick onMessageClick;

        public SendViewHolder(@NonNull View itemView, OnMessageClick onMessageClick) {
            super(itemView);
            this.onMessageClick = onMessageClick;
            binding = OutgoingMessageBinding.bind(itemView);
            binding.readMore.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == binding.readMore.getId()) {
                onMessageClick.OnReadMoreClick(binding);

            }
        }

    }

    public class ReceiveViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        IncomingMessageBinding binding;
        OnMessageClick onMessageClick;

        public ReceiveViewHolder(@NonNull View itemView, OnMessageClick onMessageClick) {
            super(itemView);
            this.onMessageClick = onMessageClick;
            binding = IncomingMessageBinding.bind(itemView);
            binding.readMore.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == binding.readMore.getId()) {
                onMessageClick.OnReadMoreClick(binding);
            }
        }
    }

}
