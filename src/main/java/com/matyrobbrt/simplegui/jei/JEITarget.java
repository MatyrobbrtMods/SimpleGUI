package com.matyrobbrt.simplegui.jei;

import org.jetbrains.annotations.Nullable;

public interface JEITarget {

    /**
     * Gets the ingredient under the mouse.
     *
     * @param mouseX x position of mouse.
     * @param mouseY y position of mouse.
     * @return hte ingredient, or {@code null}
     */
    @Nullable
    Object getIngredient(final double mouseX, final double mouseY);
}
