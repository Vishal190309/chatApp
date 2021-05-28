package com.mechat.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.mechat.app.R;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        com.mechat.app.databinding.SplashActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.splash_activity);

        Animation animationAlpha = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.logo_alpha);
        binding.textView.setAnimation(animationAlpha);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(2000);
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
}