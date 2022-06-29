package com.matyrobbrt.simplegui.client.element;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.matyrobbrt.simplegui.annotations.ChangesAccessModifier;
import com.matyrobbrt.simplegui.client.ClientUtil;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.SimpleGui;
import com.matyrobbrt.simplegui.client.SpecialFontRenderer;
import com.matyrobbrt.simplegui.client.element.builder.ElementBuilder;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class GuiElement extends AbstractWidget implements SpecialFontRenderer {

    public static final Minecraft MINECRAFT = Minecraft.getInstance();

    protected final List<GuiElement> children = new ArrayList<>();
    /**
     * Children used for data transfer
     */
    private final List<GuiElement> positionOnlyChildren = new ArrayList<>();

    private Gui gui;
    protected boolean playClickSound;
    protected int relativeX;
    protected int relativeY;
    public boolean isOverlay;

    public GuiElement(Gui gui, int x, int y, int width, int height) {
        this(gui, x, y, width, height, Utils.emptyComponent());
    }

    public GuiElement(Gui gui, int x, int y, int width, int height, Component text) {
        super(gui.getLeft() + x, gui.getTop() + y, width, height, text);
        this.relativeX = x;
        this.relativeY = y;
        this.gui = gui;
    }

    @Override
    public void updateNarration(@Nonnull NarrationElementOutput output) {
        // TODO at some point, this needs to be made to work, especially with children of children
    }

    public int getRelativeX() {
        return relativeX;
    }

    public int getRelativeY() {
        return relativeY;
    }

    @CanIgnoreReturnValue
    protected <E extends GuiElement> E addChild(ElementBuilder<E> builder) {
        return addChild(builder.build());
    }

    @CanIgnoreReturnValue
    protected <E extends GuiElement> E addChild(E element) {
        children.add(element);
        if (isOverlay) {
            element.isOverlay = true;
        }
        return element;
    }

    @CanIgnoreReturnValue
    protected <E extends GuiElement> E addPositionOnlyChild(ElementBuilder<E> builder) {
        return addPositionOnlyChild(builder.build());
    }

    @CanIgnoreReturnValue
    protected <E extends GuiElement> E addPositionOnlyChild(E element) {
        positionOnlyChildren.add(element);
        return element;
    }

    public final Gui gui() {
        return gui;
    }

    public final int getGuiLeft() {
        return gui.getLeft();
    }

    public final int getGuiTop() {
        return gui.getTop();
    }

    public final int getGuiWidth() {
        return gui.getWidth();
    }

    public final int getGuiHeight() {
        return gui.getHeight();
    }

    public final List<GuiElement> children() {
        return children;
    }

    public void tick() {
        children.forEach(GuiElement::tick);
    }

    public void resize(int prevLeft, int prevTop, int left, int top) {
        x = x - prevLeft + left;
        y = y - prevTop + top;
        children.forEach(child -> child.resize(prevLeft, prevTop, left, top));
        positionOnlyChildren.forEach(child -> child.resize(prevLeft, prevTop, left, top));
    }

    public boolean childrenContainsElement(Predicate<GuiElement> checker) {
        return children.stream().anyMatch(e -> e.containsElement(checker));
    }

    public boolean containsElement(Predicate<GuiElement> checker) {
        return checker.test(this) || childrenContainsElement(checker);
    }

    @Override
    @ChangesAccessModifier
    public void setFocused(boolean focused) {
        super.setFocused(focused);
    }

    @Override
    public boolean changeFocus(boolean focused) {
        if (this.active && this.visible) {
            setFocused(!isFocused());
            final var isFocused = isFocused();
            this.onFocusedChanged(isFocused);
            return isFocused;
        }
        return false;
    }

    public void move(int changeX, int changeY) {
        x += changeX;
        y += changeY;
        relativeX += changeX;
        relativeY += changeY;
        children.forEach(child -> child.move(changeX, changeY));
        positionOnlyChildren.forEach(child -> child.move(changeX, changeY));
    }

    public void onWindowClose(Window window) {
        children.forEach(ch -> ch.onWindowClose(window));
    }

    public boolean hasPersistentData() {
        return children.stream().anyMatch(GuiElement::hasPersistentData);
    }

    public void syncFrom(GuiElement element) {
        int numChildren = children.size();
        if (numChildren > 0) {
            for (int i = 0; i < element.children.size(); i++) {
                GuiElement prevChild = element.children.get(i);
                if (prevChild.hasPersistentData() && i < numChildren) {
                    GuiElement child = children.get(i);
                    if (child.getClass() == prevChild.getClass()) {
                        child.syncFrom(prevChild);
                    }
                }
            }
        }
    }

    public final void onRenderForeground(PoseStack matrix, int mouseX, int mouseY, int zOffset, int totalOffset) {
        if (visible) {
            final var pTicks = ClientUtil.getPartialTicks();
            matrix.translate(0, 0, zOffset);
            SimpleGui.maxZOffset = Math.max(totalOffset, SimpleGui.maxZOffset);

            matrix.translate(-getGuiLeft(), -getGuiTop(), 0);
            renderBackgroundOverlay(matrix, mouseX, mouseY);

            children.forEach(child -> child.render(matrix, mouseX, mouseY, pTicks));
            children.forEach(child -> child.doDrawBackground(matrix, mouseX, mouseY, pTicks));
            matrix.translate(gui.getLeft(), gui.getTop(), 0);
            renderForeground(matrix, mouseX, mouseY);

            children.forEach(child -> {
                matrix.pushPose();
                child.onRenderForeground(matrix, mouseX, mouseY, 50, totalOffset + 50);
                matrix.popPose();
            });
        }
    }

    /**
     * Renders this element. The given {@code poseStack} will be translated to X -{@link #getGuiLeft()} and Y -{@link #getGuiTop()}.
     *
     * @param poseStack   the {@link PoseStack} to render to
     * @param mouseX      the mouse's X position
     * @param mouseY      the mouse's Y position
     * @param partialTick the partial tick
     */
    @Override
    public void render(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    /**
     * Renders this element in the foreground. The given {@code matrix} will be translated to the {@link #gui() gui's position},
     * so if you want to render something at the element's position, use the relative coordinates.
     *
     * @param matrix the matrix
     * @param mouseX the mouse's X position
     * @param mouseY the mouse's Y position
     */
    public void renderForeground(PoseStack matrix, int mouseX, int mouseY) {
        drawButtonText(matrix, mouseX, mouseY);
    }

    /**
     * Renders the background of this element. Note, the given {@code matrix} will be translated to X -{@link #getGuiLeft()}
     * and Y -{@link #getGuiTop()}.
     *
     * @param matrix the matrix
     * @param mouseX the mouse's X position
     * @param mouseY the mouse's Y position
     */
    public void renderBackgroundOverlay(PoseStack matrix, int mouseX, int mouseY) {
    }

    public final void doDrawBackground(PoseStack matrix, int mouseX, int mouseY, float pTicks) {
        if (visible) {
            drawBackground(matrix, mouseX, mouseY, pTicks);
        }
    }

    @Override
    public void renderToolTip(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        children.stream()
                .filter(child -> child.isMouseOver(mouseX, mouseY))
                .forEach(child -> child.renderToolTip(matrix, mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ClientUtil.checkChildren(children, child -> child.mouseClicked(mouseX, mouseY, button)) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return ClientUtil.checkChildren(children, child -> child.keyPressed(keyCode, scanCode, modifiers)) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        return ClientUtil.checkChildren(children, child -> child.charTyped(c, keyCode)) || super.charTyped(c, keyCode);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double mouseXOld, double mouseYOld) {
        children.forEach(element -> element.onDrag(mouseX, mouseY, mouseXOld, mouseYOld));
        super.onDrag(mouseX, mouseY, mouseXOld, mouseYOld);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        children.forEach(element -> element.onRelease(mouseX, mouseY));
        super.onRelease(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return ClientUtil.checkChildren(children, child -> child.mouseScrolled(mouseX, mouseY, delta)) || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public Font getFont() {
        return gui.getFont();
    }

    @Override
    public int getXSize() {
        return width;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || ClientUtil.checkChildren(children, child -> child.isMouseOver(mouseX, mouseY));
    }

    /**
     * Checks if the mouse is over this element, and if no windows are in the way.
     *
     * @param mouseX the current mouse X position
     * @param mouseY the current mouse Y position
     */
    public final boolean isMouseOverCheckWindow(double mouseX, double mouseY) {
        var isHovering = isMouseOver(mouseX, mouseY);
        if (isHovering) {
            final var window = gui.getWindowHovering(mouseX, mouseY);
            if (window != null && !window.childrenContainsElement(e -> e == this)) {
                isHovering = false;
            }
        }
        return isHovering;
    }

    /**
     * Renders this element's background. The given {@code matrix} will be translated to X -{@link #getGuiLeft()} and Y -{@link #getGuiTop()}.
     *
     * @param matrix      the {@link PoseStack} to render to
     * @param mouseX      the mouse's X position
     * @param mouseY      the mouse's Y position
     * @param partialTick the partial tick
     */
    public void drawBackground(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderButton(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
    }

    protected int getButtonTextColor(int mouseX, int mouseY) {
        return getFGColor();
    }

    protected void drawButtonText(PoseStack matrix, int mouseX, int mouseY) {
        final var text = getMessage();
        if (!text.getString().isEmpty()) {
            int color = getButtonTextColor(mouseX, mouseY) | Mth.ceil(alpha * 255.0F) << 24;
            drawCenteredTextScaledBound(matrix, text, width - 4, 0, height / 2F - 4, color);
        }
    }

    protected void renderBackgroundTexture(PoseStack matrix, ResourceLocation resource, int sideWidth, int sideHeight) {
        ClientUtil.renderBackgroundTexture(matrix, resource, sideWidth, sideHeight, x, y, width, height, 256, 256);
    }

    @Override
    public void playDownSound(@Nonnull SoundManager soundHandler) {
        if (playClickSound) {
            super.playDownSound(soundHandler);
        }
    }

    protected void playClickSound() {
        super.playDownSound(MINECRAFT.getSoundManager());
    }

    @Override
    public void drawCenteredTextScaledBound(PoseStack matrix, Component text, float maxLength, float x, float y, int color) {
        SpecialFontRenderer.super.drawCenteredTextScaledBound(matrix, text, maxLength, relativeX + x, relativeY + y, color);
    }

    @FunctionalInterface
    public interface Hoverable {

        void onHover(GuiElement element, PoseStack matrix, int mouseX, int mouseY);

        static Hoverable displayTooltip(Component... components) {
            return (element, matrix, mouseX, mouseY) -> element.gui.displayTooltips(matrix, mouseX, mouseY, components);
        }

        static Hoverable displayTooltip(Supplier<Component> component) {
            return (element, matrix, mouseX, mouseY) -> element.gui.displayTooltips(matrix, mouseX, mouseY, component.get());
        }
    }

    @FunctionalInterface
    public interface Clickable {

        void onClick(GuiElement element, int mouseX, int mouseY);
    }
}
