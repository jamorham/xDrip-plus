package com.eveningoutpost.dexdrip.tidepool;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.models.UserError;


import java.io.IOException;

import lombok.RequiredArgsConstructor;
import okhttp3.*;

// jamorham

@RequiredArgsConstructor
public class InfoInterceptor implements Interceptor {

    private final String tag;

    @NonNull
    @Override
    public Response intercept(@NonNull final Chain chain) throws IOException {
        final Request request = chain.request();
        if (request != null && request.body() != null) {
            UserError.Log.d(tag, "Interceptor Body size: " + request.body().contentLength());
            //} else {
            //  UserError.Log.d(tag,"Null request body in InfoInterceptor");
        }
        return chain.proceed(request);
    }
}
