package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Constants;
import com.eveningoutpost.dexdrip.Models.Accuracy;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.Services.PlusSyncService;
import com.eveningoutpost.dexdrip.Services.WixelReader;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Experience;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.JamorhamShowcaseDrawer;
import com.eveningoutpost.dexdrip.UtilityModels.NightscoutUploader;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.PumpStatus;
import com.eveningoutpost.dexdrip.UtilityModels.SendFeedBack;
import com.eveningoutpost.dexdrip.UtilityModels.ShotStateStore;
import com.eveningoutpost.dexdrip.UtilityModels.UndoRedo;
import com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.languageeditor.LanguageEditor;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;
import com.eveningoutpost.dexdrip.stats.StatsResult;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.utils.DatabaseUtil;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.DisplayQRCode;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.utils.SdcardImportExport;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;

import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.X;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPlugin;
import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;


public class Home extends ActivityWithMenu {
    private final static String TAG = "jamorham: " + Home.class.getSimpleName();
    private final static boolean d = true;
    public final static String START_SPEECH_RECOGNITION = "START_APP_SPEECH_RECOGNITION";
    public final static String START_TEXT_RECOGNITION = "START_APP_TEXT_RECOGNITION";
    public final static String CREATE_TREATMENT_NOTE = "CREATE_TREATMENT_NOTE";
    public final static String BLOOD_TEST_ACTION = "BLOOD_TEST_ACTION";
    public final static String HOME_FULL_WAKEUP = "HOME_FULL_WAKEUP";
    public final static String GCM_RESOLUTION_ACTIVITY = "GCM_RESOLUTION_ACTIVITY";
    public final static String SNOOZE_CONFIRM_DIALOG = "SNOOZE_CONFIRM_DIALOG";
    public final static String SHOW_NOTIFICATION = "SHOW_NOTIFICATION";
    public final static String BLUETOOTH_METER_CALIBRATION = "BLUETOOTH_METER_CALIBRATION";
    public final static String ACTIVITY_SHOWCASE_INFO = "ACTIVITY_SHOWCASE_INFO";
    public final static int SENSOR_READY_ID = 4912;
    public static String menu_name = "Home Screen";
    public static boolean activityVisible = false;
    public static boolean invalidateMenu = false;
    public static boolean blockTouches = false;
    private static boolean is_follower = false;
    private static boolean is_follower_set = false;
    private static boolean is_holo = true;
    private static boolean reset_viewport = false;
    private boolean updateStuff;
    private boolean updatingPreviewViewport = false;
    private boolean updatingChartViewport = false;
    private BgGraphBuilder bgGraphBuilder;
    private static SharedPreferences prefs;
    private Viewport tempViewport = new Viewport();
    private Viewport holdViewport = new Viewport();
    private boolean isBTShare;
    private boolean isG5Share;
    private BroadcastReceiver _broadcastReceiver;
    private BroadcastReceiver newDataReceiver;
    private BroadcastReceiver statusReceiver;
    private LineChartView chart;
    private ImageButton btnSpeak;
    private ImageButton btnNote;
    private ImageButton btnApprove;
    private ImageButton btnCancel;
    private ImageButton btnCarbohydrates;
    private ImageButton btnBloodGlucose;
    private ImageButton btnInsulinDose;
    private ImageButton btnTime;
    private ImageButton btnUndo;
    private ImageButton btnRedo;
    private ImageButton btnVehicleMode;
    private TextView voiceRecognitionText;
    private TextView textCarbohydrates;
    private TextView textBloodGlucose;
    private TextView textInsulinDose;
    private TextView textTime;
    private static final int REQ_CODE_SPEECH_INPUT = 1994;
    private static final int REQ_CODE_SPEECH_NOTE_INPUT = 1995;
    private static final int SHOWCASE_UNDO = 4;
    private static final int SHOWCASE_REDO = 5;
    private static final int SHOWCASE_NOTE_LONG = 6;
    private static final int SHOWCASE_VARIANT = 7;
    public static final int SHOWCASE_STATISTICS = 8;
    private static final int SHOWCASE_G5FIRMWARE = 9;
    static final int SHOWCASE_MEGASTATUS = 10;
    public static final int SHOWCASE_MOTION_DETECTION = 11;
    public static final int SHOWCASE_MDNS = 12;
    public static final int SHOWCASE_REMINDER1 = 14;
    public static final int SHOWCASE_REMINDER2 = 15;
    public static final int SHOWCASE_REMINDER3 = 16;
    public static final int SHOWCASE_REMINDER4 = 17;
    public static final int SHOWCASE_REMINDER5 = 19;
    public static final int SHOWCASE_REMINDER6 = 20;
    private static double last_speech_time = 0;
    private PreviewLineChartView previewChart;
    private Button stepsButton;
    private Button bpmButton;
    private TextView dexbridgeBattery;
    private TextView parakeetBattery;
    private TextView sensorAge;
    private TextView currentBgValueText;
    private TextView notificationText;
    private TextView extraStatusLineText;
    private boolean alreadyDisplayedBgInfoCommon = false;
    private boolean recognitionRunning = false;
    private String display_delta = "";
    private boolean small_width = false;
    private boolean small_height = false;
    private boolean small_screen = false;
    double thisnumber = -1;
    double thisglucosenumber = 0;
    double thiscarbsnumber = 0;
    double thisinsulinnumber = 0;
    double thistimeoffset = 0;
    String thisword = "";
    String thisuuid = "";
    private static String nexttoast;
    boolean carbsset = false;
    boolean insulinset = false;
    boolean glucoseset = false;
    boolean timeset = false;
    boolean watchkeypad = false;
    boolean watchkeypadset = false;
    long watchkeypad_timestamp = -1;
    private wordDataWrapper searchWords = null;
    private AlertDialog dialog;
    private AlertDialog helper_dialog;

    private static final boolean oneshot = true;
    private static ShowcaseView myShowcase;
    private static Activity mActivity;

    private static String statusIOB = "";
    private static String statusBWP = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivity = this;

        if (!xdrip.checkAppContext(getApplicationContext())) {
            toast("Unusual internal context problem - please report");
            Log.wtf(TAG, "xdrip.checkAppContext FAILED!");
            try {
                xdrip.initCrashlytics(getApplicationContext());
                Crashlytics.log("xdrip.checkAppContext FAILED!");
            } catch (Exception e) {
                // nothing we can do really
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // not much to do
            }
            if (!xdrip.checkAppContext(getApplicationContext())) {
                toast("Cannot start - please report context problem");
                finish();
            }
        }

        if (Build.VERSION.SDK_INT < 17) {
            JoH.static_toast_long("xDrip+ will not work below Android version 4.2");
            finish();
        }

        xdrip.checkForcedEnglish(Home.this);
        menu_name=getString(R.string.home_screen);

        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeToolBarLite); // for toolbar mode

        set_is_follower();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final boolean checkedeula = checkEula();

        setContentView(R.layout.activity_home);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);

        //findViewById(R.id.home_layout_holder).setBackgroundColor(getCol(X.color_home_chart_background));
        this.dexbridgeBattery = (TextView) findViewById(R.id.textBridgeBattery);
        this.parakeetBattery = (TextView) findViewById(R.id.parakeetbattery);
        this.sensorAge = (TextView) findViewById(R.id.libstatus);
        this.extraStatusLineText = (TextView) findViewById(R.id.extraStatusLine);
        this.currentBgValueText = (TextView) findViewById(R.id.currentBgValueRealTime);
        this.bpmButton = (Button) findViewById(R.id.bpmButton);
        this.stepsButton = (Button) findViewById(R.id.walkButton);

        extraStatusLineText.setText("");
        dexbridgeBattery.setText("");
        parakeetBattery.setText("");
        sensorAge.setText("");

        if (BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            this.currentBgValueText.setTextSize(100);
        }
        this.notificationText = (TextView) findViewById(R.id.notices);
        if (BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            this.notificationText.setTextSize(40);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String packageName = getPackageName();
            //Log.d(TAG, "Maybe ignoring battery optimization");
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName) &&
                    !prefs.getBoolean("requested_ignore_battery_optimizations_new", false)) {
                Log.d(TAG, "Requesting ignore battery optimization");

                if (PersistentStore.incrementLong("asked_battery_optimization") < 40) {
                    JoH.show_ok_dialog(this, "Please Allow Permission", "xDrip+ needs whitelisting for proper performance", new Runnable() {

                        @Override
                        public void run() {
                            try {
                                final Intent intent = new Intent();

                                // ignoring battery optimizations required for constant connection
                                // to peripheral device - eg CGM transmitter.
                                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + packageName));
                                startActivity(intent);

                            } catch (ActivityNotFoundException e) {
                                final String msg = "Device does not appear to support battery optimization whitelisting!";
                                JoH.static_toast_short(msg);
                                UserError.Log.wtf(TAG, msg);
                            }
                        }
                    });
                } else {
                    JoH.static_toast_long("This app needs battery optimization whitelisting or it will not work well. Please reset app preferences");
                }
            }
        }


        // jamorham voice input et al
        this.voiceRecognitionText = (TextView) findViewById(R.id.treatmentTextView);
        this.textBloodGlucose = (TextView) findViewById(R.id.textBloodGlucose);
        this.textCarbohydrates = (TextView) findViewById(R.id.textCarbohydrate);
        this.textInsulinDose = (TextView) findViewById(R.id.textInsulinUnits);
        this.textTime = (TextView) findViewById(R.id.textTimeButton);
        this.btnBloodGlucose = (ImageButton) findViewById(R.id.bloodTestButton);
        this.btnCarbohydrates = (ImageButton) findViewById(R.id.buttonCarbs);
        this.btnInsulinDose = (ImageButton) findViewById(R.id.buttonInsulin);
        this.btnCancel = (ImageButton) findViewById(R.id.cancelTreatment);
        this.btnApprove = (ImageButton) findViewById(R.id.approveTreatment);
        this.btnTime = (ImageButton) findViewById(R.id.timeButton);
        this.btnUndo = (ImageButton) findViewById(R.id.btnUndo);
        this.btnRedo = (ImageButton) findViewById(R.id.btnRedo);
        this.btnVehicleMode = (ImageButton) findViewById(R.id.vehicleModeButton);

        hideAllTreatmentButtons();

        if (searchWords == null) {
            initializeSearchWords("");
        }

        this.btnSpeak = (ImageButton) findViewById(R.id.btnTreatment);
        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptTextInput();

            }
        });
        btnSpeak.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                promptSpeechInput();
                return true;
            }
        });

        this.btnNote = (ImageButton) findViewById(R.id.btnNote);
        btnNote.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Home.getPreferencesBooleanDefaultFalse("default_to_voice_notes")) {
                    showNoteTextInputDialog(v,0);
                } else {
                    promptSpeechNoteInput(v);
                }
                return false;
            }
        });
        btnNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Home.getPreferencesBooleanDefaultFalse("default_to_voice_notes")) {
                    showNoteTextInputDialog(v,0);
                } else {
                    promptSpeechNoteInput(v);
                }
            }
        });


        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cancelTreatment();
            }
        });

        btnApprove.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                processAndApproveTreatment();
            }
        });

        btnInsulinDose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // proccess and approve treatment
                textInsulinDose.setVisibility(View.INVISIBLE);
                btnInsulinDose.setVisibility(View.INVISIBLE);
                Treatments.create(0, thisinsulinnumber, Treatments.getTimeStampWithOffset(thistimeoffset));
                thisinsulinnumber=0;
                reset_viewport = true;
                if (hideTreatmentButtonsIfAllDone()) {
                    updateCurrentBgInfo("insulin button");
                }
            }
        });
        btnCarbohydrates.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // proccess and approve treatment
                textCarbohydrates.setVisibility(View.INVISIBLE);
                btnCarbohydrates.setVisibility(View.INVISIBLE);
                reset_viewport = true;
                Treatments.create(thiscarbsnumber, 0, Treatments.getTimeStampWithOffset(thistimeoffset));
                thiscarbsnumber=0;
                if (hideTreatmentButtonsIfAllDone()) {
                    updateCurrentBgInfo("carbs button");
                }
            }
        });

        btnBloodGlucose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reset_viewport = true;
                processCalibration();
            }
        });

        btnTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // clears time if clicked
                textTime.setVisibility(View.INVISIBLE);
                btnTime.setVisibility(View.INVISIBLE);
                if (hideTreatmentButtonsIfAllDone()) {
                    updateCurrentBgInfo("time button");
                }
            }
        });


        final DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screen_width = dm.widthPixels;
        int screen_height = dm.heightPixels;



        if (screen_width <= 320) { small_width =true; small_screen = true; }
        if (screen_height <= 320) { small_height = true; small_screen = true; }
        //final int refdpi = 320;
        Log.d(TAG, "Width height: " + screen_width + " " + screen_height+" DPI:"+dm.densityDpi);


        JoH.fixActionBar(this);
        try {
            getSupportActionBar().setTitle(R.string.app_name);
        } catch (NullPointerException e) {
            Log.e(TAG, "Couldn't set title due to null pointer");
        }
        activityVisible = true;


        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                final String bwp = intent.getStringExtra("bwp");
                if (bwp != null) {
                    statusBWP = bwp;
                    refreshStatusLine();
                } else {
                    final String iob = intent.getStringExtra("iob");
                    if (iob != null) {
                        statusIOB = iob;
                        refreshStatusLine();
                    }
                }
            }
        };


        // handle incoming extras
        final Bundle bundle = getIntent().getExtras();
        processIncomingBundle(bundle);

        checkBadSettings();
        // lower priority
        PlusSyncService.startSyncService(getApplicationContext(), "HomeOnCreate");
        ParakeetHelper.notifyOnNextCheckin(false);

        if ((checkedeula) && (!getString(R.string.app_name).equals("xDrip+"))) {
            showcasemenu(SHOWCASE_VARIANT);
        }


        if ((checkedeula) && Experience.isNewbie()) {
            if (!SdcardImportExport.handleBackup(this)) {
                if (!Home.getPreferencesStringWithDefault("units", "mgdl").equals("mmol")) {
                    Log.d(TAG, "Newbie mmol prompt");
                    if (Experience.defaultUnitsAreMmol()) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Glucose units mmol/L or mg/dL");
                        builder.setMessage("Is your typical blood glucose value:\n\n5.5 (mmol/L)\nor\n100 (mg/dL)\n\nPlease select below");

                        builder.setNegativeButton("5.5", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Home.setPreferencesString("units", "mmol");
                                Preferences.handleUnitsChange(null, "mmol", null);
                                Home.staticRefreshBGCharts();
                                toast("Settings updated to mmol/L");
                            }
                        });

                        builder.setPositiveButton("100", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.create().show();
                    }
                }
            }
        }

    }

    ////

    private void refreshStatusLine() {
        try {
            String status = ((statusIOB.length() > 0) ? ("IoB: " + statusIOB) : "")
                    + ((statusBWP.length()>0) ? (" "+statusBWP) : "");
            Log.d(TAG, "Refresh Status Line: " + status);
            //if (status.length() > 0) {
                getSupportActionBar().setSubtitle(status);
           // }
        } catch (NullPointerException e) {
            Log.e(TAG, "Could not set subtitle due to null pointer exception: " + e);
        }
    }

    public static void updateStatusLine(String key, String value) {
        final Intent homeStatus = new Intent(Intents.HOME_STATUS_ACTION);
        homeStatus.putExtra(key, value);
        LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(homeStatus);
        Log.d(TAG, "Home Status update: " + key + " / " + value);
    }

    private void checkBadSettings()
    {
        if (getPreferencesBoolean("predictive_bg",false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Settings Issue!");
                builder.setMessage("You have an old experimental glucose prediction setting enabled.\n\nThis is NOT RECOMMENDED and could mess things up badly.\n\nShall I disable this for you?");

                builder.setPositiveButton("YES, Please", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setPreferencesBoolean("predictive_bg",false);
                        toast("Setting disabled :)");
                    }
                });

                builder.setNegativeButton("No, I really know what I'm doing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
        }
    }


    // handle sending the intent
    private void processFingerStickCalibration(final double glucosenumber, final double timeoffset, boolean dontask) {
        if (glucosenumber > 0) {

            if (timeoffset < 0) {
                toaststaticnext("Got calibration in the future - cannot process!");
                return;
            }

            final Intent calintent = new Intent(getApplicationContext(), AddCalibration.class);
            calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            calintent.putExtra("bg_string", JoH.qs(glucosenumber));
            calintent.putExtra("bg_age", Long.toString((long) (timeoffset / 1000)));
            calintent.putExtra("allow_undo", "true");
            Log.d(TAG, "processFingerStickCalibration number: " + glucosenumber + " offset: " + timeoffset);

            if (dontask) {
                Log.d(TAG, "Proceeding with calibration intent without asking");
                startIntentThreadWithDelayedRefresh(calintent);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Use " + JoH.qs(glucosenumber, 1) + " for Calibration?");
                builder.setMessage("Do you want to use this synced finger-stick blood glucose result to calibrate with?\n\n(you can change when this dialog is displayed in Settings)");

                builder.setPositiveButton("YES, Calibrate", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        calintent.putExtra("note_only", "false");
                        calintent.putExtra("from_interactive", "true");
                        startIntentThreadWithDelayedRefresh(calintent);
                        dialog.dismiss();
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
            }
        }
    }


    // handle sending the intent
    private void processCalibrationNoUI(final double glucosenumber, final double timeoffset) {
                if (glucosenumber > 0) {

                    if (timeoffset < 0) {
                        toaststaticnext("Got calibration in the future - cannot process!");
                        return;
                    }

                    final Intent calintent = new Intent(getApplicationContext(), AddCalibration.class);

                    calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    calintent.putExtra("bg_string", JoH.qs(glucosenumber));
                    calintent.putExtra("bg_age", Long.toString((long) (timeoffset / 1000)));
                    calintent.putExtra("allow_undo", "true");
                    Log.d(TAG, "ProcessCalibrationNoUI number: " + glucosenumber + " offset: " + timeoffset);

                    final String calibration_type = getPreferencesStringWithDefault("treatment_fingerstick_calibration_usage","ask");
                    if (calibration_type.equals("ask")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Use BG for Calibration?");
                        builder.setMessage("Do you want to use this entered finger-stick blood glucose test to calibrate with?\n\n(you can change when this dialog is displayed in Settings)");

                        builder.setPositiveButton("YES, Calibrate", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                calintent.putExtra("note_only", "false");
                                calintent.putExtra("from_interactive", "true");
                                startIntentThreadWithDelayedRefresh(calintent);
                                dialog.dismiss();
                            }
                        });

                        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO make this a blood test entry xx
                                calintent.putExtra("note_only", "true");
                                startIntentThreadWithDelayedRefresh(calintent);
                                dialog.dismiss();
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                    } else if (calibration_type.equals("auto")) {
                        Log.d(TAG, "Creating bloodtest  record from cal input data");
                        BloodTest.createFromCal(glucosenumber, timeoffset, "Manual Entry");
                        GcmActivity.syncBloodTests();
                        if ((!Home.getPreferencesBooleanDefaultFalse("bluetooth_meter_for_calibrations_auto"))
                                && (DexCollectionType.getDexCollectionType() != DexCollectionType.Follower)
                                && (JoH.pratelimit("ask_about_auto_calibration", 86400 * 30))) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("Enable automatic calibration");
                            builder.setMessage("Entered blood tests which occur during flat trend periods can automatically be used to recalibrate after 20 minutes. This should provide the most accurate method to calibrate with.\n\nDo you want to enable this feature?");

                            builder.setPositiveButton("YES, enable", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Home.setPreferencesBoolean("bluetooth_meter_for_calibrations_auto", true);
                                    JoH.static_toast_long("Automated calibration enabled");
                                    dialog.dismiss();
                                }
                            });

                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            final AlertDialog alert = builder.create();
                            alert.show();
                        }
                        // offer choice to enable auto-calibration mode if not already enabled on pratelimit
                    } else {
                        // if use for calibration == "no" then this is a "note_only" type, otherwise it isn't
                        calintent.putExtra("note_only", calibration_type.equals("never") ? "true" : "false");
                        startIntentThreadWithDelayedRefresh(calintent);
                    }
                }
    }

    static void startIntentThreadWithDelayedRefresh(final Intent intent)
    {
        new Thread() {
            @Override
            public void run() {
                xdrip.getAppContext().startActivity(intent);
                staticRefreshBGCharts();
            }
        }.start();

        JoH.runOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                staticRefreshBGCharts();
            }
        }, 4000);
    }

    private void processCalibration() {
        // TODO BG Tests to be possible without being calibrations
        // TODO Offer Choice? Reject calibrations under various circumstances
        // This should be wrapped up in a generic method
        processCalibrationNoUI(thisglucosenumber, thistimeoffset);

        textBloodGlucose.setVisibility(View.INVISIBLE);
        btnBloodGlucose.setVisibility(View.INVISIBLE);
        if (hideTreatmentButtonsIfAllDone()) {
            updateCurrentBgInfo("bg button");
        }


    }

    private void cancelTreatment() {
        hideAllTreatmentButtons();
        WatchUpdaterService.sendWearToast("Treatment cancelled", Toast.LENGTH_SHORT);
    }

    private void processAndApproveTreatment() {
        // preserve globals before threading off
        final double myglucosenumber = thisglucosenumber;
        double mytimeoffset = thistimeoffset;
        // proccess and approve all treatments
        // TODO Handle BG Tests here also
        if (watchkeypad) {
            // calculate absolute offset
            long treatment_timestamp = watchkeypad_timestamp - (long) mytimeoffset;
            mytimeoffset = JoH.tsl() - treatment_timestamp;
            Log.d(TAG, "Watch Keypad timestamp is: " + JoH.dateTimeText(treatment_timestamp) + " Original offset: " + JoH.qs(thistimeoffset) + " New: " + JoH.qs(mytimeoffset));
            if ((mytimeoffset > (DAY_IN_MS * 3)) || (mytimeoffset < -HOUR_IN_MS * 3)) {
                Log.e(TAG, "Treatment timestamp out of range: " + mytimeoffset);
                JoH.static_toast_long("Treatment time wrong");
                WatchUpdaterService.sendWearLocalToast("Treatment error", Toast.LENGTH_LONG);
            } else {
                JoH.static_toast_long("Treatment processed");
                WatchUpdaterService.sendWearLocalToast("Treatment processed", Toast.LENGTH_LONG);
                long time = Treatments.getTimeStampWithOffset(mytimeoffset);
                // sanity check timestamp
                final Treatments exists = Treatments.byTimestamp(time);
                if (exists == null) {
                    Log.d(TAG, "processAndApproveTreatment create watchkeypad Treatment carbs=" + thiscarbsnumber + " insulin=" + thisinsulinnumber + " timestamp=" + JoH.dateTimeText(time) + " uuid=" + thisuuid);
                    Treatments.create(thiscarbsnumber, thisinsulinnumber, time, thisuuid);
                } else {
                    Log.d(TAG, "processAndApproveTreatment Treatment already exists carbs=" + thiscarbsnumber + " insulin=" + thisinsulinnumber + " timestamp=" + JoH.dateTimeText(time));
                }
            }
        } else {
            WatchUpdaterService.sendWearToast("Treatment processed", Toast.LENGTH_LONG);
            Treatments.create(thiscarbsnumber, thisinsulinnumber, Treatments.getTimeStampWithOffset(mytimeoffset));
        }
        hideAllTreatmentButtons();

        if (hideTreatmentButtonsIfAllDone()) {
            updateCurrentBgInfo("approve button");
        }
        if (watchkeypad) {
            if (myglucosenumber > 0) {
                if ((mytimeoffset > (DAY_IN_MS * 3)) || (mytimeoffset < -HOUR_IN_MS * 3)) {
                    Log.e(TAG, "Treatment bloodtest timestamp out of range: " + mytimeoffset);
                } else {
                    BloodTest.createFromCal(myglucosenumber, mytimeoffset, "Manual Entry", thisuuid);
                }
            }
            watchkeypad = false;
            watchkeypadset = false;
            watchkeypad_timestamp = -1;
        } else
            processCalibrationNoUI(myglucosenumber, mytimeoffset);
        staticRefreshBGCharts();
    }

    private void processIncomingBundle(Bundle bundle) {
        Log.d(TAG, "Processing incoming bundle");
        if (bundle != null) {
            String receivedText = bundle.getString(WatchUpdaterService.WEARABLE_VOICE_PAYLOAD);
            if (receivedText != null) {
                voiceRecognitionText.setText(receivedText);
                voiceRecognitionText.setVisibility(View.VISIBLE);
                last_speech_time = JoH.ts();
                naturalLanguageRecognition(receivedText);
            }
            if (bundle.getString(WatchUpdaterService.WEARABLE_APPROVE_TREATMENT) != null || watchkeypad)
                processAndApproveTreatment();
            else if (bundle.getString(WatchUpdaterService.WEARABLE_CANCEL_TREATMENT) != null)
                cancelTreatment();
            else if (bundle.getString(Home.START_SPEECH_RECOGNITION) != null) promptSpeechInput();
            else if (bundle.getString(Home.START_TEXT_RECOGNITION) != null) promptTextInput_old();
            else if (bundle.getString(Home.CREATE_TREATMENT_NOTE) != null) {
                try {
                    showNoteTextInputDialog(null, Long.parseLong(bundle.getString(Home.CREATE_TREATMENT_NOTE)), JoH.tolerantParseDouble(bundle.getString(Home.CREATE_TREATMENT_NOTE + "2")));
                } catch (NullPointerException e) {
                    Log.d(TAG, "Got null point exception during CREATE_TREATMENT_NOTE Intent");
                } catch (NumberFormatException e) {
                    JoH.static_toast_long("Number error: " + e);
                }
            } else if (bundle.getString(Home.HOME_FULL_WAKEUP) != null) {
                if (!JoH.isScreenOn()) {
                    final int timeout = 60000;
                    final PowerManager.WakeLock wl = JoH.getWakeLock("full-wakeup", timeout + 1000);
                    final Window win = getWindow();
                    win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
                    final Timer t = new Timer();
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            JoH.releaseWakeLock(wl);
                            finish();
                        }
                    }, timeout);
                } else {
                    Log.d(TAG, "Screen is already on so not turning on");
                }
            } else if (bundle.getString(Home.GCM_RESOLUTION_ACTIVITY) != null) {
                GcmActivity.checkPlayServices(this, this);
            } else if (bundle.getString(Home.SNOOZE_CONFIRM_DIALOG) != null) {
                GcmActivity.sendSnoozeToRemoteWithConfirm(this);
            } else if (bundle.getString(Home.SHOW_NOTIFICATION) != null) {
                final Intent notificationIntent = new Intent(this, Home.class);
                final int notification_id = bundle.getInt("notification_id");
                if ((notification_id == SENSOR_READY_ID) && (!Sensor.isActive() || BgReading.last() != null)) {
                    Log.e(TAG, "Sensor not in warm up period when notification due to fire");
                    return;
                }
                final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                JoH.showNotification(bundle.getString(SHOW_NOTIFICATION), bundle.getString("notification_body"), pendingIntent, notification_id, true, true, true);
            } else if (bundle.getString(Home.BLUETOOTH_METER_CALIBRATION) != null) {
                try {
                    processFingerStickCalibration(JoH.tolerantParseDouble(bundle.getString(Home.BLUETOOTH_METER_CALIBRATION)),
                            JoH.tolerantParseDouble(bundle.getString(Home.BLUETOOTH_METER_CALIBRATION + "2")),
                            bundle.getString(Home.BLUETOOTH_METER_CALIBRATION + "3") != null && bundle.getString(Home.BLUETOOTH_METER_CALIBRATION + "3").equals("auto"));
                } catch (NumberFormatException e) {
                    JoH.static_toast_long("Number error: " + e);
                }
            } else if (bundle.getString(Home.ACTIVITY_SHOWCASE_INFO) != null) {
                showcasemenu(SHOWCASE_MOTION_DETECTION);
            } else if (bundle.getString(Home.BLOOD_TEST_ACTION) != null) {
                Log.d(TAG, "BLOOD_TEST_ACTION");
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Blood Test Action");
                builder.setMessage("What would you like to do?");
                final String bt_uuid = bundle.getString(Home.BLOOD_TEST_ACTION + "2");
                if (bt_uuid != null) {
                    final BloodTest bt = BloodTest.byUUID(bt_uuid);
                    if (bt != null) {
                         builder.setNeutralButton("Nothing", new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog, int which) {
                                 dialog.dismiss();
                             }
                         });

                        builder.setPositiveButton("Calibrate", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                final long time_since = JoH.msSince(bt.timestamp);
                                Home.startHomeWithExtra(xdrip.getAppContext(), Home.BLUETOOTH_METER_CALIBRATION, BgGraphBuilder.unitized_string_static(bt.mgdl), Long.toString(time_since));
                                bt.addState(BloodTest.STATE_CALIBRATION);
                                GcmActivity.syncBloodTests();

                            }
                        });

                        builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                                builder.setTitle("Confirm Delete");
                                builder.setMessage("Are you sure you want to delete this Blood Test result?");
                                builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        bt.removeState(BloodTest.STATE_VALID);
                                        GcmActivity.syncBloodTests();
                                        if (Home.get_show_wear_treatments()) BloodTest.pushBloodTestSyncToWatch(bt, false);
                                        staticRefreshBGCharts();
                                        JoH.static_toast_short("Deleted!");
                                    }
                                });
                                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                final AlertDialog alert = builder.create();
                                alert.show();
                            }
                        });
                        final AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        JoH.static_toast_long("Could not find blood test data!! " + bt_uuid);
                    }
                }
            }
        }
    }


    public static void startHomeWithExtra(Context context, String extra, String text) {
        startHomeWithExtra(context, extra, text, "");
    }

    public static void startHomeWithExtra(Context context, String extra, String text, String even_more) {
        startHomeWithExtra(context, extra, text, even_more, "");
    }

    public static void startHomeWithExtra(Context context, String extra, String text, String even_more, String even_even_more) {
        Intent intent = new Intent(context, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(extra, text);
        intent.putExtra(extra + "2", even_more);
        if (even_even_more.length() > 0) intent.putExtra(extra + "3", even_even_more);
        context.startActivity(intent);
    }

    public void crowdTranslate(MenuItem x) {
        startActivity(new Intent(this, LanguageEditor.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void testFeature(MenuItem x) {
        startActivity(new Intent(this, MegaStatus.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void viewEventLog(MenuItem x) {
        startActivity(new Intent(this, ErrorsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("events", ""));
    }

    private boolean hideTreatmentButtonsIfAllDone() {
        if ((btnBloodGlucose.getVisibility() == View.INVISIBLE) &&
                (btnCarbohydrates.getVisibility() == View.INVISIBLE) &&
                (btnInsulinDose.getVisibility() == View.INVISIBLE)) {
            hideAllTreatmentButtons(); // we clear values here also
            //send toast to wear - closes the confirmation activity on the watch
            WatchUpdaterService.sendWearToast("Treatment processed", Toast.LENGTH_LONG);
            return true;
        } else {
            return false;
        }
    }

    private void hideAllTreatmentButtons() {
        textBloodGlucose.setVisibility(View.INVISIBLE);
        textCarbohydrates.setVisibility(View.INVISIBLE);
        btnApprove.setVisibility(View.INVISIBLE);
        btnCancel.setVisibility(View.INVISIBLE);
        btnCarbohydrates.setVisibility(View.INVISIBLE);
        textInsulinDose.setVisibility(View.INVISIBLE);
        btnInsulinDose.setVisibility(View.INVISIBLE);
        btnBloodGlucose.setVisibility(View.INVISIBLE);
        voiceRecognitionText.setVisibility(View.INVISIBLE);
        textTime.setVisibility(View.INVISIBLE);
        btnTime.setVisibility(View.INVISIBLE);

        // zeroing code could be functionalized
        thiscarbsnumber = 0;
        thisinsulinnumber = 0;
        thistimeoffset = 0;
        thisglucosenumber = 0;
        carbsset = false;
        insulinset = false;
        glucoseset = false;
        timeset = false;

        if (chart != null) {
            chart.setAlpha((float) 1);
        }
    }
    // jamorham voiceinput methods

    public String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {

        }
        return outputStream.toString();
    }

    private void initializeSearchWords(String jstring) {
        Log.d(TAG, "Initialize Search words");
        wordDataWrapper lcs = new wordDataWrapper();
        try {

            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.initiallexicon);

            String input = readTextFile(in_s);

            Gson gson = new Gson();
            lcs = gson.fromJson(input, wordDataWrapper.class);

        } catch (Exception e) {
            e.printStackTrace();

            Log.d(TAG, "Got exception during search word load: " + e.toString());
            Toast.makeText(getApplicationContext(),
                    "Problem loading speech lexicon!",
                    Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "Loaded Words: " + Integer.toString(lcs.entries.size()));
        searchWords = lcs;
    }

    public void promptSpeechNoteInput(View abc) {

        if (recognitionRunning) return;
        recognitionRunning = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // debug voice
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speak_your_note_text));

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_NOTE_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_recognition_is_not_supported),
                    Toast.LENGTH_LONG).show();
        }

    }

    private void promptKeypadInput() {
        Log.d(TAG, "Showing pop-up");

/*        LayoutInflater inflater = (LayoutInflater) xdrip.getAppContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.keypad_activity_phone, null);
        myPopUp = new PopupWindow(popupView, ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        myPopUp.showAtLocation(findViewById(R.id.chart), Gravity.CENTER, 0, 0);*/

        startActivity(new Intent(this, PhoneKeypadInputActivity.class));
    }

    private void promptTextInput() {

        //if (recognitionRunning) return;
        //recognitionRunning = true;

        promptKeypadInput();
    }

    private void promptTextInput_old() {

        if (recognitionRunning) return;
        recognitionRunning = true;


        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type treatment\neg: units x.x");
// Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                voiceRecognitionText.setText(input.getText().toString());
                voiceRecognitionText.setVisibility(View.VISIBLE);
                last_speech_time = JoH.ts();
                naturalLanguageRecognition(input.getText().toString());

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (dialog != null)
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();
        recognitionRunning = false;
    }

    /**
     * Showing google speech input dialog
     */
    private synchronized void promptSpeechInput() {

        if (JoH.ratelimit("speech-input",1)) {
            if (recognitionRunning) return;
            recognitionRunning = true;

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // debug voice
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    getString(R.string.speak_your_treatment));

            try {
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            } catch (ActivityNotFoundException a) {
                Toast.makeText(getApplicationContext(),
                        R.string.speech_recognition_is_not_supported,
                        Toast.LENGTH_LONG).show();
            }
        }

    }

    private String classifyWord(String word) {
        if (word == null) return null;
        if (word.equals("watchkeypad")) return "watchkeypad";
        if (word.equals("uuid")) return "uuid";
// convert fuzzy recognised word to our keyword from lexicon
        for (wordData thislex : searchWords.entries) {
            if (thislex.matchWords.contains(word)) {
                Log.d(TAG, "Matched spoken word: " + word + " => " + thislex.lexicon);
                return thislex.lexicon;
            }
        }
        Log.d(TAG, "Could not match spoken word: " + word);
        return null; // if cannot match
    }

    private void naturalLanguageRecognition(String allWords) {
        if (searchWords == null) {

            Toast.makeText(getApplicationContext(),
                    "Word lexicon not loaded!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "Processing speech input allWords: " + allWords);
        thisuuid = "";
        int end = allWords.indexOf(" uuid ");
        if (end > 0) {
            thisuuid = (end > 0 ? allWords.substring(0, end) : "");
            allWords = allWords.substring(end + 6, allWords.length());
        }
        allWords = allWords.trim();
        allWords = allWords.replaceAll(":", "."); // fix real times
        allWords = allWords.replaceAll("(\\d)([a-zA-Z])", "$1 $2"); // fix like 22mm
        allWords = allWords.replaceAll("([0-9]\\.[0-9])([0-9][0-9])", "$1 $2"); // fix multi number order like blood 3.622 grams
        allWords = allWords.toLowerCase();

        Log.d(TAG, "Processing speech input allWords second: " + allWords + " UUID: " + thisuuid);

        if (allWords.contentEquals("delete last treatment")
                || allWords.contentEquals("cancel last treatment")
                || allWords.contentEquals("erase last treatment")) {
            Treatments.delete_last(true);
            updateCurrentBgInfo("delete last treatment");
        } else if ((allWords.contentEquals("delete all treatments"))
                || (allWords.contentEquals("delete all treatment"))) {
            Treatments.delete_all(true);
            updateCurrentBgInfo("delete all treatment");
        } else if (allWords.contentEquals("delete last calibration")
                || allWords.contentEquals("clear last calibration")) {
            Calibration.clearLastCalibration();
        } else if (allWords.contentEquals("force google reboot")) {
            SdcardImportExport.forceGMSreset();
        } else if (allWords.contentEquals("enable engineering mode")) {
            Home.setPreferencesBoolean("engineering_mode", true);
            JoH.static_toast(getApplicationContext(), "Engineering mode enabled - be careful", Toast.LENGTH_LONG);
        } else if (allWords.contentEquals("vehicle mode test")) {
            ActivityRecognizedService.spoofActivityRecogniser(mActivity, JoH.tsl() + "^" + 0);
            staticRefreshBGCharts();
        } else if (allWords.contentEquals("vehicle mode quit")) {
            ActivityRecognizedService.spoofActivityRecogniser(mActivity, JoH.tsl() + "^" + 3);
            staticRefreshBGCharts();
        } else if (allWords.contentEquals("vehicle mode walk")) {
            ActivityRecognizedService.spoofActivityRecogniser(mActivity, JoH.tsl() + "^" + 2);
            staticRefreshBGCharts();
        } else if (allWords.contentEquals("delete all glucose data")) {
            deleteAllBG(null);
            LibreAlarmReceiver.clearSensorStats();
        } else if (allWords.contentEquals("delete selected glucose meter") || allWords.contentEquals("delete selected glucose metre")) {
            setPreferencesString("selected_bluetooth_meter_address","");
        } else if (allWords.contentEquals("delete all finger stick data") || (allWords.contentEquals("delete all fingerstick data"))) {
            BloodTest.cleanup(-100000);
        } else if (allWords.contentEquals("delete all persistent store")) {
            SdcardImportExport.deletePersistentStore();
        } else if (allWords.contentEquals("delete uploader queue")) {
            UploaderQueue.emptyQueue();
        } else if (allWords.contentEquals("clear battery warning")) {
            try {
                final Sensor sensor = Sensor.currentSensor();
                if (sensor != null) {
                    sensor.latest_battery_level = 0;
                    sensor.save();
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        // reset parameters for new speech
        watchkeypad = false;
        watchkeypadset = false;
        glucoseset = false;
        insulinset = false;
        carbsset = false;
        timeset = false;
        thisnumber = -1;
        thisword = "";

        final String[] wordsArray = allWords.split(" ");
        for (int i = 0; i < wordsArray.length; i++) {
            // per word in input stream
            try {
                double thisdouble = Double.parseDouble(wordsArray[i]);
                thisnumber = thisdouble; // if no exception
                handleWordPair();
            } catch (NumberFormatException nfe) {
                // detection of number or not
                final String result = classifyWord(wordsArray[i]);
                if (result != null)
                    thisword = result;
                handleWordPair();
                if (thisword.equals("note")) {
                    String note_text = "";
                    for (int j = i + 1; j < wordsArray.length; j++) {
                        if (note_text.length() > 0) note_text += " ";
                        note_text += wordsArray[j];
                    }
                    if (note_text.length() > 0) {
                        // TODO respect historic timeset?
                        Treatments.create_note(note_text, JoH.tsl());
                        staticRefreshBGCharts();
                        break; // don't process any more
                    }
                }
            }
        }
    }

    private void handleWordPair() {
        boolean preserve = false;
        if ((thisnumber == -1) || (thisword.equals(""))) return;

        Log.d(TAG, "GOT WORD PAIR: " + thisnumber + " = " + thisword);

        switch (thisword) {

            case "watchkeypad":
                if ((watchkeypadset == false) && (thisnumber > 1501968469)) {
                    watchkeypad = true;
                    watchkeypadset = true;
                    watchkeypad_timestamp = (long)(thisnumber * 1000);
                    Log.d(TAG, "Treatment entered on watchkeypad: " + Double.toString(thisnumber));
                } else {
                    Log.d(TAG, "watchkeypad already set");
                }
                break;

            case "rapid":
                if ((insulinset == false) && (thisnumber > 0)) {
                    thisinsulinnumber = thisnumber;
                    textInsulinDose.setText(Double.toString(thisnumber) + " units");
                    Log.d(TAG, "Rapid dose: " + Double.toString(thisnumber));
                    insulinset = true;
                    btnInsulinDose.setVisibility(View.VISIBLE);
                    textInsulinDose.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG, "Rapid dose already set");
                    preserve = true;
                }
                break;

            case "carbs":
                if ((carbsset == false) && (thisnumber > 0)) {
                    thiscarbsnumber = thisnumber;
                    textCarbohydrates.setText(Integer.toString((int) thisnumber) + " carbs");
                    carbsset = true;
                    Log.d(TAG, "Carbs eaten: " + Double.toString(thisnumber));
                    btnCarbohydrates.setVisibility(View.VISIBLE);
                    textCarbohydrates.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG, "Carbs already set");
                    preserve = true;
                }
                break;

            case "blood":
                if ((glucoseset == false) && (thisnumber > 0)) {
                    thisglucosenumber = thisnumber;
                    if (prefs.getString("units", "mgdl").equals("mgdl")) {
                        if (textBloodGlucose != null)
                            textBloodGlucose.setText(Double.toString(thisnumber) + " mg/dl");
                    } else {
                        if (textBloodGlucose != null)
                            textBloodGlucose.setText(Double.toString(thisnumber) + " mmol/l");
                    }

                    Log.d(TAG, "Blood test: " + Double.toString(thisnumber));
                    glucoseset = true;
                    if (textBloodGlucose != null) {
                        btnBloodGlucose.setVisibility(View.VISIBLE);
                        textBloodGlucose.setVisibility(View.VISIBLE);
                    }

                } else {
                    Log.d(TAG, "Blood glucose already set");
                    preserve = true;
                }
                break;

            case "time":
                Log.d(TAG, "processing time keyword");
                if ((timeset == false) && (thisnumber >= 0)) {

                    final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                    final DecimalFormat df = (DecimalFormat)nf;
                    //DecimalFormat df = new DecimalFormat("#");
                    df.setMinimumIntegerDigits(2);
                    df.setMinimumFractionDigits(2);
                    df.setMaximumFractionDigits(2);
                    df.setMaximumIntegerDigits(2);

                    final Calendar c = Calendar.getInstance();

                    final SimpleDateFormat simpleDateFormat1 =
                            new SimpleDateFormat("dd/M/yyyy ",Locale.US);
                    final SimpleDateFormat simpleDateFormat2 =
                            new SimpleDateFormat("dd/M/yyyy HH.mm",Locale.US); // TODO double check 24 hour 12.00 etc
                    final String datenew = simpleDateFormat1.format(c.getTime()) + df.format(thisnumber);

                    Log.d(TAG, "Time Timing data datenew: " + datenew);

                    final Date datethen;
                    final Date datenow = new Date();

                    try {
                        datethen = simpleDateFormat2.parse(datenew);
                        double difference = datenow.getTime() - datethen.getTime();
                        // is it more than 1 hour in the future? If so it must be yesterday
                        if (difference < -(1000 * 60 * 60)) {
                            difference = difference + (86400 * 1000);
                        } else {
                            // - midnight feast pre-bolus nom nom
                            if (difference > (60 * 60 * 23 * 1000))
                                difference = difference - (86400 * 1000);
                        }

                        Log.d(TAG, "Time Timing data: " + df.format(thisnumber) + " = difference ms: " + JoH.qs(difference));
                        textTime.setText(df.format(thisnumber));
                        timeset = true;
                        thistimeoffset = difference;
                        btnTime.setVisibility(View.VISIBLE);
                        textTime.setVisibility(View.VISIBLE);
                    } catch (ParseException e) {
                        // toast to explain?
                        Log.d(TAG, "Got exception parsing date time");
                    }
                } else {
                    Log.d(TAG, "Time data already set");
                    preserve = true;
                }
                break;
        } // end switch

        if (preserve == false) {
            Log.d(TAG, "Clearing speech values");
            thisnumber = -1;
            thisword = "";
        } else {
            Log.d(TAG, "Preserving speech values");
        }

        // don't show approve/cancel if we only have time
        if ((insulinset || glucoseset || carbsset) && !watchkeypad) {
            btnApprove.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);

            if (small_screen)
            {
                final float button_scale_factor = 0.60f;
                ((ViewGroup.MarginLayoutParams) btnApprove.getLayoutParams()).leftMargin=0;
                ((ViewGroup.MarginLayoutParams) btnBloodGlucose.getLayoutParams()).leftMargin=0;
                ((ViewGroup.MarginLayoutParams) btnBloodGlucose.getLayoutParams()).setMarginStart(0);
                ((ViewGroup.MarginLayoutParams) btnCancel.getLayoutParams()).setMarginStart(0);
                ((ViewGroup.MarginLayoutParams) btnApprove.getLayoutParams()).rightMargin=0;
                ((ViewGroup.MarginLayoutParams) btnCancel.getLayoutParams()).rightMargin=0;
                btnApprove.setScaleX(button_scale_factor);
                btnApprove.setScaleY(button_scale_factor);
                btnCancel.setScaleX(button_scale_factor);
                btnCancel.setScaleY(button_scale_factor);
                btnInsulinDose.setScaleX(button_scale_factor);
                btnCarbohydrates.setScaleX(button_scale_factor);
                btnCarbohydrates.setScaleY(button_scale_factor);
                btnBloodGlucose.setScaleX(button_scale_factor);
                btnBloodGlucose.setScaleY(button_scale_factor);
                btnInsulinDose.setScaleY(button_scale_factor);
                btnTime.setScaleX(button_scale_factor);
                btnTime.setScaleY(button_scale_factor);

                final int small_text_size = 12;

                textCarbohydrates.setTextSize(small_text_size);
                textInsulinDose.setTextSize(small_text_size);
                textBloodGlucose.setTextSize(small_text_size);
                textTime.setTextSize(small_text_size);

            }

        }

        if ((insulinset || glucoseset || carbsset || timeset) && !watchkeypad) {
            if (chart != null) {
                chart.setAlpha((float) 0.10);
            }
            WatchUpdaterService.sendTreatment(
                    thiscarbsnumber,
                    thisinsulinnumber,
                    thisglucosenumber,
                    thistimeoffset,
                    textTime.getText().toString());
        }

    }

    public static void toaststatic(final String msg) {
        nexttoast = msg;
        staticRefreshBGCharts();
    }

    public static void toaststaticnext(final String msg) {
        nexttoast = msg;
        UserError.Log.uel(TAG,"Home toast message next: "+msg);
    }

    public void toast(final String msg) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
                }
            });
            Log.d(TAG, "toast: " + msg);
        } catch (Exception e) {
            Log.d(TAG, "Couldn't display toast: " + msg + " / " + e.toString());
        }
    }

    public static void toastStaticFromUI(final String msg) {
        try {
            Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
            Log.d(TAG, "toast: " + msg);
        } catch (Exception e) {
            toaststaticnext(msg);
            Log.d(TAG, "Couldn't display toast (rescheduling): " + msg + " / " + e.toString());
        }
    }


    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {

  /*              Intent intent = data; // DEEEBUGGGG
                if (intent != null)
                {
                    final Bundle bundle = intent.getExtras();


                    if ((bundle != null) && (true)) {
                        for (String key : bundle.keySet()) {
                            Object value = bundle.get(key);
                            if (value != null) {
                                Log.d(TAG+" xdebug", String.format("%s %s (%s)", key,
                                        value.toString(), value.getClass().getName()));
                            }
                        }
                    }
                }*/

                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    voiceRecognitionText.setText(result.get(0));
                    voiceRecognitionText.setVisibility(View.VISIBLE);
                    last_speech_time = JoH.ts();
                    naturalLanguageRecognition(result.get(0));
                }
                recognitionRunning = false;
                break;
            }

            case REQ_CODE_SPEECH_NOTE_INPUT: {

                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //voiceRecognitionText.setText(result.get(0));
                    //voiceRecognitionText.setVisibility(View.VISIBLE);
                    //last_speech_time = JoH.ts();
                    //naturalLanguageRecognition(result.get(0));
                    String treatment_text = result.get(0).trim();
                    Log.d(TAG, "Got treatment note: " + treatment_text);
                    voiceRecognitionText.setText(result.get(0));
                    voiceRecognitionText.setVisibility(View.VISIBLE);
                    Treatments.create_note(treatment_text, 0); // timestamp?
                    if (dialog != null) {
                        dialog.cancel();
                        dialog = null;
                    }
                    Home.staticRefreshBGCharts();

                }
                recognitionRunning = false;
                break;
            }
            case NFCReaderX.REQ_CODE_NFC_TAG_FOUND:
            {
                if (NFCReaderX.useNFC()) {
                    NFCReaderX nfcReader = new NFCReaderX();
                    nfcReader.tagFound(this, data);
                }
            }

        }
    }

    class wordDataWrapper {
        public ArrayList<wordData> entries;

        wordDataWrapper() {
            entries = new ArrayList<wordData>();

        }
    }

    class wordData {
        public String lexicon;
        public ArrayList<String> matchWords;
    }

    /// jamorham end voiceinput methods

    @Override
    public String getMenuName() {
        return menu_name;
    }

    private boolean checkEula() {

        final boolean warning_agreed_to = prefs.getBoolean("warning_agreed_to", false);
        if (!warning_agreed_to) {
            startActivity(new Intent(getApplicationContext(), Agreement.class));
            finish();
            return false;
        } else {
            final boolean IUnderstand = prefs.getBoolean("I_understand", false);
            if (!IUnderstand) {
                Intent intent = new Intent(getApplicationContext(), LicenseAgreementActivity.class);
                startActivity(intent);
                finish();
                return false;
            } else {
                return true;
            }
        }
    }

    public static void staticRefreshBGCharts() {
        staticRefreshBGCharts(false);
    }

    public static void staticRefreshBGCharts(boolean override) {
        reset_viewport = true;
        if (activityVisible || override) {
            Intent updateIntent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA);
            mActivity.sendBroadcast(updateIntent);
        }
    }

    @TargetApi(21)
    private void handleFlairColors() {
        if ((Build.VERSION.SDK_INT >= 21)) {
            try {
                if (getPreferencesBooleanDefaultFalse("use_flair_colors")) {
                    getWindow().setNavigationBarColor(getCol(X.color_lower_flair_bar));
                    getWindow().setStatusBarColor(getCol(X.color_upper_flair_bar));
                }
            } catch (Exception e) {
                //
            }
        }
    }

    @Override
    protected void onResume() {

        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onResume();
        handleFlairColors();
        checkEula();
        set_is_follower();

        // status line must only have current bwp/iob data
        statusIOB="";
        statusBWP="";
        refreshStatusLine();

        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            this.currentBgValueText.setTextSize(100);
            this.notificationText.setTextSize(40);
            this.extraStatusLineText.setTextSize(40);
        }
        else if(BgGraphBuilder.isLargeTablet(getApplicationContext())) {
            this.currentBgValueText.setTextSize(70);
            this.notificationText.setTextSize(34); // 35 too big 33 works 
            this.extraStatusLineText.setTextSize(35);
        }
        
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateCurrentBgInfo("time tick");
                    updateHealthInfo("time_tick");
                }
            }
        };
        newDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                holdViewport.set(0, 0, 0, 0);
                updateCurrentBgInfo("new data");
                updateHealthInfo("new_data");
            }
        };


        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        registerReceiver(newDataReceiver, new IntentFilter(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA));

        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver,
                new IntentFilter(Intents.HOME_STATUS_ACTION));

        holdViewport.set(0, 0, 0, 0);

        if (invalidateMenu) {
            invalidateOptionsMenu();
            invalidateMenu = false;
        }
        activityVisible = true;
        updateCurrentBgInfo("generic on resume");
        updateHealthInfo("generic on resume");

        if (!JoH.getWifiSleepPolicyNever()) {
            if (JoH.ratelimit("policy-never", 3600)) {
                if (getPreferencesLong("wifi_warning_never", 0) == 0) {
                    if (!JoH.isMobileDataOrEthernetConnected()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("WiFi Sleep Policy Issue");
                        builder.setMessage("Your WiFi is set to sleep when the phone screen is off.\n\nThis may cause problems if you don't have cellular data or have devices on your local network.\n\nWould you like to go to the settings page to set:\n\nAlways Keep WiFi on during Sleep?");

                        builder.setNeutralButton("Maybe Later", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.setPositiveButton("YES, Do it", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                toast("Recommend that you change WiFi to always be on during sleep");
                                try {
                                    startActivity(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
                                } catch (ActivityNotFoundException e) {
                                    JoH.static_toast_long("Ooops this device doesn't seem to have a wifi settings page!");
                                }

                            }
                        });

                        builder.setNegativeButton("NO, Never", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                setPreferencesLong("wifi_warning_never", (long) JoH.ts());
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();

                    }
                }
            }
        }

        if (NFCReaderX.useNFC()) {
            NFCReaderX.doNFC(this);
        } else {
            NFCReaderX.disableNFC(this);
        }

        if (get_follower() || get_master()) {
          GcmActivity.checkSync(this);
        }

        NightscoutUploader.launchDownloadRest();

    }

    private void setupCharts() {
        bgGraphBuilder = new BgGraphBuilder(this);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);

        if (BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chart.getLayoutParams();
            params.topMargin = 130;
            chart.setLayoutParams(params);
        }
        chart.setBackgroundColor(getCol(X.color_home_chart_background));
        chart.setZoomType(ZoomType.HORIZONTAL);

        //Transmitter Battery Level
        final Sensor sensor = Sensor.currentSensor();
        if (sensor != null && sensor.latest_battery_level != 0 && sensor.latest_battery_level <= Constants.TRANSMITTER_BATTERY_LOW && !prefs.getBoolean("disable_battery_warning", false)) {
            Drawable background = new Drawable() {

                @Override
                public void draw(Canvas canvas) {

                    DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
                    int px = (int) (30 * (metrics.densityDpi / 160f));
                    Paint paint = new Paint();
                    paint.setTextSize(px);
                    paint.setAntiAlias(true);
                    paint.setColor(Color.parseColor("#FFFFAA"));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setAlpha(100);
                    canvas.drawText(getString(R.string.transmitter_battery), 10, chart.getHeight() / 3 - (int) (1.2 * px), paint);
                    if (sensor.latest_battery_level <= Constants.TRANSMITTER_BATTERY_EMPTY) {
                        paint.setTextSize((int) (px * 1.5));
                        canvas.drawText(getString(R.string.very_low), 10, chart.getHeight() / 3, paint);
                    } else {
                        canvas.drawText(getString(R.string.low), 10, chart.getHeight() / 3, paint);
                    }
                }

                @Override
                public void setAlpha(int alpha) {
                }

                @Override
                public void setColorFilter(ColorFilter cf) {
                }

                @Override
                public int getOpacity() {
                    return 0; // TODO Which pixel format should this be?
                }
            };
            chart.setBackground(background);
        }
        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);

        chart.setLineChartData(bgGraphBuilder.lineData());
        chart.setOnValueTouchListener(bgGraphBuilder.getOnValueSelectTooltipListener(mActivity));

        previewChart.setBackgroundColor(getCol(X.color_home_chart_background));
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        previewChart.setLineChartData(bgGraphBuilder.previewLineData(chart.getLineChartData()));
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());
        setViewport();

        if (small_height)
        {
            previewChart.setVisibility(View.GONE);

            // quick test
            Viewport moveViewPort = new Viewport(chart.getMaximumViewport());
            float tempwidth = (float) moveViewPort.width()/4;
            holdViewport.left=moveViewPort.right - tempwidth;
            holdViewport.right=moveViewPort.right + (moveViewPort.width()/24);
            holdViewport.top=moveViewPort.top;
            holdViewport.bottom=moveViewPort.bottom;
            chart.setCurrentViewport(holdViewport);
            previewChart.setCurrentViewport(holdViewport);
        } else {
            previewChart.setVisibility(View.VISIBLE);
        }

        if (insulinset || glucoseset || carbsset || timeset) {
            if (chart != null) {
                chart.setAlpha((float) 0.10);
                // TODO also set buttons alpha
            }
        }

    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart));
        } else {
            previewChart.setCurrentViewport(holdViewport);
        }
    }

    @Override
    public void onPause() {
        activityVisible = false;
        super.onPause();
        NFCReaderX.stopNFC(this);
        if (_broadcastReceiver != null) {
            try {
                unregisterReceiver(_broadcastReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "_broadcast_receiver not registered", e);
            }
        }
        if (newDataReceiver != null) {
            try {
                unregisterReceiver(newDataReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "newDataReceiver not registered", e);
            }
        }

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Exception unregistering broadcast receiver: "+ e);
        }

    }

    private static void set_is_follower() {
        is_follower = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).getString("dex_collection_method", "").equals("Follower");
        is_follower_set = true;
    }

    public static boolean get_follower() {
        if (!is_follower_set) set_is_follower();
        return Home.is_follower;
    }

    public static boolean get_engineering_mode() {
        return Home.getPreferencesBooleanDefaultFalse("engineering_mode");
    }

    public static boolean get_master() {
        // TODO optimize this
        return (!get_follower()) && (Home.getPreferencesBooleanDefaultFalse("plus_follow_master"));
    }

    public static boolean get_master_or_follower() {
        return get_follower() || get_master();
    }


    public static boolean get_show_wear_treatments() {
        return getPreferencesBooleanDefaultFalse("wear_sync") &&
                getPreferencesBooleanDefaultFalse("show_wear_treatments");
    }

    public static boolean follower_or_accept_follower() {
        return get_follower() || Home.getPreferencesBoolean("plus_accept_follower_actions", true);
    }

    public static boolean get_forced_wear() {
        return getPreferencesBooleanDefaultFalse("wear_sync") &&
                getPreferencesBooleanDefaultFalse("enable_wearG5") &&
                getPreferencesBooleanDefaultFalse("force_wearG5");
    }

    public static boolean get_enable_wear() {
        return getPreferencesBooleanDefaultFalse("wear_sync") &&
                getPreferencesBooleanDefaultFalse("enable_wearG5");
    }

    public static void startWatchUpdaterService(Context context, String action, String logTag) {
        final boolean wear_integration = getPreferencesBoolean("wear_sync", false);
        if (wear_integration) {
            Log.d(logTag, "start WatchUpdaterService with " + action);
            context.startService(new Intent(context, WatchUpdaterService.class).setAction(action));
        }
    }

    public static void startWatchUpdaterService(Context context, String action, String logTag, String key, String value) {
        final boolean wear_integration = getPreferencesBoolean("wear_sync", false);
        if (wear_integration) {
            Log.d(logTag, "start WatchUpdaterService with " + action);
            context.startService(new Intent(context, WatchUpdaterService.class).setAction(action).putExtra(key, value));
        }
    }

    public static void startWatchUpdaterService(Context context, String action, String logTag, String key, boolean value) {
        final boolean wear_integration = getPreferencesBoolean("wear_sync", false);
        if (wear_integration) {
            Log.d(logTag, "start WatchUpdaterService with " + action);
            context.startService(new Intent(context, WatchUpdaterService.class).setAction(action).putExtra(key, value));
        }
    }


    public static boolean get_holo() {
        return Home.is_holo;
    }

    public void toggleStepsVisibility(View v) {
        setPreferencesBoolean("show_pebble_movement_line", !getPreferencesBoolean("show_pebble_movement_line", true));
        staticRefreshBGCharts();
    }

    private void updateHealthInfo(String caller) {
        final PebbleMovement pm = PebbleMovement.last();
        final boolean use_pebble_health = prefs.getBoolean("use_pebble_health", true);
        if ((use_pebble_health) && (pm != null)) {
            stepsButton.setText(Integer.toString(pm.metric));
            stepsButton.setVisibility(View.VISIBLE);
            stepsButton.setAlpha(getPreferencesBoolean("show_pebble_movement_line", true) ? 1.0f : 0.3f);
        } else {
            stepsButton.setVisibility(View.INVISIBLE);
        }

        final HeartRate hr = HeartRate.last();
        if ((use_pebble_health) && (hr != null)) {
            bpmButton.setText(Integer.toString(hr.bpm));
            bpmButton.setVisibility(View.VISIBLE);
        } else {
            bpmButton.setVisibility(View.INVISIBLE);
        }
    }

    private void updateCurrentBgInfo(String source) {
        Log.d(TAG, "updateCurrentBgInfo from: " + source);

        if (!activityVisible) {
            Log.d(TAG, "Display not visible - not updating chart");
            return;
        }
        if (reset_viewport) {
            reset_viewport = false;
            holdViewport.set(0, 0, 0, 0);
            if (chart != null) chart.setZoomType(ZoomType.HORIZONTAL);
        }
        setupCharts();
        final TextView notificationText = (TextView) findViewById(R.id.notices);
        final TextView lowPredictText = (TextView) findViewById(R.id.lowpredict);
        if (BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            notificationText.setTextSize(40);
            lowPredictText.setTextSize(30);
        }
        notificationText.setText("");
        notificationText.setTextColor(Color.RED);

        UndoRedo.purgeQueues();

        if (UndoRedo.undoListHasItems()) {
            btnUndo.setVisibility(View.VISIBLE);
            showcasemenu(SHOWCASE_UNDO);
        } else {
            btnUndo.setVisibility(View.INVISIBLE);
        }

        if (UndoRedo.redoListHasItems()) {
            btnRedo.setVisibility(View.VISIBLE);
            showcasemenu(SHOWCASE_REDO);
        } else {
            btnRedo.setVisibility(View.INVISIBLE);
        }

        final DexCollectionType collector = DexCollectionType.getDexCollectionType();
        // TODO unify code using DexCollectionType methods
        boolean isBTWixel = CollectionServiceStarter.isBTWixel(getApplicationContext());
        // port this lot to DexCollectionType to avoid multiple lookups of the same preference
        boolean isDexbridgeWixel = CollectionServiceStarter.isDexBridgeOrWifiandDexBridge();
        boolean isWifiBluetoothWixel = CollectionServiceStarter.isWifiandBTWixel(getApplicationContext());
        isBTShare = CollectionServiceStarter.isBTShare(getApplicationContext());
        isG5Share = CollectionServiceStarter.isBTG5(getApplicationContext());
        boolean isWifiWixel = CollectionServiceStarter.isWifiWixel(getApplicationContext());
        alreadyDisplayedBgInfoCommon = false; // reset flag
        if (isBTShare) {
            updateCurrentBgInfoForBtShare(notificationText);
        }
        if (isG5Share) {
            updateCurrentBgInfoCommon(notificationText);
        }
        if (isBTWixel || isDexbridgeWixel || isWifiBluetoothWixel) {
            updateCurrentBgInfoForBtBasedWixel(notificationText);
        }
        if (isWifiWixel || isWifiBluetoothWixel) {
            updateCurrentBgInfoForWifiWixel(notificationText);
        } else if (is_follower || collector.equals(DexCollectionType.NSEmulator)) {
            displayCurrentInfo();
            getApplicationContext().startService(new Intent(getApplicationContext(), Notifications.class));
        } else if (!alreadyDisplayedBgInfoCommon && DexCollectionType.getDexCollectionType() == DexCollectionType.LibreAlarm) {
            updateCurrentBgInfoCommon(notificationText);
        }
        if (prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n ALL ALERTS CURRENTLY DISABLED");
        } else if (prefs.getLong("low_alerts_disabled_until", 0) > new Date().getTime()
                &&
                prefs.getLong("high_alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n LOW AND HIGH ALERTS CURRENTLY DISABLED");
        } else if (prefs.getLong("low_alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n LOW ALERTS CURRENTLY DISABLED");
        } else if (prefs.getLong("high_alerts_disabled_until", 0) > new Date().getTime()) {
            notificationText.append("\n HIGH ALERTS CURRENTLY DISABLED");
        }
        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);

        // DEBUG ONLY
        if ((BgGraphBuilder.last_noise > 0) && (prefs.getBoolean("show_noise_workings", false))) {
            notificationText.append("\nSensor Noise: " + JoH.qs(BgGraphBuilder.last_noise, 1));
            if ((BgGraphBuilder.best_bg_estimate > 0) && (BgGraphBuilder.last_bg_estimate > 0)) {
                final double estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;

                // TODO pull from BestGlucose? Check Slope + arrow etc TODO Original slope needs fixing when using plugin
               // notificationText.append("\nBG Original: " + bgGraphBuilder.unitized_string(BgReading.lastNoSenssor().calculated_value)
                try {
                    notificationText.append("\nBG Original: " + bgGraphBuilder.unitized_string(BgGraphBuilder.original_value)
                            + " \u0394 " + bgGraphBuilder.unitizedDeltaString(false, true, true)
                            + " " + BgReading.lastNoSenssor().slopeArrow());

                    notificationText.append("\nBG Estimate: " + bgGraphBuilder.unitized_string(BgGraphBuilder.best_bg_estimate)
                            + " \u0394 " + bgGraphBuilder.unitizedDeltaStringRaw(false, true, estimated_delta)
                            + " " + BgReading.slopeToArrowSymbol(estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000)));
                } catch (NullPointerException e) {
                    //
                }
            }
        }


        // TODO we need to consider noise level?
        // when to raise the alarm
        lowPredictText.setText("");
        lowPredictText.setVisibility(View.INVISIBLE);
        if (BgGraphBuilder.low_occurs_at > 0) {
            final double low_predicted_alarm_minutes = Double.parseDouble(prefs.getString("low_predict_alarm_level", "50"));
            final double now = JoH.ts();
            final double predicted_low_in_mins = (BgGraphBuilder.low_occurs_at - now) / 60000;

            if (predicted_low_in_mins > 1) {
                lowPredictText.append(getString(R.string.low_predicted)+"\n"+getString(R.string.in)+": " + (int) predicted_low_in_mins + getString(R.string.space_mins));
                if (predicted_low_in_mins < low_predicted_alarm_minutes) {
                    lowPredictText.setTextColor(Color.RED); // low front getting too close!
                } else {
                    final double previous_predicted_low_in_mins = (BgGraphBuilder.previous_low_occurs_at - now) / 60000;
                    if ((BgGraphBuilder.previous_low_occurs_at > 0) && ((previous_predicted_low_in_mins + 5) < predicted_low_in_mins)) {
                        lowPredictText.setTextColor(Color.GREEN); // low front is getting further away
                    } else {
                        lowPredictText.setTextColor(Color.YELLOW); // low front is getting nearer!
                    }
                }
                lowPredictText.setVisibility(View.VISIBLE);
            }
            BgGraphBuilder.previous_low_occurs_at = BgGraphBuilder.low_occurs_at;
        }

        if (navigationDrawerFragment == null) Log.e("Runtime", "navigationdrawerfragment is null");

        try {
            navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
        } catch (Exception e) {
            Log.e("Runtime", "Exception with navigrationdrawerfragment: " + e.toString());
        }
        if (nexttoast != null) {
            toast(nexttoast);
            nexttoast = null;
        }

        // hide the treatment recognition text after some seconds
        if ((last_speech_time > 0) && ((JoH.ts() - last_speech_time) > 20000)) {
            voiceRecognitionText.setVisibility(View.INVISIBLE);
            last_speech_time = 0;
        }

        if (ActivityRecognizedService.is_in_vehicle_mode()) {
            btnVehicleMode.setVisibility(View.VISIBLE);
        } else {
            btnVehicleMode.setVisibility(View.INVISIBLE);
        }

        //if (isG5Share) showcasemenu(SHOWCASE_G5FIRMWARE); // nov 2016 firmware warning resolved 15/12/2016
        //showcasemenu(1); // 3 dot menu

    }


    private void updateCurrentBgInfoForWifiWixel(TextView notificationText) {
        if (!WixelReader.IsConfigured(getApplicationContext())) {
            notificationText.setText(R.string.first_configure_ip_address);
            return;
        }

        updateCurrentBgInfoCommon(notificationText);

    }

    private void updateCurrentBgInfoForBtBasedWixel(TextView notificationText) {
        if ((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            notificationText.setText(R.string.unfortunately_andoird_version_no_blueooth_low_energy);
            return;
        }

        if (ActiveBluetoothDevice.first() == null) {
            notificationText.setText(R.string.first_use_menu_to_scan);
            return;
        }
        updateCurrentBgInfoCommon(notificationText);
    }

    private void updateCurrentBgInfoCommon(TextView notificationText) {
        if (alreadyDisplayedBgInfoCommon) return; // with bluetooth and wifi, skip second time
        alreadyDisplayedBgInfoCommon = true;

        final boolean isSensorActive = Sensor.isActive();
        if (!isSensorActive) {
            notificationText.setText(R.string.now_start_your_sensor);

            if (!Experience.gotData() && JoH.ratelimit("start-sensor_prompt", 20)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final Context context = this;
                builder.setTitle("Start Sensor?");
                builder.setMessage("Data Source is set to: " + DexCollectionType.getDexCollectionType().toString() + "\n\nDo you want to change settings or start sensor?");
                builder.setNegativeButton("Change settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startActivity(new Intent(context, Preferences.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
                builder.setPositiveButton("Start sensor", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startActivity(new Intent(context, StartNewSensor.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
                builder.create().show();
            }

            return;
        }

        final long now = System.currentTimeMillis();
        if (Sensor.currentSensor().started_at + 60000 * 60 * 2 >= now) {
            double waitTime = (Sensor.currentSensor().started_at + 60000 * 60 * 2 - now) / 60000.0;
            notificationText.setText(getString(R.string.please_wait_while_sensor_warms_up) + JoH.qs(waitTime,0) + getString(R.string.minutes_with_bracket));
            showUncalibratedSlope();
            return;
        }

        if (BgReading.latest(2).size() > 1) {
            List<Calibration> calibrations = Calibration.latestValid(2);
            if (calibrations.size() > 1) {
                if (calibrations.get(0).possible_bad != null && calibrations.get(0).possible_bad == true && calibrations.get(1).possible_bad != null && calibrations.get(1).possible_bad != true) {
                    notificationText.setText(R.string.possible_bad_calibration);
                }
                displayCurrentInfo();
            } else {
                notificationText.setText(R.string.please_enter_two_calibrations_to_get_started);
                showUncalibratedSlope();
                Log.d(TAG, "Asking for calibration A: Uncalculated BG readings: " + BgReading.latest(2).size() + " / Calibrations size: " + calibrations.size());
                promptForCalibration();
            }
        } else {
            if (BgReading.latestUnCalculated(2).size() < 2) {
                notificationText.setText(R.string.please_wait_need_two_readings_first);
            } else {
                List<Calibration> calibrations = Calibration.latest(2);
                if (calibrations.size() < 2) {
                    notificationText.setText(R.string.please_enter_two_calibrations_to_get_started);
                    showUncalibratedSlope();
                    Log.d(TAG, "Asking for calibration B: Uncalculated BG readings: " + BgReading.latestUnCalculated(2).size() + " / Calibrations size: " + calibrations.size());
                    promptForCalibration();
                }
            }
        }
    }

    private synchronized void promptForCalibration() {
        if ((helper_dialog != null) && (helper_dialog.isShowing())) return;
        if (JoH.ratelimit("calibrate-sensor_prompt", 10)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final Context context = this;
            builder.setTitle("Calibrate Sensor?");
            builder.setMessage("We have some readings!\n\nNext we need the first calibration blood test.\n\nReady to calibrate now?");
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    helper_dialog = null;
                }
            });
            builder.setPositiveButton("Calibrate", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    helper_dialog = null;
                    startActivity(new Intent(context, DoubleCalibrationActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            });
            helper_dialog = builder.create();
            try {
                helper_dialog.show();
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not display calibration prompt helper: " + e);
            }
        }
    }

    private void showUncalibratedSlope() {
        currentBgValueText.setText(BgReading.getSlopeArrowSymbolBeforeCalibration());
        currentBgValueText.setTextColor(getCol(X.color_predictive));
    }

    private void updateCurrentBgInfoForBtShare(TextView notificationText) {
        if ((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)) {
            notificationText.setText(R.string.unfortunately_andoird_version_no_blueooth_low_energy);
            return;
        }

        String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
        if (receiverSn.compareTo("SM00000000") == 0 || receiverSn.length() == 0) {
            notificationText.setText(R.string.please_set_dex_receiver_serial_number);
            return;
        }

        if (receiverSn.length() < 10) {
            notificationText.setText(R.string.double_check_dex_receiver_serial_number);
            return;
        }

        if (ActiveBluetoothDevice.first() == null) {
            notificationText.setText(R.string.now_pair_with_your_dexcom_share);
            return;
        }

        if (!Sensor.isActive()) {
            notificationText.setText(R.string.now_choose_start_sensor_in_settings);
            return;
        }

        displayCurrentInfo();
    }

    private void displayCurrentInfo() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        final boolean isDexbridge = CollectionServiceStarter.isDexBridgeOrWifiandDexBridge();
       // final boolean hasBtWixel = DexCollectionType.hasBtWixel();
        final boolean isLimitter = CollectionServiceStarter.isLimitter();
        //boolean isWifiWixel = CollectionServiceStarter.isWifiandBTWixel(getApplicationContext()) | CollectionServiceStarter.isWifiWixel(getApplicationContext());
      //  if (isDexbridge||isLimitter||hasBtWixel||is_follower) {
        if (DexCollectionType.hasBattery()) {
            final int bridgeBattery = prefs.getInt("bridge_battery", 0);

            if (bridgeBattery < 1) {
                //dexbridgeBattery.setText(R.string.waiting_for_packet);
                dexbridgeBattery.setVisibility(View.INVISIBLE);
            } else {
                if (isDexbridge) {
                    dexbridgeBattery.setText(getString(R.string.xbridge_battery) + ": " + bridgeBattery + "%");
                } else if (isLimitter){
                    dexbridgeBattery.setText(getString(R.string.limitter_battery) + ": " + bridgeBattery + "%");
                } else {
                    dexbridgeBattery.setText("Bridge battery"+ ": " + bridgeBattery + ((bridgeBattery < 200) ? "%" : "mV"));
                }
                }
            if (bridgeBattery < 50) dexbridgeBattery.setTextColor(Color.YELLOW);
            if (bridgeBattery < 25) dexbridgeBattery.setTextColor(Color.RED);
            else dexbridgeBattery.setTextColor(Color.GREEN);
            dexbridgeBattery.setVisibility(View.VISIBLE);

        } else {
            dexbridgeBattery.setVisibility(View.INVISIBLE);
        }

        if (DexCollectionType.hasWifi()) {
            final int bridgeBattery = prefs.getInt("parakeet_battery", 0);
            if (bridgeBattery > 0) {
                if (bridgeBattery < 50) {
                    parakeetBattery.setText(getString(R.string.parakeet_battery) + ": " + bridgeBattery + "%");

                    if (bridgeBattery < 40) {
                        parakeetBattery.setTextColor(Color.RED);
                    } else {
                        parakeetBattery.setTextColor(Color.YELLOW);
                    }
                    parakeetBattery.setVisibility(View.VISIBLE);
                } else {
                    parakeetBattery.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            parakeetBattery.setVisibility(View.INVISIBLE);
        }

        if (!prefs.getBoolean("display_bridge_battery", true)) {
            dexbridgeBattery.setVisibility(View.INVISIBLE);
            parakeetBattery.setVisibility(View.INVISIBLE);
        }

        final int sensor_age = prefs.getInt("nfc_sensor_age", 0);
        if ((sensor_age > 0) && (DexCollectionType.hasLibre())) {
            final String age_problem = (Home.getPreferencesBooleanDefaultFalse("nfc_age_problem") ? " \u26A0\u26A0\u26A0" : "");
            if (prefs.getBoolean("nfc_show_age", true)) {
                sensorAge.setText("Age: " + JoH.qs(((double) sensor_age) / 1440, 1) + "d" + age_problem);
            } else {
                try {
                    final double expires = JoH.tolerantParseDouble(Home.getPreferencesStringWithDefault("nfc_expiry_days", "14.5")) - ((double) sensor_age) / 1440;
                    sensorAge.setText(((expires >= 0) ? ("Expires: " + JoH.qs(expires, 1) + "d") : "EXPIRED! ") + age_problem);
                } catch (Exception e) {
                    Log.e(TAG, "expiry calculation: " + e);
                    sensorAge.setText("Expires: " + "???");
                }
            }
            sensorAge.setVisibility(View.VISIBLE);
            if (sensor_age < 1440) {
                sensorAge.setTextColor(Color.YELLOW);
            } else if (sensor_age < (1440 * 12)) {
                sensorAge.setTextColor(Color.GREEN);
            } else {
                sensorAge.setTextColor(Color.RED);
            }
        } else {
            sensorAge.setVisibility(View.GONE);
        }
        if (blockTouches) { sensorAge.setText("SCANNING.. DISPLAY LOCKED!"); sensorAge.setVisibility(View.VISIBLE); sensorAge.setTextColor(Color.GREEN); }

        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            dexbridgeBattery.setPaintFlags(dexbridgeBattery.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            if (get_follower()) {
                GcmActivity.requestPing();
            }
        }
        final BgReading lastBgReading = BgReading.lastNoSenssor();
        boolean predictive = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("predictive_bg", false);
        if (isBTShare) {
            predictive = false;
        }
        if (lastBgReading != null) {

            // detect broken data from G5 or other sources
            if ((lastBgReading.raw_data != 0) && (lastBgReading.raw_data * 2 == lastBgReading.filtered_data)) {
                if (JoH.ratelimit("g5-corrupt-data-warning", 1200)) {
                    final String msg = "filtered data is exactly double raw sensor data which looks wrong! (Transmitter maybe dead)" + lastBgReading.raw_data;
                    toaststaticnext(msg);
                }
            }

            displayCurrentInfoFromReading(lastBgReading, predictive);
        } else {
            display_delta = "";
        }

        if (prefs.getBoolean("extra_status_line", false)) {
            extraStatusLineText.setText(extraStatusLine());
            extraStatusLineText.setVisibility(View.VISIBLE);
        } else {
            extraStatusLineText.setText("");
            extraStatusLineText.setVisibility(View.GONE);
        }
    }

    @NonNull
    public static String extraStatusLine() {

        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs==null) return "";

        final StringBuilder extraline = new StringBuilder();
        final Calibration lastCalibration = Calibration.lastValid();
        if (prefs.getBoolean("status_line_calibration_long", false) && lastCalibration != null) {
            if (extraline.length() != 0) extraline.append(' ');
            extraline.append("slope = ");
            extraline.append(String.format("%.2f", lastCalibration.slope));
            extraline.append(' ');
            extraline.append("inter = ");
            extraline.append(String.format("%.2f", lastCalibration.intercept));
        }

        if (prefs.getBoolean("status_line_calibration_short", false) && lastCalibration != null) {
            if (extraline.length() != 0) extraline.append(' ');
            extraline.append("s:");
            extraline.append(String.format("%.2f", lastCalibration.slope));
            extraline.append(' ');
            extraline.append("i:");
            extraline.append(String.format("%.2f", lastCalibration.intercept));
        }

        if (prefs.getBoolean("status_line_avg", false)
                || prefs.getBoolean("status_line_a1c_dcct", false)
                || prefs.getBoolean("status_line_a1c_ifcc", false)
                || prefs.getBoolean("status_line_in", false)
                || prefs.getBoolean("status_line_high", false)
                || prefs.getBoolean("status_line_low", false)
                || prefs.getBoolean("status_line_stdev", false)
                || prefs.getBoolean("status_line_carbs", false)
                || prefs.getBoolean("status_line_insulin", false)
                || prefs.getBoolean("status_line_royce_ratio", false)
                || prefs.getBoolean("status_line_accuracy", false)
                || prefs.getBoolean("status_line_capture_percentage", false)
                || prefs.getBoolean("status_line_pump_reservoir", false)) {

            final StatsResult statsResult = new StatsResult(prefs, getPreferencesBooleanDefaultFalse("extra_status_stats_24h"));

            if (prefs.getBoolean("status_line_avg", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getAverageUnitised());
            }
            if (prefs.getBoolean("status_line_a1c_dcct", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getA1cDCCT());
            }
            if (prefs.getBoolean("status_line_a1c_ifcc", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getA1cIFCC());
            }
            if (prefs.getBoolean("status_line_in", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getInPercentage());
            }
            if (prefs.getBoolean("status_line_high", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getHighPercentage());
            }
            if (prefs.getBoolean("status_line_low", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getLowPercentage());
            }
            if (prefs.getBoolean("status_line_stdev", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getStdevUnitised());
            }
            if (prefs.getBoolean("status_line_carbs", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                //extraline.append("Carbs: " + statsResult.getTotal_carbs());
                extraline.append("Carbs:" + Math.round(statsResult.getTotal_carbs()));
            }
            if (prefs.getBoolean("status_line_insulin", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append("U:" + JoH.qs(statsResult.getTotal_insulin(), 2));
            }
            if (prefs.getBoolean("status_line_royce_ratio", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append("C/I:" + JoH.qs(statsResult.getRatio(), 2));
            }
            if (prefs.getBoolean("status_line_capture_percentage", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(statsResult.getCapturePercentage(false));
            }
            if (prefs.getBoolean("status_line_accuracy", false)) {
                final long accuracy_period = DAY_IN_MS * 3;
                if (extraline.length() != 0) extraline.append(' ');
                final String accuracy_report = Accuracy.evaluateAccuracy(accuracy_period);
                if ((accuracy_report != null) && (accuracy_report.length() > 0)) {
                    extraline.append(accuracy_report);
                } else {
                    final String accuracy = BloodTest.evaluateAccuracy(accuracy_period);
                    extraline.append(((accuracy != null) ? " " + accuracy : ""));
                }
            }

            if (prefs.getBoolean("status_line_pump_reservoir", false)) {
                if (extraline.length() != 0) extraline.append(' ');
                extraline.append(PumpStatus.getBolusIoBString());
                extraline.append(PumpStatus.getReservoirString());
                extraline.append(PumpStatus.getBatteryString());
            }

        }
        if (prefs.getBoolean("extra_status_calibration_plugin", false)) {
            final CalibrationAbstract plugin = getCalibrationPluginFromPreferences(); // make sure do this only once
            if (plugin != null) {
                final CalibrationAbstract.CalibrationData pcalibration = plugin.getCalibrationData();
                if (extraline.length() > 0) extraline.append("\n"); // not tested on the widget yet
                if (pcalibration != null) extraline.append("(" + plugin.getAlgorithmName() + ") s:" + JoH.qs(pcalibration.slope, 2) + " i:" + JoH.qs(pcalibration.intercept, 2));
                BgReading bgReading = BgReading.last();
                if (bgReading != null) {
                    final boolean doMgdl = prefs.getString("units", "mgdl").equals("mgdl");
                    extraline.append(" \u21D2 " + BgGraphBuilder.unitized_string(plugin.getGlucoseFromSensorValue(bgReading.age_adjusted_raw_value), doMgdl) + " " + BgGraphBuilder.unit(doMgdl));
                }
            }

            // If we are using the plugin as the primary then show xdrip original as well
            if (Home.getPreferencesBooleanDefaultFalse("display_glucose_from_plugin") || Home.getPreferencesBooleanDefaultFalse("use_pluggable_alg_as_primary")) {
                final CalibrationAbstract plugin_xdrip = getCalibrationPlugin(PluggableCalibration.Type.xDripOriginal); // make sure do this only once
                if (plugin_xdrip != null) {
                    final CalibrationAbstract.CalibrationData pcalibration = plugin_xdrip.getCalibrationData();
                    if (extraline.length() > 0)
                        extraline.append("\n"); // not tested on the widget yet
                    if (pcalibration != null)
                        extraline.append("(" + plugin_xdrip.getAlgorithmName() + ") s:" + JoH.qs(pcalibration.slope, 2) + " i:" + JoH.qs(pcalibration.intercept, 2));
                    BgReading bgReading = BgReading.last();
                    if (bgReading != null) {
                        final boolean doMgdl = prefs.getString("units", "mgdl").equals("mgdl");
                        extraline.append(" \u21D2 " + BgGraphBuilder.unitized_string(plugin_xdrip.getGlucoseFromSensorValue(bgReading.age_adjusted_raw_value), doMgdl) + " " + BgGraphBuilder.unit(doMgdl));
                    }
                }
            }

        }

        if (prefs.getBoolean("status_line_time", false)) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            if (extraline.length() != 0) extraline.append(' ');
            extraline.append(sdf.format(new Date()));
        }
        return extraline.toString();

    }

    public static long stale_data_millis()
    {
        if (DexCollectionType.getDexCollectionType() == DexCollectionType.LibreAlarm) return (60000 * 13);
        return (60000 * 11);
    }

    private void displayCurrentInfoFromReading(BgReading lastBgReading, boolean predictive) {
        double estimate = 0;
        double estimated_delta = 0;
        if (lastBgReading == null) return;
        final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        if (dg == null) return;
        //String slope_arrow = lastBgReading.slopeArrow();
        String slope_arrow = dg.delta_arrow;
        String extrastring = "";
        // when stale
        if ((new Date().getTime()) - stale_data_millis() - lastBgReading.timestamp > 0) {
            notificationText.setText(R.string.signal_missed);
            if (!predictive) {
                //  estimate = lastBgReading.calculated_value;
                estimate = dg.mgdl;
            } else {
                estimate = BgReading.estimated_bg(lastBgReading.timestamp + (6000 * 7));
            }
            currentBgValueText.setText(bgGraphBuilder.unitized_string(estimate));
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            dexbridgeBattery.setPaintFlags(dexbridgeBattery.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            // not stale
            if (notificationText.getText().length() == 0) {
                notificationText.setTextColor(Color.WHITE);
            }
            boolean bg_from_filtered = prefs.getBoolean("bg_from_filtered", false);
            if (!predictive) {
                //estimate = lastBgReading.calculated_value; // normal
                estimate = dg.mgdl;
                currentBgValueText.setTypeface(null, Typeface.NORMAL);

                // if noise has settled down then switch off filtered mode
                if ((bg_from_filtered) && (BgGraphBuilder.last_noise < BgGraphBuilder.NOISE_FORGIVE) && (prefs.getBoolean("bg_compensate_noise", false))) {
                    bg_from_filtered = false;
                    prefs.edit().putBoolean("bg_from_filtered", false).apply();
                }

                // TODO this should be partially already be covered by dg - recheck
                if (BestGlucose.compensateNoise()) {
                    estimate = BgGraphBuilder.best_bg_estimate; // this maybe needs scaling based on noise intensity
                    estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
                    slope_arrow = BgReading.slopeToArrowSymbol(estimated_delta / (BgGraphBuilder.DEXCOM_PERIOD / 60000)); // delta by minute
                    currentBgValueText.setTypeface(null, Typeface.ITALIC);
                    extrastring = "\u26A0"; // warning symbol !

                    if ((BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_HIGH) && (DexCollectionType.hasFiltered())) {
                        bg_from_filtered = true; // force filtered mode
                    }
                }

                if (bg_from_filtered) {
                    currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    estimate = lastBgReading.filtered_calculated_value;
                } else {
                    currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
                }
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                if ((lastBgReading.hide_slope) || (bg_from_filtered)) {
                    slope_arrow = "";
                }
                currentBgValueText.setText(stringEstimate + " " + slope_arrow);
            } else {
                // old depreciated prediction
                estimate = BgReading.activePrediction();
                String stringEstimate = bgGraphBuilder.unitized_string(estimate);
                currentBgValueText.setText(stringEstimate + " " + BgReading.activeSlopeArrow());
            }
            if (extrastring.length() > 0)
                currentBgValueText.setText(extrastring + currentBgValueText.getText());
        }
        int minutes = (int) (System.currentTimeMillis() - lastBgReading.timestamp) / (60 * 1000);

        if ((!small_width) || (notificationText.length() > 0)) notificationText.append("\n");
        if (!small_width) {
            notificationText.append(minutes + ((minutes == 1) ? getString(R.string.space_minute_ago) : getString(R.string.space_minutes_ago)));
        } else {
            // small screen
            notificationText.append(minutes + getString(R.string.space_mins));
            currentBgValueText.setPadding(0, 0, 0, 0);
        }

        if (small_screen) {
            if (currentBgValueText.getText().length() > 4)
                currentBgValueText.setTextSize(25);
        }

        // do we actually need to do this query here if we again do it in unitizedDeltaString
        List<BgReading> bgReadingList = BgReading.latest(2, is_follower);
        if (bgReadingList != null && bgReadingList.size() == 2) {
            // same logic as in xDripWidget (refactor that to BGReadings to avoid redundancy / later inconsistencies)?

            //display_delta = bgGraphBuilder.unitizedDeltaString(true, true, is_follower);
            display_delta = dg.unitized_delta;
            if (BestGlucose.compensateNoise()) {
                //final double estimated_delta = BgGraphBuilder.best_bg_estimate - BgGraphBuilder.last_bg_estimate;
                display_delta = bgGraphBuilder.unitizedDeltaStringRaw(true, true, estimated_delta);
                addDisplayDelta();
                if (!prefs.getBoolean("show_noise_workings", false)) {
                    notificationText.append("\nNoise: " + bgGraphBuilder.noiseString(BgGraphBuilder.last_noise));
                }
            } else {
                addDisplayDelta();
            }

        }
        if (bgGraphBuilder.unitized(estimate) <= bgGraphBuilder.lowMark) {
            currentBgValueText.setTextColor(Color.parseColor("#C30909"));
        } else if (bgGraphBuilder.unitized(estimate) >= bgGraphBuilder.highMark) {
            currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
        } else {
            currentBgValueText.setTextColor(Color.WHITE);
        }

        // TODO this should be made more efficient probably
        if (Home.getPreferencesBooleanDefaultFalse("display_glucose_from_plugin") && (PluggableCalibration.getCalibrationPluginFromPreferences() != null)) {
            currentBgValueText.setText(getString(R.string.p_in_circle) + currentBgValueText.getText()); // adds warning P in circle icon
        }
    }

    private void addDisplayDelta() {
        if (BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            notificationText.append("  ");
        } else {
            notificationText.append("\n");
        }
        notificationText.append(display_delta);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (blockTouches) return true;
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            // !?
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);


        //wear integration
        if (!prefs.getBoolean("wear_sync", false)) {
            menu.removeItem(R.id.action_open_watch_settings);
            menu.removeItem(R.id.action_resend_last_bg);
            menu.removeItem(R.id.action_sync_watch_db);//KS
        }

        //speak readings
        MenuItem menuItem = menu.findItem(R.id.action_toggle_speakreadings);
        if (prefs.getBoolean("bg_to_speech_shortcut", false)) {

            menuItem.setVisible(true);
            if (prefs.getBoolean("bg_to_speech", false)) {
                menuItem.setChecked(true);
            } else {
                menuItem.setChecked(false);
            }
        } else {
            menuItem.setVisible(false);
        }

        boolean parakeet_menu_items = false;
        if (DexCollectionType.hasWifi()) {
            parakeet_menu_items = prefs.getBoolean("plus_extra_features", false);
        }
        menu.findItem(R.id.showmap).setVisible(parakeet_menu_items);
        menu.findItem(R.id.parakeetsetup).setVisible(parakeet_menu_items);

        boolean got_data = Experience.gotData();
        menu.findItem(R.id.crowdtranslate).setVisible(got_data);

        menu.findItem(R.id.showreminders).setVisible(prefs.getBoolean("plus_show_reminders", true));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        processIncomingBundle(bundle);
    }


    private synchronized void showcasemenu(int option) {
        if ((myShowcase != null) && (myShowcase.isShowing())) return;
        if (ShotStateStore.hasShot(option)) return;
        //  if (showcaseblocked) return;
        try {
            ViewTarget target = null;
            String title = "";
            String message = "";
            int size1 = 90;
            int size2 = 14;

                switch (option) {

                    case SHOWCASE_MOTION_DETECTION:
                        target= new ViewTarget(R.id.btnNote, this); // dummy
                        size1=0;
                        size2=0;
                        title="Motion Detection Warning";
                        message="Activity Motion Detection is experimental and only some phones are compatible. Its main purpose is to detect vehicle mode.\n\nIn tests it seems higher end phones are the most likely to properly support the sensors needed for it to work.  It may also drain your battery.\n\nFor exercise related movement, Smartwatch step counters work better.";
                        break;

                 /*   case SHOWCASE_G5FIRMWARE:
                        target= new ViewTarget(R.id.btnNote, this); // dummy
                        size1=0;
                        size2=0;
                        title="G5 Firmware Warning";
                        message="Transmitters containing updated firmware which started shipping around 20th Nov 2016 appear to be currently incompatible with xDrip+\n\nWork will continue to try to resolve this issue but at the time of writing there is not yet a solution.  For the latest updates you can select the Alpha or Nightly update channel.";
                        break;*/

                    case SHOWCASE_VARIANT:
                        target= new ViewTarget(R.id.btnNote, this); // dummy
                        size1=0;
                        size2=0;
                        title="You are using an xDrip+ variant";
                        message="xDrip+ variants allow multiple apps to be installed at once. Either for advanced use or to allow xDrip-Experimental and xDrip+ to both be installed at the same time.\n\nWith variants, some things might not work properly due to one app or another having exclusive access, for example bluetooth pairing. Feedback and bug reports about variants is welcomed!";
                        break;

                    case SHOWCASE_NOTE_LONG:
                        target = new ViewTarget(R.id.btnNote, this);
                        title = getString(R.string.note_button);
                        message = getString(R.string.showcase_note_long);
                        break;
                    case SHOWCASE_REDO:
                        target = new ViewTarget(R.id.btnRedo, this);
                        title = getString(R.string.redo_button);
                        message = getString(R.string.showcase_redo);
                        break;
                    case SHOWCASE_UNDO:
                        target = new ViewTarget(R.id.btnUndo, this);
                        title = getString(R.string.undo_button);
                        message = getString(R.string.showcase_undo);
                        break;
                    case 3:
                        target = new ViewTarget(R.id.btnTreatment, this);
                        break;

                    case 1:
                        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);

                        List<View> views = toolbar.getTouchables();

                        //Log.d("xxy", Integer.toString(views.size()));
                        for (View view : views) {
                            Log.d("jamhorham showcase", view.getClass().getSimpleName());

                            if (view.getClass().getSimpleName().equals("OverflowMenuButton")) {
                                target = new ViewTarget(view);
                                break;
                            }
                        }
                        break;
                }


                if (target != null) {
                    //showcaseblocked = true;
                    myShowcase = new ShowcaseView.Builder(this)

                           /* .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showcaseblocked=false;
                                    myShowcase.hide();
                                }
                            })*/
                            .setTarget(target)
                            .setStyle(R.style.CustomShowcaseTheme2)
                            .setContentTitle(title)
                            .setContentText("\n"+message)
                            .setShowcaseDrawer(new JamorhamShowcaseDrawer(getResources(),getTheme(),size1,size2))
                            .singleShot(oneshot ? option : -1)
                            .build();

                    myShowcase.setBackgroundColor(Color.TRANSPARENT);
                    myShowcase.show();
                }

        } catch (Exception e) {
            Log.e(TAG, "Exception in showcase: " + e.toString());
        }
    }

    public void shareMyConfig(MenuItem myitem) {
        startActivity(new Intent(getApplicationContext(), DisplayQRCode.class));
    }

    public void settingsSDcardExport(MenuItem myitem) {
        startActivity(new Intent(getApplicationContext(), SdcardImportExport.class));
    }

    public void showMapFromMenu(MenuItem myitem) {
        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
    }

    public void showHelpFromMenu(MenuItem myitem) {
        startActivity(new Intent(getApplicationContext(), HelpActivity.class));
    }

    public void showRemindersFromMenu(MenuItem myitem) {
        startActivity(new Intent(getApplicationContext(), Reminders.class));
    }


    public void parakeetSetupMode(MenuItem myitem) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.are_you_sure_you_want_switch_parakeet_to_setup);

                alertDialogBuilder.setPositiveButton(R.string.yes_enter_setup_mode, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // switch parakeet to setup mode
                ParakeetHelper.parakeetSetupMode(getApplicationContext());
            }
        });


        alertDialogBuilder.setNegativeButton(R.string.nokeep_parakeet_as_it_is, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               // do nothing
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void undoButtonClick(View myitem) {
        if (UndoRedo.undoNextItem()) staticRefreshBGCharts();
    }

    public void redoButtonClick(View myitem) {
        if (UndoRedo.redoNextItem()) staticRefreshBGCharts();
    }

    public void noteDefaultMethodChanged(View myitem) {
        setPreferencesBoolean("default_to_voice_notes", !getPreferencesBooleanDefaultFalse("default_to_voice_notes"));
    }

    public void showNoteTextInputDialog(View myitem, final long timestamp) {
    showNoteTextInputDialog(myitem,timestamp,-1);
    }
    public void showNoteTextInputDialog(View myitem, final long timestamp, final double position) {
        Log.d(TAG,"showNoteTextInputDialog: ts:"+timestamp+" pos:"+position);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.note_dialog_phone, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.treatment_note_edit_text);
        final CheckBox cbx = (CheckBox) dialogView.findViewById(R.id.default_to_voice_input);
        cbx.setChecked(getPreferencesBooleanDefaultFalse("default_to_voice_notes"));

        dialogBuilder.setTitle(R.string.treatment_note);
        //dialogBuilder.setMessage("Enter text below");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String treatment_text = edt.getText().toString().trim();
                Log.d(TAG, "Got treatment note: " + treatment_text);
                Treatments.create_note(treatment_text, timestamp, position); // timestamp?
                Home.staticRefreshBGCharts();

                if (treatment_text.length()>0) {
                    // display snackbar of the snackbar
                    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Home.startHomeWithExtra(xdrip.getAppContext(), Home.CREATE_TREATMENT_NOTE, Long.toString(timestamp), Double.toString(position));
                        }
                    };
                    Home.snackBar(R.string.add_note, getString(R.string.added) + ":    " + treatment_text, mOnClickListener, mActivity);
                }

                if (getPreferencesBooleanDefaultFalse("default_to_voice_notes")) showcasemenu(SHOWCASE_NOTE_LONG);
                dialog = null;
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (getPreferencesBooleanDefaultFalse("default_to_voice_notes")) showcasemenu(SHOWCASE_NOTE_LONG);
                dialog = null;

            }
        });
        if (Treatments.byTimestamp(timestamp, (int) (2.5 * MINUTE_IN_MS)) != null) {
            dialogBuilder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // are you sure?
                    final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                    builder.setTitle("Confirm Delete");
                    builder.setMessage("Are you sure you want to delete this Treatment?");
                    builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Treatments.delete_by_timestamp(timestamp, (int) (2.5 * MINUTE_IN_MS), true); // 2.5 min resolution
                            staticRefreshBGCharts();
                            JoH.static_toast_short("Deleted!");
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                    dialog = null;
                }
            });
        }
        dialog = dialogBuilder.create();
        edt.setInputType(InputType.TYPE_CLASS_TEXT);
        edt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (dialog != null)
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });


        dialog.show();

    }

    public void doBackFillBroadcast(MenuItem myitem) {
        GcmActivity.syncBGTable2();
        toast("Starting sync to other devices");
    }

    public void deleteAllBG(MenuItem myitem) {
        BgReading.deleteALL();
        toast("Deleting ALL BG readings!");
        staticRefreshBGCharts();
    }

    public void checkForUpdate(MenuItem myitem) {
        if (JoH.ratelimit("manual-update-check",5)) {
            toast(getString(R.string.checking_for_update));
            UpdateActivity.last_check_time = -1;
            UpdateActivity.checkForAnUpdate(getApplicationContext());
        }
    }

    public void sendFeedback(MenuItem myitem) {
        startActivity(new Intent(getApplicationContext(), SendFeedBack.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (JoH.quietratelimit("button-press", 5)) {
                    if (Home.getPreferencesBooleanDefaultFalse("buttons_silence_alert")) {
                        final ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
                        if (activeBgAlert != null) {
                            AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);
                            final String msg = "Snoozing alert due to volume button press";
                            JoH.static_toast_long(msg);
                            UserError.Log.ueh(TAG, msg);
                        } else {
                            if (d) UserError.Log.d(TAG, "no active alert to snooze");
                        }
                    } else {
                        if (d) UserError.Log.d(TAG, "No action as preference is disabled");
                    }
                }
                break;
        }
        if (d) Log.d(TAG, "Keydown event: " + keyCode + " event: " + event.toString());
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_resend_last_bg:
                startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_RESEND));
                break;
            case R.id.action_open_watch_settings:
                startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_OPEN_SETTINGS));
                break;
            case R.id.action_sync_watch_db:
                startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_RESET_DB));
                break;
        }

        if (item.getItemId() == R.id.action_export_database) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    int permissionCheck = ContextCompat.checkSelfPermission(Home.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(Home.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                0);
                        return null;
                    } else {
                        return DatabaseUtil.saveSql(getBaseContext());
                    }

                }

                @Override
                protected void onPostExecute(String filename) {
                    super.onPostExecute(filename);
                    if (filename != null) {

                        snackBar(R.string.share, getString(R.string.exported_to) + filename, makeSnackBarUriLauncher(Uri.fromFile(new File(filename)), "Share database..."), Home.this);

                    /*    SnackbarManager.show(
                                Snackbar.with(Home.this)
                                        .type(SnackbarType.MULTI_LINE)
                                        .duration(4000)
                                        .text(getString(R.string.exported_to) + filename) // text to display
                                        .actionLabel("Share") // action button label
                                        .actionListener(new SnackbarUriListener(Uri.fromFile(new File(filename)))),
                                Home.this);*/
                    } else {
                        Toast.makeText(Home.this, R.string.could_not_export_database, Toast.LENGTH_LONG).show();
                    }
                }
            }.execute();

            return true;
        }

        if (item.getItemId() == R.id.action_import_db) {
            startActivity(new Intent(this, ImportDatabaseActivity.class));
            return true;
        }

       /* // jamorham additions
        if (item.getItemId() == R.id.synctreatments) {
            startActivity(new Intent(this, GoogleDriveInterface.class));
            return true;

        }*/
        ///


        if (item.getItemId() == R.id.action_export_csv_sidiary) {

            long from = Home.getPreferencesLong("sidiary_last_exportdate", 0);
            final GregorianCalendar date = new GregorianCalendar();
            final DatePickerFragment datePickerFragment = new DatePickerFragment();
            if(from > 0)datePickerFragment.setInitiallySelectedDate(from);
            datePickerFragment.setAllowFuture(false);
            datePickerFragment.setTitle(getString(R.string.sidiary_date_title));
            datePickerFragment.setDateCallback(new ProfileAdapter.DatePickerCallbacks() {
                @Override
                public void onDateSet(int year, int month, int day) {
                    date.set(year, month, day);
                    date.set(Calendar.HOUR_OF_DAY, 0);
                    date.set(Calendar.MINUTE, 0);
                    date.set(Calendar.SECOND, 0);
                    date.set(Calendar.MILLISECOND, 0);
                    new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            return DatabaseUtil.saveCSV(getBaseContext(), date.getTimeInMillis());
                        }

                        @Override
                        protected void onPostExecute(String filename) {
                            super.onPostExecute(filename);
                            if (filename != null) {
                                Home.setPreferencesLong("sidiary_last_exportdate", System.currentTimeMillis());
                                snackBar(R.string.share, getString(R.string.exported_to) + filename, makeSnackBarUriLauncher(Uri.fromFile(new File(filename)), "Share database..."), Home.this);
                            } else {
                                Toast.makeText(Home.this, "Could not export CSV :(", Toast.LENGTH_LONG).show();
                            }
                        }
                    }.execute();
                }
            });
            datePickerFragment.show(getFragmentManager(), "DatePicker");
            return true;
        }

        if (item.getItemId() == R.id.action_toggle_speakreadings) {
            prefs.edit().putBoolean("bg_to_speech", !prefs.getBoolean("bg_to_speech", false)).commit();
            invalidateOptionsMenu();
            if (prefs.getBoolean("bg_to_speech", false)) {
                BgToSpeech.testSpeech();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // TODO reduce duplicated functionality
    public static boolean getPreferencesBooleanDefaultFalse(final String pref) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if ((prefs != null) && (prefs.getBoolean(pref, false))) {
            return true;
        }
        return false;
    }

    public static boolean getPreferencesBoolean(final String pref, boolean def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if ((prefs != null) && (prefs.getBoolean(pref, def))) return true;
        return false;
    }

    public static void togglePreferencesBoolean(final String pref) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) prefs.edit().putBoolean(pref, !prefs.getBoolean(pref, false)).apply();
    }

    public static String getPreferencesStringDefaultBlank(final String pref) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getString(pref, "");
        }
        return "";
    }

    public static String getPreferencesStringWithDefault(final String pref, final String def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getString(pref, def);
        } else {
            UserError.Log.wtf(TAG, "Could not initialize preferences in getPreferencesStringWithDefault: "+pref);
            return "";
        }
    }

    public static long getPreferencesLong(final String pref, final long def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getLong(pref, def);
        }
        return def;
    }

    public static int getPreferencesInt(final String pref, final int def) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            return prefs.getInt(pref, def);
        }
        return def;
    }


    public static boolean setPreferencesBoolean(final String pref, final boolean lng) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putBoolean(pref, lng).apply();
            return true;
        }
        return false;
    }

    public static boolean setPreferencesLong(final String pref, final long lng) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putLong(pref, lng).apply();
            return true;
        }
        return false;
    }

    public static boolean setPreferencesInt(final String pref, final int num) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putInt(pref, num).apply();
            return true;
        }
        return false;
    }

    public static boolean setPreferencesString(final String pref, final String str) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().putString(pref, str).apply();
            return true;
        }
        return false;
    }

    public static boolean removePreferencesItem(final String pref) {
        if ((prefs == null) && (xdrip.getAppContext() != null)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        }
        if (prefs != null) {
            prefs.edit().remove(pref).apply();
            return true;
        }
        return false;
    }


    public static double convertToMgDlIfMmol(double value) {
        if (!getPreferencesStringWithDefault("units", "mgdl").equals("mgdl")) {
            return value * com.eveningoutpost.dexdrip.UtilityModels.Constants.MMOLL_TO_MGDL;
        } else {
            return value; // no conversion needed
        }
    }

    public static void snackBar(int buttonString, String message, View.OnClickListener mOnClickListener, Activity activity) {

        android.support.design.widget.Snackbar.make(

                activity.findViewById(android.R.id.content),
                message, android.support.design.widget.Snackbar.LENGTH_LONG)
                .setAction(buttonString, mOnClickListener)
                //.setActionTextColor(Color.RED)
                .show();
    }

    public static void staticBlockUI(Activity context, boolean state) {
        blockTouches = state;
        if (state) {
            JoH.lockOrientation(context);
        } else {
            JoH.releaseOrientation(context);
        }
    }

    // classes
    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport);
                updatingChartViewport = false;
            }
        }
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }

    private View.OnClickListener makeSnackBarUriLauncher(final Uri uri, final String text) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("application/octet-stream");
                Home.this.startActivity(Intent.createChooser(shareIntent, text));
            }
        };
    }

    public static PendingIntent getHomePendingIntent() {
        return PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
    }

   /* class SnackbarUriListener implements ActionClickListener {
        Uri uri;

        SnackbarUriListener(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void onActionClicked(Snackbar snackbar) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("application/octet-stream");
            startActivity(Intent.createChooser(shareIntent, "Share database..."));
        }
    }*/


    class MyActionItemTarget implements Target {

        //private final Toolbar toolbar;
        // private final int menuItemId;
        private final View mView;
        private final int xoffset;
        private final int yoffset;

        public MyActionItemTarget(View mView, int xoffset, int yoffset) {
            // this.toolbar = toolbar;
            //this.menuItemId = itemId;
            this.mView = mView;
            // get dp yada
            this.xoffset = xoffset;
            this.yoffset = yoffset;
        }

        @Override
        public Point getPoint() {
            int[] location = new int[2];
            mView.getLocationInWindow(location);
            int x = location[0] + mView.getWidth() / 2;
            int y = location[1] + mView.getHeight() / 2;
            return new Point(x + xoffset, y + yoffset);
        }

    }

    class ToolbarActionItemTarget implements Target {

        private final Toolbar toolbar;
        private final int menuItemId;

        public ToolbarActionItemTarget(Toolbar toolbar, @IdRes int itemId) {
            this.toolbar = toolbar;
            this.menuItemId = itemId;
        }

        @Override
        public Point getPoint() {
            return new ViewTarget(toolbar.findViewById(menuItemId)).getPoint();
        }

    }

}



