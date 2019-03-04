package com.eveningoutpost.dexdrip;

import android.content.*;
import android.os.*;
import android.text.*;
import android.widget.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.eveningoutpost.dexdrip.calibrations.*;
import com.eveningoutpost.dexdrip.utils.*;

import static com.eveningoutpost.dexdrip.xdrip.*;

public class CalibrationOverride extends ActivityWithMenu {
    Button button;
    private static final String TAG = "OverrideCalib";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(CollectionServiceStarter.isBTShare(getApplicationContext())) {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_calibration_override);
        addListenerOnButton();
    }

    @Override
    public String getMenuName() {
        return getString(R.string.override_calibration);
    }

    public void addListenerOnButton() {
            button = (Button) findViewById(R.id.save_calibration_button);

        button.setOnClickListener(v -> {
            if (Sensor.isActive()) {
                EditText value = (EditText) findViewById(R.id.bg_value);
                String string_value = value.getText().toString();
                if (!TextUtils.isEmpty(string_value)) {
                    try {
                        final double calValue = JoH.tolerantParseDouble(string_value);

                        final Calibration last_calibration = Calibration.lastValid();
                        if (last_calibration == null) {
                            Log.wtf(TAG, "Last valid calibration is null when trying to cancel it in override!");
                        } else {
                            last_calibration.sensor_confidence = 0;
                            last_calibration.slope_confidence = 0;
                            last_calibration.save();
                            CalibrationSendQueue.addToQueue(last_calibration, getApplicationContext());
                            // TODO we need to push the nixing of this last calibration
                        }

                        final Calibration calibration = Calibration.create(calValue, getApplicationContext());
                        if (calibration != null) {
                            UndoRedo.addUndoCalibration(calibration.uuid);
                            GcmActivity.pushCalibration(string_value, "0");
                           // Ob1G5StateMachine.addCalibration((int)calibration.bg, calibration.timestamp);
                            NativeCalibrationPipe.addCalibration((int)calibration.bg, calibration.timestamp);
                            //startWatchUpdaterService(v.getContext(), WatchUpdaterService.ACTION_SYNC_CALIBRATION, TAG);

                        } else {
                            Log.e(TAG, "Calibration creation resulted in null");
                            JoH.static_toast_long("Could not create calibration!");
                        }
                        Intent tableIntent = new Intent(v.getContext(), Home.class);
                        startActivity(tableIntent);
                        finish();
                    } catch (NumberFormatException e) {
                        value.setError(gs(R.string.number_error_) + e);
                    }
                } else {
                    value.setError("Calibration Can Not be blank");
                }
            } else {
                Log.w("Calibration", "ERROR, no active sensor");
            }
        });

    }
}
