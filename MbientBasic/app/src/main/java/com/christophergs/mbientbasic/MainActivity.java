package com.christophergs.mbientbasic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.app.Activity;
import android.content.*;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import static com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import static com.mbientlab.metawear.AsyncOperation.CompletionHandler;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private MetaWearBleService.LocalBinder binder;
    private MetaWearBoard mwBoard;
    private final String MW_MAC_ADDRESS = "D5:9C:DC:37:BA:AE";
    private static final String TAG = "MBIENT_TAG";

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
                        Log.e("test", "Error reading device information", error);
                    }
                });
            }

            @Override
            public void disconnected() {
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                Log.i("test", "Disconnected");
            }

            @Override
            public void failure(int status, final Throwable error) {
                Toast.makeText(MainActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                Log.e("test", "Error connecting", error);
            }

        });

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }

    public void startMbient(View view) {
        toastIt("trying to connect to mbient");
        mwBoard.connect();
    }

}
