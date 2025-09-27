package com.myapps.timewrap.Wrapvideo.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;
import android.util.Log;

public abstract class IImageFilter {
    protected int direction;
    private boolean isFullImageFilter;
    protected Bitmap mBitmapOut;
    protected Allocation mInAllocation;
    protected Allocation mOutAllocation;
    protected RenderScript mRS;
    protected ScriptC mScript;
    private long startTime;

    public void _postProcess() {
    }

    public void _preProcess() {
    }

    public abstract void _process();

    public boolean isBackgorundFilter() {
        return false;
    }

    public boolean isFakeFilter() {
        return false;
    }

    public IImageFilter(Context context) {
        this.mRS = RenderScript.create(context);
    }

    public void preProcess(Bitmap bitmap) {
        Allocation createFromBitmap = Allocation.createFromBitmap(this.mRS, bitmap, Allocation.MipmapControl.MIPMAP_NONE, 1);
        this.mInAllocation = createFromBitmap;
        if (this.mOutAllocation == null) {
            this.mOutAllocation = Allocation.createTyped(this.mRS, createFromBitmap.getType());
            this.mBitmapOut = bitmap.copy(bitmap.getConfig(), true);
        }
    }

    public Bitmap process(Bitmap bitmap) {
        preProcess(bitmap);
        this.startTime = System.currentTimeMillis();
        _preProcess();
        _process();
        Log.d("myApp", getClass().getSimpleName() + " use " + (System.currentTimeMillis() - this.startTime));
        _postProcess();
        postProcess();
        return this.mBitmapOut;
    }

    public void postProcess() {
        this.mOutAllocation.copyTo(this.mBitmapOut);
    }

    public void destory() {
        this.mScript.destroy();
        this.mScript = null;
        this.mInAllocation.destroy();
        this.mInAllocation = null;
        this.mOutAllocation.destroy();
        this.mOutAllocation = null;
        this.mRS.destroy();
        this.mRS = null;
        System.gc();
    }

    public boolean isFullImageFilter() {
        return this.isFullImageFilter;
    }

    public void setIsFullImageFilter(boolean z) {
        this.isFullImageFilter = z;
    }

    public void setDirection(int i) {
        this.direction = i;
    }
}
