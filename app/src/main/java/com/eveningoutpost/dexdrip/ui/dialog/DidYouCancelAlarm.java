package com.eveningoutpost.dexdrip.ui.dialog;

import android.content.DialogInterface.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.*;

// jamorham

// double check an alarm was intended to be cancelled

public class DidYouCancelAlarm {

    public static void dialog(final AppCompatActivity activity, Runnable runnable) {

        final AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle(R.string.cancel_alarm);
        builder.setMessage(R.string.please_confirm_to_cancel_the_alert);

        builder.setPositiveButton(R.string.yes_cancel, (OnClickListener) (dialog, which) -> runnable.run());

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
