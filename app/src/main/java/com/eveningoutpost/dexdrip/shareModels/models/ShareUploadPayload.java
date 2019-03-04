package com.eveningoutpost.dexdrip.shareModels.models;

import com.eveningoutpost.dexdrip.shareModels.ShareUploadableBg;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emma Black on 3/19/15.
 */
public class ShareUploadPayload {
    @Expose
    public String SN;

    @Expose
    public Egv[] Egvs;

    @Expose
    public long TA = -5;

    public ShareUploadPayload(String sn, ShareUploadableBg bg) {
        this.SN = sn;
        List<Egv> egvList = new ArrayList<>();
        egvList.add(new Egv(bg));
        this.Egvs = egvList.toArray(new Egv[egvList.size()]);
    }
}
