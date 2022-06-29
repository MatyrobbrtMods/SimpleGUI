package com.matyrobbrt.simplegui.client.element.window;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.element.builder.ButtonBuilder;
import com.matyrobbrt.simplegui.client.element.button.CloseButton;
import com.matyrobbrt.simplegui.inventory.WindowType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNullableByDefault
public class ConfirmationWindow extends Window {

    public static final WindowType TYPE = new WindowType(null);

    protected final List<FormattedCharSequence> message;
    protected Runnable whenConfirmed;
    protected Runnable whenDenied;
    protected boolean autoClose;

    protected Component positiveText = CommonComponents.GUI_YES;
    protected Component negativeText = CommonComponents.GUI_NO;

    @ParametersAreNonnullByDefault
    public ConfirmationWindow(Gui gui, int x, int y, Component message) {
        super(gui, x, y, 230, computeLineNumber(message) * (MINECRAFT.font.lineHeight + 2) + 36, TYPE);
        this.message = MINECRAFT.font.split(message, 200);
        setStrategy(InteractionStrategy.NONE);
        setMessage(message);
    }

    @ParametersAreNonnullByDefault
    public ConfirmationWindow(Window window, int x, int y, Component message) {
        this(window.gui(), window.getRelativeX() + x, window.getRelativeY() + y, message);
    }

    @CanIgnoreReturnValue
    public ConfirmationWindow whenConfirmed(Runnable action) {
        this.whenConfirmed = action;
        return this;
    }
    @CanIgnoreReturnValue
    public ConfirmationWindow whenDenied(Runnable action) {
        this.whenDenied = action;
        return this;
    }
    @CanIgnoreReturnValue
    public ConfirmationWindow autoClose(boolean autoClose) {
        this.autoClose = autoClose;
        return this;
    }

    @CanIgnoreReturnValue
    public ConfirmationWindow withPositiveText(@Nonnull Component positiveText) {
        this.positiveText = positiveText;
        return this;
    }
    @CanIgnoreReturnValue
    public ConfirmationWindow withNegativeText(@Nonnull Component negativeText) {
        this.negativeText = negativeText;
        return this;
    }

    @Override
    public void init() {
        super.init();
        final var messageOffset = message.size() * (MINECRAFT.font.lineHeight + 2);
        addChild(ButtonBuilder.builder(this, width / 2 - 105, messageOffset + 10)
                .withWidth(100).withHeight(20)
                .withText(positiveText)
                .onClick(composite(whenConfirmed))
        );
        addChild(ButtonBuilder.builder(this, width / 2 + 5, messageOffset + 10)
                .withWidth(100).withHeight(20)
                .withText(negativeText)
                .onClick(composite(whenDenied))
        );
    }

    private Runnable composite(Runnable runnable) {
        return () -> {
            if (autoClose)
                this.close();
            if (runnable != null)
                runnable.run();
        };
    }

    @Override
    protected void addCloseButton() {
    }

    @Override
    public void renderForeground(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        for (int i = 0; i < message.size(); i++) {
            final var msg = message.get(i);
            final var lineWidth = MINECRAFT.font.width(msg);
            MINECRAFT.font.draw(matrix, message.get(i), relativeX + (width - lineWidth) / 2, relativeY + 6 + i * (2 + MINECRAFT.font.lineHeight), 0x000000);
        }
    }

    private static int computeLineNumber(@Nonnull Component message) {
        return MINECRAFT.font.split(message, 200).size();
    }
}
