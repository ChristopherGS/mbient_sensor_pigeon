package com.christophergs.mbientbasic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.PercentFormatter;


import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StatsActivity extends AppCompatActivity implements OnSeekBarChangeListener {

    //logging variables
    private static final String TAG = "MetaWear";
    public static final int REQUEST_START_APP= 1;
    public static final float THRESHOLD= 0.04f;

    //metawear variables
    public static String EXPERIMENT_ID = null;

    //Timeline variables
    public static String[] TIMELINE_POSITIONS = {"OTHER1", "OTHER2"};
    public static String[] TIMELINE_POSITION_VALUES;

    private List<String> test;




    //MPChart variables
    private RelativeLayout mainLayout;
    private PieChart mChart;
    private SeekBar mSeekBarX, mSeekBarY;
    private TextView tvX, tvY;
    private Typeface tf;
    float ymount_F = 0;
    float ybc_F = 0;
    float ycg_F = 0;
    float ysc_F = 0;
    float omountsc_F = 0;
    float obc_F = 0;
    float ocg_F = 0;
    float OTHER_F = 1;

    private float[] yData = { ymount_F, ysc_F, ybc_F, ycg_F, omountsc_F, obc_F, ocg_F, OTHER_F };
    private String[] mParties = new String[]{ "Party A", "Party B", "Party C", "Party D", "Party E", "Party F", "Party G", "Party H" };
    private String[] xData = { "Your Mount", "Your Side Control", "Your Back Control", "Your Guard",
                                "Opponent Mount or Side Control", "Opponent Back Control", "Opponent Guard",
                                "Other"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //mChart = new PieChart(getApplicationContext());
        setContentView(R.layout.pie_layout);
        tvX = (TextView) findViewById(R.id.tvXMax);
        tvY = (TextView) findViewById(R.id.tvYMax);
        mSeekBarX = (SeekBar) findViewById(R.id.seekBar1);
        mSeekBarY = (SeekBar) findViewById(R.id.seekBar2);
        mSeekBarY.setProgress(10);
        mSeekBarX.setOnSeekBarChangeListener(this);
        mSeekBarY.setOnSeekBarChangeListener(this);
        
        EXPERIMENT_ID = getIntent().getExtras().getString("EXPERIMENT");
        Log.i(TAG, String.format("Experiment ID: %s", EXPERIMENT_ID));

        mChart = (PieChart) findViewById(R.id.chart1);

        mChart.setUsePercentValues(true);
        mChart.setExtraOffsets(5, 10, 5, 5);
        mChart.setDragDecelerationFrictionCoef(0.95f);
        mChart.setDescription("Grappling Position Analysis");

        mChart.setDrawHoleEnabled(true);
        mChart.setHoleColor(Color.WHITE);
        mChart.setTransparentCircleColor(Color.WHITE);
        mChart.setTransparentCircleAlpha(110);
        mChart.setHoleRadius(58f);
        mChart.setTransparentCircleRadius(61f);
        mChart.setDrawCenterText(true);
        mChart.setRotationAngle(0);
        // enable rotation of the chart by touch
        mChart.setRotationEnabled(true);

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
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(10f);
        mChart.setData(data);
        mChart.animateY(2000);

        Legend l = mChart.getLegend();
        l.setPosition(LegendPosition.RIGHT_OF_CHART);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        // update pie chart
        mChart.invalidate();

        int EXPERIMENT_ID_INT = 0;

        try {
            EXPERIMENT_ID_INT = Integer.parseInt(EXPERIMENT_ID);
        } catch(NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
        }

        if (EXPERIMENT_ID_INT > 9990) {
            getDummyAnalysis(EXPERIMENT_ID);
        } else {
            getAnalysis(EXPERIMENT_ID);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSuggestions: {
                toastIt("Suggestions");
                Intent navActivityIntent = new Intent(StatsActivity.this, SuggestionActivity.class);
                //navActivityIntent.putExtra(NavigationActivity.EXTRA_BT_DEVICE, btDevice);
                //Log.i(TAG, String.format("pie button eid: %s", EXPERIMENT_ID));
                //navActivityIntent.putExtra("EXPERIMENT", EXPERIMENT_ID);
                startActivityForResult(navActivityIntent, REQUEST_START_APP);
                break;
            }
            case R.id.actionDashboard: {
                toastIt("Dashboard coming soon");
                break;
            }
            case R.id.actionTogglePercent:
                mChart.setUsePercentValues(!mChart.isUsePercentValuesEnabled());
                mChart.invalidate();
                break;
            case R.id.actionToggleHole: {
                if (mChart.isDrawHoleEnabled())
                    mChart.setDrawHoleEnabled(false);
                else
                    mChart.setDrawHoleEnabled(true);
                mChart.invalidate();
                break;
            } case R.id.actionTimeline: {
                toastIt("Timeline");
                Intent tlActivityIntent = new Intent(StatsActivity.this, TimelineActivity.class);
                Log.i(TAG, String.format("passing sequence: %s", test));
                tlActivityIntent.putStringArrayListExtra("SEQUENCE", (ArrayList<String>) test);
                startActivityForResult(tlActivityIntent, REQUEST_START_APP);
                break;
            }
        }
        return true;
    }

    public float[] setDummyValues(float _ymount, float _ybc, float _ycg, float _ysc,
                                  float _omountsc, float _obc, float _ocg, float _OTHER){
        float ymount_F = _ymount;
        float ybc_F = _ybc;
        float ycg_F = _ycg;
        float ysc_F = _ysc;
        float omountsc_F = _omountsc;
        float obc_F = _obc;
        float ocg_F = _ocg;
        float OTHER_F = _OTHER;

        return new float[]{ymount_F, ysc_F, ybc_F, ycg_F, omountsc_F, obc_F, ocg_F, OTHER_F};
    }

    public void setTimelineValues(String[] values){
        Log.i(TAG, String.format("Setting Timeline Values... %s", values));

        TIMELINE_POSITIONS = values;
        test = new ArrayList<String>();

        Collections.addAll(test, values);
        /*for (String strings : values) {
            test.add(strings);
        }*/

        //arrayList is nested so we select the first
        Log.i(TAG, String.format("Shift timeline values to array... %s", test));

    }

    public void getDummyAnalysis(String EXPERIMENT_ID) {
        toastIt("Getting analysis from server");
        Log.i(TAG, String.format("Experiment ID: %s", EXPERIMENT_ID));
        final ProgressDialog tDialog;
        tDialog = new ProgressDialog(StatsActivity.this);
        tDialog.setMessage("Analyzing Data");
        tDialog.setIndeterminate(false);
        tDialog.setCancelable(true);
        tDialog.show();
        long delayInMillis = 2000;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                tDialog.dismiss();
            }
        }, delayInMillis);

        // DUMMY DATA: NOTE THAT THE ORDER OF ARGUMENTS IS DIFFERENT TO THE X-AXIS SETUP. Adjusted with "setDummyValues"
        if ("9999".equals(EXPERIMENT_ID)){
            /*float ymount_F = 0.88f; float ybc_F = 0; float ycg_F = 0; float ysc_F = 0;float omountsc_F = 0;float obc_F = 0;float ocg_F = 0;float OTHER_F = 0.12f;*/
            yData = setDummyValues(0.88f, 0, 0, 0, 0, 0, 0, 0.12f);
        }
        else if ("9998".equals(EXPERIMENT_ID)){
            /*float ymount_F = 0; float ybc_F = 0.87f; float ycg_F = 0; float ysc_F = 0;float omountsc_F = 0;float obc_F = 0;float ocg_F = 0;float OTHER_F = 0.13f;*/
            yData = setDummyValues(0, 0.87f, 0, 0, 0, 0, 0, 0.13f);
        }
        else if ("9997".equals(EXPERIMENT_ID)){
            /*float ymount_F = 0f; float ybc_F = 0; float ycg_F = 0; float ysc_F = 0.91f;float omountsc_F = 0;float obc_F = 0;float ocg_F = 0;float OTHER_F = 0.09f;*/
            yData = setDummyValues(0, 0, 0, 0.91f, 0, 0, 0, 0.09f);
        }
        else if ("9996".equals(EXPERIMENT_ID)){
            /*float ymount_F = 0; float ybc_F = 0; float ycg_F = 0; float ysc_F = 0;float omountsc_F = 0;float obc_F = 0.90f;float ocg_F = 0;float OTHER_F = 0.12f;*/
            yData = setDummyValues(0, 0, 0, 0, 0, 0.90f, 0, 0.10f);
        }
        else if ("9995".equals(EXPERIMENT_ID)){
            /*float ymount_F = 0.32f; float ybc_F = 0.28f; float ycg_F = 0; float ysc_F = 0.29f;float omountsc_F = 0;float obc_F = 0;float ocg_F = 0;float OTHER_F = 0.11f;*/
            yData = setDummyValues(0.32f, 0.28f, 0, 0.29f, 0, 0, 0, 0.11f);
        }
        else if ("9994".equals(EXPERIMENT_ID)){
            /*float ymount_F = 0; float ybc_F = 0; float ycg_F = 0; float ysc_F = 0;float omountsc_F = 0.40f;float obc_F = 0.5f;float ocg_F = 0;float OTHER_F = 0.10f;*/
            yData = setDummyValues(0, 0, 0, 0, 0.40f, 0.50f, 0, 0.10f);
        }
            ArrayList<Entry> yVals1 = new ArrayList<Entry>();
            ArrayList<Integer> keepVals = new ArrayList<>();

            for (int i = 0; i < yData.length; i++)
                if (yData[i] > THRESHOLD) {
                    yVals1.add(new Entry(yData[i], i));
                    keepVals.add(i);
                }

            ArrayList<String> xVals = new ArrayList<String>();

            Log.i(TAG, String.valueOf(xData));
            Log.i(TAG, String.valueOf(keepVals));

            for (int i = 0; i < xData.length; i++)
                if ((keepVals.contains(i))) {
                    xVals.add(xData[i]);
                }

            Log.i(TAG, String.valueOf(xVals.size()));

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
            data.setValueFormatter(new PercentFormatter());
            data.setValueTextSize(10f);
            mChart.setData(data);
            mChart.animateY(2000);
            // update pie chart
            mChart.invalidate();
        }

    public void getAnalysis(String EXPERIMENT_ID) {
        Log.i(TAG, String.format("DOWNLOADING ANALYSIS: %s", EXPERIMENT_ID));
        try {
            URL url = new URL("http://christophergs.pythonanywhere.com/api/analyze/" + EXPERIMENT_ID);
            new DownloadFilesTask().execute(url);
        } catch (Exception e) {
            Log.e(TAG, "file send error", e);
            toastIt("File sending error!");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        tvX.setText("" + (mSeekBarX.getProgress() + 1));
        tvY.setText("" + (mSeekBarY.getProgress()));

        setData(mSeekBarX.getProgress(), mSeekBarY.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public class DownloadFilesTask extends AsyncTask<URL, JSONObject, JSONObject> {
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
        protected JSONObject doInBackground(URL... my_url) {

            HttpURLConnection urlConnection = null;
            String boundary = "*****";
            DataInputStream inputStream = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            JSONObject recvdjson = null;

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

                recvdjson = new JSONObject(serverResponseMessage);
                Log.i(TAG, String.format("JSON: %s", recvdjson));

                //publishProgress(recvdjson);
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Analysis error", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

            }

            return recvdjson;
        }

        protected void onProgressUpdate(JSONObject serverResponse) {
            try {
                Log.i(TAG, String.format("updating Y data with: %s", serverResponse));

            } catch (Exception e) {
                Log.e(TAG, "error getting chart data", e);
            }
        }

        protected void onPostExecute(JSONObject serverResponse) {
            pDialog.dismiss();
            Log.i(TAG, String.format("onPostExecute: %s", serverResponse));
            try {
                Log.i(TAG, String.format("updating Y data with: %s", serverResponse));
                String ymount = (String) serverResponse.getString("ymount");
                String ybc = (String) serverResponse.getString("ybc");
                String ycg = (String) serverResponse.getString("ycg");
                String ysc = (String) serverResponse.getString("ysc");
                String omountsc = (String) serverResponse.getString("omountsc");
                String obc = (String) serverResponse.getString("obc");
                String ocg = (String) serverResponse.getString("ocg");
                String OTHER = (String) serverResponse.getString("OTHER");
                Log.i(TAG, String.format("ymount: %s", ymount));


                float ymount_F = Float.valueOf(ymount.trim()).floatValue();
                float ybc_F = Float.valueOf(ybc.trim()).floatValue();
                float ycg_F = Float.valueOf(ycg.trim()).floatValue();
                float ysc_F = Float.valueOf(ysc.trim()).floatValue();
                float omountsc_F = Float.valueOf(omountsc.trim()).floatValue();
                float obc_F = Float.valueOf(obc.trim()).floatValue();
                float ocg_F = Float.valueOf(ocg.trim()).floatValue();
                float OTHER_F = Float.valueOf(OTHER.trim()).floatValue();

                System.out.println("float ymount = " + ymount_F);
                Log.i(TAG, String.valueOf(ymount_F));

                yData = new float[]{ymount_F, ysc_F, ybc_F, ycg_F, omountsc_F, obc_F, ocg_F, OTHER_F};


                TIMELINE_POSITION_VALUES = new String[]{serverResponse.getString("predictions")};
                Log.i(TAG, String.format("Timeline positions: %s", TIMELINE_POSITION_VALUES));
                setTimelineValues(TIMELINE_POSITION_VALUES);

                ArrayList<Entry> yVals1 = new ArrayList<Entry>();
                ArrayList<Integer> keepVals = new ArrayList<>();

                for (int i = 0; i < yData.length; i++)
                    if (yData[i] > THRESHOLD) {
                        yVals1.add(new Entry(yData[i], i));
                        keepVals.add(i);
                    }

                ArrayList<String> xVals = new ArrayList<String>();

                Log.i(TAG, String.valueOf(xData));

                for (int i = 0; i < xData.length; i++)
                    if ((keepVals.contains(i))) {
                        xVals.add(xData[i]);
                    }

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
                data.setValueFormatter(new PercentFormatter());
                data.setValueTextSize(10f);
                mChart.setData(data);
                mChart.animateY(2000);
                // update pie chart
                mChart.invalidate();
            } catch (Exception e) {
                Log.e(TAG, "error getting chart data", e);
            }
        }

    }

    private void setData(int count, float range) {

        float mult = range;

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        for (int i = 0; i < count + 1; i++) {
            yVals1.add(new Entry((float) (Math.random() * mult) + mult / 5, i));
        }

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < count + 1; i++)
            xVals.add(mParties[i % mParties.length]);

        PieDataSet dataSet = new PieDataSet(yVals1, "Election Results");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // add a lot of colors

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

        dataSet.setColors(colors);
        //dataSet.setSelectionShift(0f);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        data.setValueTypeface(tf);
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        mChart.invalidate();
    }

    private void toastIt(String msg) {
        Toast.makeText(StatsActivity.this, msg, Toast.LENGTH_SHORT).show();
    }


}
