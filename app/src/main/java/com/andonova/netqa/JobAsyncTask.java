package com.andonova.netqa;

import android.os.AsyncTask;
import android.util.Log;
import com.andonova.netqa.models.JobObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class JobAsyncTask extends AsyncTask<List<JobObject>, Void, String> {

    /**
     * Runs on the background thread.
     *
     * @param lists of jobs
     * @return Returns the result from the job in String format
     */
    @Override
    protected String doInBackground(List<JobObject>... lists) {

        List<JobObject> jobs = lists[0];
        String pingResult = "";

        try {
            JobObject job = jobs.get(0);
            //PING job:
            if(job.getJobType().equals("PING")){
                String pingCmd = "ping -s "+ job.getPacketSize() +" -c "+ job.getNumPackets() +" -i "+ job.getJobPeriod() + " "+ job.getHostAddress();
                pingResult = "";
                Runtime r = Runtime.getRuntime();
                Process p = r.exec(pingCmd);
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    pingResult += inputLine;
                }
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return a String result
        return pingResult;
    }

    /**
     * Process results from the doInBackground method. Writes results to log!!
     * @param result The string returned from the doInBackground method !!!
     */
    @Override
    protected void onPostExecute(String result) {
        Log.i("asyncTaskClass", "Result from the job: "+ result);
    }

    /**
     * Runs on the main thread before the task is executed.
     * This step is normally used to set up the task, for instance by showing a progress
     * bar in the UI.
     * •doInBackground(Params...) is invoked on the background thread immediately
     * after onPreExecute() finishes
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * Runs on the main (UI) thread
     * • Receives calls from doInBackground() from background thread
     * • Use onProgressUpdate() to report any form of progress to the UI thread while the background computation is executing.
     * • For instance, you can use it to pass the data to animate a progress bar or show logs in a text field.
     * @param values - arguments passed in a call to this method, from the doInBackground method
     */
    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    /**
     * This method is invoked after the AsyncTask is cancelled (previously with cancel() method)
     * Ignores the result !!!
     * @param s - object
     */
    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }

}
