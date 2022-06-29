package com.matyrobbrt.simplegui.client.element.button;

import com.matyrobbrt.simplegui.client.ClientUtil;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.Texture;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.util.Color;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SimpleButton extends GuiElement {

    private static final int BUTTON_TEX_X = 200, BUTTON_TEX_Y = 40;
    private static final ResourceLocation TEXTURE = Utils.getResource("gui/button", "button");

    @Nullable
    private final Hoverable onHover;
    @Nullable
    private Runnable onLeftClick;
    @Nullable
    private final Runnable onRightClick;

    protected Texture texture;

    public static final int MAX_WIDTH = 200;
    public static final int MAX_HEIGHT = 20;

    public SimpleButton(Gui gui, int x, int y, int width, int height, Component text, @Nullable Runnable onLeftClick, @Nullable Hoverable onHover) {
        this(gui, x, y, width, height, text, onLeftClick, onLeftClick, onHover);
    }

    public SimpleButton(Gui gui, int x, int y, int width, int height, Component text, @Nullable Runnable onLeftClick, @Nullable Runnable onRightClick,
                          @Nullable Hoverable onHover) {
        super(gui, x, y, Math.min(width, MAX_WIDTH), Math.min(height, MAX_HEIGHT), text);
        this.onHover = onHover;
        this.onLeftClick = onLeftClick;
        this.onRightClick = onRightClick;
        playClickSound = true;
    }

    protected void onLeftClick() {
        if (onLeftClick != null) {
            onLeftClick.run();
        }
    }

    protected void onRightClick() {
        if (onRightClick != null) {
            onRightClick.run();
        }
    }

    protected void opensWindow(@Nonnull Supplier<? extends Window> window) {
        this.onLeftClick = () -> {
            final var win = window.get();
            win.setListenerTab(() -> this);
            gui().addWindow(win);
            this.active = false;
        };
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onLeftClick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && this.isFocused()) {
            if (keyCode == 257 || keyCode == 32 || keyCode == 335) {
                playClickSound();
                onLeftClick();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderToolTip(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        super.renderToolTip(matrix, mouseX, mouseY);
        if (onHover != null) {
            onHover.onHover(this, matrix, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.active && this.visible && isHoveredOrFocused()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                playClickSound();
                onRightClick();
                return true;
            }
        }
        return false;
    }

    public static final Color ACTIVE = Color.rgb(0x656565);
    public static final Color DISABLED = Color.rgb(0x313131);
    public static final Color HOVERED = Color.rgb(0x34CE8B);

    protected Color getButtonColor(@Nonnull State state) {
        return switch (state) {
            case ACTIVE -> ACTIVE;
            case DISABLED -> DISABLED;
            case HOVERED -> HOVERED;
        };
    }

    protected Color getBorderColor(@Nonnull State state) {
        return Color.BLACK;
    }

    protected Color getTextColor(@Nonnull State state) {
        return Color.WHITE;
    }

    protected void setTexture(Texture texture) {
        this.texture = texture;
    }

    @Override
    protected final int getButtonTextColor(int mouseX, int mouseY) {
        return getTextColor(getState(mouseX, mouseY)).rgb();
    }

    @Override
    public void drawBackground(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTick) {
        super.drawBackground(matrix, mouseX, mouseY, partialTick);
        drawButton(matrix, mouseX, mouseY);
        if (texture != null) {
            texture.render(matrix, x, y);
        }
    }

    protected boolean resetColorBeforeRender() {
        return true;
    }

    protected State getState(int mouseX, int mouseY) {
        if (isMouseOverCheckWindow(mouseX, mouseY)) return State.HOVERED;
        else return isActive() ? State.ACTIVE : State.DISABLED;
    }

    // TODO at some point, this should try to render bigger textures
    protected void drawButton(PoseStack matrix, int mouseX, int mouseY) {
        if (resetColorBeforeRender()) {
            ClientUtil.resetColour();
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int halfWidthLeft = width / 2;
        int halfWidthRight = width % 2 == 0 ? halfWidthLeft : halfWidthLeft + 1;
        int halfHeightTop = height / 2;
        int halfHeightBottom = height % 2 == 0 ? halfHeightTop : halfHeightTop + 1;

        final State state = getState(mouseX, mouseY);

        // This renders the borders
        final var borderColour = getBorderColor(state);
        ClientUtil.setColour(borderColour);
        // Left Top Corner
        blit(matrix, x, y, 0, 20, halfWidthLeft, halfHeightTop, BUTTON_TEX_X, BUTTON_TEX_Y);
        // Left Bottom Corner
        blit(matrix, x, y + halfHeightTop, 0, 20 + 20 - halfHeightBottom, halfWidthLeft, halfHeightBottom, BUTTON_TEX_X, BUTTON_TEX_Y);
        // Right Top Corner
        blit(matrix, x + halfWidthLeft, y, 200 - halfWidthRight, 20, halfWidthRight, halfHeightTop, BUTTON_TEX_X, BUTTON_TEX_Y);
        // Right Bottom Corner
        blit(matrix, x + halfWidthLeft, y + halfHeightTop, 200 - halfWidthRight, 20 + 20 - halfHeightBottom, halfWidthRight, halfHeightBottom, BUTTON_TEX_X, BUTTON_TEX_Y);
        ClientUtil.resetColour();

        // This renders the button itself
        final var buttonColour = getButtonColor(state);
        ClientUtil.setColour(buttonColour);
        // Left Top Corner
        blit(matrix, x, y, 0, 0, halfWidthLeft, halfHeightTop, BUTTON_TEX_X, BUTTON_TEX_Y);
        // Left Bottom Corner
        blit(matrix, x, y + halfHeightTop, 0, 20 - halfHeightBottom, halfWidthLeft, halfHeightBottom, BUTTON_TEX_X, BUTTON_TEX_Y);
        // Right Top Corner
        blit(matrix, x + halfWidthLeft, y, 200 - halfWidthRight, 0, halfWidthRight, halfHeightTop, BUTTON_TEX_X, BUTTON_TEX_Y);
        // Right Bottom Corner
        blit(matrix, x + halfWidthLeft, y + halfHeightTop, 200 - halfWidthRight, 20 - halfHeightBottom, halfWidthRight, halfHeightBottom, BUTTON_TEX_X, BUTTON_TEX_Y);
        ClientUtil.resetColour();

        renderBg(matrix, MINECRAFT, mouseX, mouseY);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    public enum State {
        DISABLED,
        ACTIVE,
        HOVERED
    }
}
