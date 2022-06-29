package com.matyrobbrt.simplegui.annotations.process.targets;

import java.util.List;

public record Method(String name, String desc, List<String> params, String returnType) {
    public Method(String name, String desc) {
        this(name, desc, null, null);
    }

    public Method excludeName() {
        return new Method(
                null, desc, params, returnType
        );
    }

    public Method excludeReturnAndParams() {
        return new Method(name, desc, null, null);
    }
}
