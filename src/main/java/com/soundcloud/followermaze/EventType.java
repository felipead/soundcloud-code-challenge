package com.soundcloud.followermaze;

import java.util.Arrays;

public enum EventType {
    UNFOLLOW("U", true, true),
    FOLLOW("F", true, true),
    BROADCAST("B", false, false),
    PRIVATE_MESSAGE("P", true, true),
    STATUS_UPDATE("S", true, false);

    private String code;
    private boolean hasFrom;
    private boolean hasTo;

    EventType(String code, boolean hasFrom, boolean hasTo) {
        this.code = code;
        this.hasFrom = hasFrom;
        this.hasTo = hasTo;
    }

    public static EventType fromCode(String code) {
        return Arrays.stream(EventType.values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(code));
    }

    public String getCode() {
        return code;
    }

    public boolean hasFrom() {
        return hasFrom;
    }

    public boolean hasTo() {
        return hasTo;
    }
}
