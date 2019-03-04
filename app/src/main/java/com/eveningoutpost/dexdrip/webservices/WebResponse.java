package com.eveningoutpost.dexdrip.webservices;

import java.nio.charset.*;

/**
 * Created by jamorham on 06/01/2018.
 *
 * Data class for webservice responses
 */

public class WebResponse {

    private static String TAG = "WebResponse";

    byte[] bytes;
    String mimeType;
    int resultCode;

    WebResponse(String str) {
        this(str, 200, "application/json");
    }

    WebResponse(String str, int resultCode, String mimeType) {
	    bytes = str.getBytes(StandardCharsets.UTF_8);
	    this.mimeType = mimeType;
        this.resultCode = resultCode;
    }
}
