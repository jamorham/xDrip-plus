package com.eveningoutpost.dexdrip.ui.dialog;

import android.content.DialogInterface.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.g5Model.*;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.*;

// jamorham

public class G6CalibrationCodeDialog {


    // ask the user for the code, check if its valid, only start sensor if it is
    public static void ask(AppCompatActivity activity, Runnable runnable) {

        final AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle(R.string.g6_sensor_code);
        builder.setMessage(R.string.please_enter_printed_calibration_code);

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(activity.getString(R.string.ok), (OnClickListener) (dialog, which) -> {
            final String code = input.getText().toString().trim();
            if (G6CalibrationParameters.checkCode(code)) {
                G6CalibrationParameters.setCurrentSensorCode(code);
                JoH.static_toast_long(activity.getString(R.string.code_accepted));
                if (runnable != null) runnable.run();
            } else {
                JoH.static_toast_long(activity.getString(R.string.invalid_or_unsupported_code));
            }
        });

        builder.setNegativeButton(activity.getString(R.string.cancel), (OnClickListener) (dialog, which) -> dialog.cancel());

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (dialog != null)
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        dialog.show();

    }
}


