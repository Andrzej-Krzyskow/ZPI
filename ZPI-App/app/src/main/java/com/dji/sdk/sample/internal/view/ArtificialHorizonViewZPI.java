package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ArtificialHorizonViewZPI extends View {

    private double pitch = 0;
    private double roll = 0;
    private Paint horizonPaint;

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
        horizonPaint.setAlpha(128); // Set opacity to 50% (128 out of 255)
        horizonPaint.setAntiAlias(true);
    }

    public void updateAttitude(float pitch, float roll) {
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
        double pitchOffset = pitch * (getHeight() / 90f); // Adjust as needed
        canvas.translate( 0, (float) pitchOffset);

        // Draw extended horizon line
        float lineLength = getWidth() * 2f; // Extend beyond the view width
        canvas.drawLine(centerX - lineLength, centerY, centerX + lineLength, centerY, horizonPaint);

        canvas.restore();
    }
}
