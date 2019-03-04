package com.eveningoutpost.dexdrip.ui.dialog;

import android.content.DialogInterface.*;
import android.graphics.Color;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.rarepebble.colorpicker.*;

/**
 * jamorham
 *
 * Show a picker dialog and save the preference and call any required refresher
 */

public class ColorPreferenceDialog {

    public static void pick(final AppCompatActivity activity, final String pref, final String title, final Runnable runnable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final ColorPickerView picker = new ColorPickerView(activity);
        int color = Color.GRAY;
        try {
            color = Pref.getInt(pref, Color.GRAY);
        } catch (Exception e) {
            //
        }
        picker.setColor(color);
        picker.showAlpha(true);
        picker.showHex(false);
        builder.setTitle(title)
                .setView(picker)
                .setPositiveButton(R.string.ok, (OnClickListener) (dialog, which) -> {
                    // save result and refresh
                    Pref.setInt(pref, picker.getColor());
                    ColorCache.invalidateCache();
                    if (runnable != null) {
                        runnable.run();
                    }
                });
        builder.setNegativeButton(R.string.cancel, (OnClickListener) (dialog, which) -> {
        });
        builder.show();
    }
}


