package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.utils.VideoFeedView;

import androidx.annotation.NonNull;

import dji.sdk.camera.VideoFeeder;

public class FullScreenVideoViewZPI extends FrameLayout implements PresentableView {

    private VideoFeedView videoFeedView;
    private Button button1;
    private Button button2;

    public FullScreenVideoViewZPI(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        // Inflate your layout
        LayoutInflater.from(context).inflate(R.layout.view_full_screen_video_zpi, this, true);

        // Initialize views
        videoFeedView = findViewById(R.id.video_feed_view);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);

        // Set up the video feed
        if (VideoFeeder.getInstance() != null) {
            setupVideoFeed();
        }

        // Set up button click listeners if needed
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
            ToastUtils.setResultToToast("button 1 clicked");
            // TODO: Implement what happens when button 1 is clicked
        });

        button2.setOnClickListener(v -> {
            ToastUtils.setResultToToast("button 2 clicked");
            // TODO: Implement what happens when button 2 is clicked
        });
    }

    @Override
    public int getDescription() {
        return R.string.camera_listview_full_screen_video; // Define this string in strings.xml

    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }
}
