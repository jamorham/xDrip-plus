package com.eveningoutpost.dexdrip;

// jamorham

import android.content.*;
import androidx.appcompat.app.*;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(final Context baseContext) {
        final Context context = xdrip.getLangContext(baseContext);
        super.attachBaseContext(context);
    }

}
