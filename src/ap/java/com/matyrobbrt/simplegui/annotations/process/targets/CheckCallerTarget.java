package com.matyrobbrt.simplegui.annotations.process.targets;

import java.util.List;

public record CheckCallerTarget(String name, String desc, List<String> callers, String exception, boolean blacklist) {
    public void verify(String unmappedName, String owner) {
        if (callers.isEmpty())
            throw new IllegalStateException("Method '%s#%s' annotated with @CheckCaller must define at least one caller!"
                    .formatted(owner, unmappedName));
    }
}
