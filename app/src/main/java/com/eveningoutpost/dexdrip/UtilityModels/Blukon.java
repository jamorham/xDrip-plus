package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by gregorybel / jamorham on 02/09/2017.
 */

public class Blukon {

    private static final String TAG = "Blukon";
    private static final String BLUKON_PIN_PREF = "Blukon-bluetooth-pin";

    private static int m_nowGlucoseOffset = 0;

    private static int m_currentBlock = 0;
    private static long m_timeDiffToLastReading = 0;

    private static String currentCommand = "";
    //TO be used later
    private static enum BLUKON_STATES {
        INITIAL
    }
    private static boolean m_getNowGlucoseDataIndexCommand = false;
    private static boolean m_gotOneTimeUnknownCmd = false;

    private static boolean m_getOlderReading = false;

    private static int GET_SENSOR_AGE_DELAY =  3 * 3600;
    private static final String BLUKON_GETSENSORAGE_TIMER = "blukon-getSensorAge-timer";
    private static boolean m_getNowGlucoseDataCommand = false;// to be sure we wait for a GlucoseData Block and not using another block
    private static long m_timeLastBg = 0;
    private static int m_currentBlockNumber = 0;

    public static String getPin() {
        final String thepin = Home.getPreferencesStringWithDefault(BLUKON_PIN_PREF, null);
        if ((thepin != null) && (thepin.length() < 3))
            return null; // TODO enforce sane minimum pin length
        return thepin;
    }

    private static void setPin(String thepin) {
        if (thepin == null) return;
        Home.setPreferencesString(BLUKON_PIN_PREF, thepin);
    }

    public static void clearPin() {
        Home.removePreferencesItem(BLUKON_PIN_PREF);
    }

    public static void initialize() {
        Home.setPreferencesInt("bridge_battery", 100); //force battery to 100% before first reading
        Home.setPreferencesInt("nfc_sensor_age", 0); //force sensor age to no-value before first reading
        m_gotOneTimeUnknownCmd = false;
        JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);
        m_getNowGlucoseDataCommand = false;
        m_getNowGlucoseDataIndexCommand = false;
        m_getOlderReading = false;
 //       m_timeLastBg = 0;
    }

    public static boolean isBlukonPacket(byte[] buffer) {
    /* -53  0xCB -117 0x8B */
        return !((buffer == null) || (buffer.length < 3)) && (buffer[0] == (byte) 0xCB || buffer[0] == (byte) 0x8B);
    }

    public static boolean checkBlukonPacket(byte[] buffer) {
        return isBlukonPacket(buffer) && getPin() != null; // TODO can't be unset yet and isn't proper subtype test yet
    }


    // .*(dexdrip|gatt|Blukon).
    public static byte[] decodeBlukonPacket(byte[] buffer) {
        int cmdFound = 0;

        if (buffer == null) {
            UserError.Log.e(TAG, "null buffer passed to decodeBlukonPacket");
            return null;
        }

        //BluCon code by gregorybel
        final String strRecCmd = CipherUtils.bytesToHex(buffer).toLowerCase();
        UserError.Log.i(TAG, "BlueCon data: " + strRecCmd);

        if (strRecCmd.equalsIgnoreCase("cb010000")) {
            UserError.Log.i(TAG, "Reset currentCommand");
            currentCommand = "";
            cmdFound = 1;
        }

        // BluconACKRespons will come in two different situations
        // 1) after we have sent an ackwakeup command
        // 2) after we have a sleep command
        if (strRecCmd.startsWith("8b0a00")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "Got ACK");

            if (currentCommand.startsWith("810a00")) {//ACK sent
                //ack received

                //This command will be asked only one time after first connect and never again
                //Try asking each time
                if (m_gotOneTimeUnknownCmd == false) {
                    currentCommand = "010d0b00";
                    UserError.Log.i(TAG, "getUnknownCmd1: " + currentCommand);
                } else {
                    if (JoH.pratelimit(BLUKON_GETSENSORAGE_TIMER, GET_SENSOR_AGE_DELAY)) {
                        currentCommand = "010d0e0127";
                        UserError.Log.i(TAG, "getSensorAge");
                    } else {
                        currentCommand = "010d0e0103";
                        m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
                        UserError.Log.i(TAG, "getNowGlucoseDataIndexCommand");
                    }
                }

            } else {
                UserError.Log.i(TAG, "Got sleep ack, resetting initialstate!");
                currentCommand = "";
            }
        }

        if (strRecCmd.startsWith("8b1a02")) {
            cmdFound = 1;
            UserError.Log.e(TAG, "Got NACK on cmd=" + currentCommand + " with error=" + strRecCmd.substring(6));

            if (strRecCmd.startsWith("8b1a020014")) {
                UserError.Log.e(TAG, "Timeout: please wait 5min or push button to restart!");
            }

            if (strRecCmd.startsWith("8b1a02000f")) {
                UserError.Log.e(TAG, "Libre sensor has been removed!");
            }

            if (strRecCmd.startsWith("8b1a020011")) {
                UserError.Log.e(TAG, "Patch read error.. please check the connectivity and re-initiate...");
            }

            m_gotOneTimeUnknownCmd = false;
            currentCommand = "";
            JoH.clearRatelimit(BLUKON_GETSENSORAGE_TIMER);// set to current time to force timer to be set back
        }

        if (currentCommand.equals("") && strRecCmd.equalsIgnoreCase("cb010000")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "wakeup received");

            currentCommand = "010d0900";
            UserError.Log.i(TAG, "getPatchInfo");

        } else if (currentCommand.startsWith("010d0900") /*getPatchInfo*/ && strRecCmd.startsWith("8bd9")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "Patch Info received");
            currentCommand = "810a00";
            UserError.Log.i(TAG, "Send ACK");

        } else if (currentCommand.startsWith("010d0b00") /*getUnknownCmd1*/ && strRecCmd.startsWith("8bdb")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "gotUnknownCmd1 (010d0b00): "+strRecCmd);

            if (!strRecCmd.equals("8bdb0101041711")) {
                UserError.Log.e(TAG, "gotUnknownCmd1 (010d0b00): "+strRecCmd);
            }

            currentCommand = "010d0a00";
            UserError.Log.i(TAG, "getUnknownCmd2 "+ currentCommand);

        } else if (currentCommand.startsWith("010d0a00") /*getUnknownCmd2*/ && strRecCmd.startsWith("8bda")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "gotUnknownCmd2 (010d0a00): "+strRecCmd);

            if (!strRecCmd.equals("8bdaaa")) {
                UserError.Log.e(TAG, "gotUnknownCmd2 (010d0a00): "+strRecCmd);
            }

            if (strRecCmd.equals("8bda02")) {
                UserError.Log.e(TAG, "gotUnknownCmd2: is maybe battery low????");
            }

            //saw one time:  battery status????

            //try asking each time m_gotOneTimeUnknownCmd = true;

            if (JoH.pratelimit(BLUKON_GETSENSORAGE_TIMER, GET_SENSOR_AGE_DELAY)) {
                currentCommand = "010d0e0127";
                UserError.Log.i(TAG, "getSensorAge");
            } else {
                currentCommand = "010d0e0103";
                m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
                UserError.Log.i(TAG, "getNowGlucoseDataIndexCommand");
            }

        } else if (currentCommand.startsWith("010d0e0127") /*getSensorAge*/ && strRecCmd.startsWith("8bde")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "SensorAge received");

            int sensorAge = sensorAge(buffer);

            if ((sensorAge > 0) && (sensorAge < 200000)) {
                Home.setPreferencesInt("nfc_sensor_age", sensorAge);//in min
            }
            currentCommand = "010d0e0103";
            m_getNowGlucoseDataIndexCommand = true;//to avoid issue when gotNowDataIndex cmd could be same as getNowGlucoseData (case block=3)
            UserError.Log.i(TAG, "getNowGlucoseDataIndexCommand");

        } else if (currentCommand.startsWith("010d0e0103") /*getNowDataIndex*/ && m_getNowGlucoseDataIndexCommand == true && strRecCmd.startsWith("8bde")) {
            cmdFound = 1;
            UserError.Log.i(TAG, "gotNowDataIndex");

            m_timeDiffToLastReading = JoH.tsl() - m_timeLastBg;
            UserError.Log.i(TAG, "+++++ time difference is: " + m_timeDiffToLastReading);

            long timeToGoBack = m_timeDiffToLastReading/1000;
            UserError.Log.i(TAG, "+++++ time difference is: " + timeToGoBack);
            timeToGoBack = timeToGoBack - 300;
            UserError.Log.i(TAG, "+++++ time difference is: " + timeToGoBack);
            timeToGoBack = timeToGoBack + 20;
            UserError.Log.i(TAG, "+++++ time difference is: " + timeToGoBack);
            timeToGoBack = timeToGoBack/60;
            UserError.Log.i(TAG, "+++++ time difference is: " + timeToGoBack);

            int blockNumber = blockNumberForNowGlucoseData(buffer);
            m_currentBlock = blockNumber;

            UserError.Log.i(TAG, "+++++ go back in time: " + timeToGoBack + " min, current block Number is " + m_currentBlock);

            if ( (m_timeDiffToLastReading > 530000) && (m_timeDiffToLastReading < (300000*16)) ) {
                m_getOlderReading = true;
                UserError.Log.i(TAG, "+++ getOlderReading = true");

                int delayedBlockNumber = blockNumberForNowGlucoseDataDelayed(blockNumber, 5);
                currentCommand = "010d0e010" + Integer.toHexString(delayedBlockNumber);//getNowGlucoseData
                m_getNowGlucoseDataIndexCommand = false;
                m_getNowGlucoseDataCommand = true;
                UserError.Log.i(TAG, "++++ getNowGlucoseDataDelayed for " + m_timeDiffToLastReading + " ms, block number " + delayedBlockNumber);
            }
            else {
                m_getOlderReading = false;
                UserError.Log.i(TAG, "++++ getOlderReading = false");

                currentCommand = "010d0e010" + Integer.toHexString(blockNumber);//getNowGlucoseData
                m_getNowGlucoseDataIndexCommand = false;
                m_getNowGlucoseDataCommand = true;
                UserError.Log.i(TAG, "++++ getNowGlucoseData");
            }

        } else if (currentCommand.startsWith("010d0e01") /*getNowGlucoseData*/ && m_getNowGlucoseDataCommand == true && strRecCmd.startsWith("8bde")) {
            cmdFound = 1;

            int currentGlucose = nowGetGlucoseValue(buffer);

            UserError.Log.i(TAG, "~~~~~~~~~~~~~~~got getNowGlucoseData=" + currentGlucose);

            m_timeLastBg = JoH.tsl();
            if ( !m_getOlderReading ) {
                processNewTransmitterData(TransmitterData.create(currentGlucose, currentGlucose, 0 /*battery level force to 0 as unknown*/, JoH.tsl()));
                currentCommand = "010c0e00";
                UserError.Log.i(TAG, "++++ Send sleep cmd");
                m_getNowGlucoseDataCommand = false;
            }
            else {
                processNewTransmitterData(TransmitterData.create(currentGlucose, currentGlucose, 0 /*battery level force to 0 as unknown*/, JoH.tsl()-300000));
                currentCommand = "010d0e010" + Integer.toHexString(m_currentBlock);
                UserError.Log.i(TAG, "++++ get next block: " + currentCommand);
                m_getOlderReading = false;
                UserError.Log.i(TAG, "+++ getOlderReading = false");
            }

        }  else if (strRecCmd.startsWith("cb020000")) {
            cmdFound = 1;
            UserError.Log.e(TAG, "is bridge battery low????!");
            Home.setPreferencesInt("bridge_battery", 50);

        } else if (strRecCmd.startsWith("cbdb0000")) {
            cmdFound = 1;
            UserError.Log.e(TAG, "is bridge battery really low????!");
            Home.setPreferencesInt("bridge_battery", 25);

        }



        if (currentCommand.length() > 0 && cmdFound == 1) {
            UserError.Log.d(TAG, "Sending reply: " + currentCommand);
            return CipherUtils.hexToBytes(currentCommand);
        } else {
            if (cmdFound == 0) {
                UserError.Log.e(TAG, "***COMMAND NOT FOUND! -> " + strRecCmd + " on currentCmd=" + currentCommand);
            }

            currentCommand = "";

            return null;
        }

    }


    /*


in getPatchInfo: blucon answer is 20 bytes long.
Bytes 13 - 19 (0 indexing) contains the bytes 0 ... 6 of block #0
Bytes 6 - 12 (0 indexing) seems to be constant on different sensors
Bytes 2 - 5 (0 indexing) are different on different sensors
Bytes 0 - 1 (0 indexing) is the ordinary block request answer (0x8B 0xD9).
Remark: Byte #17 (0 indexing) contains the SensorStatusByte.

     switch (sensorData.sensorStatusByte)
  {
    case 0x01:
      sensorData.sensorStatusString = "not yet started";
      break;
    case 0x02:
      sensorData.sensorStatusString = "starting";
      break;
    case 0x03:
      sensorData.sensorStatusString = "ready";
      break;
    case 0x04:
      sensorData.sensorStatusString = "expired";
      break;
    case 0x05:
      sensorData.sensorStatusString = "shutdown";
      break;
    case 0x06:
      sensorData.sensorStatusString = "failure";
      break;
    default:
      sensorData.sensorStatusString = "unknown state";
      break;
  }

     */

    private static synchronized void processNewTransmitterData(TransmitterData transmitterData) {
        if (transmitterData == null) {
            UserError.Log.e(TAG, "Got duplicated data! Last BG at " + JoH.dateTimeText(m_timeLastBg));
            return;
        }

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            UserError.Log.i(TAG, "processNewTransmitterData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        DexCollectionService.last_transmitter_Data = transmitterData;
        UserError.Log.d(TAG, "BgReading.create: new BG reading at " + transmitterData.timestamp + " with a timestamp of " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
    }

    /* @keencave
     * extract trend index from FRAM block #3 from the libre sensor
     * input: blucon answer to trend index request, including 6 starting protocol bytes
     * return: 2 byte containing the next absolute block index to be read from
     * the libre sensor
     */

    private static int blockNumberForNowGlucoseData(byte[] input) {
        int nowGlucoseIndex2 = 0;
        int nowGlucoseIndex3 = 0;

        nowGlucoseIndex2 = (int) (input[5] & 0x0F);

        // calculate byte position in sensor body
        nowGlucoseIndex2 = (nowGlucoseIndex2 * 6) + 4;

        // decrement index to get the index where the last valid BG reading is stored
        nowGlucoseIndex2 -= 6;
        // adjust round robin
        if (nowGlucoseIndex2 < 4)
            nowGlucoseIndex2 = nowGlucoseIndex2 + 96;

        // calculate the absolute block number which correspond to trend index
        nowGlucoseIndex3 = 3 + (nowGlucoseIndex2 / 8);

        // calculate offset of the 2 bytes in the block
        m_nowGlucoseOffset = nowGlucoseIndex2 % 8;

        UserError.Log.i(TAG, "trend block " + nowGlucoseIndex3 +" with Offset " + m_nowGlucoseOffset);

        return (nowGlucoseIndex3);
    }

    private static int blockNumberForNowGlucoseDataDelayed(int currentBlock, int delayMinutes)
    {
        int i;
        int ngi2;
        int ngi3;

        // calculate byte offset in libre FRAM
        ngi2 = (currentBlock * 6) + 4;

        // count back the byte offset
        for ( i = 0 ; i < delayMinutes+1 ; i++ ) {
            ngi2 -= 6;
            if ( ngi2 < 4 )
                ngi2 = ngi2 + 96;
        }

        // calculate the block number where to get the BG reading
        ngi3 = 3 + (ngi2/8);

        // calculate the offset in the block
        m_nowGlucoseOffset = ngi2 % 8;
        UserError.Log.i(TAG, "delayed block " + ngi3 + " with Offset " + m_nowGlucoseOffset + ", delay of " + delayMinutes + " mins");

        return(ngi3);
    }


    /* @keencave
     * rescale raw BG reading to BG data format used in xDrip+
     * use 8.5 devider
     * raw format is in 1000 range
     */
    private static int getGlucose(long rawGlucose) {
        // standard divider for raw Libre data (1000 range)
        return (int) (rawGlucose * Constants.LIBRE_MULTIPLIER);
    }

    /* @keencave
     * extract BG reading from the raw data block containing the most recent BG reading
     * input: bytearray with blucon answer including 3 header protocol bytes
     * uses nowGlucoseOffset to calculate the offset of the two bytes needed
     * return: BG reading as int
     */

    private static int nowGetGlucoseValue(byte[] input) {
        final int curGluc;
        final long rawGlucose;

        // grep 2 bytes with BG data from input bytearray, mask out 12 LSB bits and rescale for xDrip+
        rawGlucose = ((input[3 + m_nowGlucoseOffset + 1] & 0x0F) << 8) | (input[3 + m_nowGlucoseOffset] & 0xFF);
        UserError.Log.i(TAG, "rawGlucose=" + rawGlucose);

        // rescale
        curGluc = getGlucose(rawGlucose);

        return curGluc;
    }


    private static int sensorAge(byte[] input) {
        int sensorAge = ((input[3 + 5] & 0xFF) << 8) | (input[3 + 4] & 0xFF);
        UserError.Log.i(TAG, "sensorAge=" + sensorAge);

        return sensorAge;
    }

    public static void doPinDialog(final Activity activity, final Runnable runnable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Please enter " + activity.getString(R.string.blukon) + " device PIN number");
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        builder.setView(input);
        builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setPin(input.getText().toString().trim());
                if (getPin() != null) {
                    JoH.static_toast_long("Data source set to: " + activity.getString(R.string.blukon) + " pin: " + getPin());
                    runnable.run();
                } else {
                    JoH.static_toast_long("Invalid pin!");
                }
            }
        });
        builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } catch (NullPointerException e) {
            //
        }
        dialog.show();
    }
}
