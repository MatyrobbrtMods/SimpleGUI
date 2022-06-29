package com.matyrobbrt.simplegui.inventory.slot.impl;

import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;

public class OffhandSlot extends InsertableSlot.Impl {
    public OffhandSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
    }
}
