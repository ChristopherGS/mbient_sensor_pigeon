package com.christophergs.mbientchris;

import com.mbientlab.metawear.module.Led;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.DeviceInformation;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Switch;

/**
 * Created by etsai on 8/22/2015.
 */
public class HomeFragment extends ModuleFragmentBase {
    private Led ledModule;

    public HomeFragment() {
        super("Home");
    }

    public static class DfuProgressFragment extends DialogFragment {
        private ProgressDialog dfuProgress= null;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dfuProgress= new ProgressDialog(getActivity());
            dfuProgress.setTitle(getString(R.string.title_firmware_update));
            dfuProgress.setCancelable(false);
            dfuProgress.setCanceledOnTouchOutside(false);
            dfuProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dfuProgress.setProgress(0);
            dfuProgress.setMax(100);
            dfuProgress.setMessage(getString(R.string.message_dfu));
            return dfuProgress;
        }

        public void updateProgress(int newProgress) {
            if (dfuProgress != null) {
                dfuProgress.setProgress(newProgress);
            }
        }
    }

    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void initiateDfu() {
        final String DFU_PROGRESS_FRAGMENT_TAG= "dfu_progress_popup";
        DfuProgressFragment dfuProgressDialog= new DfuProgressFragment();
        dfuProgressDialog.show(getFragmentManager(), DFU_PROGRESS_FRAGMENT_TAG);

        getActivity().runOnUiThread(new Runnable() {
            final NotificationManager manager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            final Notification.Builder checkpointNotifyBuilder = new Notification.Builder(getActivity()).setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setOnlyAlertOnce(true).setOngoing(true).setProgress(0, 0, true);
            final Notification.Builder progressNotifyBuilder = new Notification.Builder(getActivity()).setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setOnlyAlertOnce(true).setOngoing(true).setContentTitle(getString(R.string.notify_dfu_uploading));
            final int NOTIFICATION_ID = 1024;

            @Override
            public void run() {
                mwBoard.updateFirmware(new MetaWearBoard.DfuProgressHandler() {
                    @Override
                    public void reachedCheckpoint(State dfuState) {
                        switch (dfuState) {
                            case INITIALIZING:
                                checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_bootloader));
                                break;
                            case STARTING:
                                checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_starting));
                                break;
                            case VALIDATING:
                                checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_validating));
                                break;
                            case DISCONNECTING:
                                checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_disconnecting));
                                break;
                        }

                        manager.notify(NOTIFICATION_ID, checkpointNotifyBuilder.build());
                    }

                    @Override
                    public void receivedUploadProgress(int progress) {
                        progressNotifyBuilder.setContentText(String.format("%d%%", progress)).setProgress(100, progress, false);
                        manager.notify(NOTIFICATION_ID, progressNotifyBuilder.build());
                        ((DfuProgressFragment) getFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).updateProgress(progress);
                    }
                }).onComplete(new AsyncOperation.CompletionHandler<Void>() {
                    final Notification.Builder builder = new Notification.Builder(getActivity()).setOnlyAlertOnce(true)
                            .setOngoing(false).setAutoCancel(true);

                    @Override
                    public void success(Void result) {
                        ((DialogFragment) getFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
                        builder.setContentTitle(getString(R.string.notify_dfu_success)).setSmallIcon(android.R.drawable.stat_sys_upload_done);
                        manager.notify(NOTIFICATION_ID, builder.build());

                        Toast.makeText(getActivity(), R.string.message_dfu_success, Toast.LENGTH_SHORT).show();

                        fragBus.resetConnectionStateHandler(5000L);
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e("MetaWearApp", "Firmware update failed", error);

                        Throwable cause= error.getCause() == null ? error : error.getCause();
                        ((DialogFragment) getFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
                        builder.setContentTitle(getString(R.string.notify_dfu_fail)).setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentText(cause.getLocalizedMessage());
                        manager.notify(NOTIFICATION_ID, builder.build());

                        Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                        fragBus.resetConnectionStateHandler(5000L);
                    }
                });
            }
        });
    }
    @Override
    protected void boardReady() throws UnsupportedModuleException {
        setupFragment(getView());
    }

    @Override
    public void reconnected() {
        setupFragment(getView());
    }

    private void configureChannel(Led.ColorChannelEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.setHighIntensity((byte) 31).setLowIntensity((byte) 31)
                .setHighTime((short) (PULSE_WIDTH >> 1)).setPulseDuration(PULSE_WIDTH)
                .setRepeatCount((byte) -1).commit();
    }

    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG= "metaboot_warning_tag", SWITCH_STREAM= "switch_stream";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning= (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }

        mwBoard.readDeviceInformation().onComplete(new AsyncOperation.CompletionHandler<DeviceInformation>() {
            @Override
            public void success(DeviceInformation result) {
                ((TextView) v.findViewById(R.id.manufacturer_value)).setText(result.manufacturer());
                ((TextView) v.findViewById(R.id.model_number_value)).setText(result.modelNumber());
                ((TextView) v.findViewById(R.id.serial_number_value)).setText(result.serialNumber());
                ((TextView) v.findViewById(R.id.firmware_revision_value)).setText(result.firmwareRevision());
                ((TextView) v.findViewById(R.id.hardware_revision_value)).setText(result.hardwareRevision());
                ((TextView) v.findViewById(R.id.device_mac_address_value)).setText(mwBoard.getMacAddress());
            }
        });

        try {
            Switch switchModule = mwBoard.getModule(Switch.class);
            switchModule.routeData().fromSensor().stream(SWITCH_STREAM).commit()
                    .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(SWITCH_STREAM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.switch_radio_group);

                                    if (msg.getData(Boolean.class)) {
                                        radioGroup.check(R.id.switch_radio_pressed);
                                    } else {
                                        radioGroup.check(R.id.switch_radio_released);
                                    }
                                }
                            });
                        }
                    });
        } catch (UnsupportedModuleException ignored) {
        }

        int[] ledResIds= new int[] {R.id.led_stop, R.id.led_red_on, R.id.led_green_on, R.id.led_blue_on};

        try {
            ledModule = mwBoard.getModule(Led.class);

            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(true);
            }
        } catch (UnsupportedModuleException e) {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(false);
            }
        }
    }
}
