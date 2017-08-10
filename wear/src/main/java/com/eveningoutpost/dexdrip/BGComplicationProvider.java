package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.google.android.gms.wearable.DataMap;

import static android.support.wearable.complications.ComplicationData.TYPE_LONG_TEXT;
import static android.support.wearable.complications.ComplicationData.TYPE_RANGED_VALUE;
import static android.support.wearable.complications.ComplicationData.TYPE_SHORT_TEXT;

public class BGComplicationProvider extends ComplicationProviderService {
    private MessageReceiver messageReceiver = new MessageReceiver();
    private final static String TAG = BGComplicationProvider.class.getSimpleName();
    protected double sgvLevel;
    protected String sgvString;
    private String rawString;
    private String delta;
    private double datetime;

    @Override
    public void onCreate() {
        super.onCreate();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(Intent.ACTION_SEND));
    }

    @Override
    public void onComplicationUpdate(int complicationId, int complicationType, ComplicationManager complicationManager) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ComplicationData.Builder builder = new ComplicationData.Builder(complicationType);

        if (complicationType == TYPE_RANGED_VALUE) {
            if (prefs.getString("units", "mgdl") == "mmol") {
                builder.setMinValue((float) 2.2).setMaxValue((float) 22.2).
                        setShortTitle(ComplicationText.plainText("mmol/l"));
            } else {
                builder.setMinValue((float) 40.0).setMaxValue((float) 400.0).
                        setShortTitle(ComplicationText.plainText("mg/dl"));
            }

            builder.setValue((float) getSgvLevel());
            complicationManager.updateComplicationData(complicationId, builder.build());
        } else if (complicationType == TYPE_SHORT_TEXT) {
            if (prefs.getString("units", "mgdl") == "mmol") {
                builder.setShortTitle(ComplicationText.plainText("mmol/l"));
            } else {
                builder.setShortTitle(ComplicationText.plainText("mg/dl"));
            }

            builder.setShortText(ComplicationText.plainText(getSgvString()));
            complicationManager.updateComplicationData(complicationId, builder.build());
        } else if (complicationType == TYPE_LONG_TEXT) {
            String value = getSgvString();

            if (getSgvString() != "HIGH" && getSgvString() != "LOW") {
                if (prefs.getString("units", "mgdl") == "mmol") {
                    value += " " + "mmol/l";
                } else {
                    value += " " + "mg/dl";
                }

                value += ", " + getDelta();
            }

            value += ", " + getMinutes();

            builder.setLongText(ComplicationText.plainText(value));
            complicationManager.updateComplicationData(complicationId, builder.build());
        } else {
            Log.e(TAG, "Complication type " + complicationType + " not configured.");
            complicationManager.noUpdateRequired(complicationId);
        }
    }

    private synchronized String getDelta() {
        return delta;
    }

    private String getMinutes() {
        String minutes = "--\'";

        if (getDatetime() != 0) {
            minutes = ((int) Math.floor((System.currentTimeMillis() - getDatetime()) / 60000)) + "\'";
        }

        return minutes;
    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("complication-provider", 60000);

            ProviderUpdateRequester updateRequester = new ProviderUpdateRequester(
                    getApplicationContext(),
                    new ComponentName("com.eveningoutpost.dexdrip", "BGComplicationProvider"));

            try {
                DataMap dataMap;
                Bundle bundle = intent.getBundleExtra("data");

                if (bundle != null) {
                    dataMap = DataMap.fromBundle(bundle);

                    setSgvLevel(dataMap.getDouble("sgvDouble"));
                    setSgvString(dataMap.getString("sgvString"));
                    setRawString(dataMap.getString("rawString"));
                    setDelta(dataMap.getString("delta"));
                    setDatetime(dataMap.getDouble("timestamp"));

                    Log.d(TAG, "CircleWatchface sgv level : " + getSgvLevel());
                    Log.d(TAG, "CircleWatchface sgv string : " + getSgvString());

                    updateRequester.requestUpdateAll();
                }
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }
    }

    private synchronized double getSgvLevel() { return sgvLevel; }
    private synchronized String getSgvString() { return sgvString; }
    private synchronized double getDatetime() { return datetime; }

    private synchronized void setSgvLevel(double sgvLevel) { this.sgvLevel = sgvLevel; }
    private synchronized void setSgvString(String sgvString) { this.sgvString = sgvString; }
    private synchronized void setRawString(String rawString) { this.rawString = rawString; }
    private synchronized void setDelta(String delta) { this.delta = delta; }
    private synchronized void setDatetime(double datetime) { this.datetime = datetime; }
}
