package com.myapps.timewrap.Wrapvideo.filters;

import android.content.Context;

class BackgroundImageFilter extends NoFilter {
    public boolean isBackgorundFilter() {
        return true;
    }

    public BackgroundImageFilter(Context context) {
        super(context);
    }
}
