package com.eveningoutpost.dexdrip;

import android.content.DialogInterface.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.g5Model.*;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.services.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.eveningoutpost.dexdrip.profileEditor.*;
import com.eveningoutpost.dexdrip.ui.dialog.*;
import com.eveningoutpost.dexdrip.utils.*;
import com.eveningoutpost.dexdrip.wearintegration.*;

import java.util.*;

import static com.eveningoutpost.dexdrip.Home.*;
import static com.eveningoutpost.dexdrip.models.BgReading.*;
import static com.eveningoutpost.dexdrip.xdrip.*;

public class StartNewSensor extends ActivityWithMenu {
    // public static String menu_name = "Start Sensor";
    private static final String TAG = "StartNewSensor";
    private Button button;
    //private DatePicker dp;
    // private TimePicker tp;
    final AppCompatActivity activity = this;
    Calendar ucalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sensor.isActive()) {
            JoH.fixActionBar(this);
            setContentView(R.layout.activity_start_new_sensor);
            button = (Button) findViewById(R.id.startNewSensor);
            //dp = (DatePicker)findViewById(R.id.datePicker);
            //tp = (TimePicker)findViewById(R.id.timePicker);
            addListenerOnButton();
        } else {
            Intent intent = new Intent(this, StopSensor.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public String getMenuName() {
        return getString(R.string.start_sensor);
    }

    public void addListenerOnButton() {
        button = (Button) findViewById(R.id.startNewSensor);

        button.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && DexCollectionType.hasBluetooth()) {
                if (!LocationHelper.locationPermission(StartNewSensor.this)) {
                    JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), gs(R.string.location_permission_needed_to_use_bluetooth), () -> activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0));
                } else {
                    sensorButtonClick();
                }
            } else {
                sensorButtonClick();
            }
        });
    }


    private void sensorButtonClick() {


        ucalendar = Calendar.getInstance();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(gs(R.string.did_you_insert_it_today));
        builder.setMessage(gs(R.string.we_need_to_know_when_the_sensor_was_inserted_to_improve_calculation_accuracy__was_it_inserted_today));
        builder.setPositiveButton(gs(R.string.yes_today), (OnClickListener) (dialog, which) -> {
            dialog.dismiss();
            askSesorInsertionTime();
        });
        builder.setNegativeButton(gs(R.string.not_today), (OnClickListener) (dialog, which) -> {
            dialog.dismiss();
            if (DexCollectionType.hasLibre()) {
                ucalendar.add(Calendar.DAY_OF_MONTH, -1);
                realStartSensor();
            } else {
                final DatePickerFragment datePickerFragment = new DatePickerFragment();
                datePickerFragment.setAllowFuture(false);
                if (!get_engineering_mode()) {
                    datePickerFragment.setEarliestDate(JoH.tsl() - (30L * 24 * 60 * 60 * 1000)); // 30 days
                }
                datePickerFragment.setTitle(gs(R.string.which_day_was_it_inserted));
                datePickerFragment.setDateCallback((year, month, day) -> {
	                ucalendar.set(year, month, day);
	                // Long enough in the past for age adjustment to be meaningless? Skip asking time
	                if ((!get_engineering_mode()) && (JoH.tsl() - ucalendar.getTimeInMillis() > (AGE_ADJUSTMENT_TIME + (1000 * 60 * 60 * 24)))) {
		                realStartSensor();
	                } else {
		                askSesorInsertionTime();
	                }
                });

                datePickerFragment.show(/*activity.*/getSupportFragmentManager(), "DatePicker");
            }
        });
        builder.create().show();
    }

    private void askSesorInsertionTime() {
        final Calendar calendar = Calendar.getInstance();

        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        timePickerFragment.setTitle(gs(R.string.what_time_was_it_inserted));
        timePickerFragment.setTimeCallback(newmins -> {
            int min = newmins % 60;
            int hour = (newmins - min) / 60;
            ucalendar.set(ucalendar.get(Calendar.YEAR), ucalendar.get(Calendar.MONTH), ucalendar.get(Calendar.DAY_OF_MONTH), hour, min);
            if (DexCollectionType.hasLibre()) {
                ucalendar.add(Calendar.HOUR_OF_DAY, -1); // hack for warmup time
            }

            realStartSensor();
        });
        timePickerFragment.show(/*activity.*/getSupportFragmentManager(), "TimePicker");
    }

    private void realStartSensor() {
        if (Ob1G5CollectionService.usingCollector() && Ob1G5StateMachine.usingG6()) {
            G6CalibrationCodeDialog.ask(this, this::realRealStartSensor);
        } else {
            realRealStartSensor();
        }
    }


    public static void startSensorForTime(long startTime) {
        Sensor.create(startTime);
        UserError.Log.ueh("NEW SENSOR", "Sensor started at " + JoH.dateTimeText(startTime));

        JoH.static_toast_long(gs(R.string.new_sensor_started));

        startWatchUpdaterService(xdrip.getAppContext(), WatchUpdaterService.ACTION_SYNC_SENSOR, TAG);

        LibreAlarmReceiver.clearSensorStats();
        // TODO this is just a timer and could be confusing - consider removing this notification
       // JoH.scheduleNotification(xdrip.getAppContext(), "Sensor should be ready", xdrip.getAppContext().getString(R.string.please_enter_two_calibrations_to_get_started), 60 * 130, Home.SENSOR_READY_ID);

        // reverse libre hacky workaround
        Treatments.SensorStart((DexCollectionType.hasLibre() ? startTime + (3600000) : startTime));

        CollectionServiceStarter.restartCollectionServiceBackground();

        Ob1G5StateMachine.startSensor(startTime);
        JoH.clearCache();
        Home.staticRefreshBGCharts();

    }

    private void realRealStartSensor() {
        long startTime = ucalendar.getTime().getTime();
        UserError.Log.i(TAG, "Starting sensor time: " + JoH.dateTimeText(ucalendar.getTime().getTime()));

        if (new Date().getTime() + 15 * 60000 < startTime) {
            Toast.makeText(this, gs(R.string.error_sensor_start_time_in_future), Toast.LENGTH_LONG).show();
            return;
        }

        startSensorForTime(startTime);

        Intent intent;
        if (Pref.getBoolean("store_sensor_location", false) && Experience.gotData()) {
            intent = new Intent(getApplicationContext(), NewSensorLocation.class);
        } else {
            intent = new Intent(getApplicationContext(), Home.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        sensorButtonClick();
                    }
                }
            }
        }
    }

    /*public void oldaddListenerOnButton() {

        button = (Button)findViewById(R.id.startNewSensor);

        button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {

              Calendar calendar = Calendar.getInstance();
              calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(),
              tp.getCurrentHour(), tp.getCurrentMinute(), 0);
              long startTime = calendar.getTime().getTime();

              Sensor.create(startTime);
              UserError.Log.i("NEW SENSOR", "Sensor started at " + startTime);

              Toast.makeText(getApplicationContext(), gs(R.string.new_sensor_started), Toast.LENGTH_LONG).show();
              CollectionServiceStarter.newStart(getApplicationContext());
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
              Intent intent;
              if(prefs.getBoolean("store_sensor_location",true)) {
                  intent = new Intent(getApplicationContext(), NewSensorLocation.class);
              } else {
                  intent = new Intent(getApplicationContext(), Home.class);
              }

              startActivity(intent);
              finish();
          }

        });

    }*/
}
