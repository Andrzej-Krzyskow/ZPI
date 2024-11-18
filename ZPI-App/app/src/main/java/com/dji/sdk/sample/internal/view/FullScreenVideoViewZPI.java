package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.utils.VideoFeedView;

import java.util.Random;

import dji.common.flightcontroller.LEDsSettings;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class FullScreenVideoViewZPI extends LinearLayout implements PresentableView {

    private VideoFeedView videoFeedView;
    private Button btnTurnOnLed;
    private Button button2;
    private FlightController flightController;
    private OverlayViewZPI overlayView;
    private Handler handler;
    private Runnable updateRunnable;

    public FullScreenVideoViewZPI(Context context) {
        super(context);
        init(context);
    }

    public FullScreenVideoViewZPI(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        DJISampleApplication.getEventBus().post(new MainActivity.RequestStartFullScreenEvent());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        DJISampleApplication.getEventBus().post(new MainActivity.RequestEndFullScreenEvent());
        handler.removeCallbacks(updateRunnable); // Stop the handler when view is detached
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_full_screen_video_zpi, this, true);

        videoFeedView = findViewById(R.id.video_feed_view);
        btnTurnOnLed = findViewById(R.id.turn_on_led);
        button2 = findViewById(R.id.button2);
        overlayView = findViewById(R.id.overlay_view);

        if (VideoFeeder.getInstance() != null) {
            setupVideoFeed();
        }

        setupButtons();

        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateOverlayCircles();
                handler.postDelayed(this, 50); // Update every 50 milliseconds (~20 FPS)
            }
        };
        handler.post(updateRunnable);
    }

    private void setupVideoFeed() {
        VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        videoFeedView.registerLiveVideo(videoFeed, true);
    }

    private void updateOverlayCircles() {
        float errorDistance = getErrorDistance();

        // Calculate the radii based on the error distance and the CEP multipliers
        float radius50 = errorDistance * 0.6745f;
        float radius93 = errorDistance * 2.0f;
        float radius99 = errorDistance * 2.576f;

        // Scale the radii to fit the view dimensions
        float scaleFactor = calculateScaleFactor();
        radius50 *= scaleFactor;
        radius93 *= scaleFactor;
        radius99 *= scaleFactor;

        overlayView.updateRadii(radius50, radius93, radius99);
    }

    private float calculateScaleFactor() {
        // Calculate a scale factor based on the view size
        // For simplicity, let's assume the maximum errorDistance corresponds to half the smaller dimension
        float maxErrorDistance = 50f; // This should match the amplitude used in getErrorDistance()
        float minViewDimension = Math.min(overlayView.getWidth(), overlayView.getHeight());
        return (minViewDimension / 2f) / maxErrorDistance;
    }


    private Random random = new Random();
    private float currentErrorDistance = 1f;
    private float targetErrorDistance = 1f;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 50; // Update every 500 milliseconds
    private float interpolationSpeed = 0.05f; // Adjust for smoother transitions


    private float getErrorDistance() {
        long currentTime = System.currentTimeMillis();

        // Check if it's time to update the target error distance
        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
            lastUpdateTime = currentTime;

            // Generate a new random target error distance between 1 and 50
            targetErrorDistance = 1 + random.nextFloat() * 20; // Random value between 1 and 50
        }

        // Interpolate towards the target error distance
        currentErrorDistance += (targetErrorDistance - currentErrorDistance) * interpolationSpeed;

        return currentErrorDistance;
    }

    private void turnOnLed() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null) {
                if (product instanceof Aircraft) {
                    flightController = ((Aircraft) product).getFlightController();
                }
            }
        }

        LEDsSettings ledsSettingsOn = new LEDsSettings.Builder().frontLEDsOn(true).build();
        if (flightController != null) {
            flightController.setLEDsEnabledSettings(ledsSettingsOn, null);

            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    LEDsSettings ledsSettingsOff = new LEDsSettings.Builder().frontLEDsOn(false).build();
                    flightController.setLEDsEnabledSettings(ledsSettingsOff, null);
                }
            }, 2500);
        }
    }

    private void turnOffLed() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null) {
                if (product instanceof Aircraft) {
                    flightController = ((Aircraft) product).getFlightController();
                }
            }
        }
        LEDsSettings ledsSettings = new LEDsSettings.Builder().frontLEDsOn(false).build();
        if (flightController != null) {
            flightController.setLEDsEnabledSettings(ledsSettings, null);
        }
    }

    private void setupButtons() {
        btnTurnOnLed.setOnClickListener(v -> {
            ToastUtils.setResultToToast("button 1 clicked led on");
            turnOnLed();
            // TODO: Implement what happens when button 1 is clicked
        });

        button2.setOnClickListener(v -> {
            ToastUtils.setResultToToast("button 2 clicked led off");
            turnOffLed();
            // TODO: Implement what happens when button 2 is clicked
        });
    }

    @Override
    public int getDescription() {
        return R.string.component_fullscreen_video_view_zpi;
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }
}
