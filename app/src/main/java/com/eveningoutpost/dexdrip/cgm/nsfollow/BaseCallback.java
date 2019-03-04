package com.eveningoutpost.dexdrip.cgm.nsfollow;

import androidx.annotation.*;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.store.FastStore;

import lombok.RequiredArgsConstructor;
import retrofit2.*;

// jamorham

// Callback template to reduce boiler plate

// TODO make reusable

@RequiredArgsConstructor
public class BaseCallback<T> implements Callback<T> {

	protected final String TAG = this.getClass().getSimpleName();

	//  final Session session;
	private final String name;
	private Runnable onSuccess;
	private Runnable onFailure;

	BaseCallback<T> setOnSuccess(final Runnable runnable) {
		this.onSuccess = runnable;
		return this;
	}

	BaseCallback<T> setOnFailure(final Runnable runnable) {
		this.onFailure = runnable;
		return this;
	}

	@Override
	public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
		if (response.isSuccessful() && response.body() != null) {
			UserError.Log.d(TAG, name + " success");
			if (onSuccess != null) {
				onSuccess.run();
			}
		} else {
			final String msg = name + " was not successful: " + response.code() + " " + response.message();
			UserError.Log.e(TAG, msg);
			status(msg);
			if (onFailure != null) {
				onFailure.run();
			}
		}
	}

	@Override
	public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
		final String msg = name + " Failed: " + t;
		UserError.Log.e(TAG, msg);
		status(msg);
		if (onFailure != null) {
			onFailure.run();
		}
	}


	private void status(final String status) {
		FastStore.getInstance().putS(TAG + ".STATUS_KEY", status);
	}

	protected String getStatus() {
		return FastStore.getInstance().getS(TAG + ".STATUS_KEY");
	}
}
