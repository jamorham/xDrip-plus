package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.squareup.okhttp.*;

/**
 * jamorham
 *
 * message base
 */

public abstract class BaseMessage {

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    public RequestBody getBody() {
        return RequestBody.create(MediaType.parse("application/json"), this.toS());
    }

}
