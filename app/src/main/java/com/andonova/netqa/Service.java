package com.andonova.netqa;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.andonova.netqa.models.JobObject;
import com.andonova.netqa.utilities.Notification;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Service extends android.app.Service {

    /* THE NEVER ENDING SERVICE */
    private static Service mCurrentService;

    protected static final int NOTIFICATION_ID = 1337;
    private static final String TAG = "Service";
    private List<JobObject> jobs = null;
    RequestQueue requestQueue = null;

    public Service() {
        super();
    }

    public static Service getmCurrentService() { return mCurrentService; }
    public static void setmCurrentService(Service mCurrentService) { Service.mCurrentService = mCurrentService; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartForeground();    // the function is declared under
        }
        mCurrentService = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "restarting Service! ");

        // it has been killed by Android and now it is restarted. We must make sure to have reinitialised everything
        if (intent == null) {
            ProcessMainClass bck = new ProcessMainClass();
            bck.launchService(this);
        }

        // make sure you call the startForeground on onStartCommand because otherwise
        // when we hide the notification on onScreen it will nto restart in Android 6 and 7
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            restartForeground();
        }

        startTimer(); //HERE!!!

        // return start sticky so if it is killed by android, it will be restarted with Intent null
        return START_STICKY;
    }


    /**
     * THIS IS REQUIRED IN ANDROID 8:
     * it starts the process in foreground. Normally this is done when screen goes off.
     * -> "The system allows apps to call Context.startForegroundService() even while the app is in the background.
     *  However, the app must call that service's startForeground() method within five seconds after the service is created."
     *  -> look at ProcessMainClass's launchService() method !!!
     */
    public void restartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "restarting foreground");
            //Foreground services must display a Notification
            try {
                Notification notification = new Notification();
                startForeground(NOTIFICATION_ID, notification.setNotification(this, "Service notification", "This is the service's notification", R.drawable.ic_sleep));
                Log.i(TAG, "restarting foreground successful");
                startTimer(); //HERE !!!
            } catch (Exception e) {
                Log.e(TAG, "Error in notification " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
        //onDestroy is called when the service is stopped by the app (i.e. the app is killed wither by Android or by the user).
        //In this case, the service sends a message(custom intent) to the BroadcastReceiver which will RESTART THE SERVICE
        // after the service stop (it is an asynchronous call so it will not be affected by the death of the service).
        Intent broadcastIntent = new Intent(Globals.RESTART_INTENT);
        sendBroadcast(broadcastIntent);

        startTimer();
    }

    /**
     * this is called when the process is killed by Android
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved called");
        // restart the never ending service
        Intent broadcastIntent = new Intent(Globals.RESTART_INTENT);
        sendBroadcast(broadcastIntent);
        // stopTimerTask();
    }

/*    *//**
     * Cancels all request in the queue.
     *//*
    public void stopApiRequest() {
        Log.i(TAG, "stopApiRequest()");
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
            requestQueue = null;
        }
    }*/

    /**
     * static to avoid multiple timers to be created when the service is called several times
     */
    private static Timer timer;
    private static TimerTask timerTask;

    public void startTimer() {
        Log.i(TAG, "Starting timer");

        //set a new Timer - if one is already running, cancel it to avoid two running at the same time
        stopTimerTask();
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        Log.i(TAG, "Scheduling...");
        //schedule the timer, to wake up every 10 minutes
        timer.schedule(timerTask, 1000, 600000);
    }

    /**
     * it sets the timer to send request to the API, every 10 minutes
     */
    public void initializeTimerTask() {
        Log.i(TAG, "initialising TimerTask");
        timerTask = new TimerTask() {
            public void run() {
                // Check the status of the network connection
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo(); //describes the status of a network interface of a given type (currently either mobile or Wi-Fi)
                // If the network interface is active and connected, send a query and get reply!
                if(networkInfo != null && networkInfo.isConnected()){
                    //Do some job in thread, based on the response. We have the response in list "jobs"
                    sendRequest();
                }
                else {
                    // Otherwise write in the log
                    Log.i(TAG, "No internet connectivity!");
                }
            }
        };
    }

    /**
     * Stop the timer task.
     */
    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Volley integration of sending request to API and getting response in object with Gson
     */
    public void sendRequest(){

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        requestQueue = new RequestQueue(cache, network);
        // Start the queue
        requestQueue.start();

        String url ="http://10.0.2.2:5000/getjobs";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.i(TAG, "Successfully loaded json!");
                    //response is type - json array
                    GsonBuilder gsonb = new GsonBuilder();
                    Gson gson = gsonb.create();
                    jobs = Arrays.asList(gson.fromJson(String.valueOf(response), JobObject[].class));
                    Log.i(TAG, jobs.get(0).toString()); //response is in jobs.get(0)
                    //Let's do the job:
                    if(jobs != null){
                        Log.i(TAG, "Let's ping..");
                        new JobAsyncTask().execute(jobs); //params=jobs !!!
                    }
                },
                error -> {
                    Log.i(TAG, "Error loading json!");
                }
        );
        // Add the request to the RequestQueue.
        requestQueue.add(jsonArrayRequest);
    }

}