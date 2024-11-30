package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.dji.sdk.sample.R;

public class ArtificialHorizonViewZPI extends View {

    private double pitch = 0;
    private double roll = 0;
    private Paint horizonPaint;
    private Paint skyPaint;
    private Paint groundPaint;

    public ArtificialHorizonViewZPI(Context context) {
        super(context);
        init();
    }

    public ArtificialHorizonViewZPI(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        horizonPaint = new Paint();
        horizonPaint.setColor(getResources().getColor(android.R.color.white));
        horizonPaint.setStrokeWidth(5f);
        horizonPaint.setStyle(Paint.Style.STROKE);

        skyPaint = new Paint();
        skyPaint.setColor(getResources().getColor(R.color.wallet_holo_blue_light)); // Define in your colors.xml
        skyPaint.setStyle(Paint.Style.FILL);

        groundPaint = new Paint();
        groundPaint.setColor(getResources().getColor(R.color.cast_expanded_controller_seek_bar_progress_background_tint_color)); // Define in your colors.xml
        groundPaint.setStyle(Paint.Style.FILL);
    }

    public void updateAttitude(double pitch, double roll) {
        this.pitch = pitch;
        this.roll = roll;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Center of the view
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        canvas.save();

        // Rotate canvas based on roll
        canvas.rotate((float) -roll, centerX, centerY);

        // Translate canvas based on pitch
        double pitchOffset = pitch * (getHeight() / 90f); // Assuming pitch range is -45 to 45 degrees
        canvas.translate( 0, (float) pitchOffset);

        // Draw sky
        Path skyPath = new Path();
        skyPath.addRect(0, 0, getWidth(), centerY, Path.Direction.CW);
        canvas.drawPath(skyPath, skyPaint);

        // Draw ground
        Path groundPath = new Path();
        groundPath.addRect(0, centerY, getWidth(), getHeight(), Path.Direction.CW);
        canvas.drawPath(groundPath, groundPaint);

        // Draw horizon line
        canvas.drawLine(0, centerY, getWidth(), centerY, horizonPaint);

        canvas.restore();
    }
}
