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

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;

import com.christophergs.mbientbasic.SensorFragment;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.data.CartesianFloat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by etsai on 8/19/2015.
 */
public abstract class ThreeAxisChartFragment extends SensorFragment {
    private final ArrayList<Entry> xAxisData= new ArrayList<>(), yAxisData= new ArrayList<>(), zAxisData= new ArrayList<>();
    private final String dataType, streamKey;
    private final float samplePeriod;
    protected final AsyncOperation.CompletionHandler<RouteManager> dataStreamManager= new AsyncOperation.CompletionHandler<RouteManager>() {
        @Override
        public void success(RouteManager result) {
            streamRouteManager= result;
            result.subscribe(streamKey, new RouteManager.MessageHandler() {
                @Override
                public void process(Message message) {
                    final CartesianFloat spin = message.getData(CartesianFloat.class);

                    LineData data = chart.getData();

                    data.addXValue(String.format("%.2f", sampleCount * samplePeriod));
                    data.addEntry(new Entry(spin.x(), sampleCount), 0);
                    data.addEntry(new Entry(spin.y(), sampleCount), 1);
                    data.addEntry(new Entry(spin.z(), sampleCount), 2);

                    sampleCount++;
                }
            });
        }
    };

    protected ThreeAxisChartFragment(String dataType, int layoutId, int sensorResId, String streamKey, float min, float max, float sampleFreq) {
        super(sensorResId, layoutId, min, max);
        this.dataType= dataType;
        this.streamKey= streamKey;
        this.samplePeriod= 1 / sampleFreq;
    }

    @Override
    protected String saveData() {
        final String CSV_HEADER = String.format("time,x-%s,y-%s,z-%s%n", dataType, dataType, dataType);
        String filename = String.format("%s_%tY%<tm%<td-%<tH%<tM%<tS%<tL.csv", getActivity().getApplicationContext().getString(sensorResId), Calendar.getInstance());
        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), filename);

        OutputStream fos;
        try {
            fos = new BufferedOutputStream(new FileOutputStream(path, true));
            fos.write(CSV_HEADER.getBytes());

            LineData data = chart.getLineData();
            LineDataSet xSpinDataSet = data.getDataSetByIndex(0), ySpinDataSet = data.getDataSetByIndex(1),
                    zSpinDataSet = data.getDataSetByIndex(2);
            for (int i = 0; i < data.getXValCount(); i++) {
                fos.write(String.format("%.3f,%.3f,%.3f,%.3f%n", i * samplePeriod,
                        xSpinDataSet.getEntryForXIndex(i).getVal(),
                        ySpinDataSet.getEntryForXIndex(i).getVal(),
                        zSpinDataSet.getEntryForXIndex(i).getVal()).getBytes());
            }
            fos.close();
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void resetData(boolean clearData) {
        if (clearData) {
            sampleCount = 0;
            chartXValues.clear();
            xAxisData.clear();
            yAxisData.clear();
            zAxisData.clear();
        }

        ArrayList<LineDataSet> spinAxisData= new ArrayList<>();
        spinAxisData.add(new LineDataSet(xAxisData, "x-" + dataType));
        spinAxisData.get(0).setColor(Color.RED);
        spinAxisData.get(0).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(yAxisData, "y-" + dataType));
        spinAxisData.get(1).setColor(Color.GREEN);
        spinAxisData.get(1).setDrawCircles(false);

        spinAxisData.add(new LineDataSet(zAxisData, "z-" + dataType));
        spinAxisData.get(2).setColor(Color.BLUE);
        spinAxisData.get(2).setDrawCircles(false);

        LineData data= new LineData(chartXValues);
        for(LineDataSet set: spinAxisData) {
            data.addDataSet(set);
        }
        data.setDrawValues(false);
        chart.setData(data);
    }
}
