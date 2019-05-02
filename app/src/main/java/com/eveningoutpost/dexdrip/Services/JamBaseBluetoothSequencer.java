package com.eveningoutpost.dexdrip.Services;

import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.RxBleProvider;
import com.eveningoutpost.dexdrip.utils.BtCallBack;
import com.eveningoutpost.dexdrip.utils.DisconnectReceiver;
import com.eveningoutpost.dexdrip.utils.time.SlidingWindowConstraint;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSED;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CONNECT_NOW;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.DISCOVER;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.SEND_QUEUE;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.SLEEP;

// jamorham

public abstract class JamBaseBluetoothSequencer extends JamBaseBluetoothService implements BtCallBack {

    private static final HashMap<UUID, String> mapToName = new HashMap<>();
    protected final RxBleClient rxBleClient = RxBleProvider.getSingleton(this.getClass().getCanonicalName());
    private volatile String myid;
    protected volatile Inst I;

    protected BaseState mState;

    protected void setMyid(final String id) {
        UserError.Log.d(TAG, "Setting myid to: " + id);
        myid = id;
        I = Inst.get(id);
    }

    {
        setMyid(TAG);
    }


    // Instance management
    public static class Inst {

        private static final ConcurrentHashMap<String, Inst> singletons = new ConcurrentHashMap<>();


        private final ConcurrentLinkedQueue<QueueItem> write_queue = new ConcurrentLinkedQueue<>();

        public final ConcurrentHashMap<UUID, Object> characteristics = new ConcurrentHashMap<>();

        public volatile Subscription scanSubscription;
        public volatile Subscription connectionSubscription;
        public volatile Subscription stateSubscription;
        public volatile Subscription discoverSubscription;
        public volatile RxBleDevice bleDevice;
        public volatile RxBleConnection connection;

        public volatile String address;
        public volatile boolean isConnected;
        public volatile boolean isNotificationEnabled;
        public volatile boolean isDiscoveryComplete;
        public volatile UUID readCharacteristic;
        public volatile UUID writeCharacteristic;
        public volatile String state;
        public volatile UUID queue_write_characterstic;
        public volatile long lastProcessedIncomingData = -1;
        public volatile int backgroundStepDelay = 100;
        public volatile int connectTimeoutMinutes = 7;

        public volatile boolean playSounds = false;
        public volatile boolean autoConnect = false;
        public volatile boolean autoReConnect = false;
        public volatile boolean retry133 = true;
        public volatile boolean discoverOnce = false;
        public volatile boolean resetWhenAlreadyConnected = false;

        public PendingIntent serviceIntent;
        public SlidingWindowConstraint reconnectConstraint;
        private PendingIntent serviceFailoverIntent;
        public long retry_time;
        public long retry_backoff;
        public long wakeup_time;
        long failover_time;
        long last_wake_up_time;
        long lastConnected;


        private Inst() {
        }

        {
            state = INIT;
        }

        public static Inst get(final String id) {
            if (id == null) return null;
            final Inst conn = singletons.get(id);
            if (conn != null) return conn;
            synchronized (JamBaseBluetoothSequencer.class) {
                final Inst double_check = singletons.get(id);
                if (double_check != null) return double_check;
                final Inst singleton = new Inst();
                singletons.put(id, singleton);
                return singleton;
            }
        }

        public int getQueueSize() {
            return write_queue.size();
        }

    }

    // address handling

    protected void setAddress(String newAddress) {
        DisconnectReceiver.addCallBack(this, TAG);
        if (emptyString(newAddress)) return;
        newAddress = newAddress.toUpperCase();

        if (!JoH.validateMacAddress(newAddress)) {
            final String msg = "Invalid MAC address: " + newAddress;
            if (JoH.quietratelimit("jam-invalid-mac", 60)) {
                UserError.Log.wtf(TAG, msg);
                JoH.static_toast_long(msg);
            }
            return;
        }

        if (I.address == null || !I.address.equals(newAddress)) {
            final String oldAddress = I.address;
            I.address = newAddress;
            newAddressEvent(oldAddress, newAddress);
        }
    }

    protected void newAddressEvent(final String oldAddress, final String newAddress) {

        // out with the old
        if (oldAddress != null) {
            stopConnect(oldAddress);
            stopWatching(oldAddress);
        }

        // in with the new
        I.bleDevice = rxBleClient.getBleDevice(newAddress);
        watchConnection(newAddress);
    }


    // connection handling


    protected synchronized void startConnect(final String address) {
        if (emptyString(address)) {
            UserError.Log.e(TAG, "Cannot connect as address is null");
            return;
        }
        UserError.Log.d(TAG, "Trying connect: " + address);
        // Attempt to establish a connection
        I.connectionSubscription = I.bleDevice.establishConnection(I.autoConnect)
                .timeout(I.connectTimeoutMinutes, TimeUnit.MINUTES)
                // .flatMap(RxBleConnection::discoverServices)
                // .observeOn(AndroidSchedulers.mainThread())
                // .doOnUnsubscribe(this::clearSubscription)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onConnectionReceived, this::onConnectionFailure);

    }

    protected synchronized void stopConnect(final String address) {
        UserError.Log.d(TAG, "Stopping connection with: " + address);
        if (I.connectionSubscription != null) {
            I.connectionSubscription.unsubscribe();
        }
        I.isConnected = false;
    }

    protected synchronized void watchConnection(final String address) {
        UserError.Log.d(TAG, "Starting to watch connection with: " + address);
        /// / Listen for connection state changes
        I.stateSubscription = I.bleDevice.observeConnectionStateChanges()
                // .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onConnectionStateChange, throwable -> {
                    UserError.Log.wtf(TAG, "Got Error from state subscription: " + throwable);
                });
    }

    protected synchronized void stopWatching(final String address) {
        UserError.Log.d(TAG, "Stopping watching: " + address);
        if (I.stateSubscription != null) {
            I.stateSubscription.unsubscribe();
        }
    }


    // We have connected to the device!
    private void onConnectionReceived(RxBleConnection this_connection) {
        //msg("Connected");
        // TODO check connection already exists - close etc?
        I.connection = this_connection;
        I.lastConnected = JoH.tsl();
        UserError.Log.d(TAG, "Initial connection going for service discovery");
        changeState(DISCOVER);
        if ((I.playSounds && (JoH.ratelimit("sequencer_connect_sound", 3)))) {
            JoH.playResourceAudio(R.raw.bt_meter_connect);
        }
    }

    private void onConnectionFailure(Throwable throwable) {
        UserError.Log.d(TAG, "received: onConnectionFailure: " + throwable);
        if (throwable instanceof BleAlreadyConnectedException) {
            UserError.Log.d(TAG, "Already connected - advancing to next stage");
            I.isConnected = true;
            changeState(mState.next());
        } else if (throwable instanceof BleDisconnectedException) {
            if (((BleDisconnectedException) throwable).state == 133) {
                if (I.retry133) {
                    if (JoH.ratelimit(TAG + "133recon", 60)) {
                        if (I.state.equals(CONNECT_NOW)) {
                            UserError.Log.d(TAG, "Automatically retrying connection");
                            Inevitable.task(TAG + "133recon", 3000, new Runnable() {
                                @Override
                                public void run() {
                                    changeState(CONNECT_NOW);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @Override
    public void btCallback(String address, String status) {
        UserError.Log.d(TAG, "Processing callback: " + address + " :: " + status);
        if (I.address == null) return;
        if (address.equals(I.address)) {
            switch (status) {
                case "DISCONNECTED":
                    I.isConnected = false;
                    break;
                case "SCAN_FOUND":
                    break;
                case "SCAN_TIMEOUT":
                    break;
                case "SCAN_FAILED":
                    break;

                default:
                    UserError.Log.e(TAG, "Unknown status callback for: " + address + " with " + status);
            }
        } else {
            UserError.Log.d(TAG, "Ignoring: " + status + " for " + address + " as we are using: " + I.address);
        }
    }


    protected synchronized void onConnectionStateChange(final RxBleConnection.RxBleConnectionState newState) {
        String connection_state = "Unknown";
        switch (newState) {
            case CONNECTING:
                connection_state = "Connecting";
                //  connecting_time = JoH.tsl();
                break;
            case CONNECTED:
                I.isConnected = true;
                I.retry_backoff = 0; // reset counter
                connection_state = "Connected";

                break;
            case DISCONNECTING:
                I.isConnected = false;
                connection_state = "Disconnecting";

                break;
            case DISCONNECTED:
                I.isConnected = false;
                connection_state = "Disconnected";
                changeState(CLOSE);
                break;
        }

        UserError.Log.d(TAG, "Connection state changed to: " + connection_state);
    }


    // service discovery

    public synchronized void discover_services() {
        //  if (state == DISCOVER) {
        if (I.discoverOnce && I.isDiscoveryComplete) {
            UserError.Log.d(TAG, "Skipping service discovery as already completed");
            changeNextState();
        } else {
            if (I.connection != null) {
                UserError.Log.d(TAG, "Discovering services");
                stopDiscover();
                I.discoverSubscription = I.connection.discoverServices(10, TimeUnit.SECONDS).subscribe(this::onServicesDiscovered, this::onDiscoverFailed);
            } else {
                UserError.Log.e(TAG, "No connection when in DISCOVER state - reset");
                // These are normally just ghosts that get here, not really connected
                if (I.resetWhenAlreadyConnected) {
                    if (JoH.ratelimit("jam-sequencer-reset", 10)) {
                        changeState(CLOSE);
                    }
                }

            }
            //} else {
            //     UserError.Log.wtf(TAG, "Attempt to discover when not in DISCOVER state");
            //  }
        }
    }

    protected synchronized void stopDiscover() {
        if (I.discoverSubscription != null) {
            I.discoverSubscription.unsubscribe();
        }
    }


    protected void onServicesDiscovered(RxBleDeviceServices services) {
        UserError.Log.d(TAG, "Services discovered okay in base sequencer");
        final Object obj = new Object();
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                I.characteristics.put(characteristic.getUuid(), obj);
            }
        }
    }

    protected void onDiscoverFailed(Throwable throwable) {
        UserError.Log.e(TAG, "Discover failure: " + throwable.toString());
        changeState(CLOSE);
        // incrementErrors();
    }

// end service discovery


    // state automata's

    public static class BaseState {
        protected final List<String> sequence = new ArrayList<>();

        public static final String INIT = "Initializing";
        public static final String CONNECT_NOW = "Connecting";
        public static final String SEND_QUEUE = "Sending Queue";
        public static final String DISCOVER = "Discover Services";
        public static final String SLEEP = "Sleeping";
        public static final String CLOSE = "Closing";
        public static final String CLOSED = "Closed";

        private Inst LI;

        {
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(SEND_QUEUE);
            //sequence.add(DISCOVER); // handled by initial connection callback
            sequence.add(SLEEP);
        }


        public BaseState setLI(final Inst LI) {
            this.LI = LI;
            return this;
        }


        public String next() {
            try {
                if (LI.state.equals(SLEEP))
                    return SLEEP; // will not auto-advance out of sleep state
                return sequence.get(sequence.indexOf(LI.state) + 1);
            } catch (Exception e) {
                return SLEEP;
            }
        }

    }


    public synchronized void changeState(final String new_state) {
        final String state = I.state;
        if (state == null) return;
        if ((state.equals(new_state)) && !state.equals(INIT) && !state.equals(SLEEP)) {
            if (!state.equals(CLOSE)) {
                UserError.Log.d(TAG, "Already in state: " + new_state.toUpperCase() + " changing to CLOSE");
                UserError.Log.d(TAG, JoH.backTrace());
                changeState(CLOSE);
            }
        } else {
            if ((state.equals(CLOSED) || state.equals(CLOSE)) && new_state.equals(CLOSE)) {
                UserError.Log.d(TAG, "Not closing as already closed");
            } else {
                UserError.Log.d(TAG, "Changing state from: " + state.toUpperCase() + " to " + new_state.toUpperCase());
                I.state = new_state;
                background_automata(I.backgroundStepDelay);
            }
        }
    }

    public void changeNextState() {
        changeState(mState.next());
    }


    protected synchronized boolean alwaysConnected() {
        if (I.isConnected || I.state.equals(CONNECT_NOW)) {
            UserError.Log.d(TAG, "Always connected passes");
            return true;
        }
        if (JoH.ratelimit(TAG + "auto-reconnect", 1)) {
            UserError.Log.d(TAG, "alwaysConnected() requesting connect");
            changeState(CONNECT_NOW);
        } else {
            UserError.Log.d(TAG, "Too frequent reconnect calls");
            setRetryTimerReal();
        }
        return false;
    }


    protected void setRetryTimerReal() {
        throw new RuntimeException("Must define setRetryTimerReal() if you are going to use it");
    }

    @Override
    protected synchronized boolean automata() {

        UserError.Log.d(TAG, "automata state: " + I.state);
        extendWakeLock(3000);
        try {
            switch (I.state) {
                case INIT:
                    UserError.Log.d(TAG, "INIT State does nothing unless overridden");
                    break;
                case CONNECT_NOW:
                    if (!I.isConnected) {
                        startConnect(I.address);
                    } else {
                        changeState(mState.next());
                    }
                    break;
                case DISCOVER:
                    discover_services();
                    break;
                case SEND_QUEUE:
                    startQueueSend();
                    break;

                case CLOSE:
                    stopConnect(I.address);
                    changeState(CLOSED);
                    break;

                case CLOSED:
                    if (I.autoReConnect) {
                        // TODO use sliding window constraint
                        if (I.reconnectConstraint != null) {
                            if (I.reconnectConstraint.checkAndAddIfAcceptable(1)) {
                                UserError.Log.d(TAG, "Attempting auto-reconnect");
                                changeState(CONNECT_NOW);
                            } else {
                                UserError.Log.d(TAG, "Not attempting auto-reconnect due to constraint");
                            }
                        } else {
                            UserError.Log.e(TAG, "No reconnectConstraint is null");
                        }

                    }

                default:
                    return false;

            }
            return true;
        } finally {
            //
        }
    }

    // Queue


    /// Queue Handling

    @RequiredArgsConstructor
    private class QueueItem {
        final byte[] data;
        final int timeoutSeconds;
        final long post_delay;
        public final String description;
        final boolean expectReply;
        final long expireAt;
        int retries = 0;
        Runnable runnable;

        boolean isExpired() {
            return expireAt != 0 && expireAt < JoH.tsl();
        }

        QueueItem setRunnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }
    }

    private static final int MAX_QUEUE_RETRIES = 3;


    public class QueueMe {

        List<byte[]> byteslist;
        long delay_ms = 100;
        int timeout_seconds = 10;
        long expireAt;
        boolean start_now;
        boolean expect_reply;
        String description = "Vanilla Queue Item";
        Runnable runnable;


        public QueueMe setByteList(List<byte[]> byteList) {
            this.byteslist = byteList;
            return this;
        }

        public QueueMe setBytes(final byte[] bytes) {
            final List<byte[]> byteList = new LinkedList<>();
            byteList.add(bytes);
            this.byteslist = byteList;
            return this;
        }

        public QueueMe setTimeout(final int timeout) {
            this.timeout_seconds = timeout;
            return this;
        }

        public QueueMe setDelayMs(final int delay) {
            this.delay_ms = delay;
            return this;
        }

        public QueueMe expireInSeconds(final int timeout) {
            this.expireAt = JoH.tsl() + (Constants.SECOND_IN_MS * timeout);
            return this;
        }

        public QueueMe setDescription(String description) {
            this.description = description;
            return this;
        }

        public QueueMe setRunnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public QueueMe now() {
            this.start_now = true;
            return this;
        }

        public QueueMe expectReply() {
            this.start_now = true;
            return this;
        }

        public void queue() {
            this.start_now = false; // make sure disabled
            add();
        }

        public void send() {
            this.start_now = true; // make sure enabled
            add();
        }

        private void add() {
            addToWriteQueue(byteslist, delay_ms, timeout_seconds, start_now, description, expect_reply, expireAt, runnable);
        }

    }


    private void addToWriteQueue(final List<byte[]> byteslist, long delay_ms, int timeout_seconds, boolean start_now, String description, boolean expect_reply, long expireAt, Runnable runnable) {
        for (final byte[] bytes : byteslist) {
            I.write_queue.add(new QueueItem(bytes, timeout_seconds, delay_ms, description, expect_reply, expireAt)
                    .setRunnable(runnable));
        }
        if (start_now) startQueueSend();
    }

    public void emptyQueue() {
        I.write_queue.clear();
    }


    public void startQueueSend() {
        Inevitable.task("sequence-start-queue " + I.address, 100, new Runnable() {
            @Override
            public void run() {
                writeMultipleFromQueue(I.write_queue);
            }
        });
    }

    private synchronized void writeMultipleFromQueue(final ConcurrentLinkedQueue<QueueItem> queue) {
        if (I.isConnected) {
            final QueueItem item = queue.poll();
            if (item != null) {
                if (!item.isExpired()) {
                    UserError.Log.d(TAG, "Starting queue send for item: " + item.description + " " + JoH.backTrace());
                    writeQueueItem(queue, item);
                } else {
                    UserError.Log.d(TAG, "Item expired from queue: (expiry: " + JoH.dateTimeText(item.expireAt) + " " + item.description);
                    writeMultipleFromQueue(queue);
                }
            } else {
                UserError.Log.d(TAG, "write queue empty");
                changeState(mState.next()); // check if this logic is sound
            }
        } else {
            UserError.Log.d(TAG, "CANNOT WRITE QUEUE AS DISCONNECTED");
        }
    }


    private void writeQueueItem(final ConcurrentLinkedQueue<QueueItem> queue, final QueueItem item) {
        extendWakeLock(2000 + item.post_delay);
        if (I.connection == null) {
            UserError.Log.e(TAG, "Cannot write queue item: " + item.description + " as we have no connection!");
            return;
        }

        if (I.queue_write_characterstic == null) {
            UserError.Log.e(TAG, "Write characteristic not set in queue write");
            return;
        }
        UserError.Log.d(TAG, "Writing to characteristic: " + I.queue_write_characterstic + " " + item.description);
        I.connection.writeCharacteristic(I.queue_write_characterstic, item.data)
                .timeout(item.timeoutSeconds, TimeUnit.SECONDS)
                .subscribe(Value -> {
                    UserError.Log.d(TAG, "Wrote request: " + item.description + " -> " + JoH.bytesToHex(Value));
                    if (item.expectReply) expectReply(queue, item);
                    if (item.post_delay > 0) {
                        // always sleep if set as new item might appear in queue
                        final long sleep_time = item.post_delay + (item.description.contains("WAKE UP") ? 2000 : 0);
                        UserError.Log.d(TAG, "sleeping " + sleep_time);
                        JoH.threadSleep(sleep_time);
                    }
                    if (item.runnable != null) {
                        item.runnable.run(); // TODO should this be handled by expect reply in that case?
                    }
                    if (!item.expectReply) {
                        writeMultipleFromQueue(queue); // start next item immediately
                    }
                    throw new OperationSuccess("write complete: " + item.description);
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        UserError.Log.d(TAG, "Throwable in: " + item.description + " -> " + throwable);
                        item.retries++;
                        if (!(throwable instanceof BleDisconnectedException)) {
                            if (item.retries > MAX_QUEUE_RETRIES) {
                                UserError.Log.d(TAG, item.description + " failed max retries @ " + item.retries + " shutting down queue");
                                queue.clear();
                                changeState(CLOSE);
                            } else {
                                writeQueueItem(queue, item);
                            }
                        } else {
                            UserError.Log.d(TAG, "Disconnected so not attempting retries");
                            I.isConnected = false;
                        }
                    } else {
                        // not disconnecting on success
                    }
                });
    }

    private void expectReply(final ConcurrentLinkedQueue<QueueItem> queue, final QueueItem item) {
        final long wait_time = 3000;
        Inevitable.task("expect-reply-" + I.address + "-" + item.description, wait_time, new Runnable() {
            @Override
            public void run() {
                if (JoH.msSince(I.lastProcessedIncomingData) > wait_time) {
                    UserError.Log.d(TAG, "GOT NO REPLY FOR: " + item.description + " @ " + item.retries);
                    item.retries++;
                    if (item.retries <= MAX_QUEUE_RETRIES) {
                        UserError.Log.d(TAG, "Retrying due to no reply: " + item.description);
                        writeQueueItem(queue, item);
                    }
                }
            }
        });
    }

    // Life Cycle

    // Turn off anything we may have turned on
    protected void shutDown() {
        stopConnect(I.address);
        stopWatching(I.address);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        shutDown();
        DisconnectReceiver.removeCallBack(TAG);
        super.onDestroy();
    }


    // utils

    public static String getUUIDName(final UUID uuid) {
        if (uuid == null) return "null";
        final String result = mapToName.get(uuid);
        return result != null ? result : "Unknown uuid: " + uuid.toString();
    }


}
