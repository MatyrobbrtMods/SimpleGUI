package com.matyrobbrt.simplegui.inventory.slot.impl;

import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import net.minecraft.world.Container;

public class HotbarSlot extends InsertableSlot.Impl {
    public HotbarSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
}
