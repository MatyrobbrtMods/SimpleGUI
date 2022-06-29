package com.matyrobbrt.simplegui.inventory;

@FunctionalInterface
public interface ContentsListener {

    /**
     * Called when the inventory's contents changed.
     */
    void onContentsChanged();

}
