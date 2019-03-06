package com.eveningoutpost.dexdrip;

// jamorham

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.*;

public class MapsActivity extends /*FragmentActivity*/AppCompatActivity implements OnMapReadyCallback {

    private static final String defaultLocation = "-31.988644,115.515637"; // default bogus position
    private static final String TAG = "jamorham map";
    public static String lastGeoLocation = defaultLocation;
    private static List<Double> longs = new ArrayList<>();
    private static List<Double> lats = new ArrayList<>();
    private static boolean active = false;
    private static GoogleMap mMap;
    private static AppCompatActivity static_activity;

    // receive updates from elsewhere
    public static void newMapLocation(String location, long when) {
        try {
            if ((location != null) && (location.length()>5)) {
                UserError.Log.i(TAG,"New location: "+location);
                try {
                    lastGeoLocation = location;
                    String[] splits = lastGeoLocation.split(",");
                    if (splits.length == 2) {
                        Double thislat = Double.parseDouble(splits[0]);
                        Double thislong = Double.parseDouble(splits[1]);
                        if ((thislat != 0) && (thislong != 0)) {
                            if (!longs.isEmpty()) {
                                if ((longs.get(longs.size() - 1).equals(thislong))
                                        && (lats.get(lats.size() - 1).equals(thislat))) {
                                    return; // dupe
                                }
                            }
                            longs.add(thislong);
                            lats.add(thislat);
                            if (longs.size() > 20) {
                                longs.remove(0);
                                lats.remove(0);
                            }
                            // relay location
                            if (Pref.getBooleanDefaultFalse("plus_follow_master")
                                    && Pref.getBooleanDefaultFalse("plus_follow_geolocation")
                                    && (!Home.get_follower())) {
                                GcmActivity.sendLocation(location);
                            }
                        }
                    }
                    if (active) {
                        static_activity.startActivity(new Intent(xdrip.getAppContext(), MapsActivity.class));
                    }
                } catch (Exception e)
                {
                    UserError.Log.e(TAG,"Got exception with new map location: "+e.toString());
                }
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in newmaplocation: " + e.toString());
        }
    }

    private static void redrawmap() {
        UserError.Log.i(TAG, "Attempting to redraw map: " + lastGeoLocation);
        if (mMap == null) return;
        mMap.clear();

        String[] splits = lastGeoLocation.split(",");
        // sanity check goes here
        LatLng mylocation;
        try {
            mylocation = new LatLng(Double.parseDouble(splits[0]), Double.parseDouble(splits[1]));
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Mylocation number exception: '" + lastGeoLocation + "'");
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            UserError.Log.e(TAG, "Mylocation array index exception: '" + lastGeoLocation + "'");
            return;
        }
            CircleOptions circleOptions = new CircleOptions()
                .center(mylocation)
                .strokeWidth(2)
                .strokeColor(Color.GRAY)
                .radius(1500);

        String title = "";

        if (lastGeoLocation.equals(defaultLocation)) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylocation, 16));
            title = "No location data yet";
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylocation, 13));
        }

        if (!lats.isEmpty()) {
            PolylineOptions mylines = new PolylineOptions();
            for (int c = 0; c < lats.size(); c++) {
                mylines.add(new LatLng(lats.get(c), longs.get(c)));
            }
            mylines.width(1);
            mylines.color(Color.parseColor("#2ba367"));
            mMap.addPolyline(mylines);
        }

        mMap.addCircle(circleOptions);
        mMap.addMarker(new MarkerOptions()
                .position(mylocation)
                .title(title)
                .alpha(0.9f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.jamorham_parakeet_marker)));

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        static_activity = this;
        active = true;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        static_activity = this;
        active = true;
        redrawmap();
    }

    @Override
    public void onPause() {
        active = false;
        static_activity = null;
        super.onPause();
    }
}
