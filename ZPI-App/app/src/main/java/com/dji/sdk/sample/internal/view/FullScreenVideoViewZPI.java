package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.LEDsSettings;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionSensorPosition;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class FullScreenVideoViewZPI extends LinearLayout implements PresentableView {

    public static final int INVALID_READING_LIMIT_TIME = 2000;
    public static final int DISTANCE_UPDATE_DELAY_TIME = 170;
    public static final int SHOT_DELAY_TIME = 4000;
    private static final int INVALID_DISTANCE = 100;
    public static final int THRESHOLD_DISTANCE = 5;
    MainContent mainContent;
    private VideoFeedView videoFeedView;
    private VideoFeeder.VideoDataListener videoDataListener;
    private FlightController flightController;
    private Handler circlesHandler;
    private CrosshairViewZPI overlayView;
    private Runnable updateRunnable;
    private Handler distanceHandler;
    private Queue<Float> recentReadings = new LinkedList<>();
    private boolean isAimModeOn = false;
    private float noseObstacleDistance = 100;
    private long lastValidReadingTime = 0;
    private Button btn_fire;
    private Button btn_aim;
    private TextView textOverlayView;
    private View greenTintOverlay;
    private TextView tvAltitude;
    private TextView tvSpeed;
    private ArtificialHorizonViewZPI artificialHorizonView;

    public FullScreenVideoViewZPI(Context context) {
        super(context);
        init(context);
    }

    public FullScreenVideoViewZPI(Context context, MainContent mainContent) {
        super(context);
        init(context);
        this.mainContent = mainContent;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        DJISampleApplication.getEventBus().post(new MainActivity.RequestStartFullScreenEvent());
        enableFullscreenMode();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        DJISampleApplication.getEventBus().post(new MainActivity.RequestEndFullScreenEvent());
        disableFullscreenMode();
        circlesHandler.removeCallbacks(updateRunnable);
        distanceHandler.removeCallbacks(distanceChecker);
        distanceHandler.removeCallbacks(distanceTextUpdater);

        if (flightController != null) {
            flightController.setStateCallback(null);
        }
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_full_screen_video_zpi, this, true);
        videoFeedView = findViewById(R.id.video_feed_view);
        btn_fire = findViewById(R.id.btn_fire);
        btn_aim = findViewById(R.id.btn_aim);
        overlayView = findViewById(R.id.overlay_view);
        textOverlayView = findViewById(R.id.text_overlay_view);
        greenTintOverlay = findViewById(R.id.green_tint_overlay);

        tvAltitude = findViewById(R.id.tv_altitude);
        tvSpeed = findViewById(R.id.tv_speed);

        artificialHorizonView = findViewById(R.id.artificial_horizon_view);
        artificialHorizonView.setVisibility(View.GONE);

        if (VideoFeeder.getInstance() != null) {
            setupVideoFeedAndCamera();
        }

        initFlightController();
        setupFlightControllerStateCallback();

        setupButtons();
        setupObstacleDistanceDetection();
        setupCircleHandler();
        startDistanceHandler();
    }

    private void enableFullscreenMode() {
        View decorView = ((MainActivity) getContext()).getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void disableFullscreenMode() {
        if (mainContent != null) {
            mainContent.enableButton();
        }
        View decorView = ((MainActivity) getContext()).getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
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
        // Check conditions for displaying crosshair
        boolean shouldShowCrosshair = isAimModeOn && noseObstacleDistance < THRESHOLD_DISTANCE;

        if (shouldShowCrosshair) {
            btn_fire.setEnabled(true);
            // Calculate the radii based on the error distance and the CEP multipliers
            float errorDistance = noseObstacleDistance * 10;

            float radius50 = errorDistance * 0.6745f;
            float radius93 = errorDistance * 2.0f;
            float radius99 = errorDistance * 2.576f;

            // Scale the radii to fit the view dimensions
            float scaleFactor = calculateScaleFactor();
            radius50 *= scaleFactor;
            radius93 *= scaleFactor;
            radius99 *= scaleFactor;

            overlayView.updateRadii(radius50, radius93, radius99);
            overlayView.showCrosshair(true);
        } else {
            btn_fire.setEnabled(false);
            // Hide the crosshair
            overlayView.showCrosshair(false);
        }
        // Always redraw the overlayView to show distance text
        overlayView.invalidate();
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

            new Handler().postDelayed(() -> {
                LEDsSettings ledsSettingsOff = new LEDsSettings.Builder().frontLEDsOn(false).build();
                flightController.setLEDsEnabledSettings(ledsSettingsOff, null);
            }, SHOT_DELAY_TIME);
        }
    }

    private void setupButtons() {
        btn_fire.setOnClickListener(v -> {
            shoot();
        });

        btn_aim.setOnClickListener(v -> {
            aim();
        });

        btn_fire.setEnabled(false);
    }

    private void aim() {
        isAimModeOn = !isAimModeOn;
        overlayView.showCrosshair(isAimModeOn);
        greenTintOverlay.setVisibility(isAimModeOn ? View.VISIBLE : View.GONE);
        artificialHorizonView.setVisibility(isAimModeOn ? View.VISIBLE : View.GONE);
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
                    float minReading = INVALID_DISTANCE;
                    for (ObstacleDetectionSector sector : visionDetectionSectorArray) {
                        float distance = sector.getObstacleDistanceInMeters();
                        if (distance >= 0 && distance != 100 && distance < minReading) { // ignore invalid readings of 100 meters
                            minReading = distance;
                            lastValidReadingTime = System.currentTimeMillis(); // update the last valid reading time
                        }
                    }
                    if (minReading < INVALID_DISTANCE) {
                        recentReadings.add(minReading);
                    }
                });
            }
        }
    }

    private void startDistanceHandler() {
        distanceHandler = new Handler();
        distanceHandler.post(distanceChecker);
        distanceHandler.post(distanceTextUpdater);
    }

    private final Runnable distanceChecker = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();

            if (!recentReadings.isEmpty()) {
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

    private final Runnable distanceTextUpdater = new Runnable() {
        @Override
        public void run() {
            String distanceText = "Obstacle Distance: " + String.format("%.2f m", noseObstacleDistance);
            textOverlayView.setText(distanceText);

            distanceHandler.postDelayed(this, 500);
        }
    };

    private void initFlightController() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
        }
    }

    private void setupFlightControllerStateCallback() {
        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState state) {
                    updateDroneState(state);
                }
            });
        }
    }
    private void updateDroneState(FlightControllerState state) {
        // Update Altitude
        final double altitude = state.getAircraftLocation().getAltitude();

        // Update Velocity
        final float velocityX = state.getVelocityX();
        final float velocityY = state.getVelocityY();
        final float velocityZ = state.getVelocityZ();
        final double speed = Math.sqrt(
                velocityX * velocityX +
                        velocityY * velocityY +
                        velocityZ * velocityZ
        );

        // Update Gyroscope Data (Attitude)
        final Attitude attitude = state.getAttitude();
        final double pitch = attitude.pitch;
        final double roll = attitude.roll;

        // Update UI on the main thread
        post(() -> {
            tvAltitude.setText(String.format("Altitude: %.1f m", altitude));
            tvSpeed.setText(String.format("Speed: %.1f m/s", speed));

            if (artificialHorizonView != null) {
                artificialHorizonView.updateAttitude((float) pitch, (float) roll);
            }
        });
    }
}
