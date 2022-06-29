package com.matyrobbrt.simplegui.util;

import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;

public enum InteractionType {
    /**
     * Manual (player) interactions.
     */
    MANUAL,
    /**
     * External third-party (automated) interactions
     */
    EXTERNAL,
    /**
     * Machine interacting with itself
     */
    INTERNAL;

    @ParametersAreNonnullByDefault
    public interface Predicate {
        Predicate TRUE = (stack, interactionType) -> true;
        Predicate FALSE = (stack, interactionType) -> false;

        boolean canExecute(ItemStack stack, InteractionType interactionType);
    }
}
