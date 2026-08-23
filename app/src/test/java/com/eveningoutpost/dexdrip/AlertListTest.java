package com.eveningoutpost.dexdrip;

import android.widget.ListView;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.AlertType;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link AlertList}.
 * <p>
 * The activity reads the units preference once in onCreate and uses it to format every alert
 * threshold it lists. Asserting the rendered text covers the read and the conversion together.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class AlertListTest extends RobolectricTestWithConfig {

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        clearPreferences();
        AlertType.remove_all();
        AlertType.add_alert(null, "spike low", false, 100, true, 1, null,
                0, 0, true, true, 20, true, true);
    }

    // ===== Units preference drives the rendered threshold ========================================

    /**
     * A 100 mg/dL alert renders as "100" under mgdl and as its mmol equivalent under mmol. Both
     * halves are asserted together: a single-unit assertion would still pass if the preference read
     * were deleted, because mgdl is also the default.
     */
    @Test
    public void thresholdTextFollowsUnitsPreference() {
        // :: Setup
        storeUnits("mgdl");

        // :: Act
        String mgdlText = firstLowThreshold();

        // :: Verify
        assertThat(mgdlText).contains("100");

        // :: Setup
        storeUnits("mmol");

        // :: Act
        String mmolText = firstLowThreshold();

        // :: Verify
        assertThat(mmolText).doesNotContain("100");
        assertThat(mmolText).contains("5"); // 100 mg/dL is 5.6 mmol/L
    }

    // ===== Helpers ===============================================================================

    @SuppressWarnings("unchecked")
    private String firstLowThreshold() {
        AlertList activity = Robolectric.buildActivity(AlertList.class).create().get();
        ListView list = activity.findViewById(R.id.listView_low);
        assertThat(list).isNotNull();
        assertThat(list.getAdapter().getCount()).isAtLeast(1);
        HashMap<String, String> row = (HashMap<String, String>) list.getAdapter().getItem(0);
        return row.get("alertThreshold");
    }

    private void storeUnits(String units) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("units", units).commit();
    }

    private void clearPreferences() {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().clear().commit();
    }
}
