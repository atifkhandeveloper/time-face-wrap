package com.myapps.timewrap.Wrapvideo.base.WarpVideoView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import com.myapps.timewrap.Wrapvideo.filters.IImageFilter;
import com.myapps.timewrap.Wrapvideo.fragments.ScanSettings;

public abstract class Slicer {
    protected Point currentScannerPoint;
    protected int increment = 5;
    protected Paint paintAntiAlias;
    protected Rect rawSliceRect;
    protected Paint scannerPaint;
    protected Rect scannerRect;
    protected int scannerWidth = 10;
    protected int screenHeight;
    protected int screenWidth;
    protected Rect scrollingSliceRect;

    public abstract void drawScanner(Canvas canvas);

    public abstract void drawSlice(Bitmap bitmap, Bitmap bitmap2, IImageFilter iImageFilter);

    public abstract boolean isScanDone();

    public Slicer(int i, int i2, int i3, int i4) {
        this.screenHeight = i2;
        this.screenWidth = i;
        this.scrollingSliceRect = new Rect();
        this.rawSliceRect = new Rect();
        this.scannerRect = new Rect();
        this.increment *= i3;
        this.currentScannerPoint = new Point(0, 0);
        Paint paint = new Paint(2);
        this.paintAntiAlias = paint;
        paint.setAntiAlias(true);
        Paint paint2 = new Paint();
        this.scannerPaint = paint2;
        paint2.setColor(i4);
        this.scannerPaint.setStyle(Paint.Style.FILL);
    }

    public static Slicer createSlicer(ScanSettings scanSettings, int i, int i2) {
        int direction = scanSettings.getDirection();
        if (direction == 1) {
            int shape = scanSettings.getShape();
            if (shape == 1) {
                return new HorizontalSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
            }
            if (shape == 2) {
                return new HorizontalCurveSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
            }
            if (shape != 3) {
                return new HorizontalSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
            }
            return new HorizontalZigZagSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
        } else if (direction != 2) {
            return new HorizontalSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
        } else {
            int shape2 = scanSettings.getShape();
            if (shape2 == 1) {
                return new VerticalSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
            }
            if (shape2 == 2) {
                return new VerticalCurveSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
            }
            if (shape2 != 3) {
                return new VerticalSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
            }
            return new VerticalZigZagSlicer(i, i2, scanSettings.getSpeed(), scanSettings.getScannerColor());
        }
    }

    public void cleanUp() {
        this.scrollingSliceRect = null;
        this.scannerRect = null;
        this.currentScannerPoint = null;
        this.paintAntiAlias = null;
        this.scannerPaint = null;
    }
}
