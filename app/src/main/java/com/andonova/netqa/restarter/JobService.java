package com.andonova.netqa.restarter;

import android.content.Intent;
import android.app.job.JobParameters;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.andonova.netqa.Globals;
import com.andonova.netqa.ProcessMainClass;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobService extends android.app.job.JobService {

    /* A JobService guarantees that the process(in our case the Service) will finish! */

    private static String TAG= JobService.class.getSimpleName();
    private static RestartServiceBroadcastReceiver restartSensorServiceReceiver;
    private static JobService instance;
    private static JobParameters jobParameters;

    /**
     *  Invoke the creation of the service (via the class ProcessMainClass)
     *  and register the receiver for any request for restarting.
     */
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        ProcessMainClass bck = new ProcessMainClass();
        bck.launchService(this);  //create(launch) the service !!!
        registerRestarterReceiver();
        instance= this;
        JobService.jobParameters= jobParameters;

        return false;
    }

    /**
     * Register the receiver ready for any request of restart!!!
     * -> we register the  receiver that will restart the background Service if it is killed!
     * -> see onDestroy of Service.
     */
    private void registerRestarterReceiver() {
        // the context can be null.
        // in case it is called from installation of new version (i.e. from manifest, the application is null.
        // So we must use context.registerReceiver. Otherwise this will crash and we try with context.getApplicationContext
        if (restartSensorServiceReceiver == null)
            restartSensorServiceReceiver = new RestartServiceBroadcastReceiver();
        else try{
            unregisterReceiver(restartSensorServiceReceiver);
        } catch (Exception e){
            // not registered
        }
        // give the time to run:
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Setting the custom intent -> RESTART_INTENT !!!
                IntentFilter filter = new IntentFilter();
                filter.addAction(Globals.RESTART_INTENT);
                try {
                    registerReceiver(restartSensorServiceReceiver, filter);
                } catch (Exception e) {
                    try {
                        getApplicationContext().registerReceiver(restartSensorServiceReceiver, filter);
                    } catch (Exception ex) {

                    }
                }
            }
        }, 1000);
    }

    /**
     *  Called if Android kills the job service !!!
     * This method is in charge of requesting to RESTART ITSELF via the Broadcast Receiver and after a while afterwards,
     * it will unregister the receiver (by delaying a bit to avoid unregistering before the receiver receives the message(custom intent).
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(TAG, "Stopping job");
        // Send RESTART_INTENT to the broadcast receiver, for restarting itself:
        Intent broadcastIntent = new Intent(Globals.RESTART_INTENT);
        sendBroadcast(broadcastIntent);
        // unregister the receiver:
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                unregisterReceiver(restartSensorServiceReceiver);
            }
        }, 1000);

        return false;
    }


    /**
     * called when the tracker is stopped for whatever reason
     */
    public static void stopJob(Context context) {
        if (instance!=null && jobParameters!=null) {
            try{
                instance.unregisterReceiver(restartSensorServiceReceiver);
            } catch (Exception e){
                // not registered
            }
            Log.i(TAG, "Finishing job");
            instance.jobFinished(jobParameters, true);
        }
    }
}