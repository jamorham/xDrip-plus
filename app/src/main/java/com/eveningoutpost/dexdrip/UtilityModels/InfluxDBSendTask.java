package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.os.AsyncTask;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.InfluxDB.InfluxDBUploader;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import java.util.ArrayList;
import java.util.List;

public class InfluxDBSendTask extends AsyncTask<String, Void, Void> {
    private Context context;
    private Exception exception;
    public List<BgSendQueue> bgsQueue = new ArrayList<>();
    public List<CalibrationSendQueue> calibrationsQueue = new ArrayList<>();

    private static final String TAG = MongoSendTask.class.getSimpleName();

    public InfluxDBSendTask(Context pContext) {
        calibrationsQueue = CalibrationSendQueue.influxQueue();
        bgsQueue = BgSendQueue.influxQueue();
        context = pContext;
    }

    public Void doInBackground(String... urls) {
        try {
            InfluxDBUploader influxDBUploader = new InfluxDBUploader(context);
            List<BgReading> bgReadings = new ArrayList<>();
            List<Calibration> calibrations = new ArrayList<>();

            for (CalibrationSendQueue job : calibrationsQueue) {
                calibrations.add(job.calibration);
            }

            for (BgSendQueue job : bgsQueue) {
                bgReadings.add(job.bgReading);
            }

            if (bgReadings.size() > 0 || calibrations.size() > 0) {
                Log.i(TAG, "InfluxDB upload started " + bgReadings.size());

                boolean uploadSuccessful = influxDBUploader.upload(bgReadings, calibrations, calibrations);

                if (uploadSuccessful) {
                    for (CalibrationSendQueue calibration : calibrationsQueue) {
                        calibration.markInfluxSuccess();
                    }

                    for (BgSendQueue bgReading : bgsQueue) {
                        bgReading.markInfluxSuccess();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "caught exception", e);
            this.exception = e;
            return null;
        }
        return null;
    }
}

