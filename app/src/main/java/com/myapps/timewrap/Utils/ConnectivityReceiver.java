package com.myapps.timewrap.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class ConnectivityReceiver extends BroadcastReceiver {
    public static CDL cdl;

    public interface CDL {
        void onNwChanged(boolean z);
    }

    public void onReceive(Context context, Intent intent) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        boolean z = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        CDL cdl2 = cdl;
        if (cdl2 != null) {
            cdl2.onNwChanged(z);
        }
    }

}
