package com.eveningoutpost.dexdrip;

import android.content.*;
import android.os.*;
import android.widget.*;

import com.eveningoutpost.dexdrip.importedLibraries.dexcom.*;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.utils.*;

public class CalibrationCheckInActivity extends ActivityWithMenu {
    public static String menu_name = "Check in calibration";
   Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_check_in);
        addListenerOnButton();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button = (Button) findViewById(R.id.check_in_calibrations);

        button.setOnClickListener(v -> {

            if (Sensor.isActive()) {
                SyncingService.startActionCalibrationCheckin(getApplicationContext());
                Toast.makeText(getApplicationContext(), "Checked in all calibrations", Toast.LENGTH_LONG).show();
                Intent tableIntent = new Intent(v.getContext(), Home.class);
                startActivity(tableIntent);
                finish();
            } else {
                UserError.Log.i("CALIBRATION", "ERROR, sensor not active");
            }
        });

    }
}
