package com.matyrobbrt.simplegui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Translations {
    CLOSE_BUTTON("button", "close");

    private final String key;

    Translations(String key, String location) {
        this.key = key + "." + SimpleGUIMod.MOD_ID + "." + location;
    }

    public MutableComponent make(Object... args) {
        return Component.translatable(key, args);
    }
}
