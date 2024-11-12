package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.utils.VideoFeedView;

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

    public FullScreenVideoViewZPI(Context context) {
        super(context);
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
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_full_screen_video_zpi, this, true);

        videoFeedView = findViewById(R.id.video_feed_view);
        btnTurnOnLed = findViewById(R.id.turn_on_led);
        button2 = findViewById(R.id.button2);

        if (VideoFeeder.getInstance() != null) {
            setupVideoFeed();
        }

        setupButtons();
    }

    private void setupVideoFeed() {
        VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        videoFeedView.registerLiveVideo(videoFeed, true);
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
