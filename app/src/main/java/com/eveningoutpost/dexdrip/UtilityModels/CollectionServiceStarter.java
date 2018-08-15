package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.DailyIntentService;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.DoNothingService;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.Services.WifiCollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Calendar;

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.Medtrum;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getCollectorServiceClass;

/**
 * Created by Emma Black on 12/22/14.
 */
public class CollectionServiceStarter {
    private Context mContext;

    private final static String TAG = CollectionServiceStarter.class.getSimpleName();
    final public static String pref_run_wear_collector = "run_wear_collector"; // only used on wear but here for code compatibility

    public static boolean isFollower(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("dex_collection_method", "").equals("Follower");
    }

    public static boolean isWifiandBTWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("WifiBlueToothWixel") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isWifiandBTLibre(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("LimiTTerWifi") == 0) {
            return true;
        }
        return false;
    }

    
    // are we in the specifc mode supporting wifi and dexbridge at the same time
    public static boolean isWifiandDexBridge()
    {
        return (DexCollectionType.getDexCollectionType() == DexCollectionType.WifiDexBridgeWixel);
    }

    // are we in any mode which supports dexbridge
    public static boolean isDexBridgeOrWifiandDexBridge()
    {
        return isWifiandDexBridge() || isDexbridgeWixel(xdrip.getAppContext());
    }

    public static boolean isBTWixelOrLimiTTer(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        return isBTWixelOrLimiTTer(collection_method);
    }

    public static boolean isBTWixelOrLimiTTer(String collection_method) {
        return collection_method.equals("BluetoothWixel")
                || collection_method.equals("LimiTTer");
    }

    public static boolean isDexbridgeWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexbridgeWixel") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isDexbridgeWixel(String collection_method) {
        return collection_method.equals("DexbridgeWixel");
    }

    public static boolean isBTShare(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomShare") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isBTShare(String collection_method) {
        return collection_method.equals("DexcomShare");
    }

    public static boolean isBTG5(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomG5") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isBTG5(String collection_method) {
        return collection_method.equals("DexcomG5");
    }

    public static boolean isWifiWixel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("WifiWixel") == 0) {
            return true;
        }
        return false;
    }

    public static boolean isWifiLibre(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("LibreWifi") == 0) {
            return true;
        }
        return false;
    }

        /*
     * LimiTTer emulates a BT-Wixel and works with the BT-Wixel service.
     * It would work without any changes but in some cases knowing that the data does not
     * come from a Dexcom sensor but from a Libre sensor might enhance the performance.
     * */

    public static boolean isLimitter() {
        return Pref.getStringDefaultBlank("dex_collection_method").equals("LimiTTer");
    }
    
    public static boolean isWifiandBTLibre() {
        return Pref.getStringDefaultBlank("dex_collection_method").equals("LimiTTerWifi");
    }
    

    public static boolean isWifiWixel(String collection_method) {
        return collection_method.equals("WifiWixel") || DexCollectionType.getDexCollectionType() == DexCollectionType.Mock;
    }
    
    public static boolean isWifiLibre(String collection_method) {
        return collection_method.equals("LibreWifi") || DexCollectionType.getDexCollectionType() == DexCollectionType.Mock;
    }
    

    public static boolean isFollower(String collection_method) {
        return collection_method.equals("Follower");
    }

    private static void newStart(final Context context) {
        new CollectionServiceStarter(context).start(context);
    }

    public void stopAll() {
        stopBtShareService();
        stopBtWixelService();
        stopWifWixelThread();
        stopFollowerThread();
        stopG5Service();
        JoH.stopService(getCollectorServiceClass(Medtrum));
    }

    public void start(Context context, String collection_method) {
        this.mContext = context;
        xdrip.checkAppContext(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        if (isBTWixelOrLimiTTer(collection_method) || isDexbridgeWixel(collection_method)) {
            Log.d("DexDrip", "Starting bt wixel collector");
            stopWifWixelThread();
            stopBtShareService();
            stopFollowerThread();
            stopG5Service();

            if (prefs.getBoolean("wear_sync", false)) {//KS
                boolean enable_wearG5 = prefs.getBoolean("enable_wearG5", false);
                boolean force_wearG5 = prefs.getBoolean("force_wearG5", false);
                this.mContext.startService(new Intent(context, WatchUpdaterService.class));
                if (!enable_wearG5 || (enable_wearG5 && !force_wearG5)) { //don't start if Wear G5 Collector Service is active
                    startBtWixelService();
                }
            }
            else {
                startBtWixelService();
            }
        } else if (isWifiWixel(collection_method) || isWifiLibre(collection_method)) {
            Log.d("DexDrip", "Starting wifi wixel collector");
            stopBtWixelService();
            stopFollowerThread();
            stopBtShareService();
            stopG5Service();

            startWifWixelThread();
        } else if (isBTShare(collection_method)) {
            Log.d("DexDrip", "Starting bt share collector");
            stopBtWixelService();
            stopFollowerThread();
            stopWifWixelThread();
            stopG5Service();

            if (prefs.getBoolean("wear_sync", false)) {//KS
                boolean enable_wearG5 = prefs.getBoolean("enable_wearG5", false);
                boolean force_wearG5 = prefs.getBoolean("force_wearG5", false);
                this.mContext.startService(new Intent(context, WatchUpdaterService.class));
                if (!enable_wearG5 || (enable_wearG5 && !force_wearG5)) { //don't start if Wear G5 Collector Service is active
                    startBtShareService();
                }
            }
            else {
                startBtShareService();
            }

        } else if (isBTG5(collection_method)) {
            Log.d(TAG, "Starting G5 collector");
            stopBtWixelService();
            stopWifWixelThread();
            stopBtShareService();

            if (prefs.getBoolean("wear_sync", false)) {//KS
                boolean enable_wearG5 = prefs.getBoolean("enable_wearG5", false);
                boolean force_wearG5 = prefs.getBoolean("force_wearG5", false);
                this.mContext.startService(new Intent(context, WatchUpdaterService.class));
                if (!enable_wearG5 || (enable_wearG5 && !force_wearG5)) { //don't start if Wear G5 Collector Service is active
                    startBtG5Service();
                } else {
                    Log.d(TAG, "Not starting because of force wear");
                }
            }
            else {
                startBtG5Service();
            }

        } else if (isWifiandBTWixel(context) || isWifiandDexBridge() || isWifiandBTLibre(context)) {
            Log.d("DexDrip", "Starting wifi and bt wixel collector");
            stopBtWixelService();
            stopFollowerThread();
            stopWifWixelThread();
            stopBtShareService();
            stopG5Service();

            // start both
            Log.d("DexDrip", "Starting wifi wixel collector first");
            startWifWixelThread();
            Log.d("DexDrip", "Starting bt wixel collector second");
            if (prefs.getBoolean("wear_sync", false)) {//KS
                boolean enable_wearG5 = prefs.getBoolean("enable_wearG5", false);
                boolean force_wearG5 = prefs.getBoolean("force_wearG5", false);
                this.mContext.startService(new Intent(context, WatchUpdaterService.class));
                if (!enable_wearG5 || (enable_wearG5 && !force_wearG5)) { //don't start if Wear G5 Collector Service is active
                    startBtWixelService();
                }
            }
            else {
                startBtWixelService();
            }
            Log.d("DexDrip", "Started wifi and bt wixel collector");
        } else if (isFollower(collection_method)) {
            stopWifWixelThread();
            stopBtShareService();
            stopBtWixelService();
            stopG5Service();

            startFollowerThread();
        } else {
            if (DexCollectionType.hasBluetooth()) {
                Log.d("DexDrip","Starting service based on collector lookup");
                JoH.startService(DexCollectionType.getCollectorServiceClass());
            }
        }

        if (prefs.getBoolean("broadcast_to_pebble", false) && (PebbleUtil.getCurrentPebbleSyncType() != 1)) {
            startPebbleSyncService();
        }

        startSyncService();
        startDailyIntentService();
        Log.d(TAG, collection_method);


    }

    public void start(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");

        start(context, collection_method);
    }

    // private constructer, use static methods to start
    private CollectionServiceStarter(Context context) {
        if (context == null) context = xdrip.getAppContext();
        this.mContext = context;
    }

    private static void restartCollectionService() {
        restartCollectionService(xdrip.getAppContext());
    }

    public static void restartCollectionServiceBackground() {
        Inevitable.task("restart-collection-service",500,() -> restartCollectionService(xdrip.getAppContext()));
    }


    public static void restartCollectionService(Context context) {
        if (context == null) context = xdrip.getAppContext();
        final CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopAll();
        Inevitable.task("restart-collection-service-start", 1000, () -> collectionServiceStarter.start(xdrip.getAppContext()));
    }

    public static void restartCollectionService(Context context, String collection_method) {
        Log.d(TAG, "restartCollectionService: " + collection_method);
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopAll();
        collectionServiceStarter.start(context, collection_method);
    }

    public static void startBtService(Context context) {
        Log.d(TAG, "startBtService: " + DexCollectionType.getDexCollectionType());
        stopBtService(context);
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        switch (DexCollectionType.getDexCollectionType()) {
            case DexcomShare:
                collectionServiceStarter.startBtShareService();
                break;
            case DexcomG5:
                collectionServiceStarter.startBtG5Service();
                break;
            case Medtrum:
                JoH.startService(getCollectorServiceClass(Medtrum));
            default:
                collectionServiceStarter.startBtWixelService();
                break;
        }
    }

    public static void stopBtService(Context context) {
        Log.d(TAG, "stopBtService call stopService");
        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(context);
        collectionServiceStarter.stopBtShareService();
        collectionServiceStarter.stopBtWixelService();
        collectionServiceStarter.stopG5Service();
        Log.d(TAG, "stopBtService should have called onDestroy");
    }

    private void startBtWixelService() {
        Log.d(TAG, "starting bt wixel service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mContext.startService(new Intent(this.mContext, DexCollectionService.class));
        }
    }

    private void stopBtWixelService() {
        Log.d(TAG, "stopping bt wixel service");
        this.mContext.stopService(new Intent(this.mContext, DexCollectionService.class));
    }

    private void startBtShareService() {
        Log.d(TAG, "starting bt share service");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mContext.startService(new Intent(this.mContext, DexShareCollectionService.class));
        }
    }

    private void startBtG5Service() {
        Log.d(TAG,"stopping G5 service");
        stopG5Service();
        Log.d(TAG, "starting G5 service");
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
       if (!Pref.getBooleanDefaultFalse(Ob1G5CollectionService.OB1G5_PREFS)) {
           G5CollectionService.keep_running = true;
           this.mContext.startService(new Intent(this.mContext, G5CollectionService.class));
       } else {
           Ob1G5CollectionService.keep_running = true;
           this.mContext.startService(new Intent(this.mContext, Ob1G5CollectionService.class));
       }
        //}
    }

    private void startPebbleSyncService() {
        Log.d(TAG, "starting PebbleWatchSync service");
        this.mContext.startService(new Intent(this.mContext, PebbleWatchSync.class));
    }

    private void startSyncService() {
        Log.d(TAG, "starting Sync service");
        this.mContext.startService(new Intent(this.mContext, SyncService.class));
    }

    private void startDailyIntentService() {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        final PendingIntent pi = PendingIntent.getService(this.mContext, 0, new Intent(this.mContext, DailyIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        final AlarmManager am = (AlarmManager) this.mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    private void stopBtShareService() {
        Log.d(TAG, "stopping bt share service");
        this.mContext.stopService(new Intent(this.mContext, DexShareCollectionService.class));
    }

    private void startWifWixelThread() {
        Log.d(TAG, "starting wifi wixel service");
        this.mContext.startService(new Intent(this.mContext, WifiCollectionService.class));
    }

    private void stopWifWixelThread() {
        Log.d(TAG, "stopping wifi wixel service");
        this.mContext.stopService(new Intent(this.mContext, WifiCollectionService.class));
    }

    private void startFollowerThread() {
        Log.d(TAG, "starting follower service");
        this.mContext.startService(new Intent(this.mContext, DoNothingService.class));
        if (Home.get_follower()) GcmActivity.requestPing();
    }

    private void stopFollowerThread() {
        Log.d(TAG, "stopping follower service");
        this.mContext.stopService(new Intent(this.mContext, DoNothingService.class));
    }

    private void stopG5Service() {
        Log.d(TAG, "stopping G5  service");
        G5CollectionService.keep_running = false; // ensure zombie stays down
        this.mContext.stopService(new Intent(this.mContext, G5CollectionService.class));
        Ob1G5CollectionService.keep_running = false; // ensure zombie stays down
        this.mContext.stopService(new Intent(this.mContext, Ob1G5CollectionService.class));
        Ob1G5CollectionService.resetSomeInternalState();
    }

}
