package com.christophergs.mbientbasic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.PercentFormatter;
import com.mbientlab.metawear.MetaWearBleService;


import org.apache.http.HttpStatus;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class StatsActivity extends AppCompatActivity {

    //logging variables
    private static final String TAG = "MetaWear";

    //metawear variables
    //public static final String EXTRA_BT_DEVICE= "EXTRA_BT_DEVICE";
    //private BluetoothDevice btDevice= null;
    public static String EXPERIMENT_ID = null;

    //MPChart variables
    private RelativeLayout mainLayout;
    private PieChart mChart;
    private SeekBar mSeekBarX, mSeekBarY;
    private float[] yData = { 5, 10, 15, 30 };
    private String[] xData = { "Your Mount", "Your Side Control", "Your Back Control", "Your Closed Guard",
                                "Opponent Mount", "Opponent Side Control", "Opponent Back Control", "Opponent Closed Guard",
                                "Other"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PieChart mChart = new PieChart(getApplicationContext());
        EXPERIMENT_ID = getIntent().getExtras().getString("EXPERIMENT");
        Log.i(TAG, String.format("Experiment ID: %s", EXPERIMENT_ID));
        setContentView(mChart);
        mChart.setDescription("Grappling Position Analysis");

        //addData(); //causes null pointer error

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        for (int i = 0; i < yData.length; i++)
            yVals1.add(new Entry(yData[i], i));

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < xData.length; i++)
            xVals.add(xData[i]);

        // add many colors
        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());


        // create pie data set
        PieDataSet dataSet = new PieDataSet(yVals1, "Time Percentage");
        dataSet.setSliceSpace(3);
        dataSet.setSelectionShift(5);
        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);

        mChart.setData(data);

        mChart.animateY(2000);

        // update pie chart
        mChart.invalidate();

        getAnalysis();
    }


    public void getAnalysis() {
        toastIt("Getting analysis from server");
        try {
            URL url = new URL("http://christophergs.pythonanywhere.com/api/analyze/"+EXPERIMENT_ID);
            new DownloadFilesTask().execute(url);
        } catch (Exception e) {
            Log.e(TAG, "file send error", e);
            toastIt("File sending error!");
        }

    }

    public float[] updateYData(JSONObject serverResponse) {
        try {
            Log.i(TAG, String.format("updating Y data with: %s", serverResponse));
            String ymount = (String) serverResponse.getString("ymount");
            String ybc = (String) serverResponse.getString("ybc");
            String ycg = (String) serverResponse.getString("ycg");
            String ysc = (String) serverResponse.getString("ysc");
            String omount = (String) serverResponse.getString("omount");
            String osc = (String) serverResponse.getString("osc");
            String obc = (String) serverResponse.getString("obc");
            String ocg = (String) serverResponse.getString("ocg");
            String OTHER = (String) serverResponse.getString("OTHER");
            Log.i(TAG, String.format("ymount: %s", ymount));

            float ymount_F = Float.valueOf(ymount.trim()).floatValue();
            float ybc_F = Float.valueOf(ybc.trim()).floatValue();
            float ycg_F = Float.valueOf(ycg.trim()).floatValue();
            float ysc_F = Float.valueOf(ysc.trim()).floatValue();
            float omount_F = Float.valueOf(omount.trim()).floatValue();
            float osc_F = Float.valueOf(osc.trim()).floatValue();
            float obc_F = Float.valueOf(obc.trim()).floatValue();
            float ocg_F = Float.valueOf(ocg.trim()).floatValue();
            float OTHER_F = Float.valueOf(OTHER.trim()).floatValue();

            System.out.println("float ymount = " + ymount_F);
            Log.i(TAG, String.valueOf(ymount_F));

            yData = new float[] { ymount_F, ysc_F, ybc_F, ycg_F, omount_F, osc_F, obc_F, ocg_F, OTHER_F };

        } catch (Exception e) {
            Log.e(TAG, "error getting chart data", e);
        }

        for (int i = 0; i < yData.length; i++){
            Log.i(TAG, String.format("backup value2: %s", yData[i]));
        }
        return yData;
    }

    public class DownloadFilesTask extends AsyncTask<URL, JSONObject, String> {
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(StatsActivity.this);
            pDialog.setMessage("Analyzing Data");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(URL... my_url) {

            HttpURLConnection urlConnection = null;
            String boundary = "*****";
            DataInputStream inputStream = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            String responseText = null;
            String serverResponseMessage = null;
            try {
                URL url = my_url[0];
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-Language", "en-US");
                urlConnection.setUseCaches(false);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(false);

                int status = urlConnection.getResponseCode();
                InputStream response = urlConnection.getInputStream();

                // Responses from the server (code and message)
                responseText = urlConnection.getResponseMessage();

                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                StringBuilder sb = new StringBuilder();
                ArrayList<String> testArray = new ArrayList<String>();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                    testArray.add(line);
                }
                serverResponseMessage = sb.toString();
                Log.i(TAG, String.format("full server response: %s", serverResponseMessage));
                Log.i(TAG, String.format("testArray: %s", testArray));
                Log.i(TAG, String.format("responseText: %s", responseText));

                JSONObject recvdjson = new JSONObject(serverResponseMessage);
                Log.i(TAG, String.format("JSON: %s", recvdjson));
                //float[] backup = updateYData(recvdjson);
                //Log.i(TAG, String.format("backup value: %s", backup));
                publishProgress(recvdjson);
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Analysis error", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

            }

            return responseText;
        }

        protected void onProgressUpdate(JSONObject serverResponse) {
            try {
                Log.i(TAG, String.format("updating Y data with: %s", serverResponse));
                String ymount = (String) serverResponse.getString("ymount");
                String ybc = (String) serverResponse.getString("ybc");
                String ycg = (String) serverResponse.getString("ycg");
                String ysc = (String) serverResponse.getString("ysc");
                String omount = (String) serverResponse.getString("omount");
                String osc = (String) serverResponse.getString("osc");
                String obc = (String) serverResponse.getString("obc");
                String ocg = (String) serverResponse.getString("ocg");
                String OTHER = (String) serverResponse.getString("OTHER");
                Log.i(TAG, String.format("ymount: %s", ymount));

                float ymount_F = Float.valueOf(ymount.trim()).floatValue();
                float ybc_F = Float.valueOf(ybc.trim()).floatValue();
                float ycg_F = Float.valueOf(ycg.trim()).floatValue();
                float ysc_F = Float.valueOf(ysc.trim()).floatValue();
                float omount_F = Float.valueOf(omount.trim()).floatValue();
                float osc_F = Float.valueOf(osc.trim()).floatValue();
                float obc_F = Float.valueOf(obc.trim()).floatValue();
                float ocg_F = Float.valueOf(ocg.trim()).floatValue();
                float OTHER_F = Float.valueOf(OTHER.trim()).floatValue();

                System.out.println("float ymount = " + ymount_F);
                Log.i(TAG, String.valueOf(ymount_F));

                yData = new float[]{ymount_F, ysc_F, ybc_F, ycg_F, omount_F, osc_F, obc_F, ocg_F, OTHER_F};

                ArrayList<Entry> yVals1 = new ArrayList<Entry>();

                for (int i = 0; i < yData.length; i++)
                    yVals1.add(new Entry(yData[i], i));

                ArrayList<String> xVals = new ArrayList<String>();

                for (int i = 0; i < xData.length; i++)
                    xVals.add(xData[i]);

                // add many colors
                ArrayList<Integer> colors = new ArrayList<Integer>();

                for (int c : ColorTemplate.VORDIPLOM_COLORS)
                    colors.add(c);

                for (int c : ColorTemplate.JOYFUL_COLORS)
                    colors.add(c);

                for (int c : ColorTemplate.COLORFUL_COLORS)
                    colors.add(c);

                for (int c : ColorTemplate.LIBERTY_COLORS)
                    colors.add(c);

                for (int c : ColorTemplate.PASTEL_COLORS)
                    colors.add(c);

                colors.add(ColorTemplate.getHoloBlue());


                // create pie data set
                PieChart mChart = new PieChart(getApplicationContext());
                PieDataSet dataSet = new PieDataSet(yVals1, "Time Percentage");
                dataSet.setSliceSpace(3);
                dataSet.setSelectionShift(5);
                dataSet.setColors(colors);

                PieData data = new PieData(xVals, dataSet);
                mChart.setData(data);
                mChart.animateY(2000);

                // update pie chart
                mChart.invalidate();

            } catch (Exception e) {
                Log.e(TAG, "error getting chart data", e);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            pDialog.dismiss();
            Log.i(TAG, String.valueOf(result));
            toastIt(result);
        }

    }

    private void toastIt(String msg) {
        Toast.makeText(StatsActivity.this, msg, Toast.LENGTH_SHORT).show();
    }


}
