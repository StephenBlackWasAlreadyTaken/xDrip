package com.eveningoutpost.dexdrip.ShareModels.UserAgentInfo;

import com.eveningoutpost.dexdrip.ShareModels.UserAgentInfo.RuntimeInfo;
import com.google.gson.annotations.Expose;

/**
 * Created by stephenblack on 6/29/15.
 */
public class UserAgent {
    @Expose
    public String sessionId;

    @Expose
    public RuntimeInfo runtimeInfo;

    public UserAgent(String aSessionId) {
        this.runtimeInfo = new RuntimeInfo();
        this.sessionId = aSessionId;
    }
}
