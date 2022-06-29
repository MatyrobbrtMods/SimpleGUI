package com.matyrobbrt.simplegui.annotations.process.targets;

public record CallOnlyOnTarget(String clazz, Method method, String side, boolean logical) {
}