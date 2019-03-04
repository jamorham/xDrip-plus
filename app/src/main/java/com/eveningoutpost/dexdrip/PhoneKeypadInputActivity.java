package com.eveningoutpost.dexdrip;

import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.eveningoutpost.dexdrip.wearintegration.*;

import java.util.*;

import static com.eveningoutpost.dexdrip.Home.*;


/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class PhoneKeypadInputActivity extends BaseActivity {

    private TextView mDialTextView;
    private Button zeroButton, oneButton, twoButton, threeButton, fourButton, fiveButton,
            sixButton, sevenButton, eightButton, nineButton, starButton, backSpaceButton;
    private ImageButton callImageButton, backspaceImageButton, insulintabbutton, carbstabbutton,
            bloodtesttabbutton, timetabbutton, speakbutton;

    private static String currenttab = "insulin";
    private static final String LAST_TAB_STORE = "phone-keypad-treatment-last-tab";
    private static final String TAG = "KeypadInput";
    private static Map<String, String> values = new HashMap<>();
    private String bgUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keypad_activity_phone);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        final int refdpi = 320;
        Log.d(TAG, "Width height: " + width + " " + height + " DPI:" + dm.densityDpi);
        getWindow().setLayout((int) Math.min(((520 * dm.densityDpi) / refdpi), width), (int) Math.min((650 * dm.densityDpi) / refdpi, height));
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.5f;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mDialTextView = (TextView) findViewById(R.id.dialed_no_textview);
        zeroButton = (Button) findViewById(R.id.zero_button);
        oneButton = (Button) findViewById(R.id.one_button);
        twoButton = (Button) findViewById(R.id.two_button);
        threeButton = (Button) findViewById(R.id.three_button);
        fourButton = (Button) findViewById(R.id.four_button);
        fiveButton = (Button) findViewById(R.id.five_button);
        sixButton = (Button) findViewById(R.id.six_button);
        sevenButton = (Button) findViewById(R.id.seven_button);
        eightButton = (Button) findViewById(R.id.eight_button);
        nineButton = (Button) findViewById(R.id.nine_button);
        starButton = (Button) findViewById(R.id.star_button);
        backSpaceButton = (Button) findViewById(R.id.backspace_button);
        // callImageButton = (ImageButton) stub.findViewById(R.id.call_image_button);
        // backspaceImageButton = (ImageButton) stub.findViewById(R.id.backspace_image_button);

        insulintabbutton = (ImageButton) findViewById(R.id.insulintabbutton);
        bloodtesttabbutton = (ImageButton) findViewById(R.id.bloodtesttabbutton);
        timetabbutton = (ImageButton) findViewById(R.id.timetabbutton);
        carbstabbutton = (ImageButton) findViewById(R.id.carbstabbutton);
        speakbutton = (ImageButton) findViewById(R.id.btnKeypadSpeak);

        mDialTextView.setText("");

        mDialTextView.setOnClickListener(v -> submitAll());

        zeroButton.setOnClickListener(v -> appCurrent("0"));

        //zeroButton.setOnLongClickListener(new View.OnLongClickListener() {
        //    @Override
        //    public boolean onLongClick(View v) {
        //        mDialTextView.setText(mDialTextView.getText() + "+");
        //        return true;
        //    }
        //});

        oneButton.setOnClickListener(v -> appCurrent("1"));

        twoButton.setOnClickListener(v -> appCurrent("2"));

        threeButton.setOnClickListener(v -> appCurrent("3"));

        fourButton.setOnClickListener(v -> appCurrent("4"));

        fiveButton.setOnClickListener(v -> appCurrent("5"));

        sixButton.setOnClickListener(v -> appCurrent("6"));

        sevenButton.setOnClickListener(v -> appCurrent("7"));

        eightButton.setOnClickListener(v -> appCurrent("8"));

        nineButton.setOnClickListener(v -> appCurrent("9"));

        starButton.setOnClickListener(v -> {
            if (!getValue(currenttab).contains(".")) appCurrent(".");
        });

        speakbutton.setOnClickListener(v -> {
            startHomeWithExtra(getApplicationContext(), Home.START_SPEECH_RECOGNITION, "ok");
            finish();
        });
        speakbutton.setOnLongClickListener(v -> {
            startHomeWithExtra(getApplicationContext(), Home.START_TEXT_RECOGNITION, "ok");
            finish();
            return true;
        });

        //hashButton.setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        mDialTextView.setText(mDialTextView.getText() + "#");
        //    }
        //});

        backSpaceButton.setOnClickListener(v -> appBackSpace());
        backSpaceButton.setOnLongClickListener(v -> {
            values.put(currenttab, "");
            updateTab();
            return true;
        });

        bloodtesttabbutton.setOnClickListener(v -> {
            currenttab = "bloodtest";
            updateTab();
        });
        insulintabbutton.setOnClickListener(v -> {
            currenttab = "insulin";
            updateTab();
        });
        carbstabbutton.setOnClickListener(v -> {
            currenttab = "carbs";
            updateTab();
        });
        timetabbutton.setOnClickListener(v -> {
            currenttab = "time";
            updateTab();
        });

        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            bgUnits = "mg/dl";
        } else {
            bgUnits = "mmol/l";
        }

        updateTab();
    }

    public static void resetValues() {
        values = new HashMap<>();
    }

    private static String getValue(String tab) {
        if (values.containsKey(tab)) {
            return values.get(tab);
        } else {
            values.put(tab, "");
            return values.get(tab);
        }
    }

    private static String appendValue(String tab, String append) {
        values.put(tab, getValue(tab) + append);
        return values.get(tab);
    }

    private static String appendCurrent(String append) {
        String cval = getValue(currenttab);
        if (cval.length() < 6) {
            if ((cval.length() == 0) && (append.equals("."))) append = "0.";
            return appendValue(currenttab, append);
        } else {
            return cval;
        }
    }

    private void appCurrent(String append) {
        appendCurrent(append);
        updateTab();
    }

    private void appBackSpace() {
        String cval = getValue(currenttab);
        if (cval.length() > 0) {
            values.put(currenttab, cval.substring(0, cval.length() - 1));
        }
        updateTab();
    }

    private boolean isNonzeroValueInTab(String tab)
    {
        try
        {
            return (0 != Double.parseDouble(getValue(tab)));
        }
        catch(NumberFormatException e) { return false; }
    }

    private boolean isInvalidTime()
    {
        String timeValue = getValue("time");
        if (timeValue.length() == 0) return false;
        if (!timeValue.contains("."))
            return (timeValue.length() < 3);

        String[] parts = timeValue.split("\\.");
        return (parts.length != 2) || (parts[0].length() == 0) || (parts[1].length() != 2);
    }

    private void submitAll() {

        boolean nonzeroBloodValue = isNonzeroValueInTab("bloodtest");
        boolean nonzeroCarbsValue = isNonzeroValueInTab("carbs");
        boolean nonzeroInsulinValue = isNonzeroValueInTab("insulin");

        // The green tick is clickable even when it's hidden, so we might get here
        // without valid data.  Ignore the click if input is incomplete
        if(!nonzeroBloodValue && !nonzeroCarbsValue && !nonzeroInsulinValue)
            return;

        if (isInvalidTime())
            return;

        // Add the dot to the time if it is missing
        String timeValue = getValue("time");
        if (timeValue.length() > 2 && !timeValue.contains(".")) {
            timeValue = timeValue.substring(0, timeValue.length()-2) + "." + timeValue.substring(timeValue.length()-2);
        }

        String mystring = "";
        if (timeValue.length() > 0) mystring += timeValue + " time ";
        if (nonzeroBloodValue) mystring += getValue("bloodtest") + " blood ";
        if (nonzeroCarbsValue) mystring += getValue("carbs") + " carbs ";
        if (nonzeroInsulinValue) mystring += getValue("insulin") + " units ";

        if (mystring.length() > 1) {
            //SendData(this, WEARABLE_VOICE_PAYLOAD, mystring.getBytes(StandardCharsets.UTF_8));
            resetValues();
            //WatchUpdaterService.receivedText(this, mystring); // reuse watch handling function to send data to home
            startHomeWithExtra(this, WatchUpdaterService.WEARABLE_VOICE_PAYLOAD, mystring); // send data to home directly
            finish();
        }
    }

    private void updateTab() {

        final int offColor = Color.DKGRAY;
        final int onColor = Color.RED;

        insulintabbutton.setBackgroundColor(offColor);
        carbstabbutton.setBackgroundColor(offColor);
        timetabbutton.setBackgroundColor(offColor);
        bloodtesttabbutton.setBackgroundColor(offColor);


        String append = "";
        switch (currenttab) {
            case "insulin":
                insulintabbutton.setBackgroundColor(onColor);
                append = " " + getString(R.string.units);
                break;
            case "carbs":
                carbstabbutton.setBackgroundColor(onColor);
                append = " " + getString(R.string.carbs);
                break;
            case "bloodtest":
                bloodtesttabbutton.setBackgroundColor(onColor);
                append = " " + bgUnits;
                break;
            case "time":
                timetabbutton.setBackgroundColor(onColor);
                append = " " + getString(R.string.when);
                break;
        }
        String value = getValue(currenttab);
        mDialTextView.setText(value + append);
        // show green tick
        boolean showSubmitButton;

        if (isInvalidTime())
            showSubmitButton = false;

        else if (currenttab.equals("time"))
            showSubmitButton = value.length() > 0 &&
                    ( isNonzeroValueInTab("bloodtest") || isNonzeroValueInTab("carbs") || isNonzeroValueInTab("insulin"));
        else
            showSubmitButton = isNonzeroValueInTab(currenttab);
        mDialTextView.getBackground().setAlpha(showSubmitButton ? 255 : 0);    }


    @Override
    protected void onResume() {
        final String savedtab = PersistentStore.getString(LAST_TAB_STORE);
        if (savedtab.length() > 0) currenttab = savedtab;
        updateTab();
        super.onResume();
    }

    @Override
    protected void onPause() {
        PersistentStore.setString(LAST_TAB_STORE, currenttab);
        super.onPause();
    }
}
