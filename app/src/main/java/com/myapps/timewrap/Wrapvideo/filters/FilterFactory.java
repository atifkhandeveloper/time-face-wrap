package com.myapps.timewrap.Wrapvideo.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class FilterFactory {
    IImageFilter filter;

    public IImageFilter createFilter(Context context, FilterItem filterItem) {
        try {
            IImageFilter iImageFilter = (IImageFilter) Class.forName(filterItem.getName()).getConstructor(new Class[]{Context.class}).newInstance(new Object[]{context});
            this.filter = iImageFilter;
            iImageFilter.setIsFullImageFilter(filterItem.isFullImageFilter());
            return this.filter;
        } catch (Exception e) {
            Log.d("myApp", "FilterManager: " + e.getClass());
            return new NoFilter(context);
        }
    }

    public Bitmap applyFilter(Bitmap bitmap) {
        IImageFilter iImageFilter = this.filter;
        if (iImageFilter == null) {
            return null;
        }
        return iImageFilter.process(bitmap);
    }
}
