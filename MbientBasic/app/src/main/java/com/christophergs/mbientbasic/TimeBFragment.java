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
import android.widget.TextView;


public class TimeBFragment extends Fragment {
    private static final String TAG = "MetaWear";
    public static final String ARG_PAGE = "ARG_PAGE";
    private int mPage;

    public TimeBFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //mPage = getArguments().getInt(ARG_PAGE);
    }

    public static TimeBFragment newInstance(int page) {
        Bundle args = new Bundle();
        TimeBFragment fragment = new TimeBFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, String.format("Inflate Fragment B"));
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_time_b, container, false);

        TextView textView = (TextView) view.findViewById(R.id.timeB);
        textView.setText("Fragment #");



        return view;
    }

}
