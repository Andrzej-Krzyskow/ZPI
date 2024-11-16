package com.dji.sdk.sample.internal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
        paint50.setColor(Color.RED);
        paint50.setStyle(Paint.Style.STROKE);
        paint50.setStrokeWidth(5);

        paint93 = new Paint();
        paint93.setColor(Color.YELLOW);
        paint93.setStyle(Paint.Style.STROKE);
        paint93.setStrokeWidth(5);

        paint99 = new Paint();
        paint99.setColor(Color.GREEN);
        paint99.setStyle(Paint.Style.STROKE);
        paint99.setStrokeWidth(5);
    }

    public void updateRadii(float radius50, float radius93, float radius99) {
        this.radius50 = radius50;
        this.radius93 = radius93;
        this.radius99 = radius99;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Get the center of the view
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw the circles
        canvas.drawCircle(centerX, centerY, radius50, paint50);
        canvas.drawCircle(centerX, centerY, radius93, paint93);
        canvas.drawCircle(centerX, centerY, radius99, paint99);
    }
}
