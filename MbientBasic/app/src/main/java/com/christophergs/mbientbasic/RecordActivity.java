package com.christophergs.mbientbasic;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;

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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;

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
    private static final String TAG = "MBIENT_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
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
        mwBoard= ((MetaWearBleService.LocalBinder) iBinder).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(stateHandler);
        Log.i(TAG, String.valueOf(mwBoard.readDeviceInformation()));



        try {
            final Accelerometer accelModule = mwBoard.getModule(Accelerometer.class);
            final Logging loggingModule = mwBoard.getModule(Logging.class);
            final Bmi160Gyro bmi160GyroModule= mwBoard.getModule(Bmi160Gyro.class);
            final Debug debugger = mwBoard.getModule(Debug.class);

            Button btn2 = (Button) findViewById(R.id.start_accel_real);
            btn2.setOnClickListener(new View.OnClickListener() {
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
            Button btn3 = (Button) findViewById(R.id.stop_accel_real);
            btn3.setOnClickListener(new View.OnClickListener() {
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
                    AsyncOperation<RouteManager> routeManagerResult = accelModule.routeData().fromAxes().log(GYRO_LOG_KEY).commit();
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
                    Log.i(TAG, "Start Recording BOTH");
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
            Button bothStartBtn = (Button) findViewById(R.id.start_both_real);
            bothStartBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    accelModule.setOutputDataRate(25.f);
                    accelModule.setAxisSamplingRange(ACC_RANGE);
                    bmi160GyroModule.configure()
                            .setFullScaleRange(FullScaleRange.FSR_2000)
                            .setOutputDataRate(OutputDataRate.ODR_25_HZ)
                            .commit();
                    AsyncOperation<RouteManager> routeManagerResult = accelModule.routeData().fromAxes().log(GYRO_LOG_KEY).commit();
                    AsyncOperation<RouteManager> routeManagerResult2 = bmi160GyroModule.routeData().fromAxes().log(LOG_KEY).commit();
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
                    routeManagerResult2.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            final long startTime = System.nanoTime();
                            Log.i(TAG, "ACCEL");
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
                    Log.i(TAG, "Start Recording BOTH");
                    loggingModule.startLogging();
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
                    loggingModule.downloadLog(0.05f, new Logging.DownloadHandler() {
                        @Override
                        public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                            Log.i(TAG, String.format("Progress= %d / %d", nEntriesLeft, totalEntries));
                        }
                    });
                    Log.i(TAG, "log download complete");
                }
            });

        } catch (UnsupportedModuleException e) {
            Log.e(TAG, "unsupported module", e);
        }

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

}
