package com.myapps.timewrap.Wrapvideo.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.internal.view.SupportMenu;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Wrapvideo.base.utils.ColorUtils;
import com.myapps.timewrap.Wrapvideo.base.utils.SharedPreferencesManager;
import com.myapps.timewrap.Wrapvideo.filters.FilterItem;

public class ScanSettings implements Parcelable {
    public static final Creator<ScanSettings> CREATOR = new Creator<ScanSettings>() {
        public ScanSettings createFromParcel(Parcel parcel) {
            return new ScanSettings(parcel);
        }

        public ScanSettings[] newArray(int i) {
            return new ScanSettings[i];
        }
    };
    private Bitmap backgroundImage;
    private int direction;
    private FilterItem filter;
    private String imagePath;
    private int saveImage;
    private int scannerColor;
    private int shape;
    private int speed;
    private boolean voiceCommandsEnabled;

    public int describeContents() {
        return 0;
    }

    public ScanSettings() {
        this.voiceCommandsEnabled = true;
        this.scannerColor = SupportMenu.CATEGORY_MASK;
        this.direction = 2;
        this.shape = 1;
        this.speed = 1;
        this.saveImage = 0;
        this.filter = getDefaultFilter();
        this.imagePath = null;
    }

    protected ScanSettings(Parcel parcel) {
        this();
    }

    private static FilterItem getDefaultFilter() {
        return new FilterItem("com.myapps.timewrap.wrapvideo.filters.NoFilter", "none", "https://youtu.be/l8xR5XiTXmM", "No", false, false, false);
    }

    public int getScannerColor() {
        return this.scannerColor;
    }

    public void setScannerColor(int i) {
        this.scannerColor = i;
    }

    public int getDirection() {
        return this.direction;
    }

    public void setDirection(int i) {
        this.direction = i;
    }

    public int getShape() {
        return this.shape;
    }

    public void setShape(int i) {
        this.shape = i;
    }

    public FilterItem getFilter() {
        return this.filter;
    }

    public void setFilter(FilterItem filterItem) {
        this.filter = filterItem;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.scannerColor);
        parcel.writeInt(this.direction);
        parcel.writeInt(this.shape);
    }

    public int getSpeed() {
        return this.speed;
    }

    public void setSpeed(int i) {
        this.speed = i;
    }

    public boolean isVoiceCommandsEnabled() {
        return this.voiceCommandsEnabled;
    }

    public void setVoiceCommandsEnabled(boolean z) {
        this.voiceCommandsEnabled = z;
    }

    public String getImagePath() {
        return this.imagePath;
    }

    public void setImagePath(String str) {
        this.imagePath = str;
    }

    public Bitmap getBackgroundImage() {
        return this.backgroundImage;
    }

    public void setBackgroundImage(Bitmap bitmap) {
        this.backgroundImage = bitmap;
    }

    public boolean isBackgroundFilter() {
        FilterItem filterItem = this.filter;
        return filterItem != null && filterItem.getName().contains("BackgroundImageFilter");
    }

    public int getSaveImage() {
        return this.saveImage;
    }

    public boolean isSaveImage() {
        return getSaveImage() == 1;
    }

    public void setSaveImage(int i) {
        this.saveImage = i;
    }

    public void readFromPreferences(Activity activity) {
        setShape(SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_SHAPE, getShape()));
        setDirection(SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_DIRECTION, getDirection()));
        setSpeed(SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_SPEED, getSpeed()));
        setScannerColor(SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_COLOR, ColorUtils.getColor(activity, R.color.dark_bg)));
        setImagePath(SharedPreferencesManager.getString(activity, SharedPreferencesManager.SCAN_FILTER_IMAGE, (String) null));
        setSaveImage(SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_SAVE_IMAGE, getSaveImage()));
        FilterItem filterItem = this.filter;
        if (filterItem != null) {
            String string = SharedPreferencesManager.getString(activity, SharedPreferencesManager.SCAN_FILTER_NAME, filterItem.getName());
            if (string == null || string == "") {
                this.filter = getDefaultFilter();
                return;
            }
            this.filter.setName(string);
            boolean z = false;
            int i = SharedPreferencesManager.getInt(activity, SharedPreferencesManager.SCAN_FILTER_FULLIMAGE, 0);
            FilterItem filterItem2 = this.filter;
            if (i == 1) {
                z = true;
            }
            filterItem2.setFullImageFilter(z);
        }
    }
}
