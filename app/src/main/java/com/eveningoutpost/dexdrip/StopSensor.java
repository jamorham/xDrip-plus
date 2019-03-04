package com.eveningoutpost.dexdrip;

import android.content.DialogInterface.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.g5Model.*;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.eveningoutpost.dexdrip.calibrations.*;
import com.eveningoutpost.dexdrip.utils.*;

import static com.eveningoutpost.dexdrip.xdrip.*;

public class StopSensor extends ActivityWithMenu {
   public Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Sensor.isActive()) {
            Intent intent = new Intent(this, StartNewSensor.class);
            startActivity(intent);
            finish();
        } else {
            JoH.fixActionBar(this);
            setContentView(R.layout.activity_stop_sensor);
            button = (Button)findViewById(R.id.stop_sensor);
            addListenerOnButton();
        }
    }

    @Override
    public String getMenuName() {
        return getString(R.string.stop_sensor);
    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.stop_sensor);

        button.setOnClickListener(v -> {
            stop();
            JoH.startActivity(Home.class);
            finish();
        });
    }

    public synchronized static void stop() {
        Sensor.stopSensor();
        Inevitable.task("stop-sensor",1000, Sensor::stopSensor);
        AlertPlayer.getPlayer().stopAlert(xdrip.getAppContext(), true, false);

        JoH.static_toast_long(gs(R.string.sensor_stopped));
        JoH.clearCache();
        LibreAlarmReceiver.clearSensorStats();
        PluggableCalibration.invalidateAllCaches();

        Ob1G5StateMachine.stopSensor();

        CollectionServiceStarter.restartCollectionServiceBackground();
        Home.staticRefreshBGCharts();
    }

    public void resetAllCalibrations(View v) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(gs(R.string.are_you_sure));
        builder.setMessage(gs(R.string.do_you_want_to_delete_and_reset_the_calibrations_for_this_sensor));

        builder.setNegativeButton("No", (OnClickListener) (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("Yes", (OnClickListener) (dialog, which) -> {
            Calibration.invalidateAllForSensor();
            dialog.dismiss();
            finish();
        });
        builder.create().show();


    }
}
