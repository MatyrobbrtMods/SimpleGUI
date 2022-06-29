package com.matyrobbrt.simplegui.client.element.builder;

import com.matyrobbrt.simplegui.client.element.GuiElement;

/**
 * A builder for {@link GuiElement gui elements}.
 *
 * @param <T> the type of the built element
 */
public interface ElementBuilder<T extends GuiElement> {
    /**
     * Builds this element.
     *
     * @return the built element
     */
    T build();
}
