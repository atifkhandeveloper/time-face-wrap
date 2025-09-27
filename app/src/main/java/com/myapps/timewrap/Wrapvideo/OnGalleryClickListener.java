package com.myapps.timewrap.Wrapvideo;

import android.net.Uri;
import com.myapps.timewrap.Wrapvideo.fragments.Video;

public interface OnGalleryClickListener {
    void onClick(Video video, String str);

    void share(Uri uri);
}
