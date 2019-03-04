package com.eveningoutpost.dexdrip.dagger;

import androidx.appcompat.app.*;

import com.eveningoutpost.dexdrip.*;
import com.eveningoutpost.dexdrip.ui.*;
import com.eveningoutpost.dexdrip.webservices.*;

import dagger.*;

/**
 * Created by jamorham on 20/09/2017.
 *
 * Interface requires method for every concrete class it is called from
 *
 */

@javax.inject.Singleton
@Component(modules = MicroStatusModule.class)
public interface MicroStatusComponent {

    void inject(SystemStatusFragment target);
    void inject(AppCompatActivity target);
    void inject(WebServicePebble target);

}

