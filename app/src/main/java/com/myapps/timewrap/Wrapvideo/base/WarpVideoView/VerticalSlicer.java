package com.myapps.timewrap.Wrapvideo.base.WarpVideoView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import com.myapps.timewrap.Wrapvideo.filters.IImageFilter;

public class VerticalSlicer extends Slicer {
    Bitmap sliceBitmap = Bitmap.createBitmap(this.increment, this.screenHeight, Bitmap.Config.ARGB_8888);

    public VerticalSlicer(int i, int i2, int i3, int i4) {
        super(i, i2, i3, i4);
    }

    public void drawSlice(Bitmap bitmap, Bitmap bitmap2, IImageFilter iImageFilter) {
        this.scrollingSliceRect = getSlice(this.currentScannerPoint);
        boolean isFullImageFilter = iImageFilter.isFullImageFilter();
        if (isFullImageFilter) {
            bitmap = iImageFilter.process(bitmap);
        }
        new Canvas(this.sliceBitmap).drawBitmap(bitmap, this.scrollingSliceRect, this.rawSliceRect, this.paintAntiAlias);
        if (!isFullImageFilter) {
            this.sliceBitmap = iImageFilter.process(this.sliceBitmap);
        }
        new Canvas(bitmap2).drawBitmap(this.sliceBitmap, this.rawSliceRect, this.scrollingSliceRect, this.paintAntiAlias);
        this.currentScannerPoint = getScanPosition(this.currentScannerPoint);
    }

    public void drawScanner(Canvas canvas) {
        this.scannerRect.set(this.currentScannerPoint.x, 0, this.currentScannerPoint.x + this.scannerWidth, this.screenHeight);
        canvas.drawRect(this.scannerRect, this.scannerPaint);
    }

    public boolean isScanDone() {
        return this.currentScannerPoint.x >= this.screenWidth;
    }

    private Rect getSlice(Point point) {
        this.rawSliceRect.set(0, 0, this.increment, this.screenHeight);
        this.scrollingSliceRect.set(point.x, 0, point.x + this.increment, this.screenHeight);
        return this.scrollingSliceRect;
    }

    private Point getScanPosition(Point point) {
        if (point.x >= this.screenWidth) {
            point.x = this.screenWidth;
        } else {
            point.x += this.increment;
        }
        return point;
    }
}
