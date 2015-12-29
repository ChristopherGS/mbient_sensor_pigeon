package com.christophergs.mbientbasic;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.nfc.Tag;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;


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

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.data.CartesianShort;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Bmi160Gyro.*;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Macro;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import static com.mbientlab.metawear.AsyncOperation.CompletionHandler;

public class RecordActivity extends AppCompatActivity implements ServiceConnection {

    public static final String EXTRA_BT_DEVICE = "com.christophergs.metawearguide.EXTRA_BT_DEVICE";
    private BluetoothDevice btDevice;
    private MetaWearBleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;
    TextView accelerationData;
    TextView gyroData;
    private Bmi160Gyro bmi160GyroModule;
    private Accelerometer accelModule;
    private Debug debugModule;
    private Logging loggingModule;
    private static final float ACC_RANGE = 8.f, ACC_FREQ = 25.f;
    private static final String LOG_KEY = "accel_log";
    private static final String GYRO_LOG_KEY = "gyro_log";
    private static final String BOTH_LOG_KEY = "both_log";
    private static final String ACCEL_STREAM_KEY = "accel_stream";
    private static final String GYRO_STREAM_KEY = "gyro_stream";
    private static final String TAG = "MBIENT_TAG";

    private void toastIt(String msg) {
        Toast.makeText(RecordActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
        accelerationData = (TextView) findViewById(R.id.accelerationData);
        gyroData = (TextView) findViewById(R.id.gyroData);
        Log.i(TAG, "Initialize");
    }

    private final ConnectionStateHandler stateHandler = new ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i(TAG, "Connected");
        }
        @Override
        public void disconnected() {
            Log.i(TAG, "Connected Lost");
        }

        @Override
        public void failure(int status, Throwable error) {
            Log.e(TAG, "Error connecting", error);
        }
    };

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mwBoard = ((MetaWearBleService.LocalBinder) iBinder).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(stateHandler);
        Log.i(TAG, String.valueOf(mwBoard.readDeviceInformation()));


        try {
            final Accelerometer accelModule = mwBoard.getModule(Accelerometer.class);
            final Logging loggingModule = mwBoard.getModule(Logging.class);
            final Bmi160Gyro bmi160GyroModule = mwBoard.getModule(Bmi160Gyro.class);
            final Debug debugger = mwBoard.getModule(Debug.class);


            Button accelStartBtn = (Button) findViewById(R.id.start_accel_real);
            accelStartBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "START ACCELEROMETER");
                    accelModule.setOutputDataRate(50.f);
                    accelModule.setAxisSamplingRange(ACC_RANGE);

                    AsyncOperation<RouteManager> routeManagerResult = accelModule.routeData().fromAxes().log(LOG_KEY).commit();

                    routeManagerResult.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            final long startTime = System.nanoTime();
                            Log.i(TAG, "HERE");
                            result.setLogMessageHandler(LOG_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final long estimatedTime = System.nanoTime() - startTime;
                                    final CartesianFloat axes = msg.getData(CartesianFloat.class);
                                    Log.i(TAG, String.format("Accel_Log: %s, %d, %tY%<tm%<td-%<tH%<tM%<tS%<tL", axes.toString(), estimatedTime, msg.getTimestamp()));
                                }
                            });
                        }
                    });
                    Log.i(TAG, "Start Recording Accelerometer");
                    loggingModule.startLogging();
                    accelModule.enableAxisSampling();
                    accelModule.start();
                }
            });
            Button accelStopBtn = (Button) findViewById(R.id.stop_accel_real);
            accelStopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Stop Recording Accelerometer");
                    accelModule.stop();
                    accelModule.disableAxisSampling();
                    loggingModule.stopLogging();

                    loggingModule.downloadLog(0.05f, new Logging.DownloadHandler() {
                        @Override
                        public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                            Log.i(TAG, String.format("Progress= %d / %d", nEntriesLeft, totalEntries));
                        }
                    });
                    Log.i(TAG, "log download complete");
                }
            });
            Button resetBtn = (Button) findViewById(R.id.reset_board_real);
            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loggingModule.clearEntries();
                    debugger.resetDevice();
                    Log.i(TAG, "reset device");
                }
            });
            Button gyroStartBtn = (Button) findViewById(R.id.start_gyro_real);
            gyroStartBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bmi160GyroModule.configure()
                            .setFullScaleRange(FullScaleRange.FSR_2000)
                            .setOutputDataRate(OutputDataRate.ODR_50_HZ)
                            .commit();
                    AsyncOperation<RouteManager> routeManagerResult = bmi160GyroModule.routeData().fromAxes().log(GYRO_LOG_KEY).commit();
                    routeManagerResult.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            final long startTime_gyro = System.nanoTime();
                            Log.i(TAG, "GYRO");
                            result.setLogMessageHandler(GYRO_LOG_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final long estimatedTime_gyro = System.nanoTime() - startTime_gyro;
                                    final CartesianFloat spinData = msg.getData(CartesianFloat.class);
                                    Log.i(TAG, String.format("Gyro_Log: %s, %d, %tY%<tm%<td-%<tH%<tM%<tS%<tL", spinData.toString(), estimatedTime_gyro, msg.getTimestamp()));
                                }
                            });
                        }
                    });
                    Log.i(TAG, "Start Recording Gyro");
                    loggingModule.startLogging();
                    bmi160GyroModule.start();

                }
            });
            Button gyroStopBtn = (Button) findViewById(R.id.stop_gyro_real);
            gyroStopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Stop Recording Gyroscope");
                    bmi160GyroModule.stop();
                    loggingModule.stopLogging();
                    loggingModule.downloadLog(0.05f, new Logging.DownloadHandler() {
                        @Override
                        public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                            Log.i(TAG, String.format("Progress= %d / %d", nEntriesLeft, totalEntries));
                        }
                    });
                    Log.i(TAG, "log download complete");
                }
            });

            //=================================================================

            Button bothStartBtn = (Button) findViewById(R.id.start_both_real);
            bothStartBtn.setOnClickListener(new View.OnClickListener() {
                //String state = Environment.getExternalStorageState();
                final String CSV_HEADER = String.format("sensor,x_axis,y_axis,z_axis,time_elapsed,timestamp,");
                //final String filename = String.format("METAWEAR_%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", Calendar.getInstance());
                final String filename = "METAWEAR_COMBO2.csv";
                File path = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), filename);

                //delete the csv file if it already exists (will be from older recordings)
                public void prep() {
                    boolean is_deleted = path.delete();
                    Log.i(TAG, "deleted: " + is_deleted);
                }

                @Override
                public void onClick(View v) {
                    prep();
                    OutputStream out;
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(path, true));
                        out.write(CSV_HEADER.getBytes());
                        out.write("\n".getBytes());
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    accelModule.setOutputDataRate(ACC_FREQ);
                    accelModule.setAxisSamplingRange(ACC_RANGE);
                    bmi160GyroModule.configure()
                            .setFullScaleRange(FullScaleRange.FSR_2000)
                            .setOutputDataRate(OutputDataRate.ODR_25_HZ)
                            .commit();
                    AsyncOperation<RouteManager> routeManagerResult = accelModule.routeData().fromAxes().log(LOG_KEY).commit();
                    AsyncOperation<RouteManager> routeManagerResult2 = bmi160GyroModule.routeData().fromAxes().log(GYRO_LOG_KEY).commit();
                    routeManagerResult2.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            final long startTime_gyro = System.nanoTime();
                            Log.i(TAG, "GYRO");
                            result.setLogMessageHandler(GYRO_LOG_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final long estimatedTime_gyro = System.nanoTime() - startTime_gyro;
                                    final CartesianFloat spinData = msg.getData(CartesianFloat.class);
                                    String gyro_entry = String.format("Gyro_Log,%s,%d", spinData.toString(),estimatedTime_gyro);
                                    String ats = msg.getTimestampAsString();
                                    Log.i(TAG, String.format("%s,%s", gyro_entry, ats));
                                    //CSV CODE
                                    String csv_gyro_entry = gyro_entry + "," + ats + ",";
                                    OutputStream out;
                                    try {
                                        out = new BufferedOutputStream(new FileOutputStream(path, true));
                                        out.write(csv_gyro_entry.getBytes());
                                        out.write("\n".getBytes());
                                        out.close();
                                    } catch (Exception e) {
                                        Log.e(TAG, "CSV creation error", e);
                                    }
                                }
                            });
                        }
                    });
                    routeManagerResult.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {

                            final long startTime = System.nanoTime();
                            Log.i(TAG, "ACCEL");
                            result.setLogMessageHandler(LOG_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    final long estimatedTime = System.nanoTime() - startTime;
                                    final CartesianFloat axes = msg.getData(CartesianFloat.class);
                                    String entry = String.format("Accel_Log,%s,%d", axes.toString(), estimatedTime);
                                    String ts = msg.getTimestampAsString();
                                    Log.i(TAG, String.format("%s,%s", entry, ts));
                                    //CSV CODE
                                    String csv_entry = entry + "," + ts + ",";
                                    OutputStream out;
                                    try {
                                        out = new BufferedOutputStream(new FileOutputStream(path, true));
                                        out.write(csv_entry.getBytes());
                                        out.write("\n".getBytes());
                                        out.close();
                                    } catch (Exception e) {
                                        Log.e(TAG, "CSV creation error", e);
                                    }
                                }
                            });
                        }
                    });
                    Log.i(TAG, "Start Recording BOTH");
                    Log.i(TAG, String.valueOf(loggingModule.getLogCapacity()));
                    loggingModule.startLogging(true);
                    accelModule.enableAxisSampling();
                    bmi160GyroModule.start();
                    accelModule.start();
                }
            });
            Button bothStopBtn = (Button) findViewById(R.id.stop_both_real);
            bothStopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Stop Recording Both");
                    bmi160GyroModule.stop();
                    accelModule.stop();
                    accelModule.disableAxisSampling();
                    loggingModule.stopLogging();
                    Log.i(TAG, String.valueOf(loggingModule.getLogCapacity()));
                    loggingModule.downloadLog(0.05f, new Logging.DownloadHandler() {
                        @Override
                        public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                            Log.i(TAG, String.format("Progress= %d / %d", nEntriesLeft, totalEntries));
                        }
                    });
                    Log.i(TAG, "log download complete");
                }
            });

            Switch accelStreamBtn = (Switch) findViewById(R.id.switchAccel);
            accelStreamBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.v("Switch State=", "" + isChecked);
                    toastIt("Stream accelerometer data");
                    if (isChecked) {
                        accelModule.setOutputDataRate(ACC_FREQ);
                        accelModule.setAxisSamplingRange(ACC_RANGE);

                        accelModule.routeData()
                                .fromAxes().stream(ACCEL_STREAM_KEY)
                                .commit().onComplete(new CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe(ACCEL_STREAM_KEY, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        CartesianFloat axes = message.getData(CartesianFloat.class);
                                        Log.i(TAG, axes.toString());
                                        //Displays acceleration data string to screen;
                                        accelerationData.setText("Acc (X,Y,Z): " + axes);
                                    }

                                });
                            }

                            @Override
                            public void failure(Throwable error) {
                                Log.e(TAG, "Error committing route", error);
                            }
                        });
                        accelModule.enableAxisSampling();
                        accelModule.start();
                    } else {
                        accelModule.disableAxisSampling();
                        accelModule.stop();
                    }
                }
            });

            Switch gyroStreamBtn = (Switch) findViewById(R.id.switchGyro);
            gyroStreamBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.v("Switch State=", "" + isChecked);
                    toastIt("Stream gyroscope data");
                    if (isChecked) {
                        bmi160GyroModule.configure()
                                .setFullScaleRange(FullScaleRange.FSR_2000)
                                .setOutputDataRate(OutputDataRate.ODR_25_HZ)
                                .commit();

                        bmi160GyroModule.routeData()
                                .fromAxes().stream(GYRO_STREAM_KEY)
                                .commit().onComplete(new CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.subscribe(GYRO_STREAM_KEY, new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        final CartesianFloat spinData = message.getData(CartesianFloat.class);
                                        Log.i(TAG, spinData.toString());
                                        //Displays acceleration data string to screen;
                                        gyroData.setText("Acc (X,Y,Z): " + spinData);
                                    }

                                });
                            }
                            @Override
                            public void failure(Throwable error) {
                                Log.e(TAG, "Error committing route", error);
                            }
                        });
                        bmi160GyroModule.start();
                    } else {
                        bmi160GyroModule.stop();
                    }
                }
            });

        } catch(UnsupportedModuleException e){
            Log.e(TAG, "module error", e);
        }
    }

    public void stopMbient(View view){
        mwBoard.disconnect();
        Toast.makeText(this, "disconnected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }



    public void sendFile(View view) {

        //ADD THE LOADING DIALOG FROM SCANNER

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

}
