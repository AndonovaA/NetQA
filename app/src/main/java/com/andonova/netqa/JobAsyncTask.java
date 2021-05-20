package com.andonova.netqa;

import android.os.AsyncTask;
import android.util.Log;
import com.andonova.netqa.models.JobObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
        StringBuilder pingResult;
        JobObject job = jobs.get(0);

        try {
            //PING job:
            if(job.getJobType().equals("PING")){
                String pingCmd = "ping -c "+ job.getNumPackets() +" -s "+ job.getPacketSize() +" "+ job.getHostAddress();
                pingResult = new StringBuilder();
                Runtime r = Runtime.getRuntime();
                Process p = r.exec(pingCmd);
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    pingResult.append(inputLine);
                }
                in.close();
                Log.i("JobAsyncTask", "***** Result from the job: "+ pingResult);

                //HTTP POST:
                try {
                    URL url = new URL ("http://192.168.0.176:5000/postresults");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json; utf-8");
                    con.setRequestProperty("Accept", " */* ");
                    con.setDoOutput(true);
                    /*            {
                                    "result":"tekst na rezultatot"
                                  }                                                                 */
                    String jsonInputString = " {\"result\": \"" + pingResult + " \"} ";
                    try(OutputStream os = con.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    Log.i("JobAsyncTask", "****** Result: "+ jsonInputString);
                    StringBuilder response = new StringBuilder();
                    try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.i("JobAsyncTask", "****** Response after POST: "+ con.getResponseCode() + " "+ con.getResponseMessage());
                        return response.toString();
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return "empty string";
    }

    /**
     * Process results from the doInBackground method.
     * @param result The string returned from the doInBackground method !!!
     */
    @Override
    protected void onPostExecute(String result) {
        //Log.i("JobAsyncTask", "Result: "+ result);

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
