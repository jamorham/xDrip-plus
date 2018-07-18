package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.INCOMPATIBLE_BASE_ID;

/**
 * Created by jamorham on 01/11/2017.
 */

public class IncompatibleApps {

    private static final String NOTIFY_MARKER = "-NOTIFY";
    private static final int RENOTIFY_TIME = 86400 * 30;

    public static void notifyAboutIncompatibleApps() {
        final Context context = xdrip.getAppContext();
        int id = INCOMPATIBLE_BASE_ID;
        String package_name;


        package_name = "com.ambrosia.linkblucon";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                id = notify(context.getString(R.string.blukon), package_name, xdrip.getAppContext().getString(R.string.offical_msg) + " " + xdrip.getAppContext().getString(R.string.use_conflict_msg), id);
            }
        }


        package_name = "it.ct.glicemia";
        if (InstalledApps.checkPackageExists(context, package_name)) {
            if (JoH.pratelimit(package_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                id = notify("Glimp", package_name, "Glimp" + " " + xdrip.getAppContext().getString(R.string.use_conflict_msg) + "\n\n" + xdrip.getAppContext().getString(R.string.use_confict_msg_glimp), id);
            }
        }

    }

    private static int notify(String short_name, String package_string, String msg, int id) {
        JoH.showNotification("Incompatible App " + short_name, "Please uninstall or disable " + package_string, null, id, true, true, null, null, ((msg.length() > 0) ? msg + "\n\n" : "") + "Another installed app may be incompatible with xDrip. The other app should be uninstalled or disabled to prevent conflicts with shared resources.\nThe package identifier is: " + package_string);
        return id + 1;
    }


}
