package com.myapps.timewrap.UI;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.Wrapvideo.OnGalleryClickListener;
import com.myapps.timewrap.Wrapvideo.fragments.Video;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class CreationAdapter extends RecyclerView.Adapter<CreationAdapter.ViewHolder> {
    int imageWidth = 90;
    OnGalleryClickListener listener;
    Activity mContext;
    private final List<Video> mValues;
    ContentResolver resolver;
    String type;

    public CreationAdapter(List<Video> list, OnGalleryClickListener onGalleryClickListener, int i, ContentResolver contentResolver, Activity activity) {
        this.listener = onGalleryClickListener;
        this.mValues = list;
        this.imageWidth = i;
        this.resolver = contentResolver;
        this.mContext = activity;
    }

    public CreationAdapter(List<Video> list, OnGalleryClickListener onGalleryClickListener, int i, ContentResolver contentResolver, Activity activity, String str) {
        this.listener = onGalleryClickListener;
        this.mValues = list;
        this.imageWidth = i;
        this.resolver = contentResolver;
        this.mContext = activity;
        this.type = str;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_creation, viewGroup, false));
    }

    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        viewHolder.mItem = this.mValues.get(i);
        if (this.type.equalsIgnoreCase(C1197util.wrapImage)) {
            ((RequestBuilder) ((RequestBuilder) ((RequestBuilder) ((RequestBuilder) Glide.with(this.mContext).load(this.mValues.get(i).getRealPath()).placeholder((int) R.drawable.icon)).diskCacheStrategy(DiskCacheStrategy.NONE)).skipMemoryCache(true)).error((int) R.drawable.icon)).into(viewHolder.mContentView);
        } else if (this.type.equalsIgnoreCase(C1197util.wrapVideo)) {
            viewHolder.mContentView.setImageURI(this.mValues.get(i).getUri());
            if (viewHolder.mItem.getBitmap() == null) {
                new BitmapWorkerTask(viewHolder.mContentView).execute(new Video[]{viewHolder.mItem});
            }
        } else if (this.type.equalsIgnoreCase(C1197util.waterfallVideo)) {
            viewHolder.mContentView.setImageResource(R.drawable.icon);
            //viewHolder.mContentView.setImageURI(this.mValues.get(i).getUri());
            if (viewHolder.mItem.getBitmap() == null) {
                new BitmapWorkerTask(viewHolder.mContentView).execute(new Video[]{viewHolder.mItem});
            }
        }
        viewHolder.mContentView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CreationAdapter.this.listener.onClick(viewHolder.mItem, CreationAdapter.this.type);
            }
        });
    }

    public int getItemCount() {
        return this.mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mContentView;
        public Video mItem;
        public final View mView;

        public ViewHolder(View view) {
            super(view);
            this.mView = view;
            this.mContentView = (ImageView) view.findViewById(R.id.imgTumb);
        }
    }

    class BitmapWorkerTask extends AsyncTask<Video, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private Video video;

        public BitmapWorkerTask(ImageView imageView) {
            this.imageViewReference = new WeakReference<>(imageView);
        }

        public Bitmap doInBackground(Video... videoArr) {
            Video video2 = videoArr[0];
            this.video = video2;
            Bitmap loadVideoThumbnail = loadVideoThumbnail(video2.getUri(), this.video.getRealPath(), CreationAdapter.this.resolver);
            this.video.setBitmap(loadVideoThumbnail);
            return loadVideoThumbnail;
        }

        public Bitmap loadVideoThumbnail(Uri uri, String str, ContentResolver contentResolver) {
            if (Build.VERSION.SDK_INT < 29) {
                return ThumbnailUtils.createVideoThumbnail(str, 1);
            }
            try {
                return contentResolver.loadThumbnail(uri, new Size(128, 128), new CancellationSignal());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void onPostExecute(Bitmap bitmap) {
            ImageView imageView;
            WeakReference<ImageView> weakReference = this.imageViewReference;
            if (weakReference != null && bitmap != null && (imageView = (ImageView) weakReference.get()) != null) {
                ((RequestBuilder) ((RequestBuilder) ((RequestBuilder) ((RequestBuilder) ((RequestBuilder) Glide.with(CreationAdapter.this.mContext).load(bitmap).placeholder((int) R.drawable.icon)).error((int) R.drawable.icon)).diskCacheStrategy(DiskCacheStrategy.NONE)).skipMemoryCache(true)).override(200, 200)).into(imageView);
            }
        }
    }
}
