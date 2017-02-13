package com.eveningoutpost.dexdrip.InfluxDB;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class InfluxDBUploader {
    private static final int SOCKET_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final String TAG = InfluxDBUploader.class.getSimpleName();
    private SharedPreferences prefs;
    private Context mContext;
    private String dbName;
    private String dbUri;
    private String dbUser;
    private String dbPassword;
    private OkHttpClient.Builder client;

    public InfluxDBUploader(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        dbName = prefs.getString("cloud_storage_influxdb_database", null);
        dbUri  = prefs.getString("cloud_storage_influxdb_uri", null);
        dbUser = prefs.getString("cloud_storage_influxdb_username", null);
        dbPassword = prefs.getString("cloud_storage_influxdb_password", null);

        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        HttpUrl url = HttpUrl.parse(dbUri);
                        String fullPath = url.encodedPath() + chain.request().url().encodedPath();
                        HttpUrl.Builder fixedUrl = chain.request().url().newBuilder().encodedPath(fullPath);
                        return chain.proceed(chain.request().newBuilder().url(fixedUrl.build()).build());
                    }});
    }

    public boolean upload(List<BgReading> glucoseDataSets, List<Calibration> meterRecords, List<Calibration> calRecords) {
        BatchPoints batchPoints = BatchPoints
                .database(dbName)
                .retentionPolicy("autogen")
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();


        for (BgReading record : glucoseDataSets) {
            if (record == null) {
                Log.e(TAG, "InfluxDB glucose record is null");
                continue;
            }
            batchPoints.point(createGlucosePoint(record));
        }

        for (Calibration record : meterRecords) {
            if (record == null) {
                Log.e(TAG, "InfluxDB meter record is null");
                continue;
            }
            batchPoints.point(createMeterPoint(record));
        }

        for (Calibration record : calRecords) {
            if (record == null) {
                Log.e(TAG, "InfluxDB calibration record is null");
                continue;
            }
            if (record.slope == 0d) continue;
            batchPoints.point(createCalibrationPoint(record));
        }

        try {
            InfluxDBFactory.connect(dbUri, dbUser, dbPassword, client).write(batchPoints);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Write to InfluxDB failed", e);
            return false;
        }
    }

    private Point createGlucosePoint(BgReading record) {
        BigDecimal delta = new BigDecimal(record.currentSlope() * 5 * 60 * 1000)
                .setScale(3, BigDecimal.ROUND_HALF_UP);

        return Point.measurement("glucose")
                .time(record.getEpochTimestamp(), TimeUnit.MILLISECONDS)
                .addField("value_mmol", record.calculated_value_mmol())
                .addField("value_mgdl", record.getMgdlValue())
                .addField("direction", record.slopeName())
                .addField("filtered", record.ageAdjustedFiltered() * 1000)
                .addField("unfiltered", record.usedRaw() * 1000)
                .addField("rssi", 100)
                .addField("noise", record.noiseValue())
                .addField("delta", delta)
                .build();
    }

    private Point createMeterPoint(Calibration record) {
        return Point.measurement("meter")
                .time(record.timestamp, TimeUnit.MILLISECONDS)
                .tag("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"))
                .tag("type", "mbg")
                .addField("mbg", record.bg)
                .build();
    }

    private Point createCalibrationPoint(Calibration record) {
        Point.Builder builder = Point.measurement("calibration")
                .time(record.timestamp, TimeUnit.MILLISECONDS)
                .tag("device", "xDrip-" + prefs.getString("dex_collection_method", "BluetoothWixel"))
                .tag("type", "cal");

        if (record.check_in) {
            builder.addField("slope", record.first_slope)
                    .addField("intercept", record.first_intercept)
                    .addField("scale", record.first_scale);
        } else {
            builder.addField("slope", (1000 / record.slope))
                    .addField("intercept", ((record.intercept * -1000) / record.slope))
                    .addField("scale", 1);
        }

        return builder.build();
    }
}
