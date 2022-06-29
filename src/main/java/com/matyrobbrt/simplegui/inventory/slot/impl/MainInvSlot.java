package com.matyrobbrt.simplegui.inventory.slot.impl;

import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import net.minecraft.world.Container;

public class MainInvSlot extends InsertableSlot.Impl {
    public MainInvSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
}
