package com.eveningoutpost.dexdrip;

import android.*;
import android.content.pm.*;
import android.os.*;
import android.support.wearable.activity.*;
import android.util.*;
import android.view.*;

import androidx.annotation.*;
import androidx.core.app.*;

import com.eveningoutpost.dexdrip.models.*;

//import android.support.v4.os.ResultReceiver;

/**
 * Simple Activity for displaying Permission Rationale to user.
 */
public class LocationPermissionActivity extends WearableActivity {//KS

    private static final String TAG = LocationPermissionActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
       UserError.Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);
        JoH.vibrateNotice();
    }

    public void onClickEnablePermission(View view) {
       UserError.Log.d(TAG, "onClickEnablePermission()");

        // On 23+ (M+) devices, GPS permission not granted. Request permission.
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_FINE_LOCATION);

    }

    /*
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

       UserError.Log.d(TAG, "onRequestPermissionsResult()");

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
               UserError.Log.i(TAG, "onRequestPermissionsResult() granted");
                finish();
            }
        }
    }
}