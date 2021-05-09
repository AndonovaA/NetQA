package com.andonova.netqa.restarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.RequiresApi;

import com.andonova.netqa.Globals;
import com.andonova.netqa.ProcessMainClass;
import static android.content.Context.JOB_SCHEDULER_SERVICE;


public class RestartServiceBroadcastReceiver extends BroadcastReceiver {

    /* BroadcastReceiver will receive a signal when someone or something kills the service; its role is to restart the service. */

    public static final String TAG = RestartServiceBroadcastReceiver.class.getSimpleName();
    private static JobScheduler jobScheduler;
    private RestartServiceBroadcastReceiver restartSensorServiceReceiver;

    /**
     * it returns the number of version code
     */
    public static long getVersionCode(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long versionCode = System.currentTimeMillis();  //PackageInfoCompat.getLongVersionCode(pInfo);
            return versionCode;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return 0;
    }

    /**
     * Invokes when signal(intent) received !!!
     *  the service sends a message(intent) to the BroadcastReceiver which will restart the service after the service stops.
     *  (it is an asynchronous call so it will not be affected by the death of the service).
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.i(TAG, "about to start timer " + context.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //We use Scheduler to schedule the job that is a JobService:
            scheduleJob(context);
        } else {
            registerRestarterReceiver(context);
            //calling directly the ProcessMainClass to create(launch) the service:
            ProcessMainClass bck = new ProcessMainClass();
            bck.launchService(context);
        }
    }

    /**
     * FOR ANDROID > 7:
     * any process called by a BroadcastReceiver is run at low priority and hence eventually killed by Android.
     * So, the service will end up being killed. In order to solve this issue, we must create the service in the BroadcastReceiver
     * in a way that keeps high priority. The answer is JobService !!!
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context) {
        if (jobScheduler == null) {
            jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        }
        ComponentName componentName = new ComponentName(context, JobService.class);
        JobInfo jobInfo = new JobInfo.Builder(1, componentName)
                .setOverrideDeadline(0)
                .setPersisted(true).build();
        // setOverrideDeadline is a dummy constraint asking to start immediately (at least one is required)
        // setPersisted  requires to restart the Job in case the phone is rebooted.
        jobScheduler.schedule(jobInfo);
    }

    /**
     * FOR ANDROID <= 7: Register the receiver for any request of restart!!!
     * -> we register the  receiver that will restart the background Service.
     *  ->  see onDestroy of Service.
     */
    private void registerRestarterReceiver(final Context context) {
        // the context can be null if app just installed and this is called from restartsensorservice
        // https://stackoverflow.com/questions/24934260/intentreceiver-components-are-not-allowed-to-register-to-receive-intents-when
        // Final decision: in case it is called from installation of new version (i.e. from manifest, the application is
        // null. So we must use context.registerReceiver. Otherwise this will crash and we try with context.getApplicationContext
        if (restartSensorServiceReceiver == null)
            restartSensorServiceReceiver = new RestartServiceBroadcastReceiver();
        else try{
            context.unregisterReceiver(restartSensorServiceReceiver);
        } catch (Exception e){
            // not registered
        }

        // give the time to run
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Setting the custom intent -> RESTART_INTENT !!!
                IntentFilter filter = new IntentFilter();
                filter.addAction(Globals.RESTART_INTENT);
                try {
                    context.registerReceiver(restartSensorServiceReceiver, filter);
                } catch (Exception e) {
                    try {
                        context.getApplicationContext().registerReceiver(restartSensorServiceReceiver, filter);
                    } catch (Exception ex) {

                    }
                }
            }
        }, 1000);

    }

    /**
     * restart the never ending service
     */
    public static void reStartTracker(Context context) {
        Log.i(TAG, "Restarting tracker");
        Intent broadcastIntent = new Intent(Globals.RESTART_INTENT);
        context.sendBroadcast(broadcastIntent);
    }

}