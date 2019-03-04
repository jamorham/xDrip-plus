package com.eveningoutpost.dexdrip;

import android.graphics.*;
import android.os.*;
import android.support.wearable.view.*;
import android.widget.*;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.models.*;

import java.text.*;
import java.util.*;

/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 */

// jamorham xdrip plus

public class KeypadInputActivity extends AppCompatActivity {

    private final static String TAG = "jamorham " + KeypadInputActivity.class.getSimpleName();
    private TextView mDialTextView;
    private Button zeroButton, oneButton, twoButton, threeButton, fourButton, fiveButton,
            sixButton, sevenButton, eightButton, nineButton, starButton, backSpaceButton;
    private ImageButton callImageButton, backspaceImageButton, insulintabbutton, carbstabbutton,
            bloodtesttabbutton, timetabbutton;
    //private GoogleApiClient mApiClient;
    private static String currenttab = "insulin";
    private static Map<String, String> values = new HashMap<>();
    private static final String WEARABLE_VOICE_PAYLOAD = "/xdrip_plus_voice_payload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(stub1 -> {
            mDialTextView = (TextView) stub1.findViewById(R.id.dialed_no_textview);
            zeroButton = (Button) stub1.findViewById(R.id.zero_button);
            oneButton = (Button) stub1.findViewById(R.id.one_button);
            twoButton = (Button) stub1.findViewById(R.id.two_button);
            threeButton = (Button) stub1.findViewById(R.id.three_button);
            fourButton = (Button) stub1.findViewById(R.id.four_button);
            fiveButton = (Button) stub1.findViewById(R.id.five_button);
            sixButton = (Button) stub1.findViewById(R.id.six_button);
            sevenButton = (Button) stub1.findViewById(R.id.seven_button);
            eightButton = (Button) stub1.findViewById(R.id.eight_button);
            nineButton = (Button) stub1.findViewById(R.id.nine_button);
            starButton = (Button) stub1.findViewById(R.id.star_button);
            backSpaceButton = (Button) stub1.findViewById(R.id.backspace_button);
            // callImageButton = (ImageButton) stub.findViewById(R.id.call_image_button);
            // backspaceImageButton = (ImageButton) stub.findViewById(R.id.backspace_image_button);

            insulintabbutton = (ImageButton) stub1.findViewById(R.id.insulintabbutton);
            bloodtesttabbutton = (ImageButton) stub1.findViewById(R.id.bloodtesttabbutton);
            timetabbutton = (ImageButton) stub1.findViewById(R.id.timetabbutton);
            carbstabbutton = (ImageButton) stub1.findViewById(R.id.carbstabbutton);


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


            updateTab();

         /*   callImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mDialTextView.getText().toString().isEmpty()) {
                        sendMessage(mDialTextView.getText().toString(), null);
                        Toast.makeText(getApplicationContext(), "Calling " + mDialTextView.getText(), Toast.LENGTH_SHORT).show();
                        mDialTextView.setText(null);
                    }
                }
            });*/
        });
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

    private String getTime() {
        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("HH.mm", Locale.US);
        final String datenew = simpleDateFormat1.format(c.getTime());
        return datenew;
    }

    private void submitAll() {

        String mystring = "";
        mystring += (JoH.tsl()/1000)+" watchkeypad ";
        mystring += (getValue("time").length() > 0) ? getValue("time") + " time " : getTime() + " time ";
        mystring += (getValue("bloodtest").length() > 0) ? getValue("bloodtest") + " blood " : "";
        mystring += (getValue("carbs").length() > 0) ? (!getValue("carbs").equals("0") ? getValue("carbs") + " carbs " : "") : "";
        mystring += (getValue("insulin").length() > 0) ? (!getValue("insulin").equals("0") ? getValue("insulin") + " units " : "") : "";

        if (mystring.length() > 1) {
            ListenerService.sendTreatment(mystring);
            //SendData(this, WEARABLE_VOICE_PAYLOAD, mystring.getBytes(StandardCharsets.UTF_8));
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
        String value = "";
        switch (currenttab) {
            case "insulin":
                insulintabbutton.setBackgroundColor(onColor);
                append = " units";
                break;
            case "carbs":
                carbstabbutton.setBackgroundColor(onColor);
                append = " carbs";
                break;
            case "bloodtest":
                bloodtesttabbutton.setBackgroundColor(onColor);
                append = " BG";  // TODO get mgdl or mmol here
                break;
            case "time":
                timetabbutton.setBackgroundColor(onColor);
                append = " time";
                break;
        }
        value = getValue(currenttab);
        mDialTextView.setText(value + append);
        // show green tick
        if (value.length() > 0) {
            mDialTextView.getBackground().setAlpha(255);
        } else {
            mDialTextView.getBackground().setAlpha(0);
        }
    }

   /* private void sendMessage(final String message, final byte[] payload) {
        Log.i(KeypadInputActivity.class.getSimpleName(), message);
        Wearable.NodeApi.getConnectedNodes(mApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                List<Node> nodes = getConnectedNodesResult.getNodes();
                for (Node node : nodes) {
                    Log.i(KeypadInputActivity.class.getSimpleName(), "WEAR sending " + message + " to " + node);
                    Wearable.MessageApi.sendMessage(mApiClient, node.getId(), message, payload).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(KeypadInputActivity.class.getSimpleName(), "WEAR Result " + sendMessageResult.getStatus());
                        }
                    });
                }
            }
        });
    }*/


    @Override
    protected void onResume() {
        super.onResume();
      /*  mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(KeypadInputActivity.class.getSimpleName(), "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
        /*Wearable.MessageApi.removeListener(mApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {

            }
        });
        mApiClient.disconnect();*/
    }
}
