package com.eveningoutpost.dexdrip.utils;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Models.UserError.ExtraLogTags;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.ParakeetHelper;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface.InstallPebbleClassicTrendWatchface;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface.InstallPebbleSnoozeControlApp;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface.InstallPebbleTrendClayWatchFace;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface.InstallPebbleTrendWatchFace;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface.InstallPebbleWatchFace;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.profileeditor.ProfileEditor;
import com.eveningoutpost.dexdrip.xDripWidget;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nightscout.core.barcode.NSBarcodeConfig;

import net.tribe7.common.base.Joiner;

import java.net.URI;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

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


    private void refreshFragments() {
        this.preferenceFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
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

                SharedPreferences.Editor editor = prefs.edit();
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
                Toast.makeText(getApplicationContext(), "Loaded " + Integer.toString(changes) + " preferences from QR code", Toast.LENGTH_LONG).show();
                PlusSyncService.clearandRestartSyncService(getApplicationContext());
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
            }

            NSBarcodeConfig barcode = new NSBarcodeConfig(scanresults);
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
                            if (userInfo[0].length() > 0 && userInfo[1].length() > 0) {
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
        this.preferenceFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                this.preferenceFragment).commit();
        processExtraData();
    }

    @Override
    protected void onResume()
    {
        super.onResume();;
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(ActivityRecognizedService.prefListener);
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

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
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
        }
    };
    private static Preference.OnPreferenceChangeListener sBindNumericPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (isNumeric(stringValue)) {
                preference.setSummary(stringValue);
                return true;
            }
            return false;
        }
    };
    private static Preference.OnPreferenceChangeListener sBindPreferenceTitleAppendToValueListenerUpdateChannel = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            boolean do_update = false;
            // detect not first run
            if (preference.getTitle().toString().contains("(")) {
                do_update = true;
            }

            preference.setTitle(preference.getTitle().toString().replaceAll("  \\([a-z0-9A-Z]+\\)$", "") + "  (" + value.toString() + ")");
            if (do_update) {
                preference.getEditor().putString(preference.getKey(), value.toString()).apply(); // update prefs now
                UpdateActivity.last_check_time = -1;
                UpdateActivity.checkForAnUpdate(preference.getContext());
            }
            return true;
        }
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


    private static void bindPreferenceSummaryToValueAndEnsureNumeric(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindNumericPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    public static class AllPrefsFragment extends PreferenceFragment {

        SharedPreferences prefs;

        private void setSummary(String pref_name) {
            try {
                // is there a cleaner way to bind these values when setting programatically?
                final String pref_val = this.prefs.getString(pref_name, "");
                findPreference(pref_name).setSummary(pref_val);
                EditTextPreference thispref = (EditTextPreference) findPreference(pref_name);
                thispref.setText(pref_val);
            } catch (Exception e) {
                Log.e(TAG, "Exception during setSummary: " + e.toString());
            }
        }

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
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("other_alerts_snooze"));

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

//            profile_carb_ratio_default.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    if (!isNumeric(newValue.toString())) {
//                        return false;
//                    }
//                    preference.setTitle(format_carb_ratio(preference.getTitle().toString(), newValue.toString()));
//                    Profile.reloadPreferences(AllPrefsFragment.this.prefs);
//                    Home.staticRefreshBGCharts();
//                    return true;
//                }
//            });

            //profile_carb_ratio_default.setTitle(format_carb_ratio(profile_carb_ratio_default.getTitle().toString(), this.prefs.getString("profile_carb_ratio_default", "")));







//            profile_insulin_sensitivity_default.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    if (!isNumeric(newValue.toString())) {
//                        return false;
//                    }
//                    do_format_insulin_sensitivity(preference, AllPrefsFragment.this.prefs, true, newValue.toString());
//                    Profile.reloadPreferences(AllPrefsFragment.this.prefs);
//                    Home.staticRefreshBGCharts();
//                    return true;
//                }
//            });

            //do_format_insulin_sensitivity(profile_insulin_sensitivity_default, this.prefs, false, null);


            locale_choice = (ListPreference) findPreference("forced_language");
            force_english = findPreference("force_english");


            update_force_english_title("");
            force_english.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                                            @Override
                                                            public boolean onPreferenceChange(Preference preference, Object newValue) {
                                                                prefs.edit().putBoolean("force_english", (boolean) newValue).commit();
                                                                SdcardImportExport.hardReset();
                                                                return true;
                                                            }
                                                        }
            );
            locale_choice.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                                            @Override
                                                            public boolean onPreferenceChange(Preference preference, Object newValue) {
                                                                prefs.edit().putString("forced_language", (String) newValue).commit();
                                                                update_force_english_title((String)newValue);
                                                                if (prefs.getBoolean("force_english", false)) {
                                                                    SdcardImportExport.hardReset();
                                                                }
                                                                return true;
                                                            }
                                                        }
            );

            final Preference profile_carb_absorption_default = findPreference("profile_carb_absorption_default");
            profile_carb_absorption_default.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!isNumeric(newValue.toString())) {
                        return false;
                    }
                    preference.setTitle(format_carb_absorption_rate(preference.getTitle().toString(), newValue.toString()));
                    Profile.reloadPreferences(AllPrefsFragment.this.prefs);
                    Home.staticRefreshBGCharts();
                    return true;
                }
            });

            profile_carb_absorption_default.setTitle(format_carb_absorption_rate(profile_carb_absorption_default.getTitle().toString(), this.prefs.getString("profile_carb_absorption_default", "")));


            refresh_extra_items();
            findPreference("plus_extra_features").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Home.invalidateMenu = true; // force redraw
                    refresh_extra_items();

                    return true;
                }
            });

            final Preference crash_reports = findPreference("enable_crashlytics");
            crash_reports.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(preference.getContext(),
                            "Crash Setting takes effect on next restart", Toast.LENGTH_LONG).show();
                    return true;
                }
            });

            bindTTSListener();
            final Preference collectionMethod = findPreference("dex_collection_method");
            final Preference runInForeground = findPreference("run_service_in_foreground");
            final Preference scanConstantly = findPreference("run_ble_scan_constantly");
            final Preference runOnMain = findPreference("run_G5_ble_tasks_on_uithread");
            final Preference reAuth = findPreference("always_get_new_keys");
            final Preference reBond = findPreference("always_unbond_G5");
            final Preference wifiRecievers = findPreference("wifi_recievers_addresses");
            final Preference predictiveBG = findPreference("predictive_bg");
            final Preference interpretRaw = findPreference("interpret_raw");

            final Preference nfcSettings = findPreference("xdrip_plus_nfc_settings");
            //DexCollectionType collectionType = DexCollectionType.getType(findPreference("dex_collection_method").)

            final ListPreference currentCalibrationPlugin = (ListPreference)findPreference("current_calibration_plugin");


            final Preference shareKey = findPreference("share_key");
            shareKey.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AllPrefsFragment.this.prefs.edit().remove("dexcom_share_session_id").apply();
                    return true;
                }
            });

            Preference.OnPreferenceChangeListener shareTokenResettingListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    AllPrefsFragment.this.prefs.edit().remove("dexcom_share_session_id").apply();
                    return true;
                }
            };

            final Preference sharePassword = findPreference("dexcom_account_password");
            sharePassword.setOnPreferenceChangeListener(shareTokenResettingListener);
            final Preference shareAccountName = findPreference("dexcom_account_name");
            shareAccountName.setOnPreferenceChangeListener(shareTokenResettingListener);

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
            final Preference pebbleTinyDots = findPreference("pebble_tiny_dots");
            final EditTextPreference pebbleSpecialValue = (EditTextPreference) findPreference("pebble_special_value");
            bindPreferenceSummaryToValueAndEnsureNumeric(pebbleSpecialValue);
            final Preference pebbleSpecialText = findPreference("pebble_special_text");
            bindPreferenceSummaryToValue(pebbleSpecialText);
            // Pebble Trend - END

            final Preference useCustomSyncKey = findPreference("use_custom_sync_key");
            final Preference CustomSyncKey = findPreference("custom_sync_key");
            final PreferenceCategory collectionCategory = (PreferenceCategory) findPreference("collection_category");
            final PreferenceScreen motionScreen = (PreferenceScreen) findPreference("xdrip_plus_motion_settings");
            final PreferenceScreen nfcScreen = (PreferenceScreen) findPreference("xdrip_plus_nfc_settings");
            final PreferenceCategory otherCategory = (PreferenceCategory) findPreference("other_category");
            final PreferenceScreen calibrationAlertsScreen = (PreferenceScreen) findPreference("calibration_alerts_screen");
            final PreferenceCategory alertsCategory = (PreferenceCategory) findPreference("alerts_category");
            final Preference disableAlertsStaleDataMinutes = findPreference("disable_alerts_stale_data_minutes");
            final PreferenceScreen calibrationSettingsScreen = (PreferenceScreen) findPreference("xdrip_plus_calibration_settings");
            final Preference adrian_calibration_mode = findPreference("adrian_calibration_mode");
            final Preference extraTagsForLogs = findPreference("extra_tags_for_logging");

            disableAlertsStaleDataMinutes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
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
                }
            });

            final Preference showShowcase = findPreference("show_showcase");
            showShowcase.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((boolean)newValue)
                    {
                        ShotStateStore.resetAllShots();
                        JoH.static_toast(preference.getContext(),getString(R.string.interface_tips_from_start),Toast.LENGTH_LONG);
                    }
                    return true;
                }
            });

            units_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    try {
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
                    }
                    return true;
                }
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

            useCustomSyncKey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    PlusSyncService.clearandRestartSyncService(context);
                    return true;
                }
            });
            CustomSyncKey.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    Context context = preference.getContext();
                    PlusSyncService.clearandRestartSyncService(context);
                    return true;
                }
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

            try {
                findPreference("nfc_scan_homescreen").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), (boolean) newValue && (NFCReaderX.useNFC()));
                        return true;
                    }
                });
            } catch (NullPointerException e) {
                Log.d(TAG, "Nullpointer looking for nfc_scan_homescreen");
            }
            try {
                findPreference("use_nfc_scan").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
                        if ((boolean) newValue) {
                            builder.setTitle("Stop! Are you sure?");
                            builder.setMessage("This can sometimes crash / break a sensor!\nWith some phones there can be problems, try on expiring sensor first for safety. You have been warned.");

                            builder.setPositiveButton("I AM SURE", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ((SwitchPreference)preference).setChecked(true);
                                    preference.getEditor().putBoolean("use_nfc_scan", true).apply();
                                    NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), (boolean) newValue && prefs.getBoolean("nfc_scan_homescreen", false));
                                }
                            });
                            builder.setNegativeButton("NOPE", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            final AlertDialog alert = builder.create();
                            alert.show();
                            return false;
                        } else {
                            NFCReaderX.handleHomeScreenScanPreference(xdrip.getAppContext(), (boolean) newValue && prefs.getBoolean("nfc_scan_homescreen", false));
                        }
                        return true;
                    }
                });
            } catch (NullPointerException e) {
                Log.d(TAG, "Nullpointer looking for nfc_scan");
            }

            final boolean engineering_mode = this.prefs.getBoolean("engineering_mode",false);

            if (engineering_mode) {
                // populate the list
                PluggableCalibration.setListPreferenceData(currentCalibrationPlugin);
            }

            if (!DexCollectionType.hasLibre(collectionType)) {
                collectionCategory.removePreference(nfcSettings);
            } else {
                if (!engineering_mode)
                    try {
                        nfcScreen.removePreference(findPreference("nfc_test_diagnostic"));
                    } catch (NullPointerException e) {
                        //
                    }
            }

            try {

                if ((collectionType != DexCollectionType.WifiWixel)
                        && (collectionType != DexCollectionType.WifiBlueToothWixel)
                        && (collectionType != DexCollectionType.WifiDexBridgeWixel)) {
                    String receiversIpAddresses;
                    receiversIpAddresses = this.prefs.getString("wifi_recievers_addresses", "");
                    // only hide if non wifi wixel mode and value not previously set to cope with
                    // dynamic mode changes. jamorham
                    if (receiversIpAddresses == null || receiversIpAddresses.equals("")) {
                        collectionCategory.removePreference(wifiRecievers);
                    }
                }

                if ((collectionType != DexCollectionType.DexbridgeWixel)
                        && (collectionType != DexCollectionType.WifiDexBridgeWixel)) {
                    collectionCategory.removePreference(transmitterId);
                    // collectionCategory.removePreference(closeGatt);
                }


                if (collectionType == DexCollectionType.DexcomG5) {
                    collectionCategory.addPreference(transmitterId);
                    collectionCategory.addPreference(scanConstantly);
                    collectionCategory.addPreference(reAuth);
                    collectionCategory.addPreference(reBond);
                    collectionCategory.addPreference(runOnMain);
                } else {
                    // collectionCategory.removePreference(transmitterId);
                    collectionCategory.removePreference(scanConstantly);
                    collectionCategory.removePreference(reAuth);
                    collectionCategory.removePreference(reBond);
                    collectionCategory.removePreference(runOnMain);
                }

                if (!engineering_mode) {
                    getPreferenceScreen().removePreference(motionScreen);
                    calibrationSettingsScreen.removePreference(adrian_calibration_mode);
                }

            } catch (NullPointerException e) {
                Log.wtf(TAG, "Got null pointer exception removing pref: " + e);
            }

            if (engineering_mode || this.prefs.getString("update_channel","").matches("alpha|nightly")) {
                ListPreference update_channel = (ListPreference)findPreference("update_channel");
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


            bindPreferenceSummaryToValue(collectionMethod);
            bindPreferenceSummaryToValue(shareKey);
//            bindPreferenceSummaryToValue(wifiRecievers);

            wifiRecievers.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    ParakeetHelper.notifyOnNextCheckin(true);
                    return true;
                }
            });

            // Pebble Trend -- START

            int currentPebbleSync = PebbleUtil.getCurrentPebbleSyncType(this.prefs);

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
            pebbleSync1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final Context context = preference.getContext();
                    if ((Boolean) newValue) {


                        pebbleType = PebbleUtil.getCurrentPebbleSyncType(PreferenceManager.getDefaultSharedPreferences(context).getString("broadcast_to_pebble_type", "1"));

                        // install watchface
                        installPebbleWatchface(pebbleType, preference);
                    }
                    // start/stop service
                    enablePebble(pebbleType, (Boolean) newValue, context);
                    return true;
                }
            });


            // Pebble Trend (just major change)
            pebbleSync2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final Context context = preference.getContext();

                    int oldPebbleType = PebbleUtil.getCurrentPebbleSyncType(AllPrefsFragment.this.prefs);
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
                        }

                        if (oldPebbleType != 1) {
                            watchCategory.addPreference(pebbleSpecialValue);
                            watchCategory.addPreference(pebbleSpecialText);
                        }

                    }

                    return true;
                }
            });


            pebbleTrend.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });

            pebbleFilteredLine.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });


            pebbleHighLine.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });

            pebbleLowLine.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });

            pebbleTrendPeriod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });
            pebbleDelta.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });
            pebbleDeltaUnits.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });
            pebbleShowArrows.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });

            pebbleTinyDots.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    context.startService(new Intent(context, PebbleWatchSync.class));
                    return true;
                }
            });
            // Pebble Trend -- END

            bindWidgetUpdater();

            extraTagsForLogs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ExtraLogTags.readPreference((String)newValue);
                    return true;
                }
            });
            bindPreferenceSummaryToValue(transmitterId);
            transmitterId.getEditText().setFilters(new InputFilter[]{new InputFilter.AllCaps()});


                    collectionMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    DexCollectionType collectionType = DexCollectionType.getType((String) newValue);

                    if (collectionType != DexCollectionType.DexcomShare) { // NOT USING SHARE
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

                    if (DexCollectionType.hasLibre(collectionType)) {
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


                    if (collectionType != DexCollectionType.BluetoothWixel
                            && collectionType != DexCollectionType.DexcomShare
                            && collectionType != DexCollectionType.WifiWixel
                            && collectionType != DexCollectionType.DexbridgeWixel
                            && collectionType != DexCollectionType.LimiTTer
                            && collectionType != DexCollectionType.DexcomG5
                            && collectionType != DexCollectionType.WifiBlueToothWixel
                            && collectionType != DexCollectionType.WifiDexBridgeWixel
                            && collectionType != DexCollectionType.LibreAlarm
                            ) {
                        collectionCategory.removePreference(runInForeground);
                    } else {
                        collectionCategory.addPreference(runInForeground);
                    }

                    // jamorham always show wifi receivers option if populated as we may switch modes dynamically
                    if (collectionType != DexCollectionType.WifiWixel
                            && collectionType != DexCollectionType.WifiBlueToothWixel
                            && collectionType != DexCollectionType.WifiDexBridgeWixel) {
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

                    if ((collectionType != DexCollectionType.DexbridgeWixel)
                            && (collectionType != DexCollectionType.WifiDexBridgeWixel)) {
                        collectionCategory.removePreference(transmitterId);
                        //collectionCategory.removePreference(closeGatt);
                        //TODO Bridge battery display support
                    } else {
                        collectionCategory.addPreference(transmitterId);
                     //   collectionCategory.addPreference(closeGatt);
                    }

                    if (collectionType == DexCollectionType.DexcomG5) {
                        collectionCategory.addPreference(transmitterId);
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
                        CollectionServiceStarter.restartCollectionService(preference.getContext(), (String) newValue);
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
                    } else {
                        CollectionServiceStarter.restartCollectionService(preference.getContext());
                    }
                    return true;
                }
            });
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
            findPreference("extra_status_stats_24h").setOnPreferenceChangeListener(new WidgetListener());

        }

        private void update_force_english_title(String param) {
            String word;
            if (param.length() == 0) {
                word = locale_choice.getEntry().toString();
            } else {
                try {
                    word = (locale_choice.getEntries()[locale_choice.findIndexOfValue(param)]).toString();
                } catch (Exception e) {
                    word = "Unknown";
                }
            }
            force_english.setTitle("Force " + word + " Text");
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

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

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


            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
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

                    JoH.runOnUiThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Snooze Control Install");
                            builder.setMessage("Install Pebble Snooze Button App?");
                            // inner
                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    context.startActivity(new Intent(context, InstallPebbleSnoozeControlApp.class));
                                }
                            });
                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }},3000);
                // outer
                }});

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
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
            findPreference("auto_configure").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
            });
        }


        private void setupBarcodeShareScanner() {
            findPreference("scan_share2_barcode").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
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
            findPreference("bg_to_speech").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((Boolean) newValue) {
                        prefs.edit().putBoolean("bg_to_speech", true).commit(); // early write before we exit method
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle("Install Text-To-Speech Data?");
                        alertDialog.setMessage("Install Text-To-Speech Data?\n(After installation of languages you might have to press \"Restart Collector\" in System Status.)");
                        alertDialog.setCancelable(true);
                        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BgToSpeech.installTTSData(getActivity());
                            }
                        });
                        alertDialog.setNegativeButton(R.string.no, null);
                        AlertDialog alert = alertDialog.create();
                        alert.show();
                        try {
                            BgToSpeech.setupTTS(preference.getContext()); // try to initialize now
                            BgReading bgReading = BgReading.last();
                            if (bgReading != null) {
                                BgToSpeech.speak(bgReading.calculated_value, bgReading.timestamp+1200000);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Got exception with TTS: " + e);
                        }
                    } else {
                        BgToSpeech.tearDownTTS();
                    }
                    return true;
                }
            });
        }


        private static Preference.OnPreferenceChangeListener sBgMissedAlertsHandler = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = preference.getContext();
                context.startService(new Intent(context, MissedReadingService.class));
                return true;
            }
        };


        private void bindBgMissedAlertsListener() {
            findPreference("other_alerts_snooze").setOnPreferenceChangeListener(sBgMissedAlertsHandler);
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

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}

