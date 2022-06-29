package com.matyrobbrt.simplegui.client.element;

import com.matyrobbrt.simplegui.client.Gui;
import net.minecraft.resources.ResourceLocation;

public abstract class TexturedElement extends GuiElement {

    protected final ResourceLocation texture;

    public TexturedElement(ResourceLocation resource, Gui gui, int x, int y, int width, int height) {
        super(gui, x, y, width, height);
        this.texture = resource;
    }

    protected ResourceLocation getTexture() {
        return texture;
    }
}
