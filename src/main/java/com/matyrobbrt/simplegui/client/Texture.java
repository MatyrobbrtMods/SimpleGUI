package com.matyrobbrt.simplegui.client;

import com.matyrobbrt.simplegui.annotations.CallOnlyOn;
import com.matyrobbrt.simplegui.util.Color;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public record Texture(ResourceLocation location, int x, int y, int width, int height, int textureWidth, int textureHeight, @Nullable
                      Supplier<Color> colour) {
    public Texture(ResourceLocation location, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        this(location, x, y, width, height, textureWidth, textureHeight, null);
    }

    public Texture(ResourceLocation location, int x, int y, int width, int height) {
        this(location, x, y, width, height, 256, 256, null);
    }

    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    public void render(PoseStack matrix, int x, int y) {
        RenderSystem.setShaderTexture(0, location());
        if (colour != null)
            ClientUtil.setColour(colour.get());
        Screen.blit(matrix, x, y, width, height, x(), y(), width(), height(), textureWidth(), textureHeight());
        if (colour != null)
            ClientUtil.resetColour();
    }
}
