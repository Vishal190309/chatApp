package com.mechat.app.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mechat.app.activity.ChatActivity;
import com.mechat.app.model.Message;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class MyFirebaseMessaging extends FirebaseMessagingService {
    private ArrayList<String> msg = new ArrayList<>();
    private String message = "";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("onMessageReceived", "onMessageReceived: " + "message Comin");
        String sented = remoteMessage.getData().get("sented");
        String user = remoteMessage.getData().get("user");

        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        String currentUser = preferences.getString("currentuser", "none");

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference().child("chats").child(user + sented).child("messages");


        if (firebaseUser != null && sented.equals(firebaseUser.getUid())) {

            if (!currentUser.equals(user)) {
                reference.orderByChild("isSeen")
                        .limitToLast(1).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            dataSnapshot.getRef().child("isSeen").setValue("delivered");
                        }
                        reference.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sendOreoNotification(remoteMessage);
                } else {
                    sendNotification(remoteMessage);
                }
            }
        }
    }

    private void sendOreoNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String bodyString = remoteMessage.getData().get("body");
        String sented = remoteMessage.getData().get("sented");
        bodyString = bodyString.replaceAll("[^a-zA-Z0-9]", " ");
        bodyString = bodyString.replaceAll("\\s+", " ");
        String[] body = null;
        Pattern pattern = Pattern.compile(" ");
        body = pattern.split(bodyString);


        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("id", user);
        Bundle bundle = new Bundle();
        bundle.putString("userid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.InboxStyle inboxStyle =
                new Notification.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        String[] finalBody = body;

        FirebaseDatabase.getInstance().getReference().child("chats").child(user + sented).child("messages")
                .orderByChild("isSeen").limitToLast(5).equalTo("delivered")
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
                            for (int j = 0; j < msg.size(); j++) {
                                inboxStyle.addLine(msg.get(j));
                            }


                            if (finalBody.length > 0) {
                                message = msg.get(msg.size() - 1);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        OreoNotification oreoNotification = new OreoNotification(this);
        Notification.Builder builder = oreoNotification.getOreoNotification(title, message, pendingIntent,
                defaultSound, icon, inboxStyle);


        int i = 0;
        if (j > 0) {
            i = j;
        }

        oreoNotification.getManager().notify(i, builder.build());

    }


    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {

                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                if (firebaseUser != null) {
                    updateToken(token);
                }

                // Log and toast

            }
        });

    }


    private void sendNotification(RemoteMessage remoteMessage) {

        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String bodyString = remoteMessage.getData().get("body");

        String sented = remoteMessage.getData().get("sented");
        bodyString = bodyString.replaceAll("[^a-zA-Z0-9]", " ");
        bodyString = bodyString.replaceAll("\\s+", " ");
        String[] body = null;
        Pattern pattern = Pattern.compile(" ");
        body = pattern.split(bodyString);
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("id", user);
        Bundle bundle = new Bundle();
        bundle.putString("userid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.InboxStyle inboxStyle =
                new Notification.InboxStyle();
        inboxStyle.setBigContentTitle(title);
        String[] finalBody = body;

        FirebaseDatabase.getInstance().getReference().child("chats").child(user + sented).child("messages")
                .orderByChild("isSeen").limitToLast(5).equalTo("delivered")
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
                            for (int j = 0; j < msg.size(); j++) {
                                inboxStyle.addLine(msg.get(j));
                            }


                            if (finalBody.length > 0) {
                                message = msg.get(msg.size() - 1);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder = new Notification.Builder(this)
                    .setSmallIcon(Integer.parseInt(icon))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(inboxStyle)
                    .setAutoCancel(true)
                    .setColor(Color.BLUE)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSound(defaultSound)
                    .setContentIntent(pendingIntent);
        } else {
            builder = new Notification.Builder(this)
                    .setSmallIcon(Integer.parseInt(icon))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(inboxStyle)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSound(defaultSound)
                    .setContentIntent(pendingIntent);
        }
        NotificationManager noti = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int i = 0;
        if (j > 0) {
            i = j;
        }

        noti.notify(i, builder.build());
    }

    private void updateToken(String refreshToken) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(refreshToken);
        reference.child(firebaseUser.getUid()).setValue(token);
    }
}
