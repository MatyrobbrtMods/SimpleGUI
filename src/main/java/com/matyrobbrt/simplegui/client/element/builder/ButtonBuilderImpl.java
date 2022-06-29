package com.matyrobbrt.simplegui.client.element.builder;

import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.Texture;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.matyrobbrt.simplegui.client.element.button.SimpleButton;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.util.Color;
import com.matyrobbrt.simplegui.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNullableByDefault
class ButtonBuilderImpl implements ButtonBuilder {
    private final Gui gui;
    private final int x, y;
    private int width = 20, height = 20;
    private Runnable onLeftClick, onRightClick;
    private Component text = Utils.emptyComponent();
    private GuiElement.Hoverable onHover;
    private Texture texture;
    private boolean playClickSound;
    private ColorProvider colour, borderColour, textColour;
    private Supplier<? extends Window> opensWindow;

    ButtonBuilderImpl(Gui gui, int x, int y) {
        this.gui = gui;
        this.x = x;
        this.y = y;
    }

    @Override
    public ButtonBuilder withWidth(int width) {
        return self(() -> this.width = width);
    }
    @Override
    public ButtonBuilder withHeight(int h) {
        return self(() -> this.height = h);
    }

    @Override
    public ButtonBuilder onClick(Runnable action) {
        onLeftClick(action);
        return onRightClick(action);
    }

    @Override
    public ButtonBuilder onLeftClick(Runnable action) {
        return self(() -> this.onLeftClick = action);
    }
    @Override
    public ButtonBuilder opensWindow(@NotNull Supplier<? extends Window> window) {
        return self(() -> this.opensWindow = window);
    }
    @Override
    public ButtonBuilder onRightClick(Runnable action) {
        return self(() -> this.onRightClick = action);
    }
    @Override
    public ButtonBuilder onHover(GuiElement.Hoverable action) {
        return self(() -> this.onHover = action);
    }

    @Override
    public ButtonBuilder withTexture(Texture texture) {
        return self(() -> this.texture = texture);
    }
    @Override
    public ButtonBuilder withText(Component text) {
        return self(() -> this.text = text == null ? Utils.emptyComponent() : text);
    }
    @Override
    public ButtonBuilder playClickSound(boolean playClickSound) {
        return self(() -> this.playClickSound = playClickSound);
    }

    @Override
    public ButtonBuilder withColor(ColorProvider provider) {
        return self(() -> this.colour = provider);
    }
    @Override
    public ButtonBuilder withBorderColor(ColorProvider provider) {
        return self(() -> this.borderColour = provider);
    }
    @Override
    public ButtonBuilder withTextColor(ColorProvider provider) {
        return self(() -> this.textColour = provider);
    }

    @Override
    public ButtonBuilder withColor(SimpleButton.State state, Color colour) {
        final var old = this.colour;
        return self(() -> this.colour = (state1) -> {
            if (state1 == state)
                return colour;
            else
                return old == null ? null : old.get(state1);
        });
    }

    @Override
    public ButtonBuilder withBorderColor(SimpleButton.State state, Color colour) {
        final var old = this.borderColour;
        return self(() -> this.borderColour = (state1) -> {
            if (state1 == state)
                return colour;
            else
                return old == null ? null : old.get(state1);
        });
    }


    @Override
    public ButtonBuilder withTextColor(SimpleButton.State state, Color colour) {
        final var old = this.textColour;
        return self(() -> this.textColour = (state1) -> {
            if (state1 == state)
                return colour;
            else
                return old == null ? null : old.get(state1);
        });
    }

    @Override
    public SimpleButton build() {
        return new SimpleButton(gui, x, y, width, height, text, onLeftClick, onRightClick, onHover) {
            {
                texture = ButtonBuilderImpl.this.texture;
                playClickSound = ButtonBuilderImpl.this.playClickSound;
                if (opensWindow != null)
                    opensWindow(opensWindow);
            }

            @Override
            protected Color getButtonColor(@Nonnull State state) {
                if (colour == null)
                    return super.getButtonColor(state);
                final var col = colour.get(state);
                if (col != null)
                    return col;
                return super.getButtonColor(state);
            }

            @Override
            protected Color getTextColor(@Nonnull State state) {
                if (textColour == null)
                    return super.getTextColor(state);
                final var col = textColour.get(state);
                if (col != null)
                    return col;
                return super.getTextColor(state);
            }

            @Override
            protected Color getBorderColor(@Nonnull State state) {
                if (borderColour == null)
                    return super.getBorderColor(state);
                final var col = borderColour.get(state);
                if (col != null)
                    return col;
                return super.getBorderColor(state);
            }
        };
    }

    protected ButtonBuilder self(Runnable action) {
        action.run();
        return this;
    }
}
