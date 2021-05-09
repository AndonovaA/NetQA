package com.andonova.netqa;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ProcessMainClass {

    public static final String TAG = ProcessMainClass.class.getSimpleName();
    private static Intent serviceIntent = null;

    public ProcessMainClass() {
    }

    /**
     * launching the Service!
     */
    public void launchService(Context context) {
        if (context == null) {
            return;
        }
        if (serviceIntent == null) {
            serviceIntent = new Intent(context, Service.class);   //intent for calling the Service class
        }
        // depending on the version of Android we either launch the simple service (version<O)
        // or we start a foreground service  () !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Foreground services must display a Notification.
            //Foreground services continue running even when the user isn’t interacting with the app.
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.i(TAG, "ProcessMainClass: start service !!!");
    }

}
