package com.matyrobbrt.simplegui.client.element.button;

import com.matyrobbrt.simplegui.Translations;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.Texture;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.util.Color;
import com.matyrobbrt.simplegui.util.Utils;
import org.jetbrains.annotations.NotNull;

public class CloseButton extends SimpleButton {
    public static final Texture TEXTURE = new Texture(
            Utils.getResource("gui/button", "close"),
            0, 0,
            12, 12,
            12, 12
    );

    public static final Hoverable ON_HOVER = Hoverable.displayTooltip(Translations.CLOSE_BUTTON.make());

    public CloseButton(Gui gui, int x, int y, Runnable action) {
        super(gui, x, y, 12, 12, Utils.emptyComponent(), action, action, ON_HOVER);
        setTexture(TEXTURE);
    }

    public CloseButton(Gui gui, int x, int y, Window window) {
        this(gui, x, y, window::close);
        setTexture(TEXTURE);
    }

    @Override
    protected Color getButtonColor(@NotNull SimpleButton.State state) {
        if (state == State.HOVERED)
            return Color.RED;
        return super.getButtonColor(state);
    }
}
