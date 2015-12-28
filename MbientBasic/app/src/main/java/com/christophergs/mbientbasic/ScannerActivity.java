package com.christophergs.mbientbasic;


import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.bletoolbox.scanner.BleScannerFragment.*;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.Scanner;
import java.util.UUID;

public class ScannerActivity extends AppCompatActivity implements ScannerCommunicationBus, ServiceConnection {
    private final static UUID[] serviceUuids;
    public static final int REQUEST_START_APP= 1;

    static {
        serviceUuids= new UUID[] {
                MetaWearBoard.METAWEAR_SERVICE_UUID,
                MetaWearBoard.METABOOT_SERVICE_UUID
        };
    }

    private MetaWearBleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_START_APP:
                ((BleScannerFragment) getFragmentManager().findFragmentById(R.id.scanner_fragment)).startBleScan();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice btDevice) {
        mwBoard= serviceBinder.getMetaWearBoard(btDevice);

        final ProgressDialog connectDialog = new ProgressDialog(this);
        connectDialog.setTitle(getString(R.string.title_connecting));
        connectDialog.setMessage(getString(R.string.message_wait));
        connectDialog.setCancelable(false);
        connectDialog.setCanceledOnTouchOutside(false);
        connectDialog.setIndeterminate(true);
        connectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mwBoard.disconnect();
            }
        });
        connectDialog.show();

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                connectDialog.dismiss();
                Intent recordActivityIntent = new Intent(ScannerActivity.this, RecordActivity.class);
                recordActivityIntent.putExtra(RecordActivity.EXTRA_BT_DEVICE, btDevice);
                ScannerActivity.this.startActivity(recordActivityIntent);
                //Intent navigationActivityIntent = new Intent(ScannerActivity.this, NavigationActivity.class);
                //navigationActivityIntent.putExtra(NavigationActivity.EXTRA_BT_DEVICE, btDevice);
                //ScannerActivity.this.startActivity(navigationActivityIntent);
                //Intent navActivityIntent = new Intent(ScannerActivity.this, NavigationActivity.class);
                //navActivityIntent.putExtra(NavigationActivity.EXTRA_BT_DEVICE, btDevice);
                //startActivityForResult(navActivityIntent, REQUEST_START_APP);
            }

            @Override
            public void disconnected() {
                mwBoard.connect();
            }

            @Override
            public void failure(int status, Throwable error) {
                mwBoard.connect();
            }
        });
        mwBoard.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        serviceBinder = (MetaWearBleService.LocalBinder) iBinder;
        serviceBinder.executeOnUiThread();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    public UUID[] getFilterServiceUuids() {
        return serviceUuids;
    }

    @Override
    public long getScanDuration() {
        return 10000L;
    }

}