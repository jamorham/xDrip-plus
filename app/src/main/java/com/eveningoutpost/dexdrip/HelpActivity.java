package com.eveningoutpost.dexdrip;

import android.os.*;
import android.webkit.*;

import com.eveningoutpost.dexdrip.models.*;

public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        JoH.fixActionBar(this);
        WebView webview = (WebView)findViewById(R.id.helpWebView);

    }
}
