package com.eveningoutpost.dexdrip.profileEditor;

import android.app.*;
import android.os.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.fragment.app.DialogFragment;

import java.util.*;

/**
 * Created by jamorham on 25/01/2017.
 */

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private String title;
    private long earliest = -1;
    private long initial = -1;
    private boolean allow_future = true;
    ProfileAdapter.DatePickerCallbacks callback;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAllowFuture(boolean future) {
        this.allow_future = future;
    }

    public void setEarliestDate(long time) {
        this.earliest = time;
    }

    public void setInitiallySelectedDate(long time) {
        this.initial = time;
    }

    public void setDateCallback(ProfileAdapter.DatePickerCallbacks callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        if (initial != -1) {
            c.setTimeInMillis(initial);
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog dp = new DatePickerDialog(getActivity(), this, year, month, day);
        if (title != null) dp.setTitle(title);
        if (!allow_future) dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        if (earliest > -1) dp.getDatePicker().setMinDate(earliest);
        return dp;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        callback.onDateSet(year, month, day);
    }
}
