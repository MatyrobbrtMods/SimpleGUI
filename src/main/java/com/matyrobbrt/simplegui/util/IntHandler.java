package com.matyrobbrt.simplegui.util;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public record IntHandler(IntSupplier getter, IntConsumer setter) implements IntSupplier, IntConsumer {
    @Override
    public int getAsInt() {
        return getter.getAsInt();
    }

    @Override
    public void accept(int value) {
        setter.accept(value);
    }
}
