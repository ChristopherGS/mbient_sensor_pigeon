package com.christophergs.mbientbasic;

import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.StrictMode;

import android.app.Activity;
import android.content.*;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.text.format.Time;

import java.io.FileWriter;


import static com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;

import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;

import static com.mbientlab.metawear.AsyncOperation.CompletionHandler;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.RouteManager;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.data.CartesianShort;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.*;
import com.mbientlab.metawear.DataSignal;


import java.io.FileOutputStream;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    TextView accelerationData;

    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();

    private MetaWearBleService.LocalBinder binder;
    private MetaWearBoard mwBoard;
    private final String MW_MAC_ADDRESS = "D5:9C:DC:37:BA:AE";
    private static final String TAG = "MBIENT_TAG";
    private Led ledModule;
    private static final float ACC_RANGE = 8.f, ACC_FREQ = 50.f;
    private static final String STREAM_KEY = "accel_stream";
    private Accelerometer accelModule = null;
    private static final String LOG_KEY = "FreeFallDetector";

    private void toastIt(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

        // TEMPORARY HACK - should use async
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }
        // Matching accelerometer data to screen
        accelerationData = (TextView) findViewById(R.id.accelerationData);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "BEGIN MBIENT CALCS");
        MetaWearBleService.LocalBinder binder = (MetaWearBleService.LocalBinder) service;
        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        // not sure what this does
        binder.executeOnUiThread();

        mwBoard = binder.getMetaWearBoard(remoteDevice);
        mwBoard.setConnectionStateHandler(new ConnectionStateHandler() {
            @Override
            public void connected() {
                toastIt("proper connection established");
                Log.d(TAG, "CONNECTION ESTABLISHED");
                mwBoard.readDeviceInformation().onComplete(new CompletionHandler<MetaWearBoard.DeviceInformation>() {
                    @Override
                    public void success(MetaWearBoard.DeviceInformation result) {
                        toastIt("Got stats");
                        Log.d(TAG, "Device Information: " + result.toString());
                    }

                    @Override
                    public void failure(Throwable error) {
                        toastIt("Error");
                        Log.e(TAG, "Error reading device information", error);
                    }


                });

               // File file = new File(/storage/emulated);
                //if (file.exists()) {

               //     boolean deleteFile (String, "MBIENTMBIENT");
               //     toastIt("Old File Deleted");
               //     Log.d(TAG, "Old File Deleted");
                //}

                turnOnLed();
            }

            @Override
            public void disconnected() {
                toastIt("Disconnected from board");
                Log.i(TAG, "Disconnected");
            }

            @Override
            public void failure(int status, final Throwable error) {
                Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error connecting", error);
            }

        });

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    public void startMbient(View view) {
        toastIt("trying to connect to mbient");
        mwBoard.connect();
    }

    public void stopMbient(View view) {
        toastIt("disconnecting");
        mwBoard.disconnect();
    }


    public void streamAccelerometer(View view) {
        toastIt("Stream accelerometer data");
        final Switch mySwitch = (Switch) view;
        final File fileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "MBIENT");

        if (!fileDir.exists()) {
            try {
                fileDir.mkdir();
            } catch (Exception e) {
                Log.e(TAG, "file directory not foundr", e);
            }
        }


        try

        {
            final Accelerometer accelModule = mwBoard.getModule(Accelerometer.class);
            final Logging loggingModule = mwBoard.getModule(Logging.class);
            if (mySwitch.isChecked()) {
                accelModule.setOutputDataRate(50.f);
                accelModule.setAxisSamplingRange(ACC_RANGE);

                Log.i(TAG, "Log size: " + loggingModule.getLogCapacity());

                accelModule.routeData()
                        .fromAxes().stream(STREAM_KEY)
                        .commit().onComplete(new CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {


                            @Override
                            public void process(Message message) {
                                FileWriter fileWriter = null;
                                CartesianFloat axes = message.getData(CartesianFloat.class);
                                Log.i(TAG, axes.toString());

                                //Displays acceleration data string to screen;
                                accelerationData.setText("Acc (X,Y,Z): " + axes);

                                String entry = axes.toString();
                                String path = fileDir + "MBIENT.csv";
                                OutputStream out;

                                try {
                                    out = new BufferedOutputStream(new FileOutputStream(path, true));
                                    out.write(entry.getBytes());
                                    out.write(",".getBytes());
                                    out.write("\n".getBytes());
                                    out.close();
                                } catch (Exception e) {
                                    Log.e(TAG, "CSV creation error", e);


                                }
                            }

                        });
                            /*
                            result.setLogMessageHandler(LOG_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    Log.i(TAG, String.format("%tY%<tm%<td-%<tH%<tM%<tS%<tL: Entered Free Fall", message.getTimestamp()));
                                }
                            });*/
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e(TAG, "Error committing route", error);
                    }
                });
                accelModule.enableAxisSampling();
                loggingModule.startLogging(true);
                accelModule.start();

            } else {
                loggingModule.stopLogging();
                accelModule.disableAxisSampling();
                accelModule.stop();

                loggingModule.downloadLog(0.f, new Logging.DownloadHandler() {
                    @Override
                    public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                        if (nEntriesLeft == 0) {
                            Log.i(TAG, "Log download complete");
                        }
                    }
                });
            }
        } catch (
                UnsupportedModuleException e
                )

        {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }


    public void sendFile(View view) {

        toastIt("attempt to send file");
        HttpURLConnection urlConnection = null;
        String boundary = "*****";
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        String pathToOurFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MBIENTMBIENT.csv";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        try {
            URL url = new URL("http://christophergs.pythonanywhere.com/api/csv");

            urlConnection = (HttpURLConnection) url.openConnection();

            FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile));
            //File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "MBIENTMBIENT.csv");


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
            String responseText = urlConnection.getResponseMessage();
            Log.i(TAG, responseText.toString());

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
    }

    public void turnOnLed() {
        try {
            Led ledModule = mwBoard.getModule(Led.class);
            ledModule.configureColorChannel(Led.ColorChannel.BLUE)
                    .setRiseTime((short) 0).setPulseDuration((short) 1000)
                    .setRepeatCount((byte) -1).setHighTime((short) 500)
                    .setHighIntensity((byte) 16).setLowIntensity((byte) 16)
                    .commit();
            ledModule.play(false);
        } catch (UnsupportedModuleException e) {
            toastIt("LED problem");
            Log.e("MainActivity", "No Led on the board", e);
        }
    }

    public void ledOff(View view) {
        Log.e(TAG, "Turn off LED");
        //ledModule.stop(true); //CAUSES CRASH
    }


}
