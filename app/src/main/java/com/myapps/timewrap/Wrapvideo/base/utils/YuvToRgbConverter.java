package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import com.myapps.timewrap.Wrapvideo.filters.ScriptC_ImageRotator;
import java.nio.ByteBuffer;

public final class YuvToRgbConverter {
    private final RenderScript f213rs;
    public Allocation inputAllocation;
    public Allocation outputAllocation;
    private int pixelCount = -1;
    private final ScriptIntrinsicYuvToRGB scriptYuvToRgb;
    private final ScriptC_ImageRotator sriptRotator;
    public ByteBuffer yuvBuffer;

    public YuvToRgbConverter(Context context) {
        RenderScript create = RenderScript.create(context);
        this.f213rs = create;
        this.sriptRotator = new ScriptC_ImageRotator(create);
        this.scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(create, Element.U8_4(create));
    }

    public final synchronized Bitmap yuvToRgb(Image image) {
        Bitmap createBitmap;
        createBitmap = Bitmap.createBitmap(image.getCropRect().width(), image.getCropRect().height(), Bitmap.Config.ARGB_8888);
        if (this.yuvBuffer == null) {
            int width = image.getCropRect().width() * image.getCropRect().height();
            this.pixelCount = width;
            this.yuvBuffer = ByteBuffer.allocateDirect((width * ImageFormat.getBitsPerPixel(35)) / 8);
        }
        this.yuvBuffer.rewind();
        imageToByteBuffer(image, this.yuvBuffer.array());
        if (this.inputAllocation == null) {
            RenderScript renderScript = this.f213rs;
            this.inputAllocation = Allocation.createSized(renderScript, new Type.Builder(renderScript, Element.YUV(renderScript)).setYuvFormat(17).create().getElement(), this.yuvBuffer.array().length);
        }
        if (this.outputAllocation == null) {
            this.outputAllocation = Allocation.createFromBitmap(this.f213rs, createBitmap);
        }
        this.inputAllocation.copyFrom(this.yuvBuffer.array());
        this.scriptYuvToRgb.setInput(this.inputAllocation);
        this.scriptYuvToRgb.forEach(this.outputAllocation);
        this.outputAllocation.copyTo(createBitmap);
        return createBitmap;
    }

    public final Bitmap rotate(Image image, Bitmap bitmap, int i, Bitmap bitmap2) {
        if (i == 0) {
            return bitmap;
        }
        ScriptC_ImageRotator scriptC_ImageRotator = this.sriptRotator;
        scriptC_ImageRotator.set_inWidth(bitmap.getWidth());
        scriptC_ImageRotator.set_inHeight(bitmap.getHeight());
        Allocation createFromBitmap = Allocation.createFromBitmap(this.f213rs, bitmap);
        bitmap.recycle();
        scriptC_ImageRotator.set_inImage(createFromBitmap);
        bitmap.getConfig();
        Allocation createFromBitmap2 = Allocation.createFromBitmap(this.f213rs, bitmap2);
        if (i == 90) {
            scriptC_ImageRotator.forEach_rotate_90_clockwise(createFromBitmap2, createFromBitmap2);
        } else if (i == 180) {
            scriptC_ImageRotator.forEach_flip_vertically(createFromBitmap2, createFromBitmap2);
        } else if (i == 270) {
            scriptC_ImageRotator.forEach_rotate_270_clockwise(createFromBitmap2, createFromBitmap2);
        }
        createFromBitmap2.copyTo(bitmap2);
        return bitmap2;
    }

    public final Bitmap rotate(Bitmap bitmap, int i) {
        int i2 = 360 - i;
        Allocation createFromBitmap = Allocation.createFromBitmap(this.f213rs, bitmap);
        this.sriptRotator.set_inWidth(bitmap.getWidth());
        this.sriptRotator.set_inHeight(bitmap.getHeight());
        this.sriptRotator.set_inImage(createFromBitmap);
        Bitmap createBitmap = Bitmap.createBitmap(newWidth(bitmap, i2), newHeight(bitmap, i2), bitmap.getConfig());
        Allocation createFromBitmap2 = Allocation.createFromBitmap(this.f213rs, createBitmap);
        if (i2 == 90) {
            this.sriptRotator.forEach_rotate_90_clockwise(createFromBitmap2, createFromBitmap2);
        } else if (i2 == 180) {
            this.sriptRotator.forEach_flip_vertically(createFromBitmap2, createFromBitmap2);
        } else if (i2 == 270) {
            this.sriptRotator.forEach_rotate_270_clockwise(createFromBitmap2, createFromBitmap2);
        }
        createFromBitmap2.copyTo(createBitmap);
        return createBitmap;
    }

    public final Bitmap flip(Bitmap bitmap) {
        Allocation createFromBitmap = Allocation.createFromBitmap(this.f213rs, bitmap);
        this.sriptRotator.set_inWidth(bitmap.getWidth());
        this.sriptRotator.set_inHeight(bitmap.getHeight());
        this.sriptRotator.set_inImage(createFromBitmap);
        Allocation createTyped = Allocation.createTyped(this.f213rs, createFromBitmap.getType());
        this.sriptRotator.forEach_flip_horizontally(createTyped, createTyped);
        createTyped.copyTo(bitmap);
        return bitmap;
    }

    public final int newHeight(Bitmap bitmap, int i) {
        return (i == 90 || i == 270) ? bitmap.getWidth() : bitmap.getHeight();
    }

    public final int newWidth(Bitmap bitmap, int i) {
        return (i == 90 || i == 270) ? bitmap.getHeight() : bitmap.getWidth();
    }

    private final void imageToByteBuffer(Image image, byte[] bArr) {
        int i;
        int i2;
        Image.Plane[] planeArr;
        Rect rect;
        YuvToRgbConverter yuvToRgbConverter = this;
        byte[] bArr2 = bArr;
        if (image.getFormat() == 35) {
            Rect cropRect = image.getCropRect();
            Image.Plane[] planes = image.getPlanes();
            int length = planes.length;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            while (i3 < length) {
                Image.Plane plane = planes[i3];
                int i6 = i4 + 1;
                if (i4 != 0) {
                    if (i4 == 1) {
                        i5 = yuvToRgbConverter.pixelCount + 1;
                    } else if (i4 != 2) {
                        i3++;
                        i4 = i6;
                    } else {
                        i5 = yuvToRgbConverter.pixelCount;
                    }
                    i = 2;
                } else {
                    i5 = 0;
                    i = 1;
                }
                ByteBuffer buffer = plane.getBuffer();
                int rowStride = plane.getRowStride();
                int pixelStride = plane.getPixelStride();
                if (i4 == 0) {
                    rect = cropRect;
                    planeArr = planes;
                    i2 = length;
                } else {
                    planeArr = planes;
                    i2 = length;
                    rect = new Rect(cropRect.left / 2, cropRect.top / 2, cropRect.right / 2, cropRect.bottom / 2);
                }
                int width = rect.width();
                int height = rect.height();
                byte[] bArr3 = new byte[plane.getRowStride()];
                int i7 = 1;
                int i8 = (pixelStride == 1 && i == 1) ? width : ((width - 1) * pixelStride) + 1;
                int i9 = 0;
                while (i9 < height) {
                    Rect rect2 = cropRect;
                    buffer.position(((rect.top + i9) * rowStride) + (rect.left * pixelStride));
                    if (pixelStride == 1 && i == 1) {
                        buffer.get(bArr2, i5, i8);
                        i5 += i8;
                    } else {
                        buffer.get(bArr3, 0, i8);
                        for (int i10 = 0; i10 < width; i10++) {
                            bArr2[i5] = bArr3[i10 * pixelStride];
                            i5 += i;
                        }
                    }
                    i9++;
                    cropRect = rect2;
                    i7 = 1;
                }
                i3 += i7;
                yuvToRgbConverter = this;
                i4 = i6;
                planes = planeArr;
                length = i2;
                cropRect = cropRect;
            }
            return;
        }
        throw new AssertionError("Assertion failed");
    }
}
