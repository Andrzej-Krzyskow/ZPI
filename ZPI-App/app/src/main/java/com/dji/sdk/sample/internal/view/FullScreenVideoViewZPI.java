package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.utils.VideoFeedView;

import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import java.util.Random;

import dji.common.flightcontroller.LEDsSettings;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class FullScreenVideoViewZPI extends LinearLayout implements PresentableView {

    private Aircraft aircraft;
    private VideoFeedView videoFeedView;
    private VideoFeeder.VideoDataListener videoDataListener;
    private Button btnTurnOnLed;
    private Button btn_aim;
    private Button mBtnOpen = (Button) findViewById(R.id.btn_open);
    private FlightController flightController;
    private OverlayViewZPI overlayView;
    private Handler handler;
    private Runnable updateRunnable;
    private Camera camera;
    private MediaManager mediaManager;
    private FetchMediaTaskScheduler scheduler;
    private ImageView mDisplayImageView;
    private List<MediaFile> mediaList = new ArrayList<MediaFile>();
    private SettingsDefinitions.StorageLocation storageLocation = SettingsDefinitions.StorageLocation.INTERNAL_STORAGE;
    private boolean areCirclesVisible = false; // Circles are hidden by default

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
        btnTurnOnLed = findViewById(R.id.btn_turn_on_led);
        btn_aim = findViewById(R.id.btn_aim);
        mDisplayImageView = (ImageView) findViewById(R.id.display_image_view);
        overlayView = findViewById(R.id.overlay_view);

        if (VideoFeeder.getInstance() != null) {
            setupVideoFeedAndCamera();

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

    private void setupVideoFeedAndCamera() {
        aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        videoDataListener = videoFeedView.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
        videoFeed.addVideoDataListener(videoDataListener);

        videoFeedView.registerLiveVideo(videoFeed, true);
        if (ModuleVerificationUtil.isCameraModuleAvailable() && aircraft.getCamera().isMediaDownloadModeSupported()) {
            camera = aircraft.getCamera();
            mediaManager = camera.getMediaManager();
            scheduler = mediaManager.getScheduler();
        }
    }

    private void shoot() {
        turnOnLed();
    }

    private void updateOverlayCircles() {
        if (areCirclesVisible) {
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
        if (flightController == null && DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
        }

        if (flightController != null) {
            LEDsSettings ledsSettingsOn = new LEDsSettings.Builder().frontLEDsOn(true).build();
            flightController.setLEDsEnabledSettings(ledsSettingsOn, null);

            new android.os.Handler().postDelayed(() -> {
                LEDsSettings ledsSettingsOff = new LEDsSettings.Builder().frontLEDsOn(false).build();
                flightController.setLEDsEnabledSettings(ledsSettingsOff, null);
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
            ToastUtils.setResultToToast("Shoot clicked");
            shoot();
        });

        btn_aim.setOnClickListener(v -> aim());
    }

    private void aim() {
        areCirclesVisible = !areCirclesVisible; // Toggle the visibility flag
        overlayView.setShowCircles(areCirclesVisible); // Update the overlay view

        // Optionally, show a toast message
        if (areCirclesVisible) {
            ToastUtils.setResultToToast("CEP Circles turned ON");
        } else {
            ToastUtils.setResultToToast("CEP Circles turned OFF");
        }
    }

    private void captureFrame() {

    }


    private void getFileList() {

        mediaManager = DJISampleApplication.getProductInstance().getCamera().getMediaManager();
        scheduler = mediaManager.getScheduler();

        if (mediaManager != null) {
            mediaManager.refreshFileListOfStorageLocation(storageLocation, djiError -> {
                if (djiError == null) {

                    List<MediaFile> medias;
                    if (storageLocation == SettingsDefinitions.StorageLocation.SDCARD) {
                        medias = mediaManager.getSDCardFileListSnapshot();
                    } else {
                        medias = mediaManager.getInternalStorageFileListSnapshot();
                    }
                    if (mediaList != null) {
                        mediaList.clear();
                    }
                    for (MediaFile media : medias) {
                        mediaList.add(media);
                    }

                }

                scheduler.resume(djiError1 -> {
                    if (djiError1 == null) {
                        // getThumbanils();
                    }
                });
            });
        }
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
