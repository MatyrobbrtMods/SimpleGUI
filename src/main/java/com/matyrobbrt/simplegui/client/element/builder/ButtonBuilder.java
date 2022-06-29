package com.matyrobbrt.simplegui.client.element.builder;

import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.Texture;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.matyrobbrt.simplegui.client.element.button.SimpleButton;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.util.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;
import java.util.function.Supplier;

/**
 * A builder for {@link SimpleButton}s.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNullableByDefault
public interface ButtonBuilder extends ElementBuilder<SimpleButton> {

    ButtonBuilder withWidth(int width);
    ButtonBuilder withHeight(int height);

    ButtonBuilder onClick(Runnable action);
    ButtonBuilder onLeftClick(Runnable action);
    ButtonBuilder onRightClick(Runnable action);
    ButtonBuilder opensWindow(@Nonnull Supplier<? extends Window> window);
    ButtonBuilder onHover(GuiElement.Hoverable action);

    ButtonBuilder withTexture(Texture texture);
    ButtonBuilder withText(Component text);
    ButtonBuilder playClickSound(boolean playClickSound);

    ButtonBuilder withColor(ColorProvider provider);
    ButtonBuilder withBorderColor(ColorProvider provider);
    ButtonBuilder withTextColor(ColorProvider provider);
    ButtonBuilder withColor(SimpleButton.State state, Color colour);
    ButtonBuilder withBorderColor(SimpleButton.State state, Color colour);
    ButtonBuilder withTextColor(SimpleButton.State state, Color colour);

    static ButtonBuilder builder(@Nonnull Gui gui, int x, int y) {
        return new ButtonBuilderImpl(gui, x, y);
    }

    static ButtonBuilder builder(@Nonnull Window window, int x, int y) {
        return new ButtonBuilderImpl(window.gui(), window.getRelativeX() + x, window.getRelativeY() + y);
    }

    @FunctionalInterface
    interface ColorProvider {
        @Nullable
        Color get(@Nonnull SimpleButton.State state);
    }
}
