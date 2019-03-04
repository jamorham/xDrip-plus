package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.annotation.*;

import com.eveningoutpost.dexdrip.adapters.*;

/**
 * Created by jamorham on 04/07/2018.
 *
 * Observable map with transparent persistence
 */

public class PrefsViewString extends ObservableArrayMapNoNotify<String, String> {

    public String getString(String name) {
        return Pref.getString(name, "");
    }

    public void setString(String name, String value) {
        Pref.setString(name, value);
        super.put(name, value);
    }

    @Override
    public String get(Object key) {
        String value = super.get(key);
        if (value == null) {
            value = getString((String) key);
            super.putNoNotify((String) key, value);
        }
        return value;
    }

    @Override
    public String put(@NonNull String key, @NonNull String value) {
        if (!(super.get(key).equals(value))) {
            setString(key, value);
        }
        return value;
    }


}
