package com.myapps.timewrap.Wrapvideo.fragments;

import android.graphics.Bitmap;
import android.net.Uri;

public class Video {
    private Bitmap bitmap;
    private String name;
    private String realPath;
    private String thumbnails;
    private Uri uri;

    public Video(Uri uri2, String str, String str2, String str3) {
        this.uri = uri2;
        this.name = str;
        this.thumbnails = str2;
        this.realPath = str3;
    }

    public Video(String str, String str2, String str3) {
        this.name = str;
        this.thumbnails = str2;
        this.realPath = str3;
    }

    public Uri getUri() {
        return this.uri;
    }

    public String getName() {
        return this.name;
    }

    public String getThumbnails() {
        return this.thumbnails;
    }

    public Bitmap getBitmap() {
        return this.bitmap;
    }

    public void setBitmap(Bitmap bitmap2) {
        this.bitmap = bitmap2;
    }

    public String getRealPath() {
        return this.realPath;
    }
}
