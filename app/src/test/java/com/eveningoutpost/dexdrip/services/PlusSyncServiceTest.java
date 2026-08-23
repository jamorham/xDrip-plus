package com.eveningoutpost.dexdrip.services;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link PlusSyncService}.
 * <p>
 * The sync kill switch is a user-facing safety control: when it is set, sync must not start and all
 * GCM activity must be ceased. Both of those are public static state, which is what makes the
 * preference read observable at all.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class PlusSyncServiceTest extends RobolectricTestWithConfig {

    private static final String DISABLE_KEY = "disable_all_sync";

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        PlusSyncService.created = false;        // static guard; nothing else resets it between tests
        GcmActivity.cease_all_activity = false; // static flag under assertion; likewise
    }

    // ===== The sync kill switch ==================================================================

    /** With sync disabled, starting the service ceases all GCM activity instead. */
    @Test
    public void disabledSyncCeasesAllActivity() {
        // :: Setup
        storeDisableAllSync(true);

        // :: Act
        PlusSyncService.startSyncService(xdrip.getAppContext(), "PlusSyncServiceTest");

        // :: Verify
        assertThat(GcmActivity.cease_all_activity).isTrue();
    }

    /**
     * With sync enabled, the kill switch stays off. Negative half of the pair with
     * disabledSyncCeasesAllActivity — the two together pin that the flag follows the preference
     * rather than being raised or cleared unconditionally.
     */
    @Test
    public void enabledSyncLeavesActivityRunning() {
        // :: Setup
        storeDisableAllSync(false);

        // :: Act
        PlusSyncService.startSyncService(xdrip.getAppContext(), "PlusSyncServiceTest");

        // :: Verify
        assertThat(GcmActivity.cease_all_activity).isFalse();
    }

    /**
     * The created guard short-circuits before the preference is ever read, so a disabled setting has
     * no effect on an already-started service. This pins the ordering of the two guards.
     */
    @Test
    public void alreadyCreatedServiceIgnoresTheSetting() {
        // :: Setup
        PlusSyncService.created = true;
        storeDisableAllSync(true);

        // :: Act
        PlusSyncService.startSyncService(xdrip.getAppContext(), "PlusSyncServiceTest");

        // :: Verify
        assertThat(GcmActivity.cease_all_activity).isFalse();
    }

    // ===== Helpers ===============================================================================

    private void storeDisableAllSync(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean(DISABLE_KEY, value).commit();
    }
}
