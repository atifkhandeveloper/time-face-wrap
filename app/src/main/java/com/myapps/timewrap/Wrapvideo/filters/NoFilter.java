package com.myapps.timewrap.Wrapvideo.filters;

import android.content.Context;
import android.graphics.Bitmap;

class NoFilter extends IImageFilter {
    public void _postProcess() {
    }

    public final void _process() {
    }

    public void preProcess(Bitmap bitmap) {
    }

    public Bitmap process(Bitmap bitmap) {
        return bitmap;
    }

    public NoFilter(Context context) {
        super(context);
    }
}
