package com.christophergs.mbientchris;

import android.content.Intent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.mbientlab.metawear.RouteManager;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by etsai on 8/19/2015.
 */
public abstract class SensorFragment extends ModuleFragmentBase {
    private static final float FPS= 30.f;
    private static final long UPDATE_PERIOD= (long) ((1 / FPS) * 1000L);

    protected final ArrayList<String> chartXValues= new ArrayList<>();
    protected ViewGroup fragContainer;
    protected LineChart chart;
    protected int sampleCount;

    protected String chartDescription;
    protected float min, max;

    protected RouteManager streamRouteManager= null;
    private final int layoutId;

    private final Runnable updateChartTask= new Runnable() {
        @Override
        public void run() {
            chart.notifyDataSetChanged();

            moveViewToLast();

            chartHandler.postDelayed(updateChartTask, UPDATE_PERIOD);
        }
    };
    private final Handler chartHandler= new Handler();

    protected SensorFragment(String sensor, int layoutId, float min, float max) {
        super(sensor);
        this.layoutId= layoutId;
        this.min= min;
        this.max= max;
    }

    private void moveViewToLast() {
        chart.setVisibleXRangeMaximum(120);
        if (sampleCount > 120) {
            chart.moveViewToX(sampleCount - 121);
        } else {
            chart.moveViewToX(sampleCount);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragContainer= container;
        setRetainInstance(true);
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = (LineChart) view.findViewById(R.id.data_chart);

        initializeChart();
        resetData(false);
        chart.invalidate();

        view.findViewById(R.id.data_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chart.resetTracking();
                chart.clear();
                resetData(true);
                chart.invalidate();
            }
        });
        ((Switch) view.findViewById(R.id.sample_control)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    moveViewToLast();
                    setup();
                    chartHandler.postDelayed(updateChartTask, UPDATE_PERIOD);
                } else {
                    chart.setVisibleXRangeMaximum(sampleCount);
                    clean();
                    if (streamRouteManager != null) {
                        streamRouteManager.remove();
                        streamRouteManager= null;
                    }
                    chartHandler.removeCallbacks(updateChartTask);
                }
            }
        });

        view.findViewById(R.id.data_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = saveData();

                if (filename != null) {
                    File dataFile = getActivity().getFileStreamPath(filename);
                    Uri contentUri = FileProvider.getUriForFile(getActivity(), "com.mbientlab.metawear.app.fileprovider", dataFile);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, filename);
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(intent, "Saving Data"));
                }
            }
        });
    }

    protected void initializeChart() {
        ///< configure axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setStartAtZero(false);
        leftAxis.setAxisMaxValue(max);
        leftAxis.setAxisMinValue(min);
        chart.getAxisRight().setEnabled(false);

        chart.setDescription(chartDescription);
    }

    protected abstract void setup();
    protected abstract void clean();
    protected abstract String saveData();
    protected abstract void resetData(boolean clearData);
}

