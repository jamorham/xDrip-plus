package com.eveningoutpost.dexdrip.tables;

import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.drawerlayout.widget.*;

import com.activeandroid.Cache;
import com.eveningoutpost.dexdrip.*;

import java.util.*;


public class SensorDataTable extends BaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Sensor Data Table";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private ArrayList<String> results = new ArrayList<>();
    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.raw_data_list);
    }

    @Override
    protected void onResume(){
        super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
        getData();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    private void getData() {
        Cursor cursor = Cache.openDatabase().rawQuery("Select * from Sensors order by _ID desc", null);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.raw_data_list_item,
                cursor,
                new String[] { "started_at", "latest_battery_level", "uuid", "uuid" },
                new int[] { R.id.raw_data_id, R.id.raw_data_value , R.id.raw_data_slope, R.id.raw_data_timestamp });

ListView listViewRawData = (ListView) findViewById(R.id.listRawData);
listViewRawData.setAdapter(adapter);
//        this.setListAdapter(adapter);
//        ListView listView = (ListView) findViewById(R.id.list);
//        listView.setAdapter(adapter);
    }

}
