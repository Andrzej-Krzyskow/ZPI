package com.dji.sdk.sample.demo.camera.adapter;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.utils.VideoFeedView;

import dji.sdk.camera.VideoFeeder;

public class FullScreenActivity extends AppCompatActivity {

    private VideoFeedView videoFeedView;
    private Button button1;
    private Button button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable full-screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_full_screen);

        // Initialize views
        videoFeedView = findViewById(R.id.video_feed_view);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);

        // Set up video feed
        setupVideoFeed();

        // Set up buttons
        setupButtons();
    }

    private void setupVideoFeed() {
        // Initialize the video feed using DJI SDK
        VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        videoFeedView.registerLiveVideo(videoFeed, true);
    }

    private void setupButtons() {
        // Handle button clicks
        button1.setOnClickListener(v -> {
            // TODO: Implement what happens when button 1 is clicked
        });

        button2.setOnClickListener(v -> {
            // TODO: Implement what happens when button 2 is clicked
        });
    }
}
