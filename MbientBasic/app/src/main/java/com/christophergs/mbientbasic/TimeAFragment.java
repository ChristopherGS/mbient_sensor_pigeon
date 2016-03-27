package com.christophergs.mbientbasic;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class TimeAFragment extends Fragment {
    private static final String TAG = "MetaWear";

    public TimeAFragment() {
        // Required empty public constructor
    }

    public static TimeAFragment newInstance(int page) {
        Bundle args = new Bundle();
        TimeAFragment fragment = new TimeAFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, String.format("Inflate Fragment A"));
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_time_a, container, false);

    }
}
