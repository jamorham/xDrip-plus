package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.*;

import com.eveningoutpost.dexdrip.*;

public class XDripDreamSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final com.eveningoutpost.dexdrip.databinding.ActivityXdripDreamSettingsBinding binding = com.eveningoutpost.dexdrip.databinding.ActivityXdripDreamSettingsBinding.inflate(getLayoutInflater());
        binding.setPrefs(new PrefsViewImpl());
        setContentView(binding.getRoot());

    }

}
