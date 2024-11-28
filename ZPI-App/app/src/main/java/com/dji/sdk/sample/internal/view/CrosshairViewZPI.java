package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CrosshairViewZPI extends View {
    private float radius50;
    private float radius93;
    private float radius99;
    private Paint paint50;
    private Paint paint93;
    private Paint paint99;
    private boolean showCircles = false; // Circles are hidden by default

    private int OPACITY = 90;
    public CrosshairViewZPI(Context context) {
        super(context);
        init();
    }

    public CrosshairViewZPI(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint50 = new Paint();
        paint50.setColor(getResources().getColor(android.R.color.holo_red_light));
        paint50.setStyle(Paint.Style.FILL);
        paint50.setAlpha(OPACITY); // 50% opacity

        paint93 = new Paint();
        paint93.setColor(getResources().getColor(android.R.color.holo_orange_light));
        paint93.setStyle(Paint.Style.FILL);
        paint93.setAlpha(OPACITY);

        paint99 = new Paint();
        paint99.setColor(getResources().getColor(android.R.color.holo_green_light));
        paint99.setStyle(Paint.Style.FILL);
        paint99.setAlpha(OPACITY);
    }

    public void updateRadii(float radius50, float radius93, float radius99) {
        this.radius50 = radius50;
        this.radius93 = radius93;
        this.radius99 = radius99;
        if (showCircles) {
            invalidate(); // Redraw the view only if circles are visible
        }
    }

    public void showCrosshair(boolean showCircles) {
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

        // Draw the innermost circle (red)
        if (radius50 > 0) {
            canvas.drawCircle(centerX, centerY, radius50, paint50);
        }

        // Draw the middle ring (orange)
        if (radius93 > radius50) {
            Path ringPath93 = new Path();
            ringPath93.addCircle(centerX, centerY, radius93, Path.Direction.CW);
            ringPath93.addCircle(centerX, centerY, radius50, Path.Direction.CCW);
            canvas.drawPath(ringPath93, paint93);
        }

        // Draw the outer ring (green)
        if (radius99 > radius93) {
            Path ringPath99 = new Path();
            ringPath99.addCircle(centerX, centerY, radius99, Path.Direction.CW);
            ringPath99.addCircle(centerX, centerY, radius93, Path.Direction.CCW);
            canvas.drawPath(ringPath99, paint99);
        }
    }
}
