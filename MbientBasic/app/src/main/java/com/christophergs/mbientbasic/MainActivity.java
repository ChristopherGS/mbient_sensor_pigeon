package com.christophergs.mbientbasic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Switch;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.app.Activity;
import android.content.*;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;


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

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private MetaWearBleService.LocalBinder binder;
    private MetaWearBoard mwBoard;
    private final String MW_MAC_ADDRESS = "D5:9C:DC:37:BA:AE";
    private static final String TAG = "MBIENT_TAG";
    private Led ledModule;
    private static final float ACC_RANGE = 8.f, ACC_FREQ= 50.f;
    private static final String STREAM_KEY= "accel_stream";
    private Accelerometer accelModule= null;

    private void toastIt( String msg ) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

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
    public void onServiceDisconnected(ComponentName componentName) { }

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
        final Switch mySwitch= (Switch) view;

        try {
            final Accelerometer accelModule = mwBoard.getModule(Accelerometer.class);
            final Logging loggingModule = mwBoard.getModule(Logging.class);
            if (mySwitch.isChecked()) {
                accelModule.setOutputDataRate(50.f);
                accelModule.setAxisSamplingRange(ACC_RANGE);

                accelModule.routeData()
                    .fromAxes().stream("accel_stream_key")
                    .commit().onComplete(new CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe("accel_stream_key", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    CartesianFloat axes= message.getData(CartesianFloat.class);
                                    Log.i(TAG, axes.toString());
                                }
                            });
                        }
                        @Override
                        public void failure(Throwable error) {
                            Log.e(TAG, "Error committing route", error);
                        }
                    });
                accelModule.enableAxisSampling();
                loggingModule.startLogging();
                accelModule.start();

            } else {
                loggingModule.stopLogging();
                accelModule.disableAxisSampling();
                accelModule.stop();
            }
        } catch (UnsupportedModuleException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void turnOnLed() {
        try {
            Led ledModule= mwBoard.getModule(Led.class);
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
