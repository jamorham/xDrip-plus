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
    private static final String TAG = "WidgetUpdateService";
    private static Class widgetClasses[] = { xDripWidget.class, gearWidget.class };


    public static void staticRefreshWidgets()
    {
        try {
            Context context = xdrip.getAppContext();
            if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length +
                AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, gearWidget.class)).length > 0) {
                context.startService(new Intent(context, WidgetUpdateService.class));
            }
        } catch (Exception e)
        {
            Log.e(TAG,"Got exception in staticRefreshWidgets: "+e);
        }
    }

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
        super.onCreate();
        //Gear widget needs clock ticks all the time to keep time updated in widget
        Log.d(TAG, "enableClockTicks");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateCurrentBgInfo();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public void updateCurrentBgInfo() {
        Log.d(TAG, "Sending update flag to widgets");
        int ids[];
        Intent intent;
        for (Class widgetClass : widgetClasses) {
            ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), widgetClass));
            if (ids.length > 0) {
                Log.d(TAG, "Updating " + ids.length + " " + widgetClass.getName() + " instances");
                intent = new Intent(this, widgetClass);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);
            }
        }
    }
}
