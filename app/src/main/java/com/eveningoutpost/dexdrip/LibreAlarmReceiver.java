
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Forecast;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.ReadingData;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.gson.Gson;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jamorham on 04/09/2016.
 */
public class LibreAlarmReceiver extends BroadcastReceiver {


    private static final String TAG = "jamorham librereceiver";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static final boolean use_raw = true;
    private static SharedPreferences prefs;
    private static long oldest = -1;
    private static long newest = -1;
    private static long oldest_cmp = -1;
    private static long newest_cmp = -1;
    private static final Object lock = new Object();
    private static long sensorAge = 0;

    public static void clearSensorStats() {
        Home.setPreferencesInt("nfc_sensor_age", 0); // reset for nfc sensors
        sensorAge = 0;
    }

    private static double convert_for_dex(int lib_raw_value) {
        return (lib_raw_value * 117.64705); // to match (raw/8.5)*1000
    }

    private static void createBGfromGD(GlucoseData gd, boolean quick) {
        final double converted;
        if (gd.glucoseLevelRaw > 0) {
            converted = convert_for_dex(gd.glucoseLevelRaw);
        } else {
            converted = 12; // RF error message - might be something else like unconstrained spline
        }
        if (gd.realDate > 0) {
            //   Log.d(TAG, "Raw debug: " + JoH.dateTimeText(gd.realDate) + " raw: " + gd.glucoseLevelRaw + " converted: " + converted);
            if ((newest_cmp == -1) || (oldest_cmp == -1) || (gd.realDate < oldest_cmp) || (gd.realDate > newest_cmp)) {
                if (BgReading.readingNearTimeStamp(gd.realDate) == null) {
                    BgReading.create(converted, converted, xdrip.getAppContext(), gd.realDate, quick); // quick lite insert
                    if ((gd.realDate < oldest) || (oldest == -1)) oldest = gd.realDate;
                    if ((gd.realDate > newest) || (newest == -1)) newest = gd.realDate;
                } else {
                    if (d)
                        Log.d(TAG, "Ignoring duplicate timestamp for: " + JoH.dateTimeText(gd.realDate));
                }
            } else {
                Log.d(TAG, "Already processed from date range: " + JoH.dateTimeText(gd.realDate));
            }
        } else {
            Log.e(TAG, "Fed a zero or negative date");
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("librealarm-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.d(TAG, "LibreReceiver onReceiver: " + intent.getAction());
                        JoH.benchmark(null);
                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        //  BundleScrubber.scrub(bundle);
                        final String action = intent.getAction();


                        if ((bundle != null) && (debug)) {
                            for (String key : bundle.keySet()) {
                                Object value = bundle.get(key);
                                if (value != null) {
                                    Log.d(TAG, String.format("%s %s (%s)", key,
                                            value.toString(), value.getClass().getName()));
                                }
                            }
                        }

                        switch (action) {
                            case Intents.LIBRE_ALARM_TO_XDRIP_PLUS:

                                // If we are not currently in a mode supporting libre then switch
                                if (!DexCollectionType.hasLibre()) {
                                    DexCollectionType.setDexCollectionType(DexCollectionType.LibreAlarm);
                                }

                                if (bundle == null) break;

                                Log.d(TAG, "Receiving LIBRE_ALARM broadcast");

                                oldest_cmp = oldest;
                                newest_cmp = newest;

                                final String data = bundle.getString("data");
                                final int bridge_battery = bundle.getInt("bridge_battery");
                                if (bridge_battery > 0)
                                    Home.setPreferencesInt("bridge_battery", bridge_battery);

                                try {
                                    ReadingData.TransferObject object =
                                            new Gson().fromJson(data, ReadingData.TransferObject.class);
                                    processReadingDataTransferObject(object);
                                } catch (Exception e) {
                                    Log.wtf(TAG, "Could not process data structure from LibreAlarm: " + e.toString());
                                    JoH.static_toast_long("LibreAlarm data format appears incompatible!? protocol changed?");

                                }
                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                break;
                        }
                    } finally {
                        JoH.benchmark("LibreReceiver process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }

    public static void processReadingDataTransferObject(ReadingData.TransferObject object) {
        // insert any recent data we can
        final List<GlucoseData> mTrend = object.data.trend;
        if (mTrend != null) {
            Collections.sort(mTrend);
            final long thisSensorAge = mTrend.get(mTrend.size() - 1).sensorTime;
            sensorAge = Home.getPreferencesInt("nfc_sensor_age", 0);
            if (thisSensorAge > sensorAge) {
                sensorAge = thisSensorAge;
                Home.setPreferencesInt("nfc_sensor_age", (int) sensorAge);
                Home.setPreferencesBoolean("nfc_age_problem", false);
                Log.d(TAG, "Sensor age advanced to: " + thisSensorAge);
            } else if (thisSensorAge == sensorAge) {
                Log.wtf(TAG, "Sensor age has not advanced: " + sensorAge);
                JoH.static_toast_long("Sensor clock has not advanced!");
                Home.setPreferencesBoolean("nfc_age_problem", true);
                return; // do not try to insert again
            } else {
                Log.wtf(TAG, "Sensor age has gone backwards!!! " + sensorAge);
                JoH.static_toast_long("Sensor age has gone backwards!!");
                sensorAge = thisSensorAge;
                Home.setPreferencesInt("nfc_sensor_age", (int) sensorAge);
                Home.setPreferencesBoolean("nfc_age_problem", true);
            }
            for (GlucoseData gd : mTrend) {
                Log.d(TAG, "DEBUG: sensor time: " + gd.sensorTime);

                if (use_raw) {
                    createBGfromGD(gd, false); // not quick for recent
                } else {
                    BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, false);
                }
            }
        }
        // munge and insert the history data if any is missing
        final List<GlucoseData> mHistory = object.data.history;
        if ((mHistory != null) && (mHistory.size() > 1)) {
            Collections.sort(mHistory);

            final List<Double> polyxList = new ArrayList<Double>();
            final List<Double> polyyList = new ArrayList<Double>();
            for (GlucoseData gd : mHistory) {
                if (d)
                    Log.d(TAG, "history : " + JoH.dateTimeText(gd.realDate) + " " + gd.glucose(true));
                polyxList.add((double) gd.realDate);
                if (use_raw) {
                    polyyList.add((double) gd.glucoseLevelRaw);
                    createBGfromGD(gd, true);
                } else {
                    polyyList.add((double) gd.glucoseLevel);
                    // add in the actual value
                    BgReading.bgReadingInsertFromInt(gd.glucoseLevel, gd.realDate, false);
                }

            }

            //ConstrainedSplineInterpolator splineInterp = new ConstrainedSplineInterpolator();
            SplineInterpolator splineInterp = new SplineInterpolator();

            try {
                PolynomialSplineFunction polySplineF = splineInterp.interpolate(
                        Forecast.PolyTrendLine.toPrimitiveFromList(polyxList),
                        Forecast.PolyTrendLine.toPrimitiveFromList(polyyList));

                final long startTime = mHistory.get(0).realDate;
                final long endTime = mHistory.get(mHistory.size() - 1).realDate;

                for (long ptime = startTime; ptime <= endTime; ptime += 300000) {
                    if (d)
                        Log.d(TAG, "Spline: " + JoH.dateTimeText((long) ptime) + " value: " + (int) polySplineF.value(ptime));
                    if (use_raw) {
                        createBGfromGD(new GlucoseData((int) polySplineF.value(ptime), ptime), true);
                    } else {
                        BgReading.bgReadingInsertFromInt((int) polySplineF.value(ptime), ptime, false);
                    }
                }
            } catch (org.apache.commons.math3.exception.NonMonotonicSequenceException e) {
                Log.e(TAG, "NonMonotonicSequenceException: " + e);
            }

        } else {
            Log.e(TAG, "no librealarm history data");
        }

    }
}
