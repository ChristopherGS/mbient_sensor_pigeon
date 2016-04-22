package com.christophergs.mbientbasic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimelineActivity extends AppCompatActivity {
    public static ArrayList<String> TIMELINE_POSITIONS;
    public static String TIMELINE_POSITIONS_REDUCED;
    public static ArrayList<String> TIMELINE_POSITIONS2;
    private static final String TAG = "MetaWear";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TIMELINE_POSITIONS = getIntent().getExtras().getStringArrayList("SEQUENCE");
        TIMELINE_POSITIONS_REDUCED = TIMELINE_POSITIONS.get(0);
        Log.i(TAG, String.format("REDUCED: %s", TIMELINE_POSITIONS_REDUCED));
        List<String> TIMELINE_POSITIONS2 = new ArrayList<String>();
        List<String> wordList = Arrays.asList(TIMELINE_POSITIONS_REDUCED);

        for (String s : wordList)
        {
            TIMELINE_POSITIONS2.add(s);
        }
        Log.i(TAG, String.format("passing sequence: %s", TIMELINE_POSITIONS2));
        setContentView(R.layout.activity_timeline2);

        ListView lv = (ListView) findViewById(R.id.listView1);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                TIMELINE_POSITIONS2 );
        lv.setAdapter(arrayAdapter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timeline, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
