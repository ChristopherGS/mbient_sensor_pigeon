/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.christophergs.mbientbasic;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.christophergs.mbientbasic.ModuleFragmentBase.FragmentBus;
import com.mbientlab.metawear.module.Debug;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        BothFragment.OnFragTestListener, MotionFragment.OnFragmentInteractionListener, HomeFragment.OnFragTestListener,
        ServiceConnection, FragmentBus {
    public static final String EXTRA_BT_DEVICE= "EXTRA_BT_DEVICE";
    private final static String FRAGMENT_KEY= "FRAGMENT_KEY";
    private static final Map<Integer, Class<? extends ModuleFragmentBase>> FRAGMENT_CLASSES;
    private static final String TAG = "MetaWear";
    public String EXPERIMENT_ID = "105"; //test 1
    public int ALTGYRO_ID = 2131624156; //test 1; //test 1

    public static final int REQUEST_START_APP= 1;

    static {
        Map<Integer, Class<? extends ModuleFragmentBase>> tempMap= new LinkedHashMap<>();
        tempMap.put(R.id.nav_home, HomeFragment.class);
        tempMap.put(R.id.nav_accelerometer, AccelerometerFragment.class);
        tempMap.put(R.id.nav_gyro, GyroFragment.class);
        tempMap.put(R.id.nav_gyro_new, GyroFragmentNew.class);
        tempMap.put(R.id.nav_both, BothFragment.class);
        tempMap.put(R.id.nav_settings, SettingsFragment.class);
        tempMap.put(R.id.nav_settings, MotionFragment.class);
        FRAGMENT_CLASSES= Collections.unmodifiableMap(tempMap);
    }



    public static class ReconnectDialogFragment extends DialogFragment implements  ServiceConnection {
        private static final String KEY_BLUETOOTH_DEVICE= "KEY_BLUETOOTH_DEVICE";

        private ProgressDialog reconnectDialog = null;
        private BluetoothDevice btDevice= null;
        private MetaWearBoard currentMwBoard= null;

        public static ReconnectDialogFragment newInstance(BluetoothDevice btDevice) {
            Bundle args= new Bundle();
            args.putParcelable(KEY_BLUETOOTH_DEVICE, btDevice);

            ReconnectDialogFragment newFragment= new ReconnectDialogFragment();
            newFragment.setArguments(args);

            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            btDevice= getArguments().getParcelable(KEY_BLUETOOTH_DEVICE);
            getActivity().getApplicationContext().bindService(new Intent(getActivity(), MetaWearBleService.class), this, BIND_AUTO_CREATE);

            reconnectDialog = new ProgressDialog(getActivity());
            reconnectDialog.setTitle(getString(R.string.title_reconnect_attempt));
            reconnectDialog.setMessage(getString(R.string.message_wait));
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.setIndeterminate(true);
            reconnectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    currentMwBoard.disconnect();
                    getActivity().finish();
                }
            });

            return reconnectDialog;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            currentMwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { }
    }

    public String setEIDValue(String my_value){
        EXPERIMENT_ID = my_value;
        return EXPERIMENT_ID;
    }

    public String getEIDValue(){
        return EXPERIMENT_ID;
    }

    public class DownloadFilesTask extends AsyncTask<URL, Void, String> {
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(NavigationActivity.this);
            pDialog.setMessage("Uploading file");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(URL... my_url) {

            HttpURLConnection urlConnection = null;
            String boundary = "*****";
            DataOutputStream outputStream = null;
            DataInputStream inputStream = null;
            String pathToOurFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/METAWEAR.csv";
            Log.i(TAG, String.format("save file path: %s", pathToOurFile));
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;

            String responseText = null;
            String serverResponseMessage = null;
            try {
                URL url = my_url[0];
                urlConnection = (HttpURLConnection) url.openConnection();
                FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile));
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                urlConnection.setRequestProperty("Content-Language", "en-US");
                urlConnection.setUseCaches(false);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                outputStream = new DataOutputStream(urlConnection.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data;name=\"android_file\";filename=\"" + pathToOurFile + "\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                //Send request

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)

                int responseCode = urlConnection.getResponseCode();
                responseText = urlConnection.getResponseMessage();

                InputStream response = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                StringBuilder sb = new StringBuilder();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                serverResponseMessage = sb.toString();
                Log.i(TAG, String.format("full server response: %s", serverResponseMessage));
                JSONObject jObject = new JSONObject(serverResponseMessage);
                String EXPERIMENT_ID = jObject.getString("id");
                Log.i(TAG, String.format("experiment id: %s", EXPERIMENT_ID));
                setEIDValue(EXPERIMENT_ID);

                response.close();
                fileInputStream.close();
                outputStream.flush();
                outputStream.close();

            } catch (Exception e) {
                Log.e(TAG, "file send error", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

            }

            return responseText;
        }

        @Override
        protected void onPostExecute(String result) {
            pDialog.dismiss();
            Log.i(TAG, String.valueOf(result));
            toastIt(result);
        }

    }

    private final String RECONNECT_DIALOG_TAG= "reconnect_dialog_tag";
    private final Handler connectScheduler= new Handler();
    private BluetoothDevice btDevice;
    private MetaWearBoard mwBoard;
    private Fragment currentFragment= null;
    private Fragment currentFragment2= null;

    private final ConnectionStateHandler connectionHandler= new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG)).dismiss();
            ((ModuleFragmentBase) currentFragment).reconnected();
            ((ModuleFragmentBase) currentFragment2).reconnected();
        }

        @Override
        public void disconnected() {
            attemptReconnect();
        }

        @Override
        public void failure(int status, Throwable error) {
            Fragment reconnectFragment= getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG);
            if (reconnectFragment != null) {
                mwBoard.connect();
            } else {
                attemptReconnect();
            }
        }
    };

    private void attemptReconnect() {
        attemptReconnect(0);
    }

    private void attemptReconnect(long delay) {
        ReconnectDialogFragment dialogFragment= ReconnectDialogFragment.newInstance(btDevice);
        dialogFragment.show(getSupportFragmentManager(), RECONNECT_DIALOG_TAG);

        if (delay != 0) {
            connectScheduler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mwBoard.connect();
                }
            }, delay);
        } else {
            mwBoard.connect();
        }
    }

    @Override
    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    @Override
    public void resetConnectionStateHandler(long delay) {
        mwBoard.setConnectionStateHandler(connectionHandler);
        attemptReconnect(delay);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        mwBoard.setConnectionStateHandler(null);
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*CS comment:
            fab is the info button in bottom right
         */
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ModuleFragmentBase) currentFragment).showHelpDialog();
                ((ModuleFragmentBase) currentFragment2).showHelpDialog();
            }
        });

        /*CS comment:
            This creates the toggle "hamburger menu". Without it you have to
            drag the menu open and closed - not that obvious
        */
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        /* CS comment:
            This listener basically gives the nav drawer life. Without it, the links don't
            do anything
         */
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        if (savedInstanceState == null) {
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_home));
        } else {
            currentFragment= getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        }

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);

        //DODGY HACK - SHOULD USE ASYNC TASK INSTEAD
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (currentFragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_KEY, currentFragment);
        }
    }

    /*CS comment
        If drawer open, when user presses back will close drawer.
        Otherwise will disconnect from board
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            mwBoard.setConnectionStateHandler(null);
            mwBoard.disconnect();
            super.onBackPressed();
        }
    }


    /*CS comment:
        This adds he "Disconnect" part of the top menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_reset:
                try {
                    mwBoard.getModule(Debug.class).resetDevice();
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_soft_reset, Snackbar.LENGTH_LONG).show();
                } catch (UnsupportedModuleException e) {
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.error_soft_reset, Snackbar.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_disconnect:
                mwBoard.setConnectionStateHandler(null);
                mwBoard.disconnect();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Log.i(TAG, String.format("Fragment ID: %d", id));


        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction= fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }

        if (currentFragment2 != null) {
            transaction.detach(currentFragment2);
        }

        //we keep tag 1 as the per the mbient code and make tag 2 the adjusted gyro,
        //knowing that the BothFragment is a copy of accelerometer with stream adjustments
        String fragmentTag= FRAGMENT_CLASSES.get(id).getCanonicalName();
        String fragmentTag2= FRAGMENT_CLASSES.get(ALTGYRO_ID).getCanonicalName(); //Gyro New
        //2131165353


        Log.i(TAG, String.format("Fragment Tag: %s", fragmentTag));

        if (FRAGMENT_CLASSES.get(id).getCanonicalName().equals("com.christophergs.mbientbasic.BothFragment")) {
            Log.i(TAG, String.format("Both fragment selected2: %s", fragmentTag));
            currentFragment= fragmentManager.findFragmentByTag(fragmentTag);
            currentFragment2= fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.GyroFragmentNew");
        } else {
            currentFragment= fragmentManager.findFragmentByTag(fragmentTag);
            Log.i(TAG, String.format("Did not find tag: %s", fragmentTag));
        }

        if (currentFragment == null) {
            try {
                currentFragment= FRAGMENT_CLASSES.get(id).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate fragment", e);
            }

            transaction.add(R.id.container, currentFragment, fragmentTag);
        }

        if (FRAGMENT_CLASSES.get(id).getCanonicalName().equals("com.christophergs.mbientbasic.BothFragment")) {

            if (currentFragment2 == null) {
                try {
                    currentFragment2= FRAGMENT_CLASSES.get(ALTGYRO_ID).getConstructor().newInstance();//new gyro
                } catch (Exception e) {
                    throw new RuntimeException("Cannot instantiate fragment", e);
                }

                transaction.add(R.id.container2, currentFragment2, fragmentTag2);
                FrameLayout f;
                f = (FrameLayout) findViewById(R.id.container2);
                f.setVisibility(View.VISIBLE);

            }
        }

        if (FRAGMENT_CLASSES.get(id).getCanonicalName().equals("com.christophergs.mbientbasic.BothFragment")) {
            transaction.attach(currentFragment);
            transaction.attach(currentFragment2);
            transaction.commit();
        } else {
                transaction.attach(currentFragment);
                transaction.commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(item.getTitle());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);


        return true;
    }

    public void onStartButtonPressed(int position) {
        //Do something with the position value passed back
        Log.i(TAG, String.format("FRAG TEST: %d", position));
        Log.i(TAG, String.format("CurrentFragment: %s", currentFragment));
        FragmentManager fragmentManager = getSupportFragmentManager();
        BothFragment f1 = (BothFragment) fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.BothFragment");
        GyroFragmentNew f2 = (GyroFragmentNew) fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.GyroFragmentNew");
        if (position == 1){
            f2.moveViewToLast();
            f1.moveViewToLast();
            f2.setup();
            f1.setup();
            f2.chartHandler.postDelayed(f2.updateChartTask, f2.UPDATE_PERIOD);
            f1.chartHandler.postDelayed(f1.updateChartTask, f1.UPDATE_PERIOD);
        } else {
            f1.chart.setVisibleXRangeMaximum(f1.sampleCount);
            f2.chart.setVisibleXRangeMaximum(f2.sampleCount);
            f1.clean();
            f2.clean();
            if (f1.streamRouteManager != null) {
                f1.streamRouteManager.remove();
                f1.streamRouteManager = null;
            }
            if (f2.streamRouteManager != null) {
                f2.streamRouteManager.remove();
                f2.streamRouteManager = null;
            }
            f1.chartHandler.removeCallbacks(f1.updateChartTask);
            f2.chartHandler.removeCallbacks(f2.updateChartTask);
        }

    }

    public void onSaveButtonPressed(int position) {
        Log.i(TAG, String.format("SAVE TEST: %d", position));
        FragmentManager fragmentManager = getSupportFragmentManager();
        BothFragment f1 = (BothFragment) fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.BothFragment");
        GyroFragmentNew f2 = (GyroFragmentNew) fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.GyroFragmentNew");

        String delete_filename = String.format("METAWEAR.csv");
        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), delete_filename);

        //delete the csv file if it already exists (will be from older recordings)
        f1.prep(path);
        String filename = f1.saveData(false); //f1 is the accelerometer, we keep the header
        String filename2 = f2.saveData(true); //f2 is the gyro, we remove the header

        sendFile();



        /*
        if (filename != null) {
            File dataFile = getFileStreamPath(filename);
            Uri contentUri = FileProvider.getUriForFile(this, "com.mbientlab.metawear.app.fileprovider", dataFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, filename);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(intent, "Saving Data"));
        }*/
    }

    public void onClearButtonPressed(int position) {
        Log.i(TAG, String.format("CLEAR TEST: %d", position));
        FragmentManager fragmentManager = getSupportFragmentManager();
        BothFragment f1 = (BothFragment) fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.BothFragment");
        GyroFragmentNew f2 = (GyroFragmentNew) fragmentManager.findFragmentByTag("com.christophergs.mbientbasic.GyroFragmentNew");
        f1.refreshChart(true);
        f2.refreshChart(true);
        Button saveButton= (Button) findViewById(R.id.layout_two_button_right);
        saveButton.setVisibility(View.VISIBLE);

    }


    public void onDashButtonPressed(int position) {
        Log.i(TAG, String.format("Dashboard TEST: %d", position));
        Intent dashActivityIntent = new Intent(NavigationActivity.this, DashboardActivity.class);
        //dashActivityIntent.putExtra(NavigationActivity.EXTRA_BT_DEVICE, btDevice);
        //Log.i(TAG, String.format("pie button eid: %s", EXPERIMENT_ID));
        //dashActivityIntent.putExtra("EXPERIMENT", EXPERIMENT_ID);
        startActivityForResult(dashActivityIntent, 1);
    }

    public void sendFile() {
        toastIt("Begin sending file to server");
        try {
            URL url = new URL("http://christophergs.pythonanywhere.com/api/csv");
            new DownloadFilesTask().execute(url);
            //now that upload is complete we remove the upload button from the UI and replace with stats
            Button saveButton= (Button) findViewById(R.id.layout_two_button_right);
            saveButton.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "file send error", e);
            toastIt("File sending error!");
        }

    }

    public void onPieButtonPressed(int position) {
        Log.i(TAG, String.format("CLEAR TEST: %d", position));
        /*MotionFragment motionFragment = new MotionFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (currentFragment != null) {
            fragmentTransaction.detach(currentFragment);
        }
        fragmentTransaction.add(android.R.id.content, motionFragment);
        fragmentTransaction.commit();*/
        EXPERIMENT_ID = getEIDValue();

        Intent navActivityIntent = new Intent(NavigationActivity.this, StatsActivity.class);
        navActivityIntent.putExtra(NavigationActivity.EXTRA_BT_DEVICE, btDevice);
        Log.i(TAG, String.format("pie button eid: %s", EXPERIMENT_ID));
        navActivityIntent.putExtra("EXPERIMENT", EXPERIMENT_ID);
        startActivityForResult(navActivityIntent, REQUEST_START_APP);
    }

    @Override
    public void onPieSetup() {

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(connectionHandler);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private void toastIt(String msg) {
        Toast.makeText(NavigationActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
