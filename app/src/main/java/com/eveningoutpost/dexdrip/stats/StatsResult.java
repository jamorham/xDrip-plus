package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import java.text.DecimalFormat;

/**
 * Created by adrian on 23/01/16.
 */
public class StatsResult {

    private final int in;
    private final int below;
    private final int above;
    private final double avg;
    private final boolean mgdl;

    public StatsResult(SharedPreferences settings){

        mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double high = Double.parseDouble(settings.getString("highValue", "170"));
        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
            low *= Constants.MMOLL_TO_MGDL;
        }
        long today = DBSearchUtil.getTodayTimestamp();
        SQLiteDatabase db = Cache.openDatabase();

        Cursor cursor= db.rawQuery("select count(*) from bgreadings  where timestamp >= " + today + " AND calculated_value >= " + low + " AND calculated_value <= " + high + " AND snyced == 0", null);
        cursor.moveToFirst();
        in = cursor.getInt(0);
        cursor.close();

        cursor= db.rawQuery("select count(*) from bgreadings  where timestamp >= " + today + " AND calculated_value > " + DBSearchUtil.CUTOFF + " AND calculated_value < " + low + " AND snyced == 0", null);
        cursor.moveToFirst();
        below = cursor.getInt(0);
        cursor.close();

        cursor= db.rawQuery("select count(*) from bgreadings  where timestamp >= " + today + " AND calculated_value > " + high + " AND snyced == 0", null);
        cursor.moveToFirst();
        above = cursor.getInt(0);
        cursor.close();

        if(getTotalReadings() > 0){
            cursor= db.rawQuery("select avg(calculated_value) from bgreadings  where timestamp >= " + today + " AND calculated_value > " + DBSearchUtil.CUTOFF + " AND snyced == 0", null);
            cursor.moveToFirst();
            avg = cursor.getDouble(0);
            cursor.close();
        } else {
            avg = 0;
        }


    }

    public int getAbove() {
        return above;
    }

    public double getAvg() {
        return avg;
    }

    public int getBelow() {
        return below;
    }

    public int getIn() {
        return in;
    }

    public int getTotalReadings(){
        return in + above + below;
    }

    public String getInPercentage(){
        return "in:" +  ((getTotalReadings()>0)?(in*100/getTotalReadings()) + "%":"-%");
    }

    public String getLowPercentage(){
        return "lo:" +  ((getTotalReadings()>0)?(below*100/getTotalReadings()) + "%":"-%");
    }

    public String getHighPercentage(){
        return "hi:" +  ((getTotalReadings()>0)?(above*100/getTotalReadings()) + "%":"-%");
    }

    public String getA1cDCCT(){
        if(getTotalReadings()==0) return "A1c:?%";
        return "A1c:" + (Math.round(10 * (avg + 46.7) / 28.7) / 10d) + "%";
    }

    public String getA1cIFCC(){
        if(getTotalReadings()==0) return "A1c:?%";
        return "A1c:" + ((int) Math.round(((avg + 46.7) / 28.7 - 2.15) * 10.929));
    }

    public String getAverageUnitised(){
        if(getTotalReadings()==0) return "Avg:?";
        if(mgdl) return "Avg:" + Math.round(avg);
        return "Avg:" + (new DecimalFormat("#.0")).format(avg*Constants.MGDL_TO_MMOLL);
    }

}
