package com.eveningoutpost.dexdrip.tidepool;

import androidx.annotation.*;



import java.io.IOException;

import okhttp3.*;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

class GzipRequestInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            final Request originalRequest = chain.request();
            if (originalRequest.body() == null
                    || originalRequest.header("Content-Encoding") != null)
                    {
                return chain.proceed(originalRequest);
            }

            final Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), gzip(originalRequest.body()))
                    .build();
            return chain.proceed(compressedRequest);
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override public MediaType contentType() {
                    return body.contentType();
                }

                @Override public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override public void writeTo(@NonNull BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }

}
