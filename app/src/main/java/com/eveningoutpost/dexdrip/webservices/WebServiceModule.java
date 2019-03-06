package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.models.*;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;


/**
 * Created by jamorham on 20/12/2017.
 *
 * dagger module
 *
 */

@Module
public class WebServiceModule {

    private static final boolean d = true;

    @Provides
    @Singleton
    @Named("RouteFinder")
    RouteFinder providesRouteFinder() {
        if (d) UserError.Log.i("INJECT", "creating RouteFinder");
        return new RouteFinder();
    }

    @Provides
    @Singleton
    @Named("WebServicePebble")
    BaseWebService providesWebServicePebble() {
        if (d) UserError.Log.i("INJECT", "creating WebServicePebble");
        return new WebServicePebble();
    }

    @Provides
    @Singleton
    @Named("WebServiceSgv")
    BaseWebService providesWebServiceSgv() {
        if (d) UserError.Log.i("INJECT", "creating WebServiceSgv");
        return new WebServiceSgv();
    }

    @Provides
    @Singleton
    @Named("WebServiceStatus")
    BaseWebService providesWebServiceStatus() {
        if (d) UserError.Log.i("INJECT", "creating WebServiceStatus");
        return new WebServiceStatus();
    }

    @Provides
    @Singleton
    @Named("WebServiceTasker")
    BaseWebService providesWebServiceTasker() {
        if (d) UserError.Log.i("INJECT", "creating WebServiceTasker");
        return new WebServiceTasker();
    }

    @Provides
    @Singleton
    @Named("WebServiceSteps")
    BaseWebService providesWebServiceSteps() {
        if (d) UserError.Log.i("INJECT", "creating WebServiceSteps");
        return new WebServiceSteps();
    }

    @Provides
    @Singleton
    @Named("WebServiceHeart")
    BaseWebService providesWebServiceHeart() {
        if (d) UserError.Log.i("INJECT", "creating WebServiceHeart");
        return new WebServiceHeart();
    }

    @Provides
    @Singleton
    @Named("WebServiceSync")
    BaseWebService providesWebServiceSync() {
        if (d) UserError.Log.i("INJECT", "creating WebServiceSync");
        return new WebServiceSync();
    }



}
