package com.eveningoutpost.dexdrip.utils;

import android.*;
import android.content.pm.*;
import android.os.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;
import androidx.core.app.*;
import androidx.core.content.*;

import com.google.zxing.client.android.*;
import com.google.zxing.integration.android.*;


public class AndroidBarcode extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String SCAN_INTENT = Intents.Scan.ACTION;
    AppCompatActivity activity;
    final static int MY_PERMISSIONS_REQUEST_CAMERA = 1003;
    final static String TAG = "jamorham barcode";

    public AndroidBarcode(AppCompatActivity activity) {
        this.activity = activity;
    }

    // callback not received in test
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new IntentIntegrator(activity).initiateScan();
                } else {
                    toast("Without camera permission we cannot scan a barcode");
                }
                return;
            }

        }
    }

    public void scan() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                new IntentIntegrator(activity).initiateScan();
            }
        } else {
            new IntentIntegrator(activity).initiateScan();
        }
    }

    private void toast(final String msg) {
        try {
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
            android.util.Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }
}