package com.eveningoutpost.dexdrip;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

public class WidgetUpdateService extends Service {
    static Context context;
    private static final String TAG = "WidgetUpdateService";
    private static Class widgetClasses[] = { xDripWidget.class, gearWidget.class };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-widget-bcast", 20000);
            //Log.d(TAG, "onReceive("+intent.getAction()+")");
            if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) updateCurrentBgInfo();
            JoH.releaseWakeLock(wl);
        }
    };

    public WidgetUpdateService() {}
    @Override
    public IBinder onBind(Intent intent) { throw new UnsupportedOperationException("Not yet implemented"); }

    @Override
    public void onCreate() {
        context = getApplicationContext();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Gear widget needs clock ticks all the time to keep time updated in widget
        Log.d(TAG, "enableClockTicks");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(broadcastReceiver, intentFilter);
        updateCurrentBgInfo();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public static void updateCurrentBgInfo() {
        Log.d(TAG, "Sending update flag to widgets");
        int ids[];
        Intent intent;
        //iterate each widget type, get IDs of all instances, update
        for (Class widgetClass : widgetClasses) {
            ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, widgetClass));
            if (ids.length > 0) {
                Log.d(TAG, "Updating " + ids.length + " " + widgetClass.getName() + " instances");
                intent = new Intent(context, widgetClass);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(intent);
            }
        }
    }
}
