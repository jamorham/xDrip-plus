package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

public class IdempotentMigrationsTest extends RobolectricTestWithConfig {

    private static final String oldPref = "calibrate_external_libre_2_algorithm";
    private static final String newPref = "calibrate_external_libre_2_algorithm_type";

    @Before
    public void before() {
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem(oldPref);
        Pref.removeItem(newPref);
    }

    @Test
    public void migrateOOP2CalibrationPreferencesTest() {

        assertWithMessage("null old 1").that(Pref.isPreferenceSet(oldPref)).isFalse();
        assertWithMessage("null new 1").that(Pref.isPreferenceSet(newPref)).isFalse();
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("null old 2").that(Pref.isPreferenceSet(oldPref)).isFalse();
        assertWithMessage("null new 2").that(Pref.isPreferenceSet(newPref)).isFalse();

        Pref.setBoolean(oldPref, true);
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("set old 3").that(Pref.isPreferenceSet(oldPref)).isTrue();
        assertWithMessage("set new 3").that(Pref.isPreferenceSet(newPref)).isTrue();
        assertWithMessage("as expected 1").that(Pref.getString(newPref, "error")).isEqualTo("calibrate_raw");

        Pref.setBoolean(oldPref, false);
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("set old 4").that(Pref.isPreferenceSet(oldPref)).isTrue();
        assertWithMessage("set new 4").that(Pref.isPreferenceSet(newPref)).isTrue();
        assertWithMessage("as expected no change 2").that(Pref.getString(newPref, "error")).isEqualTo("calibrate_raw");

        cleanup();
        Pref.setBoolean(oldPref, false);
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("set old 5").that(Pref.isPreferenceSet(oldPref)).isTrue();
        assertWithMessage("set new 5").that(Pref.isPreferenceSet(newPref)).isTrue();
        assertWithMessage("as expected 3").that(Pref.getString(newPref, "error")).isEqualTo("no_calibration");

    }

    // ===== Legacy REST URI migration =============================================================

    /** A credential-prefixed legacy URL is folded into the URI authority and gains a trailing slash. */
    @Test
    public void legacyCredentialPrefixedUrlIsRewritten() {
        // :: Setup
        storeBaseUrl("user:pass@http://ns.example.com/api/v1");

        // :: Act
        new IdempotentMigrations(xdrip.getAppContext()).performAll();

        // :: Verify
        assertThat(storedBaseUrl()).isEqualTo("http://user:pass@ns.example.com/api/v1/");
    }

    /** An already-modern URL is left alone apart from the trailing slash the migration guarantees. */
    @Test
    public void modernUrlIsLeftAlone() {
        // :: Setup
        storeBaseUrl("http://user:pass@ns.example.com/api/v1/");

        // :: Act
        new IdempotentMigrations(xdrip.getAppContext()).performAll();

        // :: Verify
        assertThat(storedBaseUrl()).isEqualTo("http://user:pass@ns.example.com/api/v1/");
    }

    // ===== Helpers ===============================================================================

    private void storeBaseUrl(String value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("cloud_storage_api_base", value).commit();
    }

    private String storedBaseUrl() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getString("cloud_storage_api_base", "");
    }
}
