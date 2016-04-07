package com.christophergs.mbientbasic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class VideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        WebView engine = (WebView) findViewById(R.id.webview);

        engine.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        engine.getSettings().setJavaScriptEnabled(true);

        engine.setHorizontalScrollBarEnabled(false);
        engine.setVerticalScrollBarEnabled(false);
        engine.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        engine.getSettings().setBuiltInZoomControls(false);
        engine.getSettings().setAppCacheEnabled(true);
        engine.setInitialScale(0);
        engine.getSettings().setLoadWithOverviewMode(true);
        engine.getSettings().setUseWideViewPort(true);

        String playVideo= "<html><body><br> <iframe class=\"youtube-player\" type=\"text/html\" style=\"border: 0; width: 100%; height: 100%; padding:0px; margin:0px\" src=\"https://www.youtube.com/embed/0e0jMrZas-g\" frameborder=\"0\"></body></html>";
        engine.loadData(playVideo, "text/html", "utf-8");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_video, menu);
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
