package com.eveningoutpost.dexdrip.ui.dialog;

import android.content.DialogInterface.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.*;

// jamorham

// ask confirmation, run a runnable

public class GenericConfirmDialog {


    public static void show(final AppCompatActivity activity, String title, String message, Runnable runnable) {

        final androidx.appcompat.app.AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message);

        builder.setPositiveButton(R.string.yes, (OnClickListener) (dialog, which) -> runnable.run());

        builder.setNegativeButton(R.string.no, (OnClickListener) (dialog, which) -> dialog.cancel());

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        // apparently possible dialog is already showing, probably due to hash code
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            //
        }
        dialog.show();
    }
}

