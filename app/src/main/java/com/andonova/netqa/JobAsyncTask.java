package com.andonova.netqa;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.andonova.netqa.models.JobObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class JobAsyncTask extends AsyncTask<List<JobObject>, Void, String> {

    SharedPreferences sharedPreferences;
    public Context mContext;

    public JobAsyncTask(Context applicationContext) {
        mContext = applicationContext;
    }

    /**
     * Runs on the background thread.
     *
     * @param lists of jobs
     * @return Returns the result from the job in String format
     */
    @Override
    protected String doInBackground(List<JobObject>... lists) {

        List<JobObject> jobs = lists[0];
        JobObject job = jobs.get(0);

        //PING job:
        if (job.getJobType().equals("PING")) {
            try {
                Log.i("JobAsyncTask", "************* doInBackground() *****************");
                return pingJob(job);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "unknown type of job";
    }

    /**
     * Process results from the doInBackground method.
     *
     * @param result The string (ping result) returned from the doInBackground method !!!
     */
    @Override
    protected void onPostExecute(String result) {
        Log.i("JobAsyncTask", "After sharedPreferences: " + result);
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
        //Get sharedPreferences
        Log.i("JobAsyncTask", "************* onPreExecute() *****************");
        sharedPreferences = mContext.getSharedPreferences("com.andonova.netqa.PREFERENCE_JOB_KEY", Context.MODE_PRIVATE);
    }

    /**
     * Runs on the main (UI) thread
     * • Receives calls from doInBackground() from background thread
     * • Use onProgressUpdate() to report any form of progress to the UI thread while the background computation is executing.
     * • For instance, you can use it to pass the data to animate a progress bar or show logs in a text field.
     *
     * @param values - arguments passed in a call to this method, from the doInBackground method
     */
    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    /**
     * This method is invoked after the AsyncTask is cancelled (previously with cancel() method)
     * Ignores the result !!!
     *
     * @param s - object
     */
    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }


    /**
     * PING type of a job
     */
    private String pingJob(JobObject job) throws IOException {

        //PING:
        StringBuilder pingResult;
        String pingCmd = "ping -c " + job.getNumPackets() + " -s " + job.getPacketSize() + " " + job.getHostAddress();
        pingResult = new StringBuilder();
        Runtime r = Runtime.getRuntime();
        Process p = r.exec(pingCmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            pingResult.append(inputLine);
        }
        in.close();
        Log.i("JobAsyncTask", "***** Result from the job: " + pingResult);

        //HTTP POST:
        URL url = new URL("http://192.168.0.176:5000/postresults");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", " */* ");
        con.setDoOutput(true);
        /*            {
                          "result":"tekst na rezultatot"
                       }                                                                 */
        String jsonInputString = " {\"result\": \"" + pingResult + " \"} ";
        OutputStream os = con.getOutputStream();
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
        Log.i("JobAsyncTask", "****** Result: " + jsonInputString);
        StringBuilder response = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        Log.i("JobAsyncTask", "****** Response after POST: " + con.getResponseCode() + " " + con.getResponseMessage());

        //Check if not sent:
        boolean full=false;
        if (con.getResponseCode() != 200) {
            //we don't have connection:
            Log.i("JobAsyncTask", "************* Not connected! *********************");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            //Write to shared preferences:
            for(int j=1; j<4; j++){
                String record = sharedPreferences.getString("jobs_ping_"+j, "none");
                if(record.equals("none")){
                    full=true;
                    editor.putString("jobs_ping_"+j, pingResult.toString());
                    editor.apply();
                    Log.i("JobAsyncTask", "********* SharedPrefs value recorded: "+ sharedPreferences.getString("jobs_ping_"+j, "none"));
                    break;
                }
            }
            if(!full){ //if all 3 keys aren't none then we'll write in the 1st key
                editor.putString("jobs_ping_1", pingResult.toString());
                editor.apply();
            }
        }
        else{
            //we have a connection:
            //Check if there were unsent responses (max 3):
            String to_send = " {\"result\": \"";
            boolean are_there_any = false;
            for(int i=1; i<4; i++){
                //Read from shared preferences:
                String post_r = sharedPreferences.getString("jobs_ping_"+i, "none");
                if(!post_r.equals("none")){
                    are_there_any=true;
                    Log.i("JobAsyncTask", "***** Result from the SharedPrefs: " + post_r);
                    to_send = to_send + post_r + "; ";
                    //delete the key-value pair:
                    SharedPreferences.Editor ed = sharedPreferences.edit();
                    ed.remove("jobs_ping_"+i);
                    ed.apply();
                }
            }
            to_send = to_send.substring(0, to_send.length() - 2); //remove the last 1characters -> "; " from the string
            to_send = to_send + " \"} ";
            Log.i("JobAsyncTask", "****** Result for the unsent: " + to_send);

            //make http POST if there are any unsent results:
            if(are_there_any) {
                byte[] input_unsent = to_send.getBytes("utf-8");
                os.write(input_unsent, 0, input_unsent.length);
                StringBuilder response_unsent = new StringBuilder();
                BufferedReader br_unsent = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                String responseLine_unsent = null;
                while ((responseLine_unsent = br_unsent.readLine()) != null) {
                    response_unsent.append(responseLine_unsent.trim());
                }
                Log.i("JobAsyncTask", "****** Response after POST for unsent: " + con.getResponseCode() + " " + con.getResponseMessage());
            }
        }
        return con.getResponseMessage();
    }//pingJob

}
