package com.mechat.app.model;

import com.mechat.app.notifications.MyResponse;
import com.mechat.app.notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=\tAAAArDzX448:APA91bGYiJ12bM9h-NVyVoIwylAjRT7uPoUFlSmAslGUnYBfYPxr5h3qU8gwPQkVsSV4t-UKtD-3cCZkfXhY-pJAgFuaWRU0bf3VxYheEDnLeaxai39-_N6bGeoj2ia12iJMZkrCqYDp"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
