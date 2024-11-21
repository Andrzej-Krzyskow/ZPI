package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.utils.VideoFeedView;

import java.util.LinkedList;
import java.util.Queue;

import java.util.Random;

import dji.common.flightcontroller.LEDsSettings;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionSensorPosition;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class FullScreenVideoViewZPI extends LinearLayout implements PresentableView {

    public static final int INVALID_READING_LIMIT_TIME = 2000;
    public static final int DISTANCE_UPDATE_DELAY_TIME = 170;
    private VideoFeedView videoFeedView;
    private VideoFeeder.VideoDataListener videoDataListener;
    private Button btnTurnOnLed;
    private Button btn_aim;
    private FlightController flightController;
    private Handler circlesHandler;
    private OverlayViewZPI overlayView;
    private Runnable updateRunnable;
    private boolean isAimModeOn = false;
    private Handler distanceHandler;
    private float noseObstacleDistance = 100;
    private long lastValidReadingTime = 0;
    private int INVALID_DISTANCE = 100;
    private Queue<Float> recentReadings = new LinkedList<>();

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
        circlesHandler.removeCallbacks(updateRunnable);
        distanceHandler.removeCallbacks(distanceChecker);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_full_screen_video_zpi, this, true);
        videoFeedView = findViewById(R.id.video_feed_view);
        btnTurnOnLed = findViewById(R.id.btn_turn_on_led);
        btn_aim = findViewById(R.id.btn_aim);
        overlayView = findViewById(R.id.overlay_view);

        if (VideoFeeder.getInstance() != null) {
            setupVideoFeedAndCamera();
        }

        setupButtons();
        setupObstacleDistanceDetection();
        setupCircleHandler();
        startDistanceCheck();
    }

    private void setupCircleHandler() {
        circlesHandler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateOverlayCircles();
                circlesHandler.postDelayed(this, 50);
            }
        };
        circlesHandler.post(updateRunnable);
    }

    private void setupVideoFeedAndCamera() {
        VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        videoDataListener = videoFeedView.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
        videoFeed.addVideoDataListener(videoDataListener);
        videoFeedView.registerLiveVideo(videoFeed, true);
    }

    private void shoot() {
        turnOnLed();
        ToastUtils.setResultToToast("Fired!");
    }

    private void updateOverlayCircles() {
        if (isAimModeOn) {
            float errorDistance = noseObstacleDistance * 10;

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

    private void setupButtons() {
        btnTurnOnLed.setOnClickListener(v -> {
            shoot();
        });

        btn_aim.setOnClickListener(v -> {
            aim();
        });

    }

    private void aim() {
        isAimModeOn = !isAimModeOn;
        overlayView.showCrosshair(isAimModeOn);
        displayAimModeToastMsg();
    }

    private void displayAimModeToastMsg() {
        if (isAimModeOn) {
            ToastUtils.setResultToToast("Aim Mode turned ON");
        } else {
            ToastUtils.setResultToToast("Aim Mode turned OFF");
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

    private void setupObstacleDistanceDetection() {
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            FlightAssistant intelligentFlightAssistant = ((Aircraft) DJISampleApplication
                    .getProductInstance())
                    .getFlightController()
                    .getFlightAssistant();

            if (intelligentFlightAssistant != null) {
                intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(visionDetectionState -> {

                    ObstacleDetectionSector[] visionDetectionSectorArray =
                            visionDetectionState.getDetectionSectors();

                    if (visionDetectionSectorArray == null
                            || visionDetectionState.getPosition() != VisionSensorPosition.NOSE) {
                        return;
                    }

                    for (int i = 1; i <= 2; i++) {
                        float distance = visionDetectionSectorArray[i].getObstacleDistanceInMeters();
                        if (distance >= 0 && distance != 100) { // ignore invalid readings of 100 meters
                            recentReadings.add(distance);
                            lastValidReadingTime = System.currentTimeMillis(); // update the last valid reading time
                        }
                    }
                });
            }
        }
    }

    private void startDistanceCheck() {
        distanceHandler = new Handler();
        distanceHandler.post(distanceChecker);
    }

    private final Runnable distanceChecker = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();

            if (!recentReadings.isEmpty()) {
                // calculate the average of available valid readings
                float sum = 0;
                for (Float reading : recentReadings) {
                    sum += reading;
                }
                noseObstacleDistance = sum / recentReadings.size();
            } else if (currentTime - lastValidReadingTime > INVALID_READING_LIMIT_TIME) {
                // if no valid readings, set distance to 100 meters
                noseObstacleDistance = INVALID_DISTANCE;
            }

            recentReadings.clear(); // clear readings for the next time window
            distanceHandler.postDelayed(this, DISTANCE_UPDATE_DELAY_TIME);
        }
    };
}
