package com.matyrobbrt.simplegui.client.element.slot;

import com.matyrobbrt.simplegui.inventory.slot.VirtualSlot;

public interface GuiVirtualSlot {

    /**
     * Checks if this GUI slot is the GUI slot of the container {@code slot}.
     */
    boolean isChild(VirtualSlot slot);

}
