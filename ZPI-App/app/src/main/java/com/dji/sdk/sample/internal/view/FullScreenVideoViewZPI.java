package com.dji.sdk.sample.internal.view;

import android.content.Context;
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
    private Camera camera;
    private MediaManager mediaManager;
    private FetchMediaTaskScheduler scheduler;
    private ImageView mDisplayImageView;
    private List<MediaFile> mediaList = new ArrayList<MediaFile>();
    private SettingsDefinitions.StorageLocation storageLocation = SettingsDefinitions.StorageLocation.INTERNAL_STORAGE;


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
        btnTurnOnLed = findViewById(R.id.btn_turn_on_led);
        btn_aim = findViewById(R.id.btn_aim);
        mDisplayImageView = (ImageView) findViewById(R.id.display_image_view);

        if (VideoFeeder.getInstance() != null) {
            setupVideoFeedAndCamera();

        }

        setupButtons();
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

        btn_aim.setOnClickListener(v -> {
            ToastUtils.setResultToToast("Aim clicked");
            aim();
        });
    }

    private void aim() {
        captureFrame();
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
