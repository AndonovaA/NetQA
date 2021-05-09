package com.andonova.netqa;

/*
 * Copyright (c) 2019. This code has been developed by Fabio Ciravegna, The University of Sheffield. All rights reserved. No part of this code can be used without the explicit written permission by the author
 */

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import com.andonova.netqa.restarter.RestartServiceBroadcastReceiver;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*setContentView(R.layout.activity_main);*/
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //We use scheduleJob() method from the receiver, to schedule the job:
            RestartServiceBroadcastReceiver.scheduleJob(getApplicationContext());
        } else {
            //We use directly ProcessMainClass to create(launch) the service:
            ProcessMainClass bck = new ProcessMainClass();
            bck.launchService(getApplicationContext());
        }

        finish();  //When calling finish() on an activity, the method onDestroy() is executed.
        //if we call finish() method from the onCreate() method,  then it will execute the whole onCreate() method first
        //and then it will execute the lifecycle method onDestroy() and the Activity gets destroyed.
        //Calling finish() in onResume(): onCreate() -> onStart() -> onResume() -> onPause() -> onStop() -> onDestroy()

        //That's why we need to call finish() from onResume() method, in order to call onResume() again, after finishing the activity.
    }
}