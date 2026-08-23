package com.eveningoutpost.dexdrip;

import android.content.Intent;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.tables.BgReadingTable;
import com.eveningoutpost.dexdrip.tables.CalibrationDataTable;
import com.eveningoutpost.dexdrip.utils.Preferences;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link NavDrawerBuilder}.
 * <p>
 * The drawer's contents are a pure function of the preference store: the licence flag collapses it
 * to a single entry, and two feature flags each add their own destinations. Asserting the target
 * activity of every intent pins each branch without knowing anything about the builder's internals.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class NavDrawerBuilderTest extends RobolectricTestWithConfig {

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
    }

    // ===== The licence flag gates the whole drawer ===============================================

    /**
     * Until the agreement is accepted the drawer offers settings and nothing else — the builder
     * returns early. This is the strongest single assertion on the preference read: any other
     * outcome means the flag was not seen.
     */
    @Test
    public void unacceptedLicenceLeavesOnlySettings() {
        // :: Setup
        storeBoolean("I_understand", false);

        // :: Act
        List<String> targets = targetClasses(new NavDrawerBuilder(xdrip.getAppContext()));

        // :: Verify
        assertThat(targets).containsExactly(Preferences.class.getName());
    }

    /** Once accepted, the drawer opens with the home screen and still ends with settings. */
    @Test
    public void acceptedLicenceOpensTheFullDrawer() {
        // :: Setup
        storeBoolean("I_understand", true);

        // :: Act
        List<String> targets = targetClasses(new NavDrawerBuilder(xdrip.getAppContext()));

        // :: Verify
        assertThat(targets.get(0)).isEqualTo(Home.class.getName());
        assertThat(targets.get(targets.size() - 1)).isEqualTo(Preferences.class.getName());
        assertThat(targets.size()).isGreaterThan(1);
    }

    // ===== Feature flags add their own entries ===================================================

    /** The data-tables flag adds both table screens, and removing it takes both away again. */
    @Test
    public void dataTablesFlagAddsTheTableScreens() {
        // :: Setup
        storeBoolean("I_understand", true);
        storeBoolean("show_data_tables", true);

        // :: Act
        List<String> withTables = targetClasses(new NavDrawerBuilder(xdrip.getAppContext()));

        // :: Verify
        assertThat(withTables).contains(BgReadingTable.class.getName());
        assertThat(withTables).contains(CalibrationDataTable.class.getName());

        // :: Setup
        storeBoolean("show_data_tables", false);

        // :: Act
        List<String> withoutTables = targetClasses(new NavDrawerBuilder(xdrip.getAppContext()));

        // :: Verify
        assertThat(withoutTables).doesNotContain(BgReadingTable.class.getName());
        assertThat(withoutTables).doesNotContain(CalibrationDataTable.class.getName());
    }

    /** The alerts-in-menu flag adds the alert list, and removing it takes it away again. */
    @Test
    public void alertsFromMainMenuFlagAddsTheAlertList() {
        // :: Setup
        storeBoolean("I_understand", true);
        storeBoolean("bg_alerts_from_main_menu", true);

        // :: Act
        List<String> withAlerts = targetClasses(new NavDrawerBuilder(xdrip.getAppContext()));

        // :: Verify
        assertThat(withAlerts).contains(AlertList.class.getName());

        // :: Setup
        storeBoolean("bg_alerts_from_main_menu", false);

        // :: Act
        List<String> withoutAlerts = targetClasses(new NavDrawerBuilder(xdrip.getAppContext()));

        // :: Verify
        assertThat(withoutAlerts).doesNotContain(AlertList.class.getName());
    }

    // ===== Structural invariant, not a preference branch =========================================

    /**
     * Labels and intents are indexed together by the drawer, so they must be equally long. This one
     * does not exercise the preference read at all; it guards the two public lists against a future
     * edit that adds an entry to one and forgets the other.
     */
    @Test
    public void labelsAndIntentsAreParallel() {
        // :: Setup
        storeBoolean("I_understand", true);

        // :: Act
        NavDrawerBuilder builder = new NavDrawerBuilder(xdrip.getAppContext());

        // :: Verify
        assertThat(builder.nav_drawer_options).hasSize(builder.nav_drawer_intents.size());
    }

    // ===== Helpers ===============================================================================

    private static List<String> targetClasses(NavDrawerBuilder builder) {
        List<String> classes = new ArrayList<>();
        for (Intent intent : builder.nav_drawer_intents) {
            classes.add(intent.getComponent().getClassName());
        }
        return classes;
    }

    private void storeBoolean(String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean(key, value).commit();
    }
}
