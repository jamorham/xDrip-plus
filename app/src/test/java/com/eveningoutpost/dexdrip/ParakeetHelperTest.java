package com.eveningoutpost.dexdrip;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link ParakeetHelper}.
 * <p>
 * Two preference-driven behaviours are pinned here: the receiver-address list is parsed into a
 * single usable Parakeet URL, and the first-run notification is armed once and then marked as done.
 * The second is a two-call handshake, which is the only way to observe the flag without touching
 * the helper's private statics.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class ParakeetHelperTest extends RobolectricTestWithConfig {

    private static final String ADDRESSES_KEY = "wifi_recievers_addresses";
    private static final String FIRST_RUN_KEY = "parakeet_first_run_done";
    private static final String PARAKEET_URL = "http://parakeet.example.com/json.get";

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        disarm();
    }

    // ===== Receiver address parsing ==============================================================

    /** A single well-formed Parakeet address is returned as-is. */
    @Test
    public void singleParakeetAddressIsReturned() {
        // :: Setup
        storeAddresses(PARAKEET_URL);

        // :: Act
        String url = ParakeetHelper.getParakeetURL(xdrip.getAppContext());

        // :: Verify
        assertThat(url).isEqualTo(PARAKEET_URL);
    }

    /** From a comma-separated list, the first address that looks like a Parakeet endpoint wins. */
    @Test
    public void firstParakeetAddressInListWins() {
        // :: Setup
        storeAddresses("http://plain.example.com/, " + PARAKEET_URL + " ,http://second.example.com/json.get");

        // :: Act
        String url = ParakeetHelper.getParakeetURL(xdrip.getAppContext());

        // :: Verify
        assertThat(url).isEqualTo(PARAKEET_URL);
    }

    /** An address list with nothing Parakeet-shaped in it yields no URL. */
    @Test
    public void nonParakeetAddressesYieldNoUrl() {
        // :: Setup
        storeAddresses("http://plain.example.com/,https://other.example.com/");

        // :: Act
        String url = ParakeetHelper.getParakeetURL(xdrip.getAppContext());

        // :: Verify
        assertThat(url).isNull();
    }

    /** With no addresses configured at all there is no URL. */
    @Test
    public void unsetAddressesYieldNoUrl() {
        // :: Act
        String url = ParakeetHelper.getParakeetURL(xdrip.getAppContext());

        // :: Verify
        assertThat(url).isNull();
    }

    /** The setup URL is the Parakeet URL with its json.get path swapped for the setcode endpoint. */
    @Test
    public void setupUrlRewritesTheJsonPath() {
        // :: Setup
        storeAddresses(PARAKEET_URL);

        // :: Act
        String url = ParakeetHelper.getParakeetSetupURL(xdrip.getAppContext());

        // :: Verify
        assertThat(url).isEqualTo("http://parakeet.example.com/setcode/2");
    }

    // ===== First-run notification handshake ======================================================

    /**
     * Arming the notification while first-run is still pending, then reporting a later check-in,
     * marks first-run as done. Both calls are needed: the arm reads the flag, the check-in writes it.
     */
    @Test
    public void checkinAfterArmingMarksFirstRunDone() {
        // :: Setup
        storeFirstRunDone(false);
        ParakeetHelper.notifyOnNextCheckin(false);

        // :: Act
        ParakeetHelper.checkParakeetNotifications(JoH.tsl() + 60000, "1,1");

        // :: Verify
        assertThat(storedFirstRunDone()).isTrue();
    }

    /**
     * With first-run already done, a plain arming request is refused — that is the whole point of
     * the flag. Clearing the flag afterwards and firing a check-in must therefore leave it clear,
     * because nothing was ever armed. This is the assertion that pins the read at line 145.
     */
    @Test
    public void armingIsRefusedOnceFirstRunIsDone() {
        // :: Setup
        storeFirstRunDone(true);
        ParakeetHelper.notifyOnNextCheckin(false); // reads the flag, must not arm
        storeFirstRunDone(false);

        // :: Act
        ParakeetHelper.checkParakeetNotifications(JoH.tsl() + 60000, "1,1");

        // :: Verify
        assertThat(storedFirstRunDone()).isFalse();
    }

    /**
     * always=true arms regardless of the flag, which is what separates an explicit request from the
     * first-run path. Same setup as the test above, opposite outcome — the pair is what makes each
     * of them mean something.
     */
    @Test
    public void armingAlwaysIgnoresTheFirstRunFlag() {
        // :: Setup
        storeFirstRunDone(true);
        ParakeetHelper.notifyOnNextCheckin(true); // arms unconditionally
        storeFirstRunDone(false);

        // :: Act
        ParakeetHelper.checkParakeetNotifications(JoH.tsl() + 60000, "1,1");

        // :: Verify
        assertThat(storedFirstRunDone()).isTrue();
    }

    /**
     * A check-in that arrives before the arming timestamp does not complete the handshake, so the
     * flag stays where it was. Negative half of the pair with checkinAfterArmingMarksFirstRunDone:
     * alone it would pass with the write deleted, together they pin that the write is conditional
     * on the timestamp.
     */
    @Test
    public void checkinBeforeArmingLeavesFirstRunPending() {
        // :: Setup
        storeFirstRunDone(false);
        ParakeetHelper.notifyOnNextCheckin(false);

        // :: Act
        ParakeetHelper.checkParakeetNotifications(JoH.tsl() - 600000, "1,1");

        // :: Verify
        assertThat(storedFirstRunDone()).isFalse();
    }

    // ===== Helpers ===============================================================================

    /**
     * Leaves waiting_for_parakeet false. The helper keeps it in a private static that nothing
     * resets between tests, and a completed handshake is the only public way to clear it.
     */
    private void disarm() {
        ParakeetHelper.notifyOnNextCheckin(true);
        ParakeetHelper.checkParakeetNotifications(JoH.tsl() + 60000, "1,1");
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
    }

    private void storeAddresses(String value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString(ADDRESSES_KEY, value).commit();
    }

    private void storeFirstRunDone(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean(FIRST_RUN_KEY, value).commit();
    }

    private boolean storedFirstRunDone() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getBoolean(FIRST_RUN_KEY, false);
    }
}
