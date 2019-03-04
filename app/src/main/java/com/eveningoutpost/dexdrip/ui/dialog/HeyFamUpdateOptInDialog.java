package com.eveningoutpost.dexdrip.ui.dialog;

import android.content.DialogInterface.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;

import static com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity.*;

// jamorham

// occasionally prompt to opt-in for updates

public class HeyFamUpdateOptInDialog {

    private static final boolean DEBUG = false;

    public static void heyFam(final AppCompatActivity activity) {
        if (DEBUG || (!Pref.getBooleanDefaultFalse(AUTO_UPDATE_PREFS_NAME)
                && Experience.ageOfThisBuildAtLeast(Constants.DAY_IN_MS * 60)
                && Experience.installedForAtLeast(Constants.DAY_IN_MS * 30)
                && Experience.gotData()
                && JoH.pratelimit("hey-fam-update-reminder", 86400 * 60))) {
            ask1(activity, () -> ask2(activity, () -> {
	            Pref.setBoolean(AUTO_UPDATE_PREFS_NAME, true);
	            JoH.static_toast_long(activity.getString(R.string.update_checking_enabled));
            }));
        }
    }

    // inform the user about this message
    private static void ask1(AppCompatActivity activity, Runnable runnable) {

        final androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle(R.string.hey_fam);
        builder.setMessage(R.string.you_have_update_checks_off);

        builder.setPositiveButton(activity.getString(R.string.ok), (OnClickListener) (dialog, which) -> runnable.run());

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

    }

    // ask the user if they would like to enable updates
    private static void ask2(AppCompatActivity activity, Runnable runnable) {

        final androidx.appcompat.app.AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.enable_update_checks);
        builder.setMessage(R.string.you_can_easily_roll_back_dialog_msg);

        builder.setPositiveButton(activity.getString(R.string.yes), (OnClickListener) (dialog, which) -> runnable.run());

        builder.setNegativeButton(activity.getString(R.string.cancel), (OnClickListener) (dialog, which) -> dialog.cancel());

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

    }
}

