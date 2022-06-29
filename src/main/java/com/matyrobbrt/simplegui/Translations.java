package com.matyrobbrt.simplegui;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;

public enum Translations {
    CLOSE_BUTTON("button", "close");

    private final String key;

    Translations(String key, String location) {
        this.key = key + "." + SimpleGUIMod.MOD_ID + "." + location;
    }

    public MutableComponent make(Object... args) {
        return new TranslatableComponent(key, args);
    }
}
