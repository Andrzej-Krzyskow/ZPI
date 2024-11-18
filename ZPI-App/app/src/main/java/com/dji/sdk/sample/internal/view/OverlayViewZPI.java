package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OverlayViewZPI extends View {
    private float radius50;
    private float radius93;
    private float radius99;
    private Paint paint50;
    private Paint paint93;
    private Paint paint99;
    private boolean showCircles = false; // Circles are hidden by default

    public OverlayViewZPI(Context context) {
        super(context);
        init();
    }

    public OverlayViewZPI(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint50 = new Paint();
        paint50.setColor(getResources().getColor(android.R.color.holo_red_light));
        paint50.setStyle(Paint.Style.STROKE);
        paint50.setStrokeWidth(5);

        paint93 = new Paint();
        paint93.setColor(getResources().getColor(android.R.color.holo_orange_light));
        paint93.setStyle(Paint.Style.STROKE);
        paint93.setStrokeWidth(5);

        paint99 = new Paint();
        paint99.setColor(getResources().getColor(android.R.color.holo_green_light));
        paint99.setStyle(Paint.Style.STROKE);
        paint99.setStrokeWidth(5);
    }

    public void updateRadii(float radius50, float radius93, float radius99) {
        this.radius50 = radius50;
        this.radius93 = radius93;
        this.radius99 = radius99;
        if (showCircles) {
            invalidate(); // Redraw the view only if circles are visible
        }
    }

    public void setShowCircles(boolean showCircles) {
        this.showCircles = showCircles;
        invalidate(); // Redraw the view to show or hide circles
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!showCircles) {
            // Do not draw anything if showCircles is false
            return;
        }

        // Get the center of the view
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw the circles if radii are greater than zero
        if (radius50 > 0) {
            canvas.drawCircle(centerX, centerY, radius50, paint50);
        }
        if (radius93 > 0) {
            canvas.drawCircle(centerX, centerY, radius93, paint93);
        }
        if (radius99 > 0) {
            canvas.drawCircle(centerX, centerY, radius99, paint99);
        }
    }
}
