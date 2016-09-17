package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.Models.BgReading;//KS
import com.eveningoutpost.dexdrip.Models.Calibration;//KS
import com.eveningoutpost.dexdrip.Models.Sensor;//KS
import com.eveningoutpost.dexdrip.Services.G5CollectionService;//KS

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.text.SimpleDateFormat;//KS
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * Created by stephenblack on 12/26/14.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String OPEN_SETTINGS = "/openwearsettings";
    private static final String SYNC_DB_PATH = "/syncweardb";//KS
    private static final String WEARABLE_CALIBRATION_DATA_PATH = "/nightscout_watch_cal_data";//KS
    private static final String WEARABLE_SENSOR_DATA_PATH = "/nightscout_watch_sensor_data";//KS
    private static final String ACTION_SYNC_DB = "com.eveningoutpost.dexdrip.wearintegration.SyncDB";//KS
    private static final String ACTION_RESEND = "com.dexdrip.stephenblack.nightwatch.RESEND_DATA";
    private static final String ACTION_SENDDATA = "com.dexdrip.stephenblack.nightwatch.SEND_DATA";
    private static final String ACTION_RESEND_BULK = "com.dexdrip.stephenblack.nightwatch.RESEND_BULK_DATA";
    private static final String FIELD_SENDPATH = "field_xdrip_plus_sendpath";
    private static final String FIELD_PAYLOAD = "field_xdrip_plus_payload";
    private static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    private static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String TAG = "jamorham listener";
    SharedPreferences mPrefs;//KS
    Context mContext;//KS
    public static boolean mLocationPermissionApproved;//KS
    public static long last_send_previous = 0;//KS

    GoogleApiClient googleApiClient;
    private static long lastRequest = 0;

    public class DataRequester extends AsyncTask<Void, Void, Void> {
        Context mContext;
        final String path;
        final byte[] payload;

        DataRequester(Context context, String thispath, byte[] thispayload) {
            mContext = context;
            path = thispath;
            payload = thispayload;
            Log.d(TAG, "DataRequester: " + thispath);
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//KS
            boolean connectG5 = sharedPrefs.getBoolean("connectG5", false); //KS

            if (googleApiClient.isConnected()) {
                if (!path.equals(ACTION_RESEND) || (System.currentTimeMillis() - lastRequest > 20 * 1000)) { // enforce 20-second debounce period
                    lastRequest = System.currentTimeMillis();

                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    int count = nodes.getNodes().size();//KS
                    Log.d(TAG, "doInBackground connected.  NodeApi.GetConnectedNodesResult await count=" + count);//KS
                    if (count > 0) {//KS
                        if (connectG5)
                            stopBtG5Service();

                        for (Node node : nodes.getNodes()) {
                            Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, payload);
                            if (connectG5) {//KS
                                DataMap datamap = getWearBGData(36);//KS resendData for last 3 hours
                                while (datamap != null) {
                                    Log.d(TAG, "doInBackground send getWearBGData BGs to phone");
                                    Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), SYNC_DB_PATH, datamap.toByteArray());
                                    datamap = getWearBGData(36);
                                }
                            }
                        }
                    }
                    else {
                        if (connectG5) {//KS
                            Log.d(TAG, "doInBackground connected but getConnectedNodes returns 0.  start G5 service");
                            startBtG5Service();
                        }
                    }
                } else {
                    Log.d(TAG, "Debounce limit hit - not sending");
                }
            } else {
                Log.d(TAG, "Not connected for sending");
                googleApiClient.connect();
            }
            return null;
        }
    }

    private DataMap getWearBGData(int count) {//KS
        java.text.DateFormat df = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
        Date date = new Date();
        if(googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) { googleApiConnect(); }
        long currentTime = new Date().getTime() - (60000 * 60 * 24);

        date.setTime(last_send_previous);
        Log.d(TAG, "getWearBGData last_send_previous:" + df.format(date));

        BgReading last_bg = BgReading.last();
        if (last_bg != null) {
            date.setTime(last_bg.timestamp);
            Log.d(TAG, "getWearBGData last_bg.timestamp:" + df.format(date));
        }

        if (last_bg != null && last_send_previous <= last_bg.timestamp) {//startTime
            date.setTime(last_bg.timestamp);
            Log.d(TAG, "getWearBGData last_send_previous < last_bg.timestamp:" + df.format(date));
            List<BgReading> graph_bgs = BgReading.latestForGraphAsc(count, last_send_previous);
            if (!graph_bgs.isEmpty()) {
                Log.d(TAG, "getWearBGData graph_bgs count = " + graph_bgs.size());
                DataMap entries = dataMap(last_bg);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (BgReading bg : graph_bgs) {
                    dataMaps.add(dataMap(bg));
                    date.setTime(bg.timestamp);
                    Log.d(TAG, "getWearBGData bg.timestamp:" + df.format(date));
                    last_send_previous = bg.timestamp + 1;
                    date.setTime(last_send_previous);
                    Log.d(TAG, "getWearBGData set last_send_previous:" + df.format(date));
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                return entries;
            }
            else
                Log.d(TAG, "getWearBGData graph_bgs count = 0");
        }
        return null;
    }

    private DataMap dataMap(BgReading bg) {//KS
        DataMap dataMap = new DataMap();
        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    public void requestData() {
        sendData(WEARABLE_RESEND_PATH, null);
    }

    public void sendData(String path, byte[] payload) {
        if (path == null) return;
        new DataRequester(this, path, payload).execute();
    }

    public void googleApiConnect() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//KS
        listenForChangeInSettings();//KS
        mContext = getApplicationContext();//KS
        if (intent != null && ACTION_RESEND.equals(intent.getAction())) {
            googleApiConnect();
            requestData();
        } else if (intent != null && ACTION_SENDDATA.equals(intent.getAction())) {
            final Bundle bundle = intent.getExtras();
            sendData(bundle.getString(FIELD_SENDPATH), bundle.getByteArray(FIELD_PAYLOAD));
        }
        return START_STICKY;
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {//KS
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if(key.compareTo("connectG5") == 0) {
                Log.i("listenerService5", "connectG changed!");
                if (!mPrefs.getBoolean("connectG5", false)) {
                    Log.d(TAG, "connectG5 changed to false. Stop G5 Collector.");
                    stopBtG5Service();
                }
                else {
                    Log.d(TAG, "prefListener connectG5 changed to true.");
                }
            }
        }
    };

    public void listenForChangeInSettings() {//KS
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);
        // TODO do we need an unregister!?
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {


                String path = event.getDataItem().getUri().getPath();
                if (path.equals(OPEN_SETTINGS)) {
                    //TODO: OpenSettings
                    Intent intent = new Intent(this, NWPreferences.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } else if (path.equals(WEARABLE_DATA_PATH)) {

                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    setWearSharedPrefs(dataMap);//KS Set Preferences with transmitter id, etc.

                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra("data", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                } else if (path.equals(WEARABLE_TREATMENT_PAYLOAD)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent intent = new Intent(getApplicationContext(), Simulation.class);
                    intent.putExtra(WEARABLE_TREATMENT_PAYLOAD, dataMap.toBundle());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                } else if (path.equals(WEARABLE_TOAST_NOTIFICATON)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent intent = new Intent(getApplicationContext(), Simulation.class);
                    intent.putExtra(path, dataMap.toBundle());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                } else if (path.equals(SYNC_DB_PATH)) {//KS
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH=" + path);
                    //Sensor.DeleteAndInitDb(getApplicationContext());//KS TODO test
                } else if (path.equals(WEARABLE_SENSOR_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncSensorData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_CALIBRATION_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncCalibrationData(dataMap);
                }
            }
        }
    }

    public void setWearSharedPrefs(DataMap dataMap) {//KS
        Log.d(TAG, "onDataChanged dataMap=" + dataMap);

        String dex_txid = dataMap.getString("dex_txid", "ABCDEF");//KS 4023GU
        Log.d(TAG, "onDataChanged dataMap dex_txid=" + dex_txid);

        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefs.putString("dex_txid", dex_txid);
        prefs.commit();
        String pref_txid = mPrefs.getString("dex_txid", "NOTFOUND");//KS TEST
        Log.d(TAG, "onDataChanged prefs set dex_txid=" + pref_txid);

        String units = dataMap.getString("units", "ABCDEF");//KS
        Log.d(TAG, "onDataChanged dataMap units=" + units);
        prefs.putString("units", units);
        prefs.commit();
        Log.d(TAG, "onDataChanged prefs units=" + mPrefs.getString("units", ""));

        Double high = dataMap.getDouble("high");
        Double low = dataMap.getDouble("low");
        Log.d(TAG, "onDataChanged dataMap highMark=" + high + " highMark=" + low);
        prefs.putString("highValue", high.toString());
        prefs.putString("lowValue", low.toString());
        prefs.commit();
        //Double highMark = Double.parseDouble(mPrefs.getString("highValue", "0"));
        //Double lowMark = Double.parseDouble(mPrefs.getString("lowValue", "0"));
        //Log.d(TAG, "onDataChanged prefs highMark=" + highMark + " highMark=" + lowMark);
    }

    public void syncSensorData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncSensorData");
        java.text.DateFormat df = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
        Date date = new Date();
        if (dataMap != null) {

            //Get Transmittor ID required to connect to sensor via Bluetooth
            String dex_txid = dataMap.getString("dex_txid", "ABCDEF");//KS 4023GU
            Log.d(TAG, "onDataChanged dataMap dex_txid=" + dex_txid);
            SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
            prefs.putString("dex_txid", dex_txid);
            prefs.commit();
            Log.d(TAG, "onDataChanged prefs set dex_txid=" + mPrefs.getString("dex_txid", "ABCDEF"));

            String uuid = dataMap.getString("uuid");
            Log.d(TAG, "syncSensorData add Sensor for uuid=" + uuid);
            long started_at = dataMap.getLong("started_at");
            Integer latest_battery_level = dataMap.getInt("latest_battery_level");
            String sensor_location = dataMap.getString("sensor_location");
            Sensor.InitDb(context);//ensure database has already been initialized
            if (uuid != null && !uuid.isEmpty()) {
                date.setTime(started_at);
                Log.d(TAG, "syncSensorData add Sensor for uuid=" + uuid + " timestamp=" + started_at + " timeString=" + df.format(date));
                Sensor sensor = Sensor.getByUuid(uuid);
                if (sensor == null) {
                    Log.d(TAG, "syncSensorData create new Sensor...");
                    Sensor newsensor = Sensor.create(started_at, uuid);
                    if (newsensor != null) {
                        Log.i(TAG, "syncSensorData Created Sensor with uuid=" + uuid + " started at=" + started_at);
                        Sensor.updateBatteryLevel(newsensor, latest_battery_level);
                        Sensor.updateSensorLocation(sensor_location);
                    } else
                        Log.e(TAG, "syncSensorData Failed to create new Sensor for uuid=" + uuid);
                } else
                    Log.d(TAG, "syncSensorData Sensor already exists with uuid=" + uuid);
            }
        }
    }

    public void syncCalibrationData(DataMap dataMap) {//KS
        Log.d(TAG, "syncCalibrationData");
        java.text.DateFormat df = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
        Date date = new Date();
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();
            Log.d(TAG, "syncCalibrationData add Calibration Table entries count=" + entries.size());
            //if (entries.size() > 0)
            //    Calibration.clear_all_existing_calibrations();
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                for (DataMap entry : entries) {
                    if (entry != null) {
                        Log.d(TAG, "syncCalibrationData add Calibration Table entry=" + entry);
                        Calibration calibration = new Calibration();

                        Calibration uuidexists = Calibration.getByTimestamp(entry.getLong("timestamp"));
                        if (uuidexists == null) {
                            date.setTime(entry.getLong("timestamp"));
                            Log.d(TAG, "syncCalibrationData create Calibration for uuid=" + entry.getString("uuid") + " timestamp=" + entry.getLong("timestamp") + " timeString=" + df.format(date));
                            Calibration.createCalibration(entry.getDouble("adjusted_raw_value"),
                                    entry.getDouble("bg"),
                                    entry.getBoolean("check_in"),
                                    entry.getDouble("distance_from_estimate"),
                                    entry.getDouble("estimate_bg_at_time_of_calibration"),
                                    entry.getDouble("estimate_raw_at_time_of_calibration"),
                                    entry.getDouble("first_decay"),
                                    entry.getDouble("first_intercept"),
                                    entry.getDouble("first_scale"),
                                    entry.getDouble("first_slope"),
                                    entry.getDouble("intercept"),
                                    entry.getBoolean("possible_bad"),
                                    entry.getLong("raw_timestamp"),
                                    entry.getDouble("raw_value"),
                                    entry.getDouble("second_decay"),
                                    entry.getDouble("second_intercept"),
                                    entry.getDouble("second_scale"),
                                    entry.getDouble("second_slope"),
                                    sensor,
                                    entry.getDouble("sensor_age_at_time_of_estimation"),
                                    entry.getDouble("sensor_confidence"),
                                    entry.getString("sensor_uuid"),
                                    entry.getDouble("slope"),
                                    entry.getDouble("slope_confidence"),
                                    entry.getLong("timestamp"),
                                    entry.getString("uuid"));
                        }
                        else
                            Log.d(TAG, "syncCalibrationData Calibration already exists for uuid=" + entry.getString("uuid") + " timestamp=" + entry.getLong("timestamp"));
                    }
                }
            }

            //KS Debug only
            List<Calibration> cals = Calibration.latest(6);//1 days worth 12 * 24hr  OR Long.MAX_VALUE for all records
            if (cals != null && !cals.isEmpty()) {
                Log.d(TAG, "syncCalibrationData cals in wear db count = " + cals.size());
                for (Calibration cal : cals) {
                    String json = cal.toS();
                    Log.d(TAG, "syncCalibrationData cal: " + json);
                }
            }
        }
    }

    private void startBtG5Service() {//KS
        Log.d(TAG, "startBtG5Service");
        Context myContext = getApplicationContext();
        if (checkLocationPermissions()) {
            Log.d(TAG, "startBtG5Service start G5CollectionService");
            myContext.startService(new Intent(myContext, G5CollectionService.class));
            Log.d(TAG, "startBtG5Service AFTER startService G5CollectionService mLocationPermissionApproved " + mLocationPermissionApproved);
        }
    }

    private boolean checkLocationPermissions() {
        Context myContext = getApplicationContext();
        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        myContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermissions  mLocationPermissionApproved:" + mLocationPermissionApproved);

        // Display Activity to get user permission
        if (!mLocationPermissionApproved) {
            Intent permissionIntent = new Intent(getApplicationContext(), LocationPermissionActivity.class);
            permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissionIntent);
        }
        // Enables app to handle 23+ (M+) style permissions.
        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermissions mLocationPermissionApproved:" + mLocationPermissionApproved);
        return mLocationPermissionApproved;
    }

    private void stopBtG5Service() {//KS
        Log.d(TAG, "stopBtG5Service");
        if (mLocationPermissionApproved) {
            Context myContext = getApplicationContext();
            Log.d(TAG, "stopBtG5Service call stopService");
            myContext.stopService(new Intent(myContext, G5CollectionService.class));
            Log.d(TAG, "stopBtG5Service should have called onDestroy");
        }
    }

    public static void requestData(Context context) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_RESEND);
        context.startService(intent);
    }

    // generic send data
    public static void SendData(Context context, String path, byte[] payload) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_SENDDATA);
        intent.putExtra(FIELD_SENDPATH, path);
        intent.putExtra(FIELD_PAYLOAD, payload);
        context.startService(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        requestData();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        if (googleApiClient != null) {
            Wearable.MessageApi.removeListener(googleApiClient, this);
        }
    }
}
