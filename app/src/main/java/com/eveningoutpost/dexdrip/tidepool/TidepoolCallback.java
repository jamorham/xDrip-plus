package com.eveningoutpost.dexdrip.tidepool;

import androidx.annotation.*;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.store.FastStore;

import lombok.RequiredArgsConstructor;
import retrofit2.*;

// jamorham

// Callback template to reduce boiler plate

@RequiredArgsConstructor
class TidepoolCallback<T> implements Callback<T> {

    final Session session;
    final String name;
    final Runnable onSuccess;

    Runnable onFailure;

    TidepoolCallback<T> setOnFailure(final Runnable runnable) {
        this.onFailure = runnable;
        return this;
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (response.isSuccessful() && response.body() != null) {
            UserError.Log.d(TidepoolUploader.TAG, name + " success");
            session.populateBody(response.body());
            session.populateHeaders(response.headers());
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            final String msg = name + " was not successful: " + response.code() + " " + response.message();
            UserError.Log.e(TidepoolUploader.TAG, msg);
            status(msg);
            if (onFailure != null) {
                onFailure.run();
            }
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        final String msg = name + " Failed: " + t;
        UserError.Log.e(TidepoolUploader.TAG, msg);
        status(msg);
        if (onFailure != null) {
            onFailure.run();
        }
    }


    private static void status(final String status) {
        FastStore.getInstance().putS(TidepoolUploader.STATUS_KEY, status);
    }
}
