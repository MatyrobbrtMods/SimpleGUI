package com.matyrobbrt.simplegui.annotations.process.targets;

import com.google.gson.annotations.SerializedName;

public record PrivateOverrideTarget(Method method, Target target, Input input) {
    public record Target(String clazz, String method) {}
    public record Input(String clazz, String method, @SerializedName("interface") boolean isInterface) {}
}
