package com.eveningoutpost.dexdrip.tables;


import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.*;
import androidx.drawerlayout.widget.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;

import java.util.*;


public class BgReadingTable extends BaseActivity /*ListActivity*/ implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "BG Data Table";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.OldAppTheme); // or null actionbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.raw_data_list);
    }

    @Override
    protected void onResume() {
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
        final List<BgReading> latest = BgReading.latest(5000);
        ListAdapter adapter = new BgReadingAdapter(this, latest);

        ListView listViewRawData = (ListView) findViewById(R.id.listRawData);
        listViewRawData.setAdapter(adapter);
//        this.setListAdapter(adapter);
    }

    public static class BgReadingCursorAdapterViewHolder {
        TextView raw_data_id;
        TextView raw_data_value;
        TextView raw_data_slope;
        TextView raw_data_timestamp;

        public BgReadingCursorAdapterViewHolder(View root) {
            raw_data_id = (TextView) root.findViewById(R.id.raw_data_id);
            raw_data_value = (TextView) root.findViewById(R.id.raw_data_value);
            raw_data_slope = (TextView) root.findViewById(R.id.raw_data_slope);
            raw_data_timestamp = (TextView) root.findViewById(R.id.raw_data_timestamp);
        }
    }

    public static class BgReadingAdapter extends BaseAdapter {
        private final Context         context;
        private final List<BgReading> readings;

        public BgReadingAdapter(Context context, List<BgReading> readings) {
            this.context = context;
            if(readings == null)
                readings = new ArrayList<>();

            this.readings = readings;
        }

        public View newView(Context context, ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.raw_data_list_item, parent, false);

            final BgReadingCursorAdapterViewHolder holder = new BgReadingCursorAdapterViewHolder(view);
            view.setTag(holder);

            return view;
        }

        void bindView(View view, final Context context, final BgReading bgReading) {
            final BgReadingCursorAdapterViewHolder tag = (BgReadingCursorAdapterViewHolder) view.getTag();
            tag.raw_data_id.setText(BgGraphBuilder.unitized_string_with_units_static(bgReading.calculated_value)
                    + "  " + JoH.qs(bgReading.calculated_value, 1)
                    + " " + (!bgReading.isBackfilled() ? bgReading.slopeArrow() : ""));
            tag.raw_data_value.setText("Aged raw: " + JoH.qs(bgReading.age_adjusted_raw_value, 2));
            tag.raw_data_slope.setText(bgReading.isBackfilled() ? "Backfilled" : "Raw: " + JoH.qs(bgReading.raw_data, 2));
            tag.raw_data_timestamp.setText(new Date(bgReading.timestamp).toString());

            if (bgReading.ignoreForStats) {
                // red invalid/cancelled/overridden
                view.setBackgroundColor(Color.parseColor("#660000"));
            } else {
                // normal grey
                view.setBackgroundColor(Color.parseColor("#212121"));
            }

            view.setLongClickable(true);
            view.setOnLongClickListener(v -> {
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
	                switch (which) {
		                case DialogInterface.BUTTON_POSITIVE:
			                bgReading.ignoreForStats = true;
			                bgReading.save();
			                if (Pref.getBooleanDefaultFalse("wear_sync"))
				                BgReading.pushBgReadingSyncToWatch(bgReading, false);
			                break;

		                case DialogInterface.BUTTON_NEGATIVE:
			                bgReading.ignoreForStats = false;
			                bgReading.save();
			                if (Pref.getBooleanDefaultFalse("wear_sync"))
				                BgReading.pushBgReadingSyncToWatch(bgReading, false);
			                break;
	                }
                };

                AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
                builder.setMessage("Flag reading as \"bad\".\nFlagged readings have no impact on the statistics.").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return true;
            });

        }

        @Override
        public int getCount() {
            return readings.size();
        }

        @Override
        public BgReading getItem(int position) {
            return readings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = newView(context, parent);

            bindView(convertView, context, getItem(position));
            return convertView;
        }
    }
}
