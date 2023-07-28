package com.homo.game.login.facade.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class UserInfoResponse implements Serializable {
    private final static String CHANNEL_UID = "channelUid";
    private final static String EXTRA = "extra";
    private final static String SYSTEM_TAG = "systemTag";
    private final static String USER_TAG = "systemTag";

    Map<String, Object> info = new HashMap<>();

    public String channelUid() {
        return (String) info.get(CHANNEL_UID);
    }

    public String extra() {
        return (String) info.get(EXTRA);
    }

    public String systemTag() {
        return (String) info.get(SYSTEM_TAG);
    }

    public String userTag() {
        return (String) info.get(USER_TAG);
    }
}
