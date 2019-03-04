package com.eveningoutpost.dexdrip.utils;

import android.annotation.*;
import android.appwidget.*;
import android.content.*;
import android.content.DialogInterface.*;
import android.content.res.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.*;
import android.widget.*;

import androidx.appcompat.app.*;
import androidx.preference.*;

import com.eveningoutpost.dexdrip.*;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.services.*;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.eveningoutpost.dexdrip.utilitymodels.pebble.*;
import com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface.*;
import com.eveningoutpost.dexdrip.calibrations.*;
import com.eveningoutpost.dexdrip.cgm.nsfollow.*;
import com.eveningoutpost.dexdrip.insulin.inpen.*;
import com.eveningoutpost.dexdrip.profileEditor.*;
import com.eveningoutpost.dexdrip.tidepool.*;
import com.eveningoutpost.dexdrip.ui.*;
import com.eveningoutpost.dexdrip.watch.lefun.*;
import com.eveningoutpost.dexdrip.wearintegration.*;
import com.eveningoutpost.dexdrip.webservices.*;
import com.google.zxing.integration.android.*;
import com.nightscout.core.barcode.*;

import net.tribe7.common.base.*;

import java.net.*;
import java.text.*;
import java.util.*;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class Preferences extends PreferenceActivity {
    private static final String TAG = "jamorham PREFS";
    private static byte[] staticKey;
    private AllPrefsFragment preferenceFragment;

    private static Preference units_pref;
    private static String static_units;
    private static Preference profile_insulin_sensitivity_default;
    private static Preference profile_carb_ratio_default;

    private static ListPreference locale_choice;
    private static Preference force_english;
    private static Preference nfc_expiry_days;

    private static AllPrefsFragment pFragment;



    private void refreshFragments() {
        this.preferenceFragment = new AllPrefsFragment();
        pFragment = this.preferenceFragment;
        getFragmentManager().
        /*getSupportFragmentManager().*/beginTransaction().replace(android.R.id.content,
                this.preferenceFragment).commit();
    }


    public interface OnServiceTaskCompleted {
        void onTaskCompleted(byte[] result);
    }

    public class ServiceCallback implements OnServiceTaskCompleted {
        @Override
        public void onTaskCompleted(byte[] result) {
            if (result.length > 0) {
                if ((staticKey == null) || (staticKey.length != 16)) {
                    toast("Error processing security key");
                } else {
                    byte[] plainbytes = JoH.decompressBytesToBytes(CipherUtils.decryptBytes(result, staticKey));
                    staticKey = null;
                    Log.d(TAG, "Plain bytes size: " + plainbytes.length);
                    if (plainbytes.length > 0) {
                        SdcardImportExport.storePreferencesFromBytes(plainbytes, getApplicationContext());
                    } else {
                        toast("Error processing data - empty");
                    }
                }
            } else {
                toast("Error processing settings - no data - try again?");
            }
        }
    }


    private void toast(final String msg) {
        try {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
            android.util.Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }

    private void installxDripPlusPreferencesFromQRCode(SharedPreferences prefs, String data) {
        Log.d(TAG, "installing preferences from QRcode");
        try {
            Map<String, String> prefsmap = DisplayQRCode.decodeString(data);
            if (prefsmap != null) {
                if (prefsmap.containsKey(getString(R.string.all_settings_wizard))) {
                    if (prefsmap.containsKey(getString(R.string.wizard_key))
                            && prefsmap.containsKey(getString(R.string.wizard_uuid))) {
                        staticKey = CipherUtils.hexToBytes(prefsmap.get(getString(R.string.wizard_key)));

                        new WebAppHelper(new ServiceCallback()).executeOnExecutor(xdrip.executor, getString(R.string.wserviceurl) + "/joh-getsw/" + prefsmap.get(getString(R.string.wizard_uuid)));
                    } else {
                        Log.d(TAG, "Incorrectly formatted wizard pref");
                    }
                    return;
                }

                final SharedPreferences.Editor editor = prefs.edit();
                int changes = 0;
                for (Map.Entry<String, String> entry : prefsmap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    //            Log.d(TAG, "Saving preferences: " + key + " = " + value);
                    if (value.equals("true") || (value.equals("false"))) {
                        editor.putBoolean(key, Boolean.parseBoolean(value));
                        changes++;
                    } else if (!value.equals("null")) {
                        editor.putString(key, value);
                        changes++;
                    }
                }
                editor.apply();
                refreshFragments();
                ExtraLogTags.readPreference(Pref.getStringDefaultBlank("extra_tags_for_logging"));
                Toast.makeText(getApplicationContext(), "Loaded " + changes + " preferences from QR code", Toast.LENGTH_LONG).show();
                PlusSyncService.clearandRestartSyncService(getApplicationContext());
                DesertSync.settingsChanged(); // refresh
                if (prefs.getString("dex_collection_method", "").equals("Follower")) {
                    PlusSyncService.clearandRestartSyncService(getApplicationContext());
                    GcmActivity.last_sync_request = 0;
                    GcmActivity.requestBGsync();
                }
            } else {
                android.util.Log.e(TAG, "Got null prefsmap during decode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception installing preferences");
        }

    }


    public static Boolean getBooleanPreferenceViaContextWithoutException(Context context, String key, Boolean defaultValue) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);

        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (scanResult == null || scanResult.getContents() == null) {
            return;
        }
        if (scanResult.getFormatName().equals("QR_CODE")) {

            String scanresults = scanResult.getContents();
            if (scanresults.startsWith(DisplayQRCode.qrmarker)) {
                installxDripPlusPreferencesFromQRCode(prefs, scanresults);
                return;
            }

            final NSBarcodeConfig barcode = new NSBarcodeConfig(scanresults);
            if (barcode.hasMongoConfig()) {
                if (barcode.getMongoUri().isPresent()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("cloud_storage_mongodb_uri", barcode.getMongoUri().get());
                    editor.putString("cloud_storage_mongodb_collection", barcode.getMongoCollection().or("entries"));
                    editor.putString("cloud_storage_mongodb_device_status_collection", barcode.getMongoDeviceStatusCollection().or("devicestatus"));
                    editor.putBoolean("cloud_storage_mongodb_enable", true);
                    editor.apply();
                }
                if (barcode.hasApiConfig()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("cloud_storage_api_enable", true);
                    editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                    editor.apply();
                } else {
                    prefs.edit().putBoolean("cloud_storage_api_enable", false).apply();
                }
            }
            if (barcode.hasApiConfig()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_api_enable", true);
                editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                editor.apply();
            } else {
                prefs.edit().putBoolean("cloud_storage_api_enable", false).apply();
            }

            if (barcode.hasMqttConfig()) {
                if (barcode.getMqttUri().isPresent()) {
                    URI uri = URI.create(barcode.getMqttUri().or(""));
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (userInfo.length == 2) {
                            String endpoint = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
                            if (!userInfo[0].isEmpty() && !userInfo[1].isEmpty()) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("cloud_storage_mqtt_endpoint", endpoint);
                                editor.putString("cloud_storage_mqtt_user", userInfo[0]);
                                editor.putString("cloud_storage_mqtt_password", userInfo[1]);
                                editor.putBoolean("cloud_storage_mqtt_enable", true);
                                editor.apply();
                            }
                        }
                    }
                }
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_mqtt_enable", false);
                editor.apply();
            }
        } else if (scanResult.getFormatName().equals("CODE_128")) {
            Log.d(TAG, "Setting serial number to: " + scanResult.getContents());
            prefs.edit().putString("share_key", scanResult.getContents()).apply();
        }
        refreshFragments();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            setTheme(R.style.OldAppTheme);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set theme");
        }
        super.onCreate(savedInstanceState);

        refreshFragments();
        processExtraData();

        // cannot be in onResume as we display dialog to set
        try {
            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceFragment.lockListener.prefListener);
        } catch (Exception e) {
            Log.e(TAG,"Got exception registering lockListener: "+e+ " "+(preferenceFragment.lockListener == null));
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener);
 //TODO       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && DexCollectionType.hasBluetooth()) {
 //           LocationHelper.requestLocationForBluetooth(this); // double check!
 //       }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(LeFunEntry.prefListener);
    }

    @Override
    protected void onPause()
    {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener);
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(LeFunEntry.prefListener);
        pFragment = null;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        try {
            PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(preferenceFragment.lockListener.prefListener);
        } catch (Exception e) {
            //
        }
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.pref_general);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AllPrefsFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        setIntent(intent);
        if (!processExtraData()) {
            super.onNewIntent(intent);
        }
    }

    private boolean processExtraData() {
        final Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final String str = bundle.getString("refresh");
                if (str != null) {
                    refreshProfileRatios();
                    return true;
                }
            }
        }
        return false;
    }

    private static void refreshProfileRatios() {
        profile_carb_ratio_default.setTitle(format_carb_ratio(profile_carb_ratio_default.getTitle().toString(), ProfileEditor.minMaxCarbs(ProfileEditor.loadData(false))));
        profile_insulin_sensitivity_default.setTitle(format_insulin_sensitivity(profile_insulin_sensitivity_default.getTitle().toString(), ProfileEditor.minMaxSens(ProfileEditor.loadData(false))));
    }

    private static void restartPebble() {
        xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), PebbleWatchSync.class));
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

        } else if (preference instanceof RingtonePreference) {
            // For ringtone preferences, look up the correct display value
            // using RingtoneManager.
            if (TextUtils.isEmpty(stringValue)) {
                // Empty values correspond to 'silent' (no ringtone).
                preference.setSummary(R.string.pref_ringtone_silent);

            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(
                        preference.getContext(), Uri.parse(stringValue));

                if (ringtone == null) {
                    // Clear the summary if there was a lookup error.
                    preference.setSummary(null);
                } else {
                    // Set the summary to reflect the new ringtone display
                    // name.
                    String name = ringtone.getTitle(preference.getContext());
                    preference.setSummary(name);
                }
            }

        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    };
    private static Preference.OnPreferenceChangeListener sBindNumericPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();
        if (isNumeric(stringValue)) {
            preference.setSummary(stringValue);
            return true;
        }
        return false;
    };
    private static Preference.OnPreferenceChangeListener sBindPreferenceTitleAppendToValueListenerUpdateChannel = (preference, value) -> {

        boolean do_update = false;
        // detect not first run
        if (preference.getTitle().toString().contains("(")) {
            do_update = true;
        }

        preference.setTitle(preference.getTitle().toString().replaceAll("  \\([a-z0-9A-Z]+\\)$", "") + "  (" + value.toString() + ")");
        if (do_update) {
            preference.getEditor().putString(preference.getKey(), value.toString()).apply(); // update prefs now
            UpdateActivity.last_check_time = -2;
            UpdateActivity.checkForAnUpdate(preference.getContext());
        }
        return true;
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceTitleAppendToIntegerValueListener = (preference, value) -> {

        boolean do_update = false;
        // detect not first run
        if (preference.getTitle().toString().contains("(")) {
            do_update = true;
        }

        preference.setTitle(preference.getTitle().toString().replaceAll("  \\([a-z0-9A-Z]+\\)$", "") + "  (" + value.toString() + ")");
        if (do_update) {
            preference.getEditor().putInt(preference.getKey(), (int)value).apply(); // update prefs now
        }
        return true;
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceTitleAppendToStringValueListener = (preference, value) -> {

        boolean do_update = false;
        // detect not first run
        if (preference.getTitle().toString().contains("(")) {
            do_update = true;
        }

        preference.setTitle(preference.getTitle().toString().replaceAll("  \\([a-z0-9A-Z.]+\\)$", "") + "  (" + value.toString() + ")");
        if (do_update) {
            preference.getEditor().putString(preference.getKey(), (String)value).apply(); // update prefs now
        }
        return true;
    };



    private static String format_carb_ratio(String oldValue, String newValue) {
        return oldValue.replaceAll(" \\(.*\\)$", "") + "  (" + newValue + "g per Unit)";
    }

    private static String format_carb_absorption_rate(String oldValue, String newValue) {
        return oldValue.replaceAll(" \\(.*\\)$", "") + "  (" + newValue + "g per hour)";
    }

    private static String format_insulin_sensitivity(String oldValue, String newValue) {
        try {
            return oldValue.replaceAll("  \\(.*\\)$", "") + "  (" + newValue + " " + static_units + " per U)";
        } catch (Exception e) {
            return "ERROR - Invalid number";
        }
    }

    /*private static void do_format_insulin_sensitivity(Preference preference, SharedPreferences prefs, boolean from_change, String newValue) {
        if (newValue == null) {
            newValue = prefs.getString("profile_insulin_sensitivity_default", "54");
        }
        try {
            Profile.setSensitivityDefault(Double.parseDouble(newValue));
        } catch (Exception e) {
            Log.e(TAG, "Invalid insulin sensitivity: " + newValue);
        }

        EditTextPreference thispref = (EditTextPreference) preference;
                thispref.setText(newValue);
        if (from_change) {
            preference.getEditor().putString("profile_insulin_sensitivitiy", newValue);
        }

        preference.setTitle(format_insulin_sensitivity(preference.getTitle().toString(), newValue));
    }*/


    private static void bindPreferenceSummaryToValue(Preference preference) {
        try {
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference summary: " + e.toString());
        }
    }

    private static void bindPreferenceTitleAppendToValueUpdateChannel(Preference preference) {
        try {
            preference.setOnPreferenceChangeListener(sBindPreferenceTitleAppendToValueListenerUpdateChannel);
            sBindPreferenceTitleAppendToValueListenerUpdateChannel.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference title: " + e.toString());
        }
    }

    private static void bindPreferenceTitleAppendToStringValue(Preference preference) {
        try {
            preference.setOnPreferenceChangeListener(sBindPreferenceTitleAppendToStringValueListener);
            sBindPreferenceTitleAppendToStringValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference title: " + e.toString());
        }
    }


    private static void bindPreferenceTitleAppendToIntegerValue(Preference preference) {
        try {
            preference.setOnPreferenceChangeListener(sBindPreferenceTitleAppendToIntegerValueListener);
            sBindPreferenceTitleAppendToIntegerValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getInt(preference.getKey(), 0));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference title: " + e.toString());
        }
    }

    private static void bindPreferenceTitleAppendToIntegerValueFromLogSlider(Preference preference, NamedSliderProcessor ref, String name, boolean unitize) {

        final Preference.OnPreferenceChangeListener listener = (preference1, value) -> {

            boolean do_update = false;
            // detect not first run
            if (preference1.getTitle().toString().contains("(")) {
                do_update = true;
            }
            final int result = ref.interpolate(name, (int)value);

            preference1.setTitle(preference1.getTitle().toString().replaceAll("  \\([a-z0-9A-Z \\.]+\\)$", "") + "  (" + (unitize ? BgGraphBuilder.unitized_string_static_no_interpretation_short(result) : result) + ")");
            if (do_update) {
                preference1.getEditor().putInt(preference1.getKey(), (int) value).apply(); // update prefs now
            }
            return true;
        };

        try {
            preference.setOnPreferenceChangeListener(listener);
            listener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getInt(preference.getKey(), 0));
        } catch (Exception e) {
            Log.e(TAG, "Got exception binding preference title: " + e.toString());
        }
    }




    private static void bindPreferenceSummaryToValueAndEnsureNumeric(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindNumericPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    public static class AllPrefsFragment extends PreferenceFragment {

        SharedPreferences prefs;

        public LockScreenWallPaper.PrefListener lockListener = new LockScreenWallPaper.PrefListener();

        private void setSummary(String pref_name) {
     /*       try {
                // is there a cleaner way to bind these values when setting programatically?
                final String pref_val = this.prefs.getString(pref_name, "");
                findPreference(pref_name).setSummary(pref_val);
                EditTextPreference thispref = (EditTextPreference) findPreference(pref_name);
                thispref.setText(pref_val);
            } catch (Exception e) {
                Log.e(TAG, "Exception during setSummary: " + e.toString());
            }
            */
            setSummary_static(this, pref_name);
        }

        private static void setSummary_static(AllPrefsFragment allPrefsFragment, String pref_name) {
            try {
                // is there a cleaner way to bind these values when setting programatically?
                final String pref_val = allPrefsFragment.prefs.getString(pref_name, "");
                allPrefsFragment.findPreference(pref_name).setSummary(pref_val);
                EditTextPreference thispref = (EditTextPreference) allPrefsFragment.findPreference(pref_name);
                thispref.setText(pref_val);
            } catch (Exception e) {
                Log.e(TAG, "Exception during setSummary: " + e.toString());
            }
        }



        @SuppressLint("ApplySharedPref")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            static_units = this.prefs.getString("units", "mgdl");
            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("highValue"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("lowValue"));
            units_pref = findPreference("units");
            bindPreferenceSummaryToValue(units_pref);

            addPreferencesFromResource(R.xml.pref_notifications);
            bindPreferenceSummaryToValue(findPreference("bg_alert_profile"));
            bindPreferenceSummaryToValue(findPreference("calibration_notification_sound"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("calibration_snooze"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("bg_unclear_readings_minutes"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("disable_alerts_stale_data_minutes"));
            bindPreferenceSummaryToValue(findPreference("falling_bg_val"));
            bindPreferenceSummaryToValue(findPreference("rising_bg_val"));
            bindPreferenceSummaryToValue(findPreference("other_alerts_sound"));
            bindPreferenceSummaryToValue(findPreference("bridge_battery_alert_level"));

            addPreferencesFromResource(R.xml.pref_data_source);

            addPreferencesFromResource(R.xml.pref_data_sync);
            setupBarcodeConfigScanner();
            setupBarcodeShareScanner();
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_uri"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_device_status_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_api_base"));

            addPreferencesFromResource(R.xml.pref_advanced_settings);
            addPreferencesFromResource(R.xml.xdrip_plus_prefs);

            bindPreferenceSummaryToValue(findPreference("persistent_high_threshold_mins"));
            bindPreferenceSummaryToValue(findPreference("persistent_high_repeat_mins"));

            bindPreferenceTitleAppendToValueUpdateChannel(findPreference("update_channel"));



            profile_insulin_sensitivity_default = findPreference("profile_insulin_sensitivity_default");
            profile_carb_ratio_default = findPreference("profile_carb_ratio_default");
            refreshProfileRatios();

            nfc_expiry_days = findPreference("nfc_expiry_days");
            locale_choice = (ListPreference) findPreference("forced_language");
            force_english = findPreference("force_english");


            update_force_english_title("");
            force_english.setOnPreferenceChangeListener((preference, newValue) -> {
                prefs.edit().putBoolean("force_english", (boolean) newValue).commit();
                SdcardImportExport.hardReset();
                return true;
            });
            locale_choice.setOnPreferenceChangeListener((preference, newValue) -> {
                prefs.edit().putString("forced_language", (String) newValue).commit();
                update_force_english_title((String)newValue);
                if (prefs.getBoolean("force_english", false)) {
                    SdcardImportExport.hardReset();
                }
                return true;
            });

            findPreference("disable_all_sync").setOnPreferenceChangeListener((preference, newValue) -> {
                prefs.edit().putBoolean("disable_all_sync", (boolean) newValue).commit();
                SdcardImportExport.hardReset();
                return true;
            });

            // this gets cached in a static final field at the moment so needs hard reset
            findPreference("g5-battery-warning-level").setOnPreferenceChangeListener((preference, newValue) -> {
                prefs.edit().putString("g5-battery-warning-level", (String) newValue).commit();
                G5BaseService.resetTransmitterBatteryStatus();
                SdcardImportExport.hardReset();
                return true;
            });

            findPreference("use_ob1_g5_collector_service").setOnPreferenceChangeListener((preference, newValue) -> {
                new Thread(() -> {
	                try {
		                Thread.sleep(1000);
	                } catch (InterruptedException e) {
		                //
	                }
	                CollectionServiceStarter.restartCollectionService(xdrip.getAppContext());
                }).start();

                return true;
            });

            final Preference profile_carb_absorption_default = findPreference("profile_carb_absorption_default");
            profile_carb_absorption_default.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!isNumeric(newValue.toString())) {
                    return false;
                }
                preference.setTitle(format_carb_absorption_rate(preference.getTitle().toString(), newValue.toString()));
                Profile.reloadPreferences(AllPrefsFragment.this.prefs);
                Home.staticRefreshBGCharts();
                return true;
            });

            profile_carb_absorption_default.setTitle(format_carb_absorption_rate(profile_carb_absorption_default.getTitle().toString(), this.prefs.getString("profile_carb_absorption_default", "")));


            refresh_extra_items();
            findPreference("plus_extra_features").setOnPreferenceChangeListener((preference, newValue) -> {
                Home.invalidateMenu = true; // force redraw
                refresh_extra_items();

                return true;
            });

            final Preference crash_reports = findPreference("enable_crashlytics");
            crash_reports.setOnPreferenceChangeListener((preference, newValue) -> {
                Toast.makeText(preference.getContext(),
                        "Crash Setting takes effect on next restart", Toast.LENGTH_LONG).show();
                return true;
            });

            bindTTSListener();
            final Preference collectionMethod = findPreference("dex_collection_method");
            final Preference runInForeground = findPreference("run_service_in_foreground");
            final Preference g5nonraw = findPreference("g5_non_raw_method");
            final Preference g5extendedsut = findPreference("g5_extended_sut");
            final Preference scanConstantly = findPreference("run_ble_scan_constantly");
            final Preference runOnMain = findPreference("run_G5_ble_tasks_on_uithread");
            final Preference reAuth = findPreference("always_get_new_keys");
            final Preference reBond = findPreference("always_unbond_G5");
            final Preference wifiRecievers = findPreference("wifi_recievers_addresses");
            final Preference predictiveBG = findPreference("predictive_bg");
            final Preference interpretRaw = findPreference("interpret_raw");
            final Preference bfappid = findPreference("bugfender_appid");
            final Preference nfcSettings = findPreference("xdrip_plus_nfc_settings");
            final Preference bluereadersettings = findPreference("xdrip_blueReader_advanced_settings");
            //DexCollectionType collectionType = DexCollectionType.getType(findPreference("dex_collection_method").)

            final ListPreference currentCalibrationPlugin = (ListPreference)findPreference("current_calibration_plugin");


            final Preference shareKey = findPreference("share_key");
            shareKey.setOnPreferenceClickListener(preference -> {
                AllPrefsFragment.this.prefs.edit().remove("dexcom_share_session_id").apply();
                return true;
            });

            Preference.OnPreferenceChangeListener shareTokenResettingListener = (preference, newValue) -> {
                AllPrefsFragment.this.prefs.edit().remove("dexcom_share_session_id").apply();
                return true;
            };

            final Preference sharePassword = findPreference("dexcom_account_password");
            sharePassword.setOnPreferenceChangeListener(shareTokenResettingListener);
            final Preference shareAccountName = findPreference("dexcom_account_name");
            shareAccountName.setOnPreferenceChangeListener(shareTokenResettingListener);

            final Preference tidepoolTestLogin = findPreference("tidepool_test_login");
            tidepoolTestLogin.setOnPreferenceClickListener(preference -> {
                Inevitable.task("tidepool-upload", 200, TidepoolUploader::doLoginFromUi);
                return false;
            });

            final Preference tidePoolType = findPreference("tidepool_dev_servers");
            tidePoolType.setOnPreferenceChangeListener((preference, newValue) -> {
                    TidepoolUploader.resetInstance();
                    return true;
            });

            final Preference nsFollowDownload = findPreference("nsfollow_download_treatments");
            final Preference nsFollowUrl = findPreference("nsfollow_url");
            try {
                nsFollowUrl.setOnPreferenceChangeListener((preference, newValue) -> {
                    NightscoutFollow.resetInstance();
                    return true;
                });
            } catch (Exception e) {
                //
            }

            final Preference inpen_enabled = findPreference("inpen_enabled");
            try {
                inpen_enabled.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (boolean) newValue) {
                        LocationHelper.requestLocationForBluetooth((AppCompatActivity) preference.getContext()); // double check!
                    }
                    InPenEntry.startWithRefresh();
                    return true;
                });
            } catch (Exception e) {
                //
            }



            final Preference scanShare = findPreference("scan_share2_barcode");
            final EditTextPreference transmitterId = (EditTextPreference) findPreference("dex_txid");
           // final Preference closeGatt = findPreference("close_gatt_on_ble_disconnect");

            final Preference pebbleSync2 = findPreference("broadcast_to_pebble_type");
            final Preference pebbleSync1 = findPreference("broadcast_to_pebble");

            // Pebble Trend - START
            final Preference watchIntegration = findPreference("watch_integration");
            final PreferenceCategory watchCategory = (PreferenceCategory) findPreference("pebble_integration");
            //final ListPreference pebbleType = (ListPreference) findPreference("watch_integration");
            final Preference pebbleTrend = findPreference("pebble_display_trend");
            final Preference pebbleFilteredLine = findPreference("pebble_filtered_line");
            final Preference pebbleHighLine = findPreference("pebble_high_line");
            final Preference pebbleLowLine = findPreference("pebble_low_line");
            final Preference pebbleTrendPeriod = findPreference("pebble_trend_period");
            final Preference pebbleDelta = findPreference("pebble_show_delta");
            final Preference pebbleDeltaUnits = findPreference("pebble_show_delta_units");
            final Preference pebbleShowArrows = findPreference("pebble_show_arrows");
            final Preference pebbleVibrateNoSignal = findPreference("pebble_vibrate_no_signal");
            final Preference pebbleVibrateNoBluetooth = findPreference("pebble_vibrate_no_bluetooth");
            final Preference pebbleTinyDots = findPreference("pebble_tiny_dots");
            final EditTextPreference pebbleSpecialValue = (EditTextPreference) findPreference("pebble_special_value");
            bindPreferenceSummaryToValueAndEnsureNumeric(pebbleSpecialValue);
            final Preference pebbleSpecialText = findPreference("pebble_special_text");
            bindPreferenceSummaryToValue(pebbleSpecialText);
            // Pebble Trend - END

            bindPreferenceSummaryToValue(findPreference("node_wearG5"));//KS
            bindPreferenceSummaryToValue(findPreference("wear_logs_prefix"));
            bindPreferenceSummaryToValue(findPreference("disable_wearG5_on_missedreadings_level"));

            final Preference useCustomSyncKey = findPreference("use_custom_sync_key");
            final Preference CustomSyncKey = findPreference("custom_sync_key");
            final PreferenceCategory collectionCategory = (PreferenceCategory) findPreference("collection_category");
            final PreferenceCategory flairCategory = (PreferenceCategory) findPreference("xdrip_plus_display_colorset9_android5plus");
            //final PreferenceScreen updateScreen = (PreferenceScreen) findPreference("xdrip_plus_update_settings");
            final PreferenceScreen loggingScreen = (PreferenceScreen) findPreference("xdrip_logging_adv_settings");
            final PreferenceScreen motionScreen = (PreferenceScreen) findPreference("xdrip_plus_motion_settings");
            final PreferenceScreen nfcScreen = (PreferenceScreen) findPreference("xdrip_plus_nfc_settings");
            final PreferenceCategory otherCategory = (PreferenceCategory) findPreference("other_category");
            final PreferenceScreen calibrationAlertsScreen = (PreferenceScreen) findPreference("calibration_alerts_screen");
            final PreferenceCategory alertsCategory = (PreferenceCategory) findPreference("alerts_category");
            final Preference disableAlertsStaleDataMinutes = findPreference("disable_alerts_stale_data_minutes");
            final PreferenceScreen calibrationSettingsScreen = (PreferenceScreen) findPreference("xdrip_plus_calibration_settings");
            final PreferenceScreen colorScreen = (PreferenceScreen) findPreference("xdrip_plus_color_settings");
            final Preference old_school_calibration_mode = findPreference("old_school_calibration_mode");
            final Preference extraTagsForLogs = findPreference("extra_tags_for_logging");
            final Preference enableBF = findPreference("enable_bugfender");
            final PreferenceCategory displayCategory = (PreferenceCategory) findPreference("xdrip_plus_display_category");


            lockListener.setSummaryPreference(findPreference("pick_numberwall_start"));

            final Preference enableAmazfit = findPreference("pref_amazfit_enable_key");


            enableAmazfit.setOnPreferenceChangeListener((preference, newValue) -> {
               final Context context = preference.getContext();
               boolean enabled = (boolean) newValue;
                if (enabled==true) {
                    context.startService(new Intent(context, Amazfitservice.class));

                }else {
                    context.stopService(new Intent(context, Amazfitservice.class));
                }

             return true;
             });

            // TODO build list of preferences to cause wear refresh from list
            findPreference("wear_sync").setOnPreferenceChangeListener((preference, newValue) -> {
                        WatchUpdaterService.startSelf();
                        return true;
                    }
            );

            // TODO build list of preferences to cause wear refresh from list
            findPreference("use_wear_heartrate").setOnPreferenceChangeListener((preference, newValue) -> {
                        WatchUpdaterService.startSelf();
                        return true;
                    }
            );

            findPreference("bluetooth_meter_enabled").setOnPreferenceChangeListener((preference, newValue) -> {
                if ((boolean) newValue) {
                    if (preference.getSharedPreferences().getString("selected_bluetooth_meter_address", "").length() > 5) {
                        BluetoothGlucoseMeter.start_service("auto");
                    } else {
                        return false;
                    }
                } else {
                    BluetoothGlucoseMeter.stop_service();
                }
                return true;
            });

            findPreference("scan_and_pair_meter").setSummary(prefs.getString("selected_bluetooth_meter_info", ""));

            findPreference("xdrip_webservice").setOnPreferenceChangeListener((preference, newValue) -> {
                preference.getEditor().putBoolean(preference.getKey(), (boolean) newValue).apply(); // write early for method below
                XdripWebService.immortality(); // start or stop service when preference toggled
                return true;
            });

            findPreference("xdrip_webservice_open").setOnPreferenceChangeListener((preference, newValue) -> {
                preference.getEditor().putBoolean(preference.getKey(), (boolean) newValue).apply(); // write early for method below
                XdripWebService.settingsChanged(); // refresh
                return true;
            });

            findPreference("desert_sync_enabled").setOnPreferenceChangeListener((preference, newValue) -> {
                preference.getEditor().putBoolean(preference.getKey(), (boolean) newValue).apply(); // write early for method below
                DesertSync.settingsChanged(); // refresh
                return true;
            });



            if (enableBF != null ) enableBF.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.getEditor().putBoolean(preference.getKey(),(boolean)newValue).apply();
                xdrip.initBF();
                return true;
            }

            );

            disableAlertsStaleDataMinutes.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!isNumeric(newValue.toString())) {
                    return false;
                }
                if ((Integer.parseInt(newValue.toString())) < 10) {
                    Toast.makeText(preference.getContext(),
                            "Value must be at least 10 minutes", Toast.LENGTH_LONG).show();
                    return false;
                }
                preference.setSummary(newValue.toString());
                return true;
            });

            final Preference showShowcase = findPreference("show_showcase");
            showShowcase.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((boolean)newValue)
                {
                    ShotStateStore.resetAllShots();
                    JoH.static_toast(preference.getContext(),getString(R.string.interface_tips_from_start),Toast.LENGTH_LONG);
                }
                return true;
            });




            units_pref.setOnPreferenceChangeListener((preference, newValue) -> {

                handleUnitsChange(preference, newValue, pFragment);
               /* try {
                    final Double highVal = Double.parseDouble(AllPrefsFragment.this.prefs.getString("highValue", "0"));
                    final Double lowVal = Double.parseDouble(AllPrefsFragment.this.prefs.getString("lowValue", "0"));
                    final Double default_insulin_sensitivity = Double.parseDouble(AllPrefsFragment.this.prefs.getString("profile_insulin_sensitivity_default", "54"));
                    final Double default_target_glucose = Double.parseDouble(AllPrefsFragment.this.prefs.getString("plus_target_range", "100"));

                    static_units = newValue.toString();
                    if (newValue.toString().equals("mgdl")) {
                        if (highVal < 36) {
                            AllPrefsFragment.this.prefs.edit().putString("highValue", Long.toString(Math.round(highVal * Constants.MMOLL_TO_MGDL))).apply();
                            AllPrefsFragment.this.prefs.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                            AllPrefsFragment.this.prefs.edit().putString("plus_target_range", Long.toString(Math.round(default_target_glucose * Constants.MMOLL_TO_MGDL))).apply();
                            ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                            Profile.invalidateProfile();
                          }
                        if (lowVal < 36) {
                            AllPrefsFragment.this.prefs.edit().putString("lowValue", Long.toString(Math.round(lowVal * Constants.MMOLL_TO_MGDL))).apply();
                            AllPrefsFragment.this.prefs.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                            AllPrefsFragment.this.prefs.edit().putString("plus_target_range", Long.toString(Math.round(default_target_glucose * Constants.MMOLL_TO_MGDL))).apply();
                            ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                            Profile.invalidateProfile();
                        }

                    } else {
                        if (highVal > 35) {
                            AllPrefsFragment.this.prefs.edit().putString("highValue", JoH.qs(highVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                            AllPrefsFragment.this.prefs.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                            AllPrefsFragment.this.prefs.edit().putString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL,1)).apply();
                            ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                            Profile.invalidateProfile();
                        }
                        if (lowVal > 35) {
                            AllPrefsFragment.this.prefs.edit().putString("lowValue", JoH.qs(lowVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                            AllPrefsFragment.this.prefs.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                            AllPrefsFragment.this.prefs.edit().putString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL,1)).apply();
                            ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                            Profile.invalidateProfile();
                        }
                    }
                    preference.setSummary(newValue.toString());
                    setSummary("highValue");
                    setSummary("lowValue");
                    if (profile_insulin_sensitivity_default != null) {
                        Log.d(TAG, "refreshing profile insulin sensitivity default display");
                        profile_insulin_sensitivity_default.setTitle(format_insulin_sensitivity(profile_insulin_sensitivity_default.getTitle().toString(), ProfileEditor.minMaxSens(ProfileEditor.loadData(false))));

//                            do_format_insulin_sensitivity(profile_insulin_sensitivity_default, AllPrefsFragment.this.prefs, false, null);
                    }
                    Profile.reloadPreferences(AllPrefsFragment.this.prefs);

                } catch (Exception e) {
                    Log.e(TAG, "Got excepting processing high/low value preferences: " + e.toString());
                }*/
                return true;
            });

            // jamorham xDrip+ prefs
            if (this.prefs.getString("custom_sync_key", "").equals("")) {
                this.prefs.edit().putString("custom_sync_key", CipherUtils.getRandomHexKey()).apply();
            }
            bindPreferenceSummaryToValue(findPreference("custom_sync_key")); // still needed?

            bindPreferenceSummaryToValue(findPreference("xplus_insulin_dia"));
            bindPreferenceSummaryToValue(findPreference("xplus_liver_sensitivity"));
            bindPreferenceSummaryToValue(findPreference("xplus_liver_maximpact"));

            bindPreferenceSummaryToValue(findPreference("low_predict_alarm_level"));
            Profile.validateTargetRange();
            bindPreferenceSummaryToValue(findPreference("plus_target_range"));

            useCustomSyncKey.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = preference.getContext();
                PlusSyncService.clearandRestartSyncService(context);
                return true;
            });
            CustomSyncKey.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.setSummary(newValue.toString());
                Context context = preference.getContext();
                PlusSyncService.clearandRestartSyncService(context);
                return true;
            });


            DexCollectionType collectionType = DexCollectionType.getType(this.prefs.getString("dex_collection_method", "BluetoothWixel"));

            Log.d(TAG, collectionType.name());
            if (collectionType != DexCollectionType.DexcomShare) {
                collectionCategory.removePreference(shareKey);
                collectionCategory.removePreference(scanShare);
                otherCategory.removePreference(interpretRaw);
                alertsCategory.addPreference(calibrationAlertsScreen);
            } else {
                otherCategory.removePreference(predictiveBG);
                alertsCategory.removePreference(calibrationAlertsScreen);
                this.prefs.edit().putBoolean("calibration_notifications", false).apply();
            }

            if (collectionType != DexCollectionType.Medtrum) {
                try {
                    collectionCategory.removePreference(findPreference("medtrum_use_native"));
                    collectionCategory.removePreference(findPreference("medtrum_a_hex"));
                } catch (Exception e) {
                    //
                }
            }

            if (collectionType != DexCollectionType.NSFollow) {
                try {
                    collectionCategory.removePreference(nsFollowUrl);
                    collectionCategory.removePreference(nsFollowDownload);
                } catch (Exception e) {
                    //
                }
            }

            try {
                findPreference("nfc_scan_homescreen").setOnPreferenceChangeListener((preference, newValue) -> {
                    NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), (boolean) newValue && (NFCReaderX.useNFC()));
                    return true;
                });
            } catch (NullPointerException e) {
                Log.d(TAG, "Nullpointer looking for nfc_scan_homescreen");
            }
            try {
                findPreference("use_nfc_scan").setOnPreferenceChangeListener((preference, newValue) -> {
                    final AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(preference.getContext());
                    if ((boolean) newValue) {
                        builder.setTitle("Stop! Are you sure?");
                        builder.setMessage("This can sometimes crash / break a sensor!\nWith some phones there can be problems, try on expiring sensor first for safety. You have been warned.");

                        builder.setPositiveButton("I AM SURE", (dialog, which) -> {
	                        dialog.dismiss();
	                        ((SwitchPreference) preference).setChecked(true);
	                        preference.getEditor().putBoolean("use_nfc_scan", true).apply();
	                        NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), (boolean) newValue && prefs.getBoolean("nfc_scan_homescreen", false));
                        });
                        builder.setNegativeButton("NOPE", (dialog, which) -> dialog.dismiss());
                        final androidx.appcompat.app.AlertDialog alert = builder.create();
                        alert.show();
                        return false;
                    } else {
                        NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), (boolean) newValue && prefs.getBoolean("nfc_scan_homescreen", false));
                    }
                    return true;
                });
            } catch (NullPointerException e) {
                Log.d(TAG, "Nullpointer looking for nfc_scan");
            }

            try {
                findPreference("external_blukon_algorithm").setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabled = (Boolean) newValue;
                    findPreference("retrieve_blukon_history").setEnabled(!isEnabled);
                    return true;
                });
            } catch (NullPointerException e) {
                //
            }

            final boolean engineering_mode = this.prefs.getBoolean("engineering_mode",false);

            if (!engineering_mode) {
                try {
                    displayCategory.removePreference(findPreference("bg_compensate_noise_ultrasensitive"));
                } catch (Exception e) {
                    //
                }
            }


            if (!engineering_mode) {
                try {
                    ((PreferenceScreen) findPreference("dexcom_server_upload_screen")).removePreference(findPreference("share_test_key"));
                } catch (Exception e) {
                    //
                }
            }

            //if (engineering_mode) {
                // populate the list
                PluggableCalibration.setListPreferenceData(currentCalibrationPlugin);

                currentCalibrationPlugin.setOnPreferenceChangeListener((preference, newValue) -> {
                    PluggableCalibration.invalidateCache(); // current
                    PluggableCalibration.invalidateCache(newValue.toString()); // next
                    PluggableCalibration.invalidatePluginCache(); // reset the object cache
                    return true;
                });
            //}

            if (!DexCollectionType.hasLibre(collectionType)) {
                collectionCategory.removePreference(nfcSettings);
            } else {
                // has libre
                if (!engineering_mode)
                    try {
                        nfcScreen.removePreference(findPreference("nfc_test_diagnostic"));
                    } catch (NullPointerException e) {
                        //
                    }
                set_nfc_expiry_change_listeners();
                update_nfc_expiry_preferences(null);
            }

            if (!DexCollectionService.getBestLimitterHardwareName().equals("BlueReader")) {
                collectionCategory.removePreference(bluereadersettings);
            } else {
                findPreference("blueReader_turn_off_value").setTitle(getString(R.string.blueReader_turnoffvalue) + " (" + prefs.getInt("blueReader_turn_off_value", 5) + ")");

                findPreference("blueReader_turn_off_value").setOnPreferenceChangeListener((preference, newValue) ->
                        {
                            prefs.edit().putInt("blueReader_turn_off_value", (Integer) newValue).commit();
                            preference.setTitle(getString(R.string.blueReader_turnoffvalue) + " (" + newValue + ")");
                            return true;
                        }
                );

            }

            try {

                try {
                    if (!DexCollectionType.hasWifi()) {
                        final String receiversIpAddresses = this.prefs.getString("wifi_recievers_addresses", "").trim();
                        // only hide if non wifi wixel mode and value not previously set to cope with
                        // dynamic mode changes. jamorham
                        if (receiversIpAddresses.equals("")) {
                            collectionCategory.removePreference(wifiRecievers);
                        }
                    }
                } catch (NullPointerException e) {
                    Log.wtf(TAG, "Nullpointer wifireceivers ", e);
                }

                if ((collectionType != DexCollectionType.DexbridgeWixel)
                        && (collectionType != DexCollectionType.WifiDexBridgeWixel)) {
                    try {
                        collectionCategory.removePreference(transmitterId);
                        // collectionCategory.removePreference(closeGatt);
                    } catch (NullPointerException e) {
                        Log.wtf(TAG, "Nullpointer removing txid ", e);
                    }
                }

                if (Build.VERSION.SDK_INT < 21) {
                    try {
                        colorScreen.removePreference(flairCategory);
                    } catch (Exception e) { //
                    }
                }
                if (Build.VERSION.SDK_INT < 23) {
                    try {
                        ((PreferenceGroup)findPreference("xdrip_plus_display_category")).removePreference(findPreference("xdrip_plus_number_icon"));
                    } catch (Exception e) { //
                    }
                }

               // if (!Experience.gotData()) {
               //     try {
               //     collectionCategory.removePreference(runInForeground);
               //     } catch (Exception e) { //
               //     }
               // }

                // remove master ip input if we are the master
                if (Home.get_master()) {
                    final PreferenceScreen desert_sync_screen = (PreferenceScreen) findPreference("xdrip_plus_desert_sync_settings");
                    try {
                        desert_sync_screen.removePreference(findPreference("desert_sync_master_ip"));

                    } catch (Exception e) {
                        //
                    }
                }


                final PreferenceScreen g5_settings_screen = (PreferenceScreen) findPreference("xdrip_plus_g5_extra_settings");
                if (collectionType == DexCollectionType.DexcomG5) {
                    try {
                        collectionCategory.addPreference(transmitterId);

                        collectionCategory.addPreference(g5_settings_screen);
                        //collectionCategory.addPreference(g5nonraw);
                        //collectionCategory.addPreference(scanConstantly);
                        //collectionCategory.addPreference(reAuth);
                        //collectionCategory.addPreference(reBond);
                        //collectionCategory.addPreference(runOnMain);
                    } catch (NullPointerException e) {
                        Log.wtf(TAG, "Null pointer adding G5 prefs ", e);
                    }
                } else {
                    try {
                        // collectionCategory.removePreference(transmitterId);

                        collectionCategory.removePreference(g5_settings_screen);
                       // collectionCategory.removePreference(scanConstantly);
                       // collectionCategory.removePreference(g5nonraw);
                       // collectionCategory.removePreference(reAuth);
                       // collectionCategory.removePreference(reBond);
                       // collectionCategory.removePreference(runOnMain);
                    } catch (NullPointerException e) {
                        Log.wtf(TAG, "Null pointer removing G5 prefs ", e);
                    }
                }

                if (!engineering_mode) {
                    try {
                        if (!Experience.gotData()) getPreferenceScreen().removePreference(motionScreen);
                        calibrationSettingsScreen.removePreference(old_school_calibration_mode);
                    } catch (NullPointerException e) {
                        Log.wtf(TAG, "Nullpointer with engineering mode s ", e);
                    }
                }
                if ((!engineering_mode) || (!this.prefs.getBoolean("enable_bugfender", false))) {
                    loggingScreen.removePreference(bfappid);
                }

            } catch (NullPointerException e) {
                Log.wtf(TAG, "Got null pointer exception removing pref: ", e);
            }

            if (engineering_mode || this.prefs.getString("update_channel", "").matches("alpha|nightly")) {
                ListPreference update_channel = (ListPreference) findPreference("update_channel");
                update_channel.setEntryValues(getResources().getStringArray(R.array.UpdateChannelE));
                update_channel.setEntries(getResources().getStringArray(R.array.UpdateChannelDetailE));
            }

            final DecimalFormat df = new DecimalFormat("#.#");

            if (this.prefs.getString("units", "mgdl").compareTo("mmol") != 0) {
                df.setMaximumFractionDigits(0);
                pebbleSpecialValue.setDefaultValue("99");
                if (pebbleSpecialValue.getText().compareTo("5.5") == 0) {
                    pebbleSpecialValue.setText(df.format(Double.valueOf(pebbleSpecialValue.getText()) * Constants.MMOLL_TO_MGDL));
                }
            } else {
                df.setMaximumFractionDigits(1);
                pebbleSpecialValue.setDefaultValue("5.5");
                if (pebbleSpecialValue.getText().compareTo("99") == 0) {
                    pebbleSpecialValue.setText(df.format(Double.valueOf(pebbleSpecialValue.getText()) / Constants.MMOLL_TO_MGDL));
                }
            }

            try {
                findPreference("calibration_notifications").setOnPreferenceChangeListener((preference, newValue) -> {
                    // clear any pending alerts
                    final UserNotification userNotification = UserNotification.lastCalibrationAlert();
                    if (userNotification != null) {
                        userNotification.delete();
                    }
                    return true;
                });
            } catch (Exception e) {
                //
            }

            bindPreferenceSummaryToValue(collectionMethod);
            bindPreferenceSummaryToValue(shareKey);

            final NamedSliderProcessor processor = new BgToSpeech();
            bindPreferenceTitleAppendToIntegerValueFromLogSlider(findPreference("speak_readings_change_time"), processor, "time", false);
            bindPreferenceTitleAppendToIntegerValueFromLogSlider(findPreference("speak_readings_change_threshold"), processor, "threshold", true);


            final NamedSliderProcessor tidepoolProcessor = new UploadChunk();
            bindPreferenceTitleAppendToIntegerValueFromLogSlider(findPreference("tidepool_window_latency"), tidepoolProcessor, "latency", false);


            wifiRecievers.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.setSummary(newValue.toString());
                ParakeetHelper.notifyOnNextCheckin(true);
                return true;
            });

            bindPreferenceTitleAppendToStringValue(findPreference("retention_days_bg_reading"));

            bindPreferenceTitleAppendToStringValue(findPreference("pendiq_pin"));

            try {
                bindPreferenceTitleAppendToStringValue(findPreference("inpen_prime_units"));
                bindPreferenceTitleAppendToStringValue(findPreference("inpen_prime_minutes"));
            } catch (Exception e) {
                //
            }

            // Pebble Trend -- START

            int currentPebbleSync = PebbleUtil.getCurrentPebbleSyncType();

            if (currentPebbleSync == 1) {
                watchCategory.removePreference(pebbleSpecialValue);
                watchCategory.removePreference(pebbleSpecialText);
            }

            if ((currentPebbleSync != 3) && (currentPebbleSync != 4) && (currentPebbleSync != 5)) {
                watchCategory.removePreference(pebbleTrend);
                watchCategory.removePreference(pebbleFilteredLine);
                watchCategory.removePreference(pebbleTinyDots);
                watchCategory.removePreference(pebbleHighLine);
                watchCategory.removePreference(pebbleLowLine);
                watchCategory.removePreference(pebbleTrendPeriod);
                watchCategory.removePreference(pebbleTrendPeriod);
                watchCategory.removePreference(pebbleDelta);
                watchCategory.removePreference(pebbleDeltaUnits);
                watchCategory.removePreference(pebbleShowArrows);
                watchCategory.removePreference(pebbleVibrateNoSignal);
            }

            // master switch for pebble
            pebbleSync1.setOnPreferenceChangeListener((preference, newValue) -> {
                final Context context = preference.getContext();
                if ((Boolean) newValue) {


                    pebbleType = PebbleUtil.getCurrentPebbleSyncType(PreferenceManager.getDefaultSharedPreferences(context).getString("broadcast_to_pebble_type", "1"));

                    // install watchface
                    installPebbleWatchface(pebbleType, preference);
                }
                // start/stop service
                enablePebble(pebbleType, (Boolean) newValue, context);
                return true;
            });


            // Pebble Trend (just major change)
            pebbleSync2.setOnPreferenceChangeListener((preference, newValue) -> {
                final Context context = preference.getContext();

                int oldPebbleType = PebbleUtil.getCurrentPebbleSyncType();
                int pebbleType = PebbleUtil.getCurrentPebbleSyncType(newValue);

                // install watchface
                installPebbleWatchface(pebbleType, preference);

                // start/stop service
                enablePebble(pebbleType, getBooleanPreferenceViaContextWithoutException(context,"broadcast_to_pebble",false), context);

                // configuration options
                if (oldPebbleType != pebbleType) {

                    // REMOVE ALL
                    if (oldPebbleType == 2) {
                        watchCategory.removePreference(pebbleSpecialValue);
                        watchCategory.removePreference(pebbleSpecialText);
                    } else {
                        watchCategory.removePreference(pebbleTrend);
                        watchCategory.removePreference(pebbleFilteredLine);
                        watchCategory.removePreference(pebbleTinyDots);
                        watchCategory.removePreference(pebbleHighLine);
                        watchCategory.removePreference(pebbleLowLine);
                        watchCategory.removePreference(pebbleTrendPeriod);
                        watchCategory.removePreference(pebbleDelta);
                        watchCategory.removePreference(pebbleDeltaUnits);
                        watchCategory.removePreference(pebbleShowArrows);
                        watchCategory.removePreference(pebbleSpecialValue);
                        watchCategory.removePreference(pebbleSpecialText);
                        watchCategory.removePreference(pebbleVibrateNoSignal);
                        watchCategory.removePreference(pebbleVibrateNoBluetooth);
                    }

                    // Add New one
                    if ((pebbleType == 3) || (pebbleType == 4) || (pebbleType == 5)) {
                        watchCategory.addPreference(pebbleTrend);
                        watchCategory.addPreference(pebbleFilteredLine);
                        watchCategory.addPreference(pebbleTinyDots);
                        watchCategory.addPreference(pebbleHighLine);
                        watchCategory.addPreference(pebbleLowLine);
                        watchCategory.addPreference(pebbleTrendPeriod);
                        watchCategory.addPreference(pebbleDelta);
                        watchCategory.addPreference(pebbleDeltaUnits);
                        watchCategory.addPreference(pebbleShowArrows);
                        watchCategory.addPreference(pebbleVibrateNoSignal);
                        watchCategory.addPreference(pebbleVibrateNoBluetooth);
                    }

                    if (oldPebbleType != 1) {
                        watchCategory.addPreference(pebbleSpecialValue);
                        watchCategory.addPreference(pebbleSpecialText);
                    }

                }

                return true;
            });

            // TODO reduce code duplication more
            pebbleTrend.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            pebbleFilteredLine.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });


            pebbleHighLine.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
	            return true;
            });

            pebbleLowLine.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            pebbleTrendPeriod.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });
            pebbleDelta.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });
            pebbleDeltaUnits.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });
            pebbleShowArrows.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            pebbleTinyDots.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            pebbleVibrateNoBluetooth.setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            // TODO this attaches to the wrong named instance of use_pebble_health - until restructured so that there is only one instance
            findPreference("use_pebble_health").setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            findPreference("pebble_show_bwp").setOnPreferenceChangeListener((preference, newValue) -> {
                restartPebble();
                return true;
            });

            // Pebble Trend -- END

            bindWidgetUpdater();

            extraTagsForLogs.setOnPreferenceChangeListener((preference, newValue) -> {
                ExtraLogTags.readPreference((String)newValue);
                return true;
            });

            bindPreferenceSummaryToValue(transmitterId); // duplicated below but this sets initial value
            transmitterId.getEditText().setFilters(new InputFilter[]{new InputFilter.AllCaps()}); // TODO filter O ?
            transmitterId.setOnPreferenceChangeListener((preference, newValue) -> {
                new Thread(() -> {
	                try {
		                Thread.sleep(1000);
	                } catch (InterruptedException e) {
		                //
	                }
	                Log.d(TAG, "Trying to restart collector due to tx id change");
	                CollectionServiceStarter.restartCollectionService(xdrip.getAppContext());
                }).start();
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);

                return true;
            });

            // when changing collection method
                    collectionMethod.setOnPreferenceChangeListener((preference, newValue) -> {

                        DexCollectionType collectionType1 = DexCollectionType.getType((String) newValue);

                        if (collectionType1 != DexCollectionType.DexcomShare) { // NOT USING SHARE
                            collectionCategory.removePreference(shareKey);
                            collectionCategory.removePreference(scanShare);
                            otherCategory.removePreference(interpretRaw);
                            otherCategory.addPreference(predictiveBG);
                            alertsCategory.addPreference(calibrationAlertsScreen);
                        } else {
                            collectionCategory.addPreference(shareKey);
                            collectionCategory.addPreference(scanShare);
                            otherCategory.addPreference(interpretRaw);
                            otherCategory.removePreference(predictiveBG);
                            alertsCategory.removePreference(calibrationAlertsScreen);
                            AllPrefsFragment.this.prefs.edit().putBoolean("calibration_notifications", false).apply();
                        }

                        if (DexCollectionType.hasLibre(collectionType1)) {
                            collectionCategory.addPreference(nfcSettings);
                            NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), prefs.getBoolean("nfc_scan_homescreen", false) && prefs.getBoolean("use_nfc_scan", false));
                            if (!engineering_mode)
                                try {
                                    nfcScreen.removePreference(findPreference("nfc_test_diagnostic"));
                                } catch (NullPointerException e) {
                                    //
                                }
                        } else {
                            collectionCategory.removePreference(nfcSettings);
                            NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), false); // always disable
                        }


                    /*    if ((collectionType != DexCollectionType.BluetoothWixel
                                && collectionType != DexCollectionType.DexcomShare
                                && collectionType != DexCollectionType.WifiWixel
                                && collectionType != DexCollectionType.DexbridgeWixel
                                && collectionType != DexCollectionType.LimiTTer
                                && collectionType != DexCollectionType.DexcomG5
                                && collectionType != DexCollectionType.WifiBlueToothWixel
                                && collectionType != DexCollectionType.WifiDexBridgeWixel
                                && collectionType != DexCollectionType.LibreAlarm
                                ) || (!Experience.gotData())) {
                            collectionCategory.removePreference(runInForeground);
                        } else {
                            collectionCategory.addPreference(runInForeground);
                        }*/

                        // jamorham always show wifi receivers option if populated as we may switch modes dynamically
                        if (!DexCollectionType.hasWifi()) {
                            String receiversIpAddresses;
                            receiversIpAddresses = AllPrefsFragment.this.prefs.getString("wifi_recievers_addresses", "");
                            if (receiversIpAddresses == null || receiversIpAddresses.trim().equals("")) {
                                collectionCategory.removePreference(wifiRecievers);
                            } else {
                                collectionCategory.addPreference(wifiRecievers);
                            }
                        } else {
                            collectionCategory.addPreference(wifiRecievers);
                        }

                        if ((collectionType1 != DexCollectionType.DexbridgeWixel)
                                && (collectionType1 != DexCollectionType.WifiDexBridgeWixel)) {
                            collectionCategory.removePreference(transmitterId);
                            //collectionCategory.removePreference(closeGatt);
                            //TODO Bridge battery display support
                        } else {
                            collectionCategory.addPreference(transmitterId);
                         //   collectionCategory.addPreference(closeGatt);
                        }

                        if (collectionType1 == DexCollectionType.DexcomG5) {
                            collectionCategory.addPreference(transmitterId);
                            // TODO add debug menu
                        }

                        if (collectionType1 == DexCollectionType.NSFollow) {
                            collectionCategory.addPreference(nsFollowUrl);
                            collectionCategory.addPreference(nsFollowDownload);
                        }


                        String stringValue = newValue.toString();
                        if (preference instanceof ListPreference) {
                            ListPreference listPreference = (ListPreference) preference;
                            int index = listPreference.findIndexOfValue(stringValue);
                            preference.setSummary(
                                    index >= 0
                                            ? listPreference.getEntries()[index]
                                            : null);

                        } else if (preference instanceof RingtonePreference) {
                            if (TextUtils.isEmpty(stringValue)) {
                                preference.setSummary(R.string.pref_ringtone_silent);

                            } else {
                                Ringtone ringtone = RingtoneManager.getRingtone(
                                        preference.getContext(), Uri.parse(stringValue));
                                if (ringtone == null) {
                                    preference.setSummary(null);
                                } else {
                                    String name = ringtone.getTitle(preference.getContext());
                                    preference.setSummary(name);
                                }
                            }
                        } else {
                            preference.setSummary(stringValue);
                        }

                        if (preference.getKey().equals("dex_collection_method")) {
                            //CollectionServiceStarter.restartCollectionService(preference.getContext(), (String) newValue);

                            if (newValue.equals("Follower")) {
                                // reset battery whenever changing collector type
                                AllPrefsFragment.this.prefs.edit().putInt("bridge_battery",0).apply();
                                AllPrefsFragment.this.prefs.edit().putInt("parakeet_battery",0).apply();
                                if (AllPrefsFragment.this.prefs.getBoolean("plus_follow_master",false))
                                {
                                    AllPrefsFragment.this.prefs.edit().putBoolean("plus_follow_master", false).apply();
                                    JoH.static_toast(preference.getContext(),"Turning off xDrip+ Sync Master for Followers!",Toast.LENGTH_LONG);
                                }
                                GcmActivity.requestBGsync();
                            }
                        //} else {
                        //    CollectionServiceStarter.restartCollectionService(preference.getContext());
                        }
                        CollectionServiceStarter.restartCollectionServiceBackground();
                        return true;
                    });
        }



        // all this boiler plate for a dynamic interface seems excessive and boring, I would love to know a helper library to simplify this
        private void set_nfc_expiry_change_listeners() {
            nfc_expiry_days.setOnPreferenceChangeListener((preference, newValue) -> {
                // have to pre-save it
                preference.getEditor().putString("nfc_expiry_days", (String) newValue).apply();
                update_nfc_expiry_preferences(null);
                return true;
            });
            final Preference nfc_show_age = findPreference("nfc_show_age");
            nfc_show_age.setOnPreferenceChangeListener((preference, newValue) -> {
                update_nfc_expiry_preferences((Boolean) newValue);
                return true;
            });
        }

        private void update_nfc_expiry_preferences(Boolean show_age) {
            try {
	            final PreferenceScreen nfcScreen = (PreferenceScreen) findPreference("xdrip_plus_nfc_settings");
                final String nfc_expiry_days_string = AllPrefsFragment.this.prefs.getString("nfc_expiry_days", "14.5");

                final CheckBoxPreference nfc_show_age = (CheckBoxPreference) findPreference("nfc_show_age");
                nfc_show_age.setSummaryOff("Show the sensor expiry time based on " + nfc_expiry_days_string + " days");
                if (show_age == null) show_age = nfc_show_age.isChecked();
                if (show_age) {
                    nfcScreen.removePreference(nfc_expiry_days);
                } else {
                    nfc_expiry_days.setOrder(3);
                    nfcScreen.addPreference(nfc_expiry_days);
                }
            } catch (NullPointerException e) {
                //
            }
        }

        private void bindWidgetUpdater() {
            findPreference("widget_range_lines").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("extra_status_line").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("widget_status_line").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_calibration_long").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_calibration_short").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_avg").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_a1c_dcct").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_a1c_ifcc").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_in").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_high").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_low").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("extra_status_line").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_capture_percentage").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_realtime_capture_percentage").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("extra_status_stats_24h").setOnPreferenceChangeListener(new WidgetListener());

        }

        private void update_force_english_title(String param) {
            try {
                String word;
                if (param.isEmpty()) {
                    word = locale_choice.getEntry().toString();
                } else {
                    try {
                        word = (locale_choice.getEntries()[locale_choice.findIndexOfValue(param)]).toString();
                    } catch (Exception e) {
                        word = "Unknown";
                    }
                }
                force_english.setTitle("Force " + word + " Text");
            } catch (NullPointerException e) {
                Log.e(TAG, "Nullpointer in update_force_english_title: " + e);
            }
        }

        private static void recursive_notify_all_preference_screens(PreferenceGroup preferenceGroup) {
            if (preferenceGroup instanceof PreferenceScreen) {
                ((BaseAdapter) ((PreferenceScreen) preferenceGroup).getRootAdapter()).notifyDataSetChanged();
            } else {
                for (int index = 0; index < preferenceGroup.getPreferenceCount(); index++) {
                    final Preference pref = preferenceGroup.getPreference(index);
                    if (pref instanceof PreferenceGroup) {
                        recursive_notify_all_preference_screens((PreferenceGroup) pref);
                    }
                }
            }
        }

        private void installPebbleWatchface(final int pebbleType, Preference preference) {

            final Context context = preference.getContext();

            if (pebbleType == 1)
                return;

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);

            builder.setTitle("Pebble Install");

            switch (pebbleType)
                {
                    case 2:
                        builder.setMessage("Install Standard Pebble Watchface?");
                        break;
                    case 3:
                        builder.setMessage("Install Pebble Trend Watchface?");
                        break;
                    case 4:
                        builder.setMessage("Install Pebble Classic Trend Watchface?");
                        break;
                    case 5:
                        builder.setMessage("Install Pebble Clay Trend Watchface?");
                        break;
                }


            builder.setPositiveButton("YES", (OnClickListener) (dialog, which) -> {
                dialog.dismiss();

                switch (pebbleType) {
                    case 2:
                        context.startActivity(new Intent(context, InstallPebbleWatchFace.class));
                        break;
                    case 3:
                        context.startActivity(new Intent(context, InstallPebbleTrendWatchFace.class));
                        break;
                    case 4:
                        context.startActivity(new Intent(context, InstallPebbleClassicTrendWatchface.class));
                        break;
                    case 5:
                        context.startActivity(new Intent(context, InstallPebbleTrendClayWatchFace.class));
                        break;
                }

                JoH.runOnUiThreadDelayed(() -> {
	                androidx.appcompat.app.AlertDialog.Builder builder1 = new androidx.appcompat.app.AlertDialog.Builder(context);
	                builder1.setTitle("Snooze Control Install");
	                builder1.setMessage("Install Pebble Snooze Button App?");
	                // inner
	                builder1.setPositiveButton("YES", (OnClickListener) (dialog12, which12) -> {
		                dialog12.dismiss();
		                context.startActivity(new Intent(context, InstallPebbleSnoozeControlApp.class));
	                });
	                builder1.setNegativeButton("NO", (OnClickListener) (dialog1, which1) -> dialog1.dismiss());
	                androidx.appcompat.app.AlertDialog alert = builder1.create();
	                alert.show();
                },3000);
            // outer
            });

            builder.setNegativeButton("NO", (OnClickListener) (dialog, which) -> dialog.dismiss());

            androidx.appcompat.app.AlertDialog alert = builder.create();
            alert.show();
        }

        private static int pebbleType = 1;
        private void enablePebble(int newValueInt, boolean enabled, Context context) {
            Log.d(TAG,"enablePebble called with: "+newValueInt+" "+enabled);
            if (pebbleType == 1) {
                if (enabled && (newValueInt != 1)) {
                    context.stopService(new Intent(context, PebbleWatchSync.class));
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    Log.d(TAG,"Starting pebble service type: "+newValueInt);
                }
            } else {
                if (!enabled || (newValueInt == 1)) {
                    context.stopService(new Intent(context, PebbleWatchSync.class));
                    Log.d(TAG, "Stopping pebble service type: " + newValueInt);
                }


            }

            pebbleType = enabled ? newValueInt : 1;
            PebbleWatchSync.setPebbleType(pebbleType);

        }


        private void setupBarcodeConfigScanner() {
            findPreference("auto_configure").setOnPreferenceClickListener(preference -> {
                new AndroidBarcode((AppCompatActivity) getActivity()).scan();
                return true;
            });
        }


        private void setupBarcodeShareScanner() {
            findPreference("scan_share2_barcode").setOnPreferenceClickListener(preference -> {
                new AndroidBarcode((AppCompatActivity) getActivity()).scan();
                return true;
            });
        }

        private void refresh_extra_items() {
            try {
                if (this.prefs == null) return;
                if (!this.prefs.getBoolean("plus_extra_features", false)) {
                    // getPreferenceScreen().removePreference(findPreference("plus_follow_master"));

                } else {
                    // getPreferenceScreen().addPreference(findPreference("plus_follow_master"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Got exception in refresh extra: " + e.toString());
            }
        }

        private void bindTTSListener() {
            findPreference("bg_to_speech").setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    prefs.edit().putBoolean("bg_to_speech", true).apply(); // early write before we exit method
                    final androidx.appcompat.app.AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
                    alertDialog.setTitle(R.string.install_text_to_speech_data_question);
                    alertDialog.setMessage(getString(R.string.install_text_to_speech_data_question) + "\n" + getString(R.string.after_installation_of_languages_you_might_have_to));
                    alertDialog.setCancelable(true);
                    alertDialog.setPositiveButton(R.string.ok, (dialog, which) -> SpeechUtil.installTTSData(getActivity()));
                    alertDialog.setNegativeButton(R.string.no, null);
                    final androidx.appcompat.app.AlertDialog alert = alertDialog.create();
                    alert.show();
                    try {
                        BgToSpeech.testSpeech();
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception with TTS: " + e);
                    }
                } else {
                    BgToSpeech.tearDownTTS();
                }
                return true;
            });

            findPreference("speech_speed").setOnPreferenceChangeListener((preference, newValue) ->
                    {
                        prefs.edit().putInt("speech_speed", (Integer) newValue).apply();
                        try {
                            BgToSpeech.testSpeech();
                        } catch (Exception e) {
                            Log.e(TAG, "Got exception with TTS: " + e);
                        }
                        return true;
                    }
            );
            findPreference("speech_pitch").setOnPreferenceChangeListener((preference, newValue) ->
                    {
                        prefs.edit().putInt("speech_pitch", (Integer) newValue).apply();
                        try {
                            BgToSpeech.testSpeech();
                        } catch (Exception e) {
                            Log.e(TAG, "Got exception with TTS: " + e);
                        }
                        return true;
                    }
            );
        }


        // Will update the widget if any setting relevant to the widget gets changed.
        private static class WidgetListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = preference.getContext();
                if (AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0) {
                    context.startService(new Intent(context, WidgetUpdateService.class));
                }
                return true;
            }
        }
    }

    public static void handleUnitsChange(Preference preference, Object newValue, AllPrefsFragment allPrefsFragment) {
        try {
            SharedPreferences preferences;
            if (preference!= null) {
                preferences = preference.getSharedPreferences();
            } else {
                preferences = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
            }

            final double highVal = Double.parseDouble(preferences.getString("highValue", "0"));
            final double lowVal = Double.parseDouble(preferences.getString("lowValue", "0"));
            final double default_insulin_sensitivity = Double.parseDouble(preferences.getString("profile_insulin_sensitivity_default", "54"));
            final double default_target_glucose = Double.parseDouble(preferences.getString("plus_target_range", "100"));


            static_units = newValue.toString();
            if (newValue.toString().equals("mgdl")) {
                if (highVal < 36) {
                    ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                    preferences.edit().putString("highValue", Long.toString(Math.round(highVal * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("plus_target_range", Long.toString(Math.round(default_target_glucose * Constants.MMOLL_TO_MGDL))).apply();
                    Profile.invalidateProfile();
                }
                if (lowVal < 36) {
                    ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                    preferences.edit().putString("lowValue", Long.toString(Math.round(lowVal * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("plus_target_range", Long.toString(Math.round(default_target_glucose * Constants.MMOLL_TO_MGDL))).apply();
                    Profile.invalidateProfile();
                }

            } else {
                if (highVal > 35) {
                    ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                    preferences.edit().putString("highValue", JoH.qs(highVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                    preferences.edit().putString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL,1)).apply();
                    Profile.invalidateProfile();
                }
                if (lowVal > 35) {
                    ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                    preferences.edit().putString("lowValue", JoH.qs(lowVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                    preferences.edit().putString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL,1)).apply();
                    Profile.invalidateProfile();
                }
            }
            if (preference != null) preference.setSummary(newValue.toString());
            if (allPrefsFragment != null) {
                allPrefsFragment.setSummary("highValue");
                allPrefsFragment.setSummary("lowValue");
            }
            if (profile_insulin_sensitivity_default != null) {
                Log.d(TAG, "refreshing profile insulin sensitivity default display");
                profile_insulin_sensitivity_default.setTitle(format_insulin_sensitivity(profile_insulin_sensitivity_default.getTitle().toString(), ProfileEditor.minMaxSens(ProfileEditor.loadData(false))));

//                            do_format_insulin_sensitivity(profile_insulin_sensitivity_default, AllPrefsFragment.this.prefs, false, null);
            }
            Profile.reloadPreferences(preferences);

        } catch (Exception e) {
            Log.e(TAG, "Got excepting processing high/low value preferences: " + e.toString());
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}

