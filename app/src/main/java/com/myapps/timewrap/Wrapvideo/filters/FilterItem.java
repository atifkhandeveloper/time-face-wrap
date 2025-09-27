package com.myapps.timewrap.Wrapvideo.filters;

public class FilterItem {
    boolean directional;
    String icon;
    boolean isFullImageFilter;
    boolean isPremium;
    String name;
    String preview;
    String title;

    public FilterItem(String str, String str2, String str3, String str4, boolean z, boolean z2, boolean z3) {
        this.name = str;
        this.icon = str2;
        this.preview = str3;
        this.title = str4;
        this.isFullImageFilter = z;
        this.directional = z2;
        this.isPremium = z3;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String str) {
        this.name = str;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String str) {
        this.icon = str;
    }

    public String getPreview() {
        return this.preview;
    }

    public void setPreview(String str) {
        this.preview = str;
    }

    public boolean isFullImageFilter() {
        return this.isFullImageFilter;
    }

    public void setFullImageFilter(boolean z) {
        this.isFullImageFilter = z;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public boolean isDirectional() {
        return this.directional;
    }

    public void setDirectional(boolean z) {
        this.directional = z;
    }

    public boolean isPremium() {
        return this.isPremium;
    }

    public void setPremium(boolean z) {
        this.isPremium = z;
    }
}
