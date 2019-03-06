package com.eveningoutpost.dexdrip.utilitymodels;

// jamorham

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.core.app.*;
import androidx.core.content.*;

import com.eveningoutpost.dexdrip.*;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.*;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.Response;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import okhttp3.*;

import static com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper.*;

public class UpdateActivity extends BaseActivity {

    public static final String AUTO_UPDATE_PREFS_NAME = "auto_update_download";
    private static final String useInternalDownloaderPrefsName = "use_internal_downloader";
    private static final String last_update_check_time = "last_update_check_time";
    private static final String TAG = "jamorham update";
    private static OkHttpClient httpClient = null;
    public static long last_check_time = 0;
    private static SharedPreferences prefs;
    private static int versionnumber = 0;
    private static int newversion = 0;
    private static String lastDigest = "";
    private final static int MY_PERMISSIONS_REQUEST_STORAGE_DOWNLOAD = 105;
    private static boolean downloading = false;
    private static final boolean debug = false;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView updateMessageText;
    private ScrollView mScrollView;
    private File dest_file;
    private static String DOWNLOAD_URL = "";
    private static int FILE_SIZE = -1;
    private static String MESSAGE = "";
    private static String CHECKSUM = "";

    public static void checkForAnUpdate(final Context context) {
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ((last_check_time != -1) && (!prefs.getBoolean(AUTO_UPDATE_PREFS_NAME, true))) return;
        if (last_check_time == 0)
            last_check_time = prefs.getLong(last_update_check_time, 0);
        if (((JoH.tsl() - last_check_time) > 86300000) || (debug)) {
            last_check_time = JoH.tsl();
            prefs.edit().putLong(last_update_check_time, last_check_time).apply();

            String channel = prefs.getString("update_channel", "beta");
            UserError.Log.i(TAG, "Checking for a software update, channel: " + channel);

            String subversion = "";
            if (!context.getString(R.string.app_name).equals("xDrip+")) {
                subversion = context.getString(R.string.app_name).replaceAll("[^a-zA-Z0-9]", "");
                UserError.Log.i(TAG, "Using subversion: " + subversion);
            }

            final String CHECK_URL = context.getString(R.string.wserviceurl) + "/update-check/" + channel + subversion;
            DOWNLOAD_URL = "";
            newversion = 0;

            new Thread(() -> {
                try {
                    if (httpClient == null) {
                        httpClient = enableTls12OnPreLollipop(new OkHttpClient.Builder()
                                .connectTimeout(30, TimeUnit.SECONDS)
                                .readTimeout(60, TimeUnit.SECONDS)
                                .writeTimeout(20, TimeUnit.SECONDS))
                                .build();
                    }
                    getVersionInformation(context);
                    if (versionnumber == 0) return;

                    String locale = "";
                    try {
                        locale = Locale.getDefault().toString();
                        if (locale == null) locale = "";
                    } catch (Exception e) {
                        // do nothing
                    }


                    final Request request = new Request.Builder()
                            // Mozilla header facilitates compression
                            .header("User-Agent", "Mozilla/5.0")
                            .header("Connection", "close")
                            .url(CHECK_URL + "?r=" + (System.currentTimeMillis() / 100000) % 9999999 + "&ln=" + JoH.urlEncode(locale))
                            .build();

                    final Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful()) {

	                    final String[] lines = response.body().string().split("\\r?\\n");
                        if (lines.length > 1) {
                            try {
                                newversion = Integer.parseInt(lines[0]);
                                if ((newversion > versionnumber) || (debug)) {
                                    if (lines[1].startsWith("http")) {
                                        UserError.Log.i(TAG, "Notifying user of new update available our version: " + versionnumber + " new: " + newversion);
                                        DOWNLOAD_URL = lines[1];
                                        if (lines.length > 2) {
                                            try {
                                                FILE_SIZE = Integer.parseInt(lines[2]);
                                            } catch (NumberFormatException | NullPointerException e) {
                                                UserError.Log.e(TAG, "Got exception processing update download parameters");
                                            }
                                        } else {
                                            FILE_SIZE = -1;
                                        }
                                        if (lines.length > 3) {
                                            MESSAGE = lines[3];
                                        } else {
                                            MESSAGE = "";
                                        }
                                        if (lines.length > 4) {
                                            CHECKSUM = lines[4];
                                        } else {
                                            CHECKSUM = "";
                                        }

                                        final Intent intent = new Intent(context, UpdateActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intent);

                                    } else {
                                        UserError.Log.e(TAG, "Error parsing second line of update reply");
                                    }
                                } else {
                                    UserError.Log.i(TAG, "Our current version is the most recent: " + versionnumber + " vs " + newversion);
                                }
                            } catch (Exception e) {
                                UserError.Log.e(TAG, "Got exception parsing update version: " + e.toString());
                            }
                        } else {
                            UserError.Log.i(TAG, "zero lines received in reply");
                        }
                        UserError.Log.i(TAG, "Success getting latest software version");
                    } else {
                        UserError.Log.i(TAG, "Failure getting update URL data: code: " + response.code());
                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception in reading http update version " + e.toString());
                }
                httpClient = null; // for GC
            }).start();
        }
    }

    private static String getDownloadFolder() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).toString();

    }

    private static void getVersionInformation(Context context) {
        // try {
        if (versionnumber == 0) {
            versionnumber = BuildConfig.buildVersion;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.activity_update);
        JoH.fixActionBar(this);

        progressText = (TextView) findViewById(R.id.progresstext);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressText.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        mScrollView = (ScrollView) findViewById(R.id.updateScrollView);
        updateMessageText = (TextView) findViewById(R.id.updatemessage);

        Switch autoUpdateSwitch = (Switch) findViewById(R.id.autoupdate);
        autoUpdateSwitch.setChecked(prefs.getBoolean(AUTO_UPDATE_PREFS_NAME, true));
        autoUpdateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(AUTO_UPDATE_PREFS_NAME, isChecked).apply();
            UserError.Log.i(TAG, "Auto Updates IsChecked:" + isChecked);
        });

        CheckBox useInternalDownloader = (CheckBox) findViewById(R.id.internaldownloadercheckBox);
        useInternalDownloader.setChecked(prefs.getBoolean(useInternalDownloaderPrefsName, true));
        useInternalDownloader.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(useInternalDownloaderPrefsName, isChecked).apply();
            UserError.Log.i(TAG, "Use internal downloader IsChecked:" + isChecked);
        });

        TextView detail = (TextView) findViewById(R.id.updatedetail);
        detail.setText(getString(R.string.new_version_date_colon) + newversion + "\n" + getString(R.string.old_version_date_colon) + versionnumber);
        TextView channel = (TextView) findViewById(R.id.update_channel);
        channel.setText(getString(R.string.update_channel_colon_space) + JoH.ucFirst(prefs.getString("update_channel", "beta")));

        updateMessageText.setText(MESSAGE);
    }

    public void closeActivity(View myview) {
        downloading = false;
        finish();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE_DOWNLOAD);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE_DOWNLOAD) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                downloadNow(null);
            } else {
                JoH.static_toast_long(this, "Cannot download without storage permission");
            }
        }
    }

    public void downloadNow(View myview) {
        if (!DOWNLOAD_URL.isEmpty()) {
            if (prefs.getBoolean(useInternalDownloaderPrefsName, true)) {
                if (checkPermissions()) {
                    if (downloading) {
                        JoH.static_toast_long(this, "Already downloading!");
                    } else {
                        downloading = true;
                        JoH.static_toast_long(this, "Attempting background download...");
                        mScrollView.post(() -> mScrollView.fullScroll(ScrollView.FOCUS_DOWN));
                        new AsyncDownloader().executeOnExecutor(xdrip.executor);

                    }
                } else {
                    JoH.static_toast_long(this, "Need permission to download file");
                }
            } else {
                viewIntentDownload(DOWNLOAD_URL);
                finish();
            }

        } else {
            UserError.Log.e(TAG, "Download button pressed but no download URL");
        }
    }

    private void viewIntentDownload(final String DOWNLOAD_URL) {
        final Intent downloadActivity = new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL + "&rr=" + JoH.tsl()));
        startActivity(downloadActivity);
    }

    // TODO WebAppHelper could/should implement these features too or we could use the download manager
    private class AsyncDownloader extends AsyncTask<Void, Long, Boolean> {
        private final String URL = DOWNLOAD_URL + "&rr=" + JoH.tsl();

        private final OkHttpClient.Builder okbuilder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true);

        private final OkHttpClient client = enableTls12OnPreLollipop(okbuilder).build();
        private String filename;

        @Override
        protected Boolean doInBackground(Void... params) {

            final Request request = new Request.Builder()
                    .header("User-Agent", "Mozilla/5.0 (jamorham)")
                    .header("Accept-Encoding", "")
                    .header("Connection", "close")
                    .url(URL)
                    .build();


            try {
                final Response response = client.newCall(request).execute();
                filename = response.header("Content-Disposition", "");
                final Matcher matcher = Pattern.compile("attachment;filename=\"(.*?)\"").matcher(filename);
                if (matcher.find()) {
                    filename = matcher.group(1);
                } else {
                    filename = "";
                    final Matcher matcher2 = Pattern.compile("/([^/]*?.apk)").matcher(URL);
                    if (matcher2.find()) {
                        filename = matcher2.group(1);
                    }
                }
                if (filename.length() < 5) {
                    filename = "xDrip-plus-" + newversion + ".apk";
                }

                UserError.Log.i(TAG, "Filename: " + filename);
                if (response.code() == 200) {
                    lastDigest = "";
                    InputStream inputStream = null;
                    FileOutputStream outputStream = null;
                    try {

                        dest_file = new File(getDownloadFolder(), filename);
                        try {
                            if (dest_file.exists())
                                dest_file.delete();
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "Got exception deleting existing file: " + e);
                        }

                        outputStream = new FileOutputStream(dest_file);
                        inputStream = response.body().byteStream();
                        MessageDigest messageDigest = null;
                        DigestInputStream digestInputStream = null;
                        try {
                            messageDigest = MessageDigest.getInstance("SHA256");
                            digestInputStream = new DigestInputStream(inputStream, messageDigest);
                        } catch (NoSuchAlgorithmException e) {
                            //
                        }
                        byte[] buff = new byte[1024 * 4];
                        long downloaded = 0;
                        long target = response.body().contentLength();
                        if (target == -1)
                            target = FILE_SIZE; // get this from update server alternately
                        publishProgress(0L, target);
                        while (true) {

                            int last_read = (digestInputStream != null) ? digestInputStream.read(buff) : inputStream.read(buff);
                            if (last_read == -1) {
                                break;
                            }
                            outputStream.write(buff, 0, last_read);
                            downloaded += last_read;
                            publishProgress(downloaded, target);
                            if (isCancelled() || !downloading) {
                                return false;
                            }
                        }
                        if (messageDigest != null)
                            lastDigest = JoH.bytesToHex(messageDigest.digest()).toLowerCase();
                        return downloaded == target;

                    } catch (IOException e) {
                        UserError.Log.e(TAG, "Download error: " + e.toString());
                        JoH.static_toast_long(getApplicationContext(), "Data error: ");
                        return false;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                } else {
                    return false;
                }
            } catch (SocketTimeoutException e) {
                JoH.static_toast_long(getApplicationContext(), "Download timeout!");
                return false;
            } catch (IOException e) {
                UserError.Log.e(TAG, "Exception in download: " + e);
                if (e instanceof javax.net.ssl.SSLHandshakeException) {
                    if (JoH.ratelimit("internal-update-fallback", 15)) {
                        JoH.static_toast_long("Internal problems - trying with android system");
                        viewIntentDownload(URL);
                    }
                }
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            progressText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMax(values[1].intValue());
            progressBar.setProgress(values[0].intValue());

            long kbprogress = values[0] / 1024;
            long kbmax = values[1] / 1024;
            if (values[1] > 0) {
                progressText.setText(String.format(Locale.getDefault(), "%d / %d KB", kbprogress, kbmax));
            } else {
                progressText.setText(String.format(Locale.getDefault(), "%d KB", kbprogress));
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressText.setText(result ? "Downloaded" : "Failed");
            downloading = false;
            if (result) {
                if ((filename != null) && (filename.length() > 5) && (dest_file != null)) {
                    if ((CHECKSUM.isEmpty()) || (lastDigest.isEmpty()) || (CHECKSUM.equals(lastDigest))) {
                        try {
                            try {
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.addCompletedDownload(filename, "xDrip+ update version " + newversion, false, "application/vnd.android.package-archive", getDownloadFolder() + "/" + filename, FILE_SIZE, true);
                            } catch (Exception e) {
                                UserError.Log.e(TAG, "Download manager error: " + e);
                            }

                            final Intent installapk = new Intent(Intent.ACTION_VIEW);
                            installapk.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            installapk.setDataAndType(Uri.fromFile(dest_file), "application/vnd.android.package-archive");
                            startActivity(installapk);
                            finish();
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "Got exception trying to install apk: " + e);
                            JoH.static_toast_long(getApplicationContext(), "Update is in your downloads folder");
                        }
                    } else {
                        UserError.Log.e(TAG, "Checksum doesn't match: " + lastDigest + " vs " + CHECKSUM);
                        try {
                            dest_file.delete();
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "Got exception deleting corrupt file: " + e);
                        }
                        JoH.static_toast_long("File appears corrupt!");
                        finish();
                    }
                }
            } else {
                JoH.static_toast_long(getApplicationContext(), "Failed!");
                try {
                    if ((dest_file != null) && dest_file.exists())
                        dest_file.delete();
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Got exception deleting existing file: " + e);
                }
            }

        }
    }

    public static void clearLastCheckTime() {
        Pref.setLong(last_update_check_time,0L);
    }
}
