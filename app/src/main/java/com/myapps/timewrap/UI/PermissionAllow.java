package com.myapps.timewrap.UI;

import android.content.Context;
import android.os.Build;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.util.List;

public class PermissionAllow {


    public static void GetPermission(Context context) {

        if (Build.VERSION.SDK_INT >= 33) {
            ((TedPermission.Builder) ((TedPermission.Builder) TedPermission.create().setPermissions("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO", "android.permission.CAMERA", "android.permission.RECORD_AUDIO")).setPermissionListener(new PermissionListener() {
                public void onPermissionDenied(List<String> list) {
                    //Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                    //intent.setData(Uri.fromParts("package", SecondActivity.this.getPackageName(), (String) null));
                    //startActivityForResult(intent, 1000);
                }

                public void onPermissionGranted() {
                    //after permission

                }
            })).check();
        } else {
            ((TedPermission.Builder) ((TedPermission.Builder) TedPermission.create().setPermissions("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA", "android.permission.RECORD_AUDIO")).setPermissionListener(new PermissionListener() {
                public void onPermissionDenied(List<String> list) {
                    //Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                    //intent.setData(Uri.fromParts("package", SecondActivity.this.getPackageName(), (String) null));
                    //startActivityForResult(intent, 1000);
                }

                public void onPermissionGranted() {
                    //after permission

                }
            })).check();
        }

    }
}
