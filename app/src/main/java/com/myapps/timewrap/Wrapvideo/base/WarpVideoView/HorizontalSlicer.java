package com.myapps.timewrap.Wrapvideo.base.WarpVideoView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import com.myapps.timewrap.Wrapvideo.filters.IImageFilter;

public class HorizontalSlicer extends Slicer {
    Bitmap sliceBitmap = Bitmap.createBitmap(this.screenWidth, this.increment, Bitmap.Config.ARGB_8888);

    public HorizontalSlicer(int i, int i2, int i3, int i4) {
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
        this.scannerRect.set(0, this.currentScannerPoint.y, this.screenWidth, this.currentScannerPoint.y + this.scannerWidth);
        canvas.drawRect(this.scannerRect, this.scannerPaint);
    }

    public boolean isScanDone() {
        return this.currentScannerPoint.y >= this.screenHeight;
    }

    private Rect getSlice(Point point) {
        this.rawSliceRect.set(0, 0, this.screenWidth, this.increment);
        this.scrollingSliceRect.set(0, point.y, this.screenWidth, point.y + this.increment);
        return this.scrollingSliceRect;
    }

    private Point getScanPosition(Point point) {
        if (point.y >= this.screenHeight) {
            point.y = this.screenHeight;
        } else {
            point.y += this.increment;
        }
        return point;
    }
}
