package com.christophergs.mbientbasic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class SuggestionActivity extends AppCompatActivity {
    ListView list;
    private List<String> List_file;
    TextView tvName = null;
    private static final String TAG = "MetaWear";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion);
        //populateUsersList();
        // Construct the data source
        ArrayList<User> arrayOfUsers = User.getUsers();
        // Create the adapter to convert the array to views
        final UsersAdapter adapter = new UsersAdapter(this, arrayOfUsers);
        // Attach the adapter to a ListView
        ListView list = (ListView) findViewById(R.id.dsg_suggestions);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                // When clicked, show a toast with the TextView text
                Log.i(TAG, String.format("Video Playing, id: %s, position: %s, view: %s", id, position, view));
                User user = adapter.getItem(position);
                Log.i(TAG, String.format("Video Playing, user name: %s, user home: %s", user.name, user.hometown));
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=0e0jMrZas-g")));
                Intent videoActivityIntent = new Intent(SuggestionActivity.this, VideoActivity.class);
                //videoActivityIntent.putExtra(NavigationActivity.YOUTUBE_LINK, youTubeLink);
                Log.i(TAG, String.format("loading video"));
                //navActivityIntent.putExtra("EXPERIMENT", EXPERIMENT_ID);
                startActivityForResult(videoActivityIntent, 1);
            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_suggestion, menu);
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

    public static class User {
        public String name;
        public String hometown;
        public String gi;

        public User(String name, String hometown) {
            this.name = name;
            this.hometown = hometown;
        }

        public static ArrayList<User> getUsers() {
            ArrayList<User> users = new ArrayList<User>();
            users.add(new User("Side Mount Escape - Hip Escape", "Jiu-Jitsu Brotherhood"));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=V7vmzcc3ldA\">Side Mount Escape - Sit up</a>", "Marcelo Garcia" ));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=jxiHY2AtG8w\">Side Mount Escape (No Gi)</a>", "Stephan Kesting"));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=x0j7muxW_1Y\">Side Mount Escape - Turtle</a>", "Ricardo Cavalcanti"));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=4l9tP2gRuOo\">Side Mount Escape - Arm positions</a>", "Kurt Osiander" ));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=g5dG5kLqlFw\">Side Mount Escapes - Rapid overview</a>", "Jason Scully"));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=7lxafCrBvq0\">Side Mount Escapes - Fundamentals</a>", "Damian Maia"));
            users.add(new User("<a href=\"https://www.youtube.com/watch?v=1d-HZD_R08s\">Side Mount - Surviving</a>", "Ryron Gracie"));
            return users;

        }
    }

    public class UsersAdapter extends ArrayAdapter<User> {
        public UsersAdapter(Context context, ArrayList<User> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            User user = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_row_layout, parent, false);
            }
            // Lookup view for data population
            //TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            TextView tvHome = (TextView) convertView.findViewById(R.id.secondLine);
            TextView tvName = (TextView) convertView.findViewById(R.id.firstLine);
            tvName.setMovementMethod(LinkMovementMethod.getInstance());

            // Populate the data into the template view using the data object
            tvName.setText(Html.fromHtml(user.name));
            tvHome.setText(user.hometown);

            // Return the completed view to render on screen
            return convertView;
        }
    }

}
