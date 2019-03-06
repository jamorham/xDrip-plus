package com.eveningoutpost.dexdrip;

import android.annotation.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.widget.*;

import com.activeandroid.query.*;
import com.eveningoutpost.dexdrip.importedLibraries.dexcom.*;
import com.eveningoutpost.dexdrip.importedLibraries.dexcom.records.*;
import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.utilitymodels.*;

import java.nio.charset.*;
import java.util.*;

import rx.Observable;
import rx.functions.*;


public class ShareTest extends BaseActivity {
    private final static String TAG = ShareTest.class.getSimpleName();
    Button button;
    Button closeButton;
    Button readButton;
    Button bondButton;
    TextView details;

    private String mDeviceName;
    private String mDeviceAddress;
    private boolean is_connected = false;
    private boolean reconnecting = false;
    SharedPreferences prefs;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private ForegroundServiceStarter foregroundServiceStarter;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;
    int mStartMode;
    private Context mContext = null;

    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    private BluetoothGattService mShareService;
    private BluetoothGattCharacteristic mAuthenticationCharacteristic;
    private BluetoothGattCharacteristic mSendDataCharacteristic;
    private BluetoothGattCharacteristic mReceiveDataCharacteristic;
    private BluetoothGattCharacteristic mHeartBeatCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;
    private BluetoothGattCharacteristic mResponseCharacteristic;

    //Gatt Tasks
    public final int GATT_NOTHING = 0;
    public final int GATT_SETUP = 1;
    public final int GATT_WRITING_COMMANDS = 2;
    public final int GATT_READING_RESPONSE = 3;
    public int successfulWrites;

    //RXJAVA FUN
    Action1<byte[]> mDataResponseListener;

    public ReadDataShare mReadDataShare;

    public int currentGattTask;
    public int step;

    public List<byte[]> writePackets;
    public int recordType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_test);
        button = (Button) findViewById(R.id.connect);
        closeButton = (Button) findViewById(R.id.closeConnect);
        bondButton = (Button) findViewById(R.id.bond);
        readButton = (Button) findViewById(R.id.read);
        details = (TextView) findViewById(R.id.connection_details);
        addListenerOnButton();
        addListenerOnCloseButton();
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        UserError.Log.i(TAG, "CLOSING CONNECTION");
    }

    public void addListenerOnButton() {
        button.setOnClickListener(v -> attemptConnection());
        readButton.setOnClickListener(v -> attemptRead());
        bondButton.setOnClickListener(v -> bond(mBluetoothGatt));
    }

    public void addListenerOnCloseButton() {
        closeButton.setOnClickListener(v -> {
            close();
            details.setText("");
        });
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("NewApi")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                mConnectionState = STATE_CONNECTED;
                ActiveBluetoothDevice.connected();
                UserError.Log.i(TAG, "Connected to GATT server.");
                UserError.Log.i(TAG, "Connection state: Bonded - " + device.getBondState());

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    currentGattTask = GATT_SETUP;
                    mBluetoothGatt.discoverServices();

                } else {
                    device.setPin("000000".getBytes());
                    device.createBond();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                ActiveBluetoothDevice.disconnected();
                UserError.Log.w(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                UserError.Log.i(TAG, "Services Discovered: " + status);
                authenticateConnection(gatt);

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                UserError.Log.i(TAG, "Characteristic Read");
                byte[] value = characteristic.getValue();
                if(value != null) {
                    UserError.Log.i(TAG, "VALUE" + Arrays.toString(value));
                } else {
                    UserError.Log.w(TAG, "Characteristic was null");
                }
                nextGattStep();
            } else {
                UserError.Log.w(TAG, "Characteristic failed to read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UserError.Log.i(TAG, "Characteristic changed");
            UUID charUuid = characteristic.getUuid();
            UserError.Log.i(TAG, "Characteristic Update Received: " + charUuid);
            if(charUuid.compareTo(mResponseCharacteristic.getUuid()) == 0) {
                UserError.Log.i(TAG, "mResponseCharacteristic Update");
            }
            if(charUuid.compareTo(mCommandCharacteristic.getUuid()) == 0) {
                UserError.Log.i(TAG, "mCommandCharacteristic Update");
            }
            if(charUuid.compareTo(mHeartBeatCharacteristic.getUuid()) == 0) {
                UserError.Log.i(TAG, "mHeartBeatCharacteristic Update");
            }
            if(charUuid.compareTo(mReceiveDataCharacteristic.getUuid()) == 0) {
                UserError.Log.i(TAG, "mReceiveDataCharacteristic Update");
                byte[] value = characteristic.getValue();
                if(value != null) {
                    UserError.Log.i(TAG, "Characteristic: " + Arrays.toString(value));
                    UserError.Log.i(TAG, "Characteristic: " + Arrays.toString(value));
                    UserError.Log.i(TAG, "Characteristic getstring: " + characteristic.getStringValue(0));
                    UserError.Log.i(TAG, "SUBSCRIBED TO RESPONSE LISTENER");
                    Observable.just(characteristic.getValue()).subscribe(mDataResponseListener);
                } else {
                    UserError.Log.w(TAG, "Characteristic was null");
                }
            }
            UserError.Log.i(TAG, "NEW VALUE: " + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UserError.Log.i(TAG, "Wrote a discriptor, status: " + status);
            if(step == 2 && currentGattTask == GATT_SETUP) {
                setListeners(2);
            } else if(step == 3) {
                setListeners(3);
            } else if(step == 4) {
                setListeners(4);
            } else if(step == 5) {
                UserError.Log.i(TAG, "Done setting Listeners");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UserError.Log.i(TAG, "Wrote a characteristic: " + status);
            nextGattStep();
        }
    };

    public void attemptConnection() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (device != null) {
            details.append("\nConnection state: " + " Device is not null");
            mConnectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        }

        UserError.Log.i(TAG, "Connection state: " + mConnectionState);
        details.append("\nConnection state: " + mConnectionState);
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
            ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                    .orderBy("_ID desc")
                    .executeSingle();
            if (btDevice != null) {
                details.append("\nBT Device: " + btDevice.name);
                mDeviceName = btDevice.name;
                mDeviceAddress = btDevice.address;
                mBluetoothAdapter = mBluetoothManager.getAdapter();
                boolean newConnection = true;
                if(newConnection) {
                    is_connected = connect(mDeviceAddress);
                    details.append("\nConnecting...: ");
                }
            }
        }
    }

    public void attemptRead() {
        final ReadDataShare readData = new ReadDataShare(this);
        final Action1<Long> systemTimeListener = s -> {

            UserError.Log.i(TAG, "Made the full round trip, got " + s + " as the system time");
            UserError.Log.i("SYSTTIME", "Made the full round trip, got " + s + " as the system time");
            final long addativeSystemTimeOffset = new Date().getTime() - s;
            UserError.Log.i(TAG, "Made the full round trip, got " + addativeSystemTimeOffset + " offset");
            UserError.Log.i("SYSTTIME", "Made the full round trip, got " + addativeSystemTimeOffset + " offset");

            final Action1<CalRecord[]> calRecordListener = calRecords -> {
	            UserError.Log.i(TAG, "Made the full round trip, got " + calRecords.length + " Cal Records");
	            Calibration.create(calRecords, addativeSystemTimeOffset, getApplicationContext());

	            final Action1<SensorRecord[]> sensorRecordListener = sensorRecords -> {
		            UserError.Log.i(TAG, "Made the full round trip, got " + sensorRecords.length + " Sensor Records");
		            BgReading.create(sensorRecords, addativeSystemTimeOffset, getApplicationContext());

		            final Action1<EGVRecord[]> evgRecordListener = egvRecords -> {
			            UserError.Log.i(TAG, "Made the full round trip, got " + egvRecords.length + " EVG Records");
			            BgReading.create(egvRecords, addativeSystemTimeOffset, getApplicationContext());
		            };
		            readData.getRecentEGVs(evgRecordListener);
	            };
	            readData.getRecentSensorRecords(sensorRecordListener);
            };
            readData.getRecentCalRecords(calRecordListener);
        };
        readData.readSystemTime(systemTimeListener);
    }

    public void bond(BluetoothGatt gatt) {
        reconnecting = true;
        attemptConnection();
    }

    @SuppressLint("NewApi")
    public boolean connect(final String address) {

        details.append("\nConnecting to device");
        UserError.Log.i(TAG, "CONNECTING TO DEVICE");
        if (mBluetoothAdapter == null || address == null) {
            details.append("\nBT adapter is null");
            UserError.Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            details.append("\nTrying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        } else {
            device = mBluetoothAdapter.getRemoteDevice(address);
            device.setPin("000000".getBytes());
            if (device == null) {
                UserError.Log.w(TAG, "Device not found.  Unable to connect.");
                details.append("\nDevice not found.  Unable to connect.");
                return false;
            }
            mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
            UserError.Log.i(TAG, "Trying to create a new connection.");
            details.append("\nTrying to create a new connection to device");
            mConnectionState = STATE_CONNECTING;
            return true;
        }
    }

    @SuppressLint("NewApi")
    public void authenticateConnection(BluetoothGatt bluetoothGatt) {
        UserError.Log.i(TAG, "Trying to auth");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
        if(bluetoothGatt != null) {
            mBluetoothGatt = bluetoothGatt;
            mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService);
            if (mShareService != null) {
                mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode);
                if(mAuthenticationCharacteristic != null) {
                    UserError.Log.i(TAG, "Auth Characteristic found: " + mAuthenticationCharacteristic.toString());
                    mAuthenticationCharacteristic.setValue((receiverSn + "000000").getBytes(StandardCharsets.US_ASCII));
                    currentGattTask = GATT_SETUP;
                    step = 1;
                    bluetoothGatt.writeCharacteristic(mAuthenticationCharacteristic);
                } else {
                    UserError.Log.w(TAG, "Authentication Characteristic IS NULL");
                }
            } else {
                UserError.Log.w(TAG, "CRADLE SERVICE IS NULL");
            }
        }
    }

    public void assignCharacteristics() {
        mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver);
        mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse);
        mHeartBeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat);
        mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command);
        mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response);
    }

    public void setListeners(int listener_number) {

        UserError.Log.i(TAG, "Setting Listener: #" + listener_number);
        if(listener_number == 1) {
            step = 3;
            setCharacteristicIndication(mReceiveDataCharacteristic);
        } else if(listener_number == 3) {
            setCharacteristicIndication(mResponseCharacteristic);
            step = 5;
        }
     }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            UserError.Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
        UserError.Log.w(TAG, "bt Disconnected");
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            UserError.Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic){ setCharacteristicNotification(characteristic, true);}
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        UserError.Log.i(TAG, "Characteristic setting notification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        UserError.Log.i(TAG, "UUID FOUND: " + characteristic.getUuid());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
        UserError.Log.i(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic){ setCharacteristicIndication(characteristic, true);}
    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        UserError.Log.i(TAG, "Characteristic setting notification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        UserError.Log.i(TAG, "UUID FOUND: " + characteristic.getUuid());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
        UserError.Log.i(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    UserError.Log.i(TAG, "CALLBACK RECIEVED Bonded");
                    currentGattTask = GATT_SETUP;
                    mBluetoothGatt.discoverServices();
                } else if (state == BluetoothDevice.BOND_NONE){
                    UserError.Log.i(TAG, "CALLBACK RECIEVED: Not Bonded");
                    Toast.makeText(getApplicationContext(), "unBonded", Toast.LENGTH_LONG).show();
                } else if (state == BluetoothDevice.BOND_BONDING) {
                    UserError.Log.i(TAG, "CALLBACK RECIEVED: Trying to bond");
                    Toast.makeText(getApplicationContext(), "trying to bond", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public void writeCommand(List<byte[]> packets, int aRecordType, Action1<byte[]> dataResponseListener) {
        mDataResponseListener = dataResponseListener;
        successfulWrites = 0;
        writePackets = packets;
        recordType = aRecordType;
        step = 0;
        currentGattTask = GATT_WRITING_COMMANDS;
        gattWritingStep();
    }

    private void nextGattStep() {
        UserError.Log.i(TAG, "Next Gatt Step");
        step++;
        switch (currentGattTask) {
        case GATT_NOTHING:
            UserError.Log.i(TAG, "Next NOTHING: " + step);
            break;
        case GATT_SETUP:
            UserError.Log.i(TAG, "Next GATT SETUP: " + step);
            gattSetupStep();
            break;
        case GATT_WRITING_COMMANDS:
            UserError.Log.i(TAG, "Next GATT WRITING: " + step);
            gattWritingStep();
            break;
        }
    }

    public void clearGattTask() {
        currentGattTask = GATT_NOTHING;
        step = 0;
    }

    private void gattSetupStep() {
        step = 1;
        assignCharacteristics();
        setListeners(1);
    }

    private void gattWritingStep() {
        UserError.Log.i(TAG, "Writing command to the Gatt, step: " + step);
        int index = step;
        if (index <= (writePackets.size() - 1)) {
            UserError.Log.i(TAG, "Writing: " + Arrays.toString(writePackets.get(index)) + " index: " + index);
            mSendDataCharacteristic.setValue(writePackets.get(index));
            mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic);
        } else {
            clearGattTask();
        }
    }
}
