/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.christophergs.mbientbasic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.christophergs.mbientbasic.R;
import com.github.mikephil.charting.components.YAxis;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.christophergs.mbientbasic.help.HelpOption;
import com.christophergs.mbientbasic.help.HelpOptionAdapter;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Accelerometer.AccRange;
import com.mbientlab.metawear.module.Bmi160Accelerometer.OutputDataRate;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;

import java.io.File;

/**
 * Created by etsai on 8/19/2015.
 */
public class BothFragment extends ThreeAxisChartFragment {
    private static final float[] MMA845Q_RANGES= {2.f, 4.f, 8.f}, BMI160_RANGES= {2.f, 4.f, 8.f, 16.f};
    private static final float INITIAL_RANGE= 16.f, ACC_FREQ= 25.f;
    private static final String STREAM_KEY= "accel_stream";

    private Spinner accRangeSelection;
    private Bmi160Accelerometer accelModule= null;
    private int rangeIndex= 3;
    private static final String TAG = "MetaWear";
    OnFragTestListener mCallback;



    public BothFragment() {
        super("acceleration", R.layout.fragment_sensor_config_spinner,
                R.string.navigation_fragment_accelerometer, STREAM_KEY, -INITIAL_RANGE, INITIAL_RANGE, ACC_FREQ);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.config_option_title)).setText(R.string.config_name_acc_range);

        accRangeSelection = (Spinner) view.findViewById(R.id.config_option_spinner);
        accRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rangeIndex = position;

                final YAxis leftAxis = chart.getAxisLeft();
                if (accelModule instanceof Bmi160Accelerometer) {
                    leftAxis.setAxisMaxValue(BMI160_RANGES[rangeIndex]);
                    leftAxis.setAxisMinValue(-BMI160_RANGES[rangeIndex]);
                } else if (accelModule instanceof Mma8452qAccelerometer) {
                    leftAxis.setAxisMaxValue(MMA845Q_RANGES[rangeIndex]);
                    leftAxis.setAxisMinValue(-MMA845Q_RANGES[rangeIndex]);
                }

                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fillRangeAdapter();

        ((Switch) view.findViewById(R.id.sample_control)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mCallback.onStartButtonPressed(1);
                } else {
                    mCallback.onStartButtonPressed(2);
                }
            }
        });

        Button saveButton= (Button) view.findViewById(R.id.layout_two_button_right);
        saveButton.setText(R.string.label_analyze);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onSaveButtonPressed(1);
            }
        });

        Button chartButton= (Button) view.findViewById(R.id.layout_two_button_center);
        chartButton.setText(R.string.label_pie);
        chartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onPieButtonPressed(1);
            }
        });

        Button clearButton= (Button) view.findViewById(R.id.layout_two_button_left);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onClearButtonPressed(1);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnFragTestListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragTestListener");
        }
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException{
        accelModule= mwBoard.getModule(Bmi160Accelerometer.class);

        fillRangeAdapter();
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_acc_range, R.string.config_desc_acc_range));
    }

    public void setup() {
        accelModule.configureAxisSampling()
            .setOutputDataRate(OutputDataRate.ODR_25_HZ)
            .setFullScaleRange(AccRange.AR_16G)
            .commit();

        AsyncOperation<RouteManager> routeManagerResult= accelModule.routeData().fromAxes().stream(STREAM_KEY).commit();
        routeManagerResult.onComplete(dataStreamManager);
        routeManagerResult.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                accelModule.enableAxisSampling();
                accelModule.start();
                /*result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {
                    @Override
                    public void process(Message msg) {
                        final long estimatedTime = System.nanoTime() - startTime;
                        final CartesianFloat axes = msg.getData(CartesianFloat.class);
                        Log.i(TAG, String.format("Accel_Log: %s, %d, %tY%<tm%<td-%<tH%<tM%<tS%<tL", axes.toString(), estimatedTime, msg.getTimestamp()));
                    }
                });*/
            }
        });
    }

    public String blah(String foo) {
        Log.i(TAG, String.format("blah says: %s", foo));
        return foo;
    }

    @Override
    public void clean() {
        accelModule.stop();
        accelModule.disableAxisSampling();
    }

    private void fillRangeAdapter() {
        ArrayAdapter<CharSequence> spinnerAdapter= null;
        if (accelModule instanceof Bmi160Accelerometer) {
            spinnerAdapter= ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.values_bmi160_acc_range, android.R.layout.simple_spinner_item);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        } else if (accelModule instanceof Mma8452qAccelerometer) {
            spinnerAdapter= ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.values_mma8452q_acc_range, android.R.layout.simple_spinner_item);
        }

        if (spinnerAdapter != null) {
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            accRangeSelection.setAdapter(spinnerAdapter);
            accRangeSelection.setSelection(rangeIndex);
        }
    }

    public interface OnFragTestListener{
        public void onStartButtonPressed(int position);
        public void onSaveButtonPressed(int position);
        public void onClearButtonPressed(int position);
        public void onPieButtonPressed(int position);
    }
}
