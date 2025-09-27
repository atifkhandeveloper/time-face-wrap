package com.myapps.timewrap.Wrapvideo.base.WarpVideoView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.internal.view.SupportMenu;
import com.myapps.timewrap.Wrapvideo.filters.IImageFilter;

public class HorizontalCurveSlicer extends Slicer {
    Bitmap curveBitmap;
    Paint fillPaint;
    Matrix matrix = new Matrix();
    Path path;
    Rect pathBounds = new Rect();
    Paint porterDuffPaint;
    Path scannerPath;
    Bitmap sliceBitmap;

    public HorizontalCurveSlicer(int i, int i2, int i3, int i4) {
        super(i, i2, i3, i4);
        Paint paint = new Paint();
        this.fillPaint = paint;
        paint.setColor(SupportMenu.CATEGORY_MASK);
        this.fillPaint.setStyle(Paint.Style.FILL);
        Paint paint2 = new Paint();
        this.porterDuffPaint = paint2;
        paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        this.increment = 10;
        createPath((((float) i2) * 1.0f) / 3.0f);
    }

    public void drawSlice(Bitmap bitmap, Bitmap bitmap2, IImageFilter iImageFilter) {
        Bitmap bitmap3 = this.curveBitmap;
        if (bitmap3 == null) {
            this.curveBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            bitmap3.eraseColor(0);
        }
        if (iImageFilter.isFakeFilter()) {
            new Canvas(bitmap2).drawBitmap(this.curveBitmap, this.rawSliceRect, this.scrollingSliceRect, this.paintAntiAlias);
            this.currentScannerPoint = getScanPosition(this.currentScannerPoint);
            return;
        }
        boolean isFullImageFilter = iImageFilter.isFullImageFilter();
        if (isFullImageFilter) {
            bitmap = iImageFilter.process(bitmap);
        }
        new Canvas(this.sliceBitmap).drawBitmap(bitmap, this.scrollingSliceRect, this.rawSliceRect, this.paintAntiAlias);
        if (!isFullImageFilter) {
            this.sliceBitmap = iImageFilter.process(this.sliceBitmap);
        }
        Canvas canvas = new Canvas(this.curveBitmap);
        canvas.drawPath(this.path, this.fillPaint);
        canvas.drawBitmap(this.sliceBitmap, 0.0f, 0.0f, this.porterDuffPaint);
        new Canvas(bitmap2).drawBitmap(this.curveBitmap, this.rawSliceRect, this.scrollingSliceRect, this.paintAntiAlias);
        this.currentScannerPoint = getScanPosition(this.currentScannerPoint);
    }

    public void drawScanner(Canvas canvas) {
        canvas.drawPath(this.scannerPath, this.scannerPaint);
    }

    public boolean isScanDone() {
        return this.scrollingSliceRect.top >= this.screenHeight;
    }

    public void cleanUp() {
        super.cleanUp();
        this.path = null;
        this.fillPaint = null;
        this.porterDuffPaint = null;
        Bitmap bitmap = this.curveBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            this.curveBitmap = null;
        }
    }

    private Point getScanPosition(Point point) {
        if (point.y >= this.screenHeight) {
            point.y = this.screenHeight;
        } else {
            point.y += this.increment;
        }
        this.matrix.reset();
        this.matrix.setTranslate(0.0f, (float) this.increment);
        this.scannerPath.transform(this.matrix);
        Rect rect = this.pathBounds;
        rect.set(rect.left, this.pathBounds.top + this.increment, this.pathBounds.right, this.pathBounds.bottom + this.increment);
        this.scrollingSliceRect.set(this.scrollingSliceRect.left, this.scrollingSliceRect.top + this.increment, this.scrollingSliceRect.right, this.scrollingSliceRect.bottom + this.increment);
        return point;
    }

    public void createPath(float f) {
        Path path2 = new Path();
        this.path = path2;
        path2.setFillType(Path.FillType.EVEN_ODD);
        this.path.lineTo(0.0f, (float) this.increment);
        float f2 = (-f) / 2.0f;
        float f3 = f / 2.0f;
        this.path.arcTo(new RectF(0.0f, ((float) this.increment) + f2, (float) this.screenWidth, ((float) this.increment) + f3), 180.0f, -180.0f);
        this.path.lineTo((float) this.screenWidth, 0.0f);
        this.path.moveTo(0.0f, 0.0f);
        this.path.arcTo(new RectF(0.0f, f2, (float) this.screenWidth, f3), 180.0f, -180.0f);
        RectF rectF = new RectF();
        this.path.computeBounds(rectF, false);
        rectF.roundOut(this.pathBounds);
        this.rawSliceRect = new Rect(this.pathBounds);
        this.sliceBitmap = Bitmap.createBitmap(this.pathBounds.width(), this.pathBounds.height(), Bitmap.Config.ARGB_8888);
        this.scrollingSliceRect = new Rect(this.pathBounds.left, this.pathBounds.top - this.pathBounds.bottom, this.pathBounds.right, this.pathBounds.bottom - this.pathBounds.bottom);
        this.matrix.setTranslate(0.0f, (float) (-this.pathBounds.bottom));
        Path path3 = new Path(this.path);
        this.scannerPath = path3;
        path3.transform(this.matrix);
    }
}
