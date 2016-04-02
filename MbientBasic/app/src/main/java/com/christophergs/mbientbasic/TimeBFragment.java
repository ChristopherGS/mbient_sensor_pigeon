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
    private String time_;
    private String percent_;

    public TimeBFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPage = getArguments().getInt(ARG_PAGE);
        time_ = args.getString("TIME");
        percent_ = args.getString("PERCENT");
    }

    public static TimeBFragment newInstance(int page, String time_arg, String percent_arg) {
        Bundle args = new Bundle();
        TimeBFragment fragment = new TimeBFragment();
        args.putInt(ARG_PAGE, page);
        args.putString("TIME", time_arg);
        args.putString("PERCENT", percent_arg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, String.format("Inflate Fragment B"));
        Log.i(TAG, String.format("Inflate Fragment B Time: %s", time_));
        Log.i(TAG, String.format("Inflate Fragment B Percent: %s", percent_));
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_time_b, container, false);

        TextView textView1 = (TextView) view.findViewById(R.id.dashboard_time_number_y);
        TextView textView2 = (TextView) view.findViewById(R.id.dashboard_percentage_number_y);
        TextView textView3 = (TextView) view.findViewById(R.id.dashboard_time_number_y2);
        TextView textView4 = (TextView) view.findViewById(R.id.dashboard_percentage_number_y2);
        TextView textView5 = (TextView) view.findViewById(R.id.dashboard_time_number_y3);
        TextView textView6 = (TextView) view.findViewById(R.id.dashboard_percentage_number_y3);
        TextView textView7 = (TextView) view.findViewById(R.id.dashboard_time_number_y4);
        TextView textView8 = (TextView) view.findViewById(R.id.dashboard_percentage_number_y4);

        TextView textView9 = (TextView) view.findViewById(R.id.dashboard_time_number_o);
        TextView textView10 = (TextView) view.findViewById(R.id.dashboard_percentage_number_o);
        TextView textView11 = (TextView) view.findViewById(R.id.dashboard_time_number_o2);
        TextView textView12 = (TextView) view.findViewById(R.id.dashboard_percentage_number_o2);
        TextView textView13 = (TextView) view.findViewById(R.id.dashboard_time_number_o3);
        TextView textView14 = (TextView) view.findViewById(R.id.dashboard_percentage_number_o3);
        TextView textView15 = (TextView) view.findViewById(R.id.dashboard_time_number_o4);
        TextView textView16 = (TextView) view.findViewById(R.id.dashboard_percentage_number_o4);

        if (mPage == 1){
            textView1.setText("12 mins");
            textView2.setText("10%");
            textView3.setText("3 mins");
            textView4.setText("4%");
            textView5.setText("1 min");
            textView6.setText("1%");
            textView7.setText("0 mins");
            textView8.setText("0%");
            textView9.setText("0 mins");
            textView10.setText("0%");
            textView11.setText("18 mins");
            textView12.setText("14%");
            textView13.setText("13 mins");
            textView14.setText("12%");
            textView15.setText("3 mins");
            textView16.setText("2%");
        } else if (mPage == 2) {
            textView1.setText("31 mins");
            textView2.setText("17%");
            textView3.setText("29 mins");
            textView4.setText("14%");
            textView5.setText("50 mins");
            textView6.setText("6%");
            textView7.setText("2 mins");
            textView8.setText("1%");
            textView9.setText("33 mins");
            textView10.setText("16%");
            textView11.setText("17 mins");
            textView12.setText("9%");
            textView13.setText("12 mins");
            textView14.setText("2%");
            textView15.setText("14 mins");
            textView16.setText("3%");
        } else if (mPage == 3) {
            textView1.setText("51 minutes");
            textView2.setText("23%");
            textView3.setText("44 minutes");
            textView4.setText("10%");
            textView5.setText("22 minutes");
            textView6.setText("6%");
            textView7.setText("19 mins");
            textView8.setText("3%");
            textView9.setText("12 mins");
            textView10.setText("3%");
            textView11.setText("68 mins");
            textView12.setText("13%");
            textView13.setText("22 mins");
            textView14.setText("4%");
            textView15.setText("17 mins");
            textView16.setText("3%");
        } else {
            textView1.setText("410 mins");
            textView2.setText("28%");
            textView3.setText("108 mins");
            textView4.setText("10%");
            textView5.setText("70 mins");
            textView6.setText("9%");
            textView7.setText("320 mins");
            textView8.setText("15%");
            textView9.setText("1 min");
            textView10.setText("1%");
            textView11.setText("145 mins");
            textView12.setText("19%");
            textView13.setText("98 mins");
            textView14.setText("5%");
            textView15.setText("22 mins");
            textView16.setText("2%");
        }

        return view;
    }

}
