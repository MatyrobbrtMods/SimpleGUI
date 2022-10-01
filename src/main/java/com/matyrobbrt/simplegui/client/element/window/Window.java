package com.matyrobbrt.simplegui.client.element.window;

import com.matyrobbrt.simplegui.client.ClientUtil;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.SimpleGui;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.matyrobbrt.simplegui.client.element.TexturedElement;
import com.matyrobbrt.simplegui.client.element.button.CloseButton;
import com.matyrobbrt.simplegui.inventory.EmptyContainer;
import com.matyrobbrt.simplegui.inventory.SelectedWindowData;
import com.matyrobbrt.simplegui.inventory.WindowType;
import com.matyrobbrt.simplegui.inventory.slot.VirtualSlot;
import com.matyrobbrt.simplegui.util.Color;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Window extends TexturedElement implements VirtualSlot.GuiWindow {

    public static final ResourceLocation BASE_BACKGROUND = Utils.getResource("gui", "base");
    public static final ResourceLocation SHADOW = Utils.getResource("gui", "shadow");
    public static final ResourceLocation BLUR = Utils.getResource("gui", "blur");

    private static final Color OVERLAY_COLOR = Color.rgbai(60, 60, 60, 128);

    protected final SelectedWindowData data;
    private boolean dragging = false;
    private double dragX;
    private double dragY;
    private int prevDX;
    private int prevDY;

    private Consumer<Window> closeListener;
    private Consumer<Window> reattachListener;

    @Nonnull
    protected InteractionStrategy interactionStrategy = InteractionStrategy.CONTAINER;

    private static SelectedWindowData.WindowPosition calculateOpenPosition(Gui gui, SelectedWindowData windowData, int x, int y, int width, int height) {
        final var lastPosition = windowData.getLastPosition();
        int lastX = lastPosition.x();
        if (lastX != Integer.MAX_VALUE) {
            final var guiLeft = gui.getLeft();
            if (guiLeft + lastX < 0) {
                lastX = -guiLeft;
            } else if (guiLeft + lastX + width > MINECRAFT.getWindow().getGuiScaledWidth()) {
                lastX = MINECRAFT.getWindow().getGuiScaledWidth() - guiLeft - width;
            }
        }
        int lastY = lastPosition.y();
        if (lastY != Integer.MAX_VALUE) {
            final var guiTop = gui.getTop();
            if (guiTop + lastY < 0) {
                lastY = -guiTop;
            } else if (guiTop + lastY + height > MINECRAFT.getWindow().getGuiScaledHeight()) {
                lastY = MINECRAFT.getWindow().getGuiScaledHeight() - guiTop - height;
            }
        }
        return new SelectedWindowData.WindowPosition(lastX == Integer.MAX_VALUE ? x : lastX, lastY == Integer.MAX_VALUE ? y : lastY);
    }

    public Window(Gui gui, int x, int y, int width, int height, WindowType windowType) {
        this(gui, x, y, width, height, windowType == WindowType.UNSPECIFIED ? SelectedWindowData.UNSPECIFIED : new SelectedWindowData(windowType));
    }

    public Window(Gui gui, int x, int y, int width, int height, SelectedWindowData windowData) {
        this(gui, calculateOpenPosition(gui, windowData, x, y, width, height), width, height, windowData);
    }

    private Window(Gui gui, SelectedWindowData.WindowPosition calculatedPosition, int width, int height, SelectedWindowData windowData) {
        super(BASE_BACKGROUND, gui, calculatedPosition.x(), calculatedPosition.y(), width, height);
        this.data = windowData;
        isOverlay = true;
        active = true;
    }

    public void init() {
        if (!isFocusOverlay()) {
            addCloseButton();
        }
    }

    protected void addCloseButton() {
        addChild(new CloseButton(gui(), relativeX + width - 6 - 12, relativeY + 6, this));
    }

    public void onFocusLost() {
    }

    public void onFocused() {
        gui().setSelectedWindow(data);
    }

    protected void setStrategy(@Nonnull InteractionStrategy strategy) {
        this.interactionStrategy = strategy;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final var res = super.mouseClicked(mouseX, mouseY, button);
        if (isMouseOver(mouseX, mouseY)) {
            // Handle dragging
            if (mouseY < y + 18) {
                dragging = true;
                dragX = mouseX;
                dragY = mouseY;
                prevDX = 0;
                prevDY = 0;
            }
        } else if (!res && interactionStrategy.allowContainer()) {
            if (gui() instanceof SimpleGui<?> gui) {
                final var c = gui.getMenu();
                if (!(c instanceof EmptyContainer)) {
                    // allow interaction with slots
                    if (mouseX >= getGuiLeft() && mouseX < getGuiLeft() + getGuiWidth() && mouseY >= getGuiTop() + getGuiHeight() - 90) {
                        return false;
                    }
                }
            }
        }
        return res || !interactionStrategy.allowAll();
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double mouseXOld, double mouseYOld) {
        super.onDrag(mouseX, mouseY, mouseXOld, mouseYOld);
        if (dragging) {
            final int newDX = (int) Math.round(mouseX - dragX), newDY = (int) Math.round(mouseY - dragY);
            final var changeX = Math.max(-x, Math.min(MINECRAFT.getWindow().getGuiScaledWidth() - (x + width), newDX - prevDX));
            final var changeY = Math.max(-y, Math.min(MINECRAFT.getWindow().getGuiScaledHeight() - (y + height), newDY - prevDY));
            prevDX = newDX;
            prevDY = newDY;
            move(changeX, changeY);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        dragging = false;
    }

    @Override
    public void renderBackgroundOverlay(PoseStack matrix, int mouseX, int mouseY) {
        if (isFocusOverlay()) {
            ClientUtil.renderColourOverlay(matrix, 0, 0, MINECRAFT.getWindow().getGuiScaledWidth(), MINECRAFT.getWindow().getGuiScaledHeight(), OVERLAY_COLOR);
        } else {
            RenderSystem.setShaderColor(1, 1, 1, 0.75F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ClientUtil.renderBackgroundTexture(matrix, SHADOW, 4, 4, x - 3, y - 3, width + 6, height + 6, 256, 256);
            RenderSystem.disableBlend();
            ClientUtil.resetColour();
        }
        renderBackgroundTexture(matrix, getTexture(), 4, 4);
    }

    /**
     * @deprecated does nothing
     * @see #renderForeground(PoseStack, int, int)
     */
    @Override
    @Deprecated
    public final void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }

    public void setListenerTab(Supplier<? extends GuiElement> elementSupplier) {
        setTabListeners(window -> elementSupplier.get().active = true, window -> elementSupplier.get().active = false);
    }

    public void setTabListeners(Consumer<Window> closeListener, Consumer<Window> reattachListener) {
        this.closeListener = closeListener;
        this.reattachListener = reattachListener;
    }

    @Override
    public void resize(int prevLeft, int prevTop, int left, int top) {
        super.resize(prevLeft, prevTop, left, top);
        if (reattachListener != null) {
            reattachListener.accept(this);
        }
    }

    public void renderBlur(PoseStack matrix) {
        RenderSystem.setShaderColor(1, 1, 1, 0.3F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ClientUtil.renderBackgroundTexture(matrix, BLUR, 4, 4, relativeX, relativeY, width, height, 256, 256);
        RenderSystem.disableBlend();
        ClientUtil.resetColour();
    }

    public void close() {
        gui().removeWindow(this);
        children.forEach(ch -> ch.onWindowClose(this));
        if (closeListener != null) {
            closeListener.accept(this);
        }
        data.updateLastPosition(relativeX, relativeY);
    }

    protected boolean isFocusOverlay() {
        return false;
    }

    @Override
    public void drawTitleText(PoseStack matrix, Component text, float y) {
        if (isFocusOverlay()) {
            super.drawTitleText(matrix, text, y);
        } else {
            final var leftShift = getTitlePadStart();
            final var xSize = getXSize() - leftShift - getTitlePadEnd();
            final var maxLength = xSize - 12;
            final float textWidth = getStringWidth(text);
            final float scale = Math.min(1, maxLength / textWidth);
            final float left = relativeX + xSize / 2F;
            drawScaledCenteredText(matrix, text, left + leftShift, relativeY + y, titleTextColor(), scale);
        }
    }

    protected int getTitlePadStart() {
        return 12;
    }
    protected int getTitlePadEnd() {
        return 0;
    }

    public enum InteractionStrategy {
        NONE,
        CONTAINER,
        ALL;

        boolean allowContainer() {
            return this != NONE;
        }

        boolean allowAll() {
            return this == ALL;
        }
    }
}
