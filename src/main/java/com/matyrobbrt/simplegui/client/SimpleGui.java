package com.matyrobbrt.simplegui.client;

import static com.matyrobbrt.simplegui.client.element.window.Window.BASE_BACKGROUND;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.matyrobbrt.simplegui.annotations.CheckCaller;
import com.matyrobbrt.simplegui.annotations.PrivateOverride;
import com.matyrobbrt.simplegui.client.element.GuiElement;
import com.matyrobbrt.simplegui.client.element.slot.SlotFactory;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.client.element.slot.GuiVirtualSlot;
import com.matyrobbrt.simplegui.inventory.SelectedWindowData;
import com.matyrobbrt.simplegui.inventory.SimpleMenu;
import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import com.matyrobbrt.simplegui.inventory.slot.VirtualSlot;
import com.matyrobbrt.simplegui.util.col.LRU;
import com.matyrobbrt.simplegui.util.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class SimpleGui<C extends AbstractContainerMenu> extends VirtualSlotScreen<C> implements Gui, SpecialFontRenderer {

    protected final LRU<Window> windows = new LRU<>();
    protected final List<GuiElement> focusListeners = new ArrayList<>();
    public boolean switchingToJEI;

    private boolean hasClicked = false;

    public static int maxZOffset;
    private int maxZOffsetNoWindows;

    protected SimpleGui(C container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    public void removed() {
        if (!switchingToJEI) {
            windows.forEach(Window::close);
            super.removed();
        }
    }

    @Override
    protected void init() {
        super.init();
        addGuiElements();
    }

    protected void addSlots(boolean includeVirtual) {
        final var size = menu.slots.size();
        for (var i = 0; i < size; i++) {
            final var slot = menu.slots.get(i);
            if (slot instanceof VirtualSlot && !includeVirtual) continue;
            final var gui = SlotFactory.Registry.create(slot, this, slot.x - 1, slot.y - 1);
            if (gui != null) addRenderableWidget(gui);
        }
    }

    /**
     * Called to add gui elements to the GUI. Add elements before calling super if they should be before the slots, and after if they should be after the slots. Most
     * elements can and should be added after the slots.
     */
    protected void addGuiElements() {
    }

    /**
     * Like {@link #addRenderableWidget(GuiEventListener)}, except doesn't add the element as narratable.
     */
    @CanIgnoreReturnValue
    @SuppressWarnings("unchecked")
    protected <T extends GuiElement> T addElement(T element) {
        renderables.add(element);
        ((List<GuiEventListener>) children()).add(element);
        return element;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        children().stream().filter(GuiElement.class::isInstance).map(child -> (GuiElement) child).forEach(GuiElement::tick);
        windows.forEach(Window::tick);
    }

    @Override
    public void addFocusListener(GuiElement element) {
        focusListeners.add(element);
    }

    @Override
    public void removeFocusListener(GuiElement element) {
        focusListeners.remove(element);
    }

    @Override
    public void focusChange(GuiElement changed) {
        focusListeners.stream().filter(e -> e != changed).forEach(e -> e.setFocused(false));
    }

    @Override
    public void incrementFocus(GuiElement current) {
        int index = focusListeners.indexOf(current);
        if (index != -1) {
            GuiElement next = focusListeners.get((index + 1) % focusListeners.size());
            next.setFocused(true);
            focusChange(next);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        return getWindowHovering(mouseX, mouseY) == null && super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton);
    }

    @PrivateOverride("init")
    public void actuallyInit(@Nonnull Minecraft minecraft, int width, int height) {
        switchingToJEI = false;

        record PreviousElement(int index, GuiElement element) {
        }
        final List<PreviousElement> prevElements = new ArrayList<>();
        for (int i = 0; i < children().size(); i++) {
            GuiEventListener widget = children().get(i);
            if (widget instanceof GuiElement element && element.hasPersistentData()) {
                prevElements.add(new PreviousElement(i, element));
            }
        }

        focusListeners.removeIf(element -> !element.isOverlay);
        int prevLeft = leftPos, prevTop = topPos;
        super.init(minecraft, width, height);

        windows.forEach(window -> window.resize(prevLeft, prevTop, leftPos, topPos));

        prevElements.forEach((final PreviousElement e) -> {
            if (e.index() < children().size()) {
                final var widget = children().get(e.index());
                if (widget.getClass() == e.element().getClass()) {
                    ((GuiElement) widget).syncFrom(e.element());
                }
            }
        });
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        final var modelViewStack = RenderSystem.getModelViewStack();

        matrix.translate(0, 0, 300);
        modelViewStack.translate(-leftPos, -topPos, 0);
        RenderSystem.applyModelViewMatrix();

        children().stream().filter(GuiElement.class::isInstance).forEach(c -> ((GuiElement) c).doDrawBackground(matrix, mouseX, mouseY, ClientUtil.getPartialTicks()));
        modelViewStack.translate(leftPos, topPos, 0);
        RenderSystem.applyModelViewMatrix();
        drawForegroundText(matrix, mouseX, mouseY);

        int zOffset = 200;
        maxZOffsetNoWindows = maxZOffset = zOffset;
        for (GuiEventListener widget : children()) {
            if (widget instanceof GuiElement element) {
                matrix.pushPose();
                element.onRenderForeground(matrix, mouseX, mouseY, zOffset, zOffset);
                matrix.popPose();
            }
        }
        maxZOffsetNoWindows = maxZOffset;
        int windowSeparation = 150;

        for (LRU<Window>.LRUIterator iter = windows.descendingIterator(); iter.hasNext(); ) {
            Window overlay = iter.next();
            zOffset = maxZOffset + windowSeparation;
            matrix.pushPose();
            overlay.onRenderForeground(matrix, mouseX, mouseY, zOffset, zOffset);
            if (iter.hasNext()) {
                overlay.renderBlur(matrix);
            }
            matrix.popPose();
        }
        GuiElement tooltipElement = getWindowHovering(mouseX, mouseY);
        if (tooltipElement == null) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener widget = children().get(i);
                if (widget instanceof GuiElement element && element.isMouseOver(mouseX, mouseY)) {
                    tooltipElement = element;
                    break;
                }
            }
        }

        modelViewStack.translate(0, 0, maxZOffset);

        modelViewStack.translate(-leftPos, -topPos, 0);
        RenderSystem.applyModelViewMatrix();
        if (tooltipElement != null) {
            tooltipElement.renderToolTip(matrix, mouseX, mouseY);
        }
        renderTooltip(matrix, mouseX, mouseY);
        modelViewStack.translate(leftPos, topPos, 0);

        modelViewStack.translate(0, 0, 200);
        RenderSystem.applyModelViewMatrix();

        maxZOffsetNoWindows = -(maxZOffset - windowSeparation * windows.size());
    }

    public <T extends VirtualSlot> List<T> findVirtualSlots(Class<? extends T> clazz, @Nullable SelectedWindowData windowData) {
        //noinspection unchecked
        return menu.slots.stream().filter(it -> clazz.isInstance(it) && it instanceof InsertableSlot insertable && insertable.exists(windowData))
                .map(it -> (T) it)
                .toList();
    }

    protected void drawForegroundText(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
    }

    @Nonnull
    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        Window window = getWindowHovering(mouseX, mouseY);
        return window == null ? super.getChildAt(mouseX, mouseY) : Optional.of(window);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        hasClicked = true;
        final var top = windows.isEmpty() ? null : windows.iterator().next();
        final var focusedOpt = windows.stream().filter(overlay -> overlay.mouseClicked(mouseX, mouseY, button))
                .findFirst();
        if (focusedOpt.isPresent()) {
            final var focused = focusedOpt.get();
            if (windows.contains(focused)) {
                setFocused(focused);
                if (button == 0) {
                    setDragging(true);
                }
                if (top != focused) {
                    top.onFocusLost();
                    windows.moveUp(focused);
                    focused.onFocused();
                }
            }
            return true;
        }

        for (int i = children().size() - 1; i >= 0; i--) {
            GuiEventListener listener = children().get(i);
            if (listener.mouseClicked(mouseX, mouseY, button)) {
                setFocused(listener);
                if (button == 0) {
                    setDragging(true);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (hasClicked) {
            windows.forEach(w -> w.onRelease(mouseX, mouseY));
            return super.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return windows.stream().anyMatch(window -> window.keyPressed(keyCode, scanCode, modifiers)) ||
                ClientUtil.checkChildren(children(), child -> child.keyPressed(keyCode, scanCode, modifiers)) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        return windows.stream().anyMatch(window -> window.charTyped(c, keyCode)) || ClientUtil.checkChildren(children(), child -> child.charTyped(c, keyCode)) ||
                super.charTyped(c, keyCode);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double pDragX, double pDragY) {
        super.mouseDragged(mouseX, mouseY, button, pDragX, pDragY);
        return getFocused() != null && isDragging() && button == 0 && getFocused().mouseDragged(mouseX, mouseY, button, pDragX, pDragY);
    }

    @Nullable
    @Override
    protected Slot findActualSlot(double mouseX, double mouseY) {
        var checkedWindow = false;
        var overNoButtons = false;
        Window window = null;
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            var xPos = slot.x;
            var yPos = slot.y;
            var virtual = false;
            if (slot instanceof VirtualSlot virtualSlot) {
                virtual = true;
                if (!isVirtualSlotAvailable(virtualSlot)) {
                    continue;
                }
                xPos = virtualSlot.getActualX();
                yPos = virtualSlot.getActualY();
            }
            if (super.isHovering(xPos, yPos, 16, 16, mouseX, mouseY)) {
                if (!checkedWindow) {
                    checkedWindow = true;
                    window = getWindowHovering(mouseX, mouseY);
                    overNoButtons = overNoButtons(window, mouseX, mouseY);
                }
                if (overNoButtons && slot.isActive()) {
                    if (window == null) {
                        return slot;
                    } else if (virtual && window.childrenContainsElement(element -> element instanceof GuiVirtualSlot v && v.isChild((VirtualSlot) slot))) {
                        return slot;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isMouseOverSlot(@Nonnull Slot slot, double mouseX, double mouseY) {
        if (slot instanceof VirtualSlot virtual) {
            if (isVirtualSlotAvailable(virtual)) {
                final var xPos = virtual.getActualX();
                final var yPos = virtual.getActualY();
                if (super.isHovering(xPos, yPos, 16, 16, mouseX, mouseY)) {
                    final var window = getWindowHovering(mouseX, mouseY);
                    if (window == null || window.childrenContainsElement(element -> element instanceof GuiVirtualSlot v && v.isChild(virtual))) {
                        return overNoButtons(window, mouseX, mouseY);
                    }
                }
            }
            return false;
        }
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    private boolean overNoButtons(@Nullable Window window, double mouseX, double mouseY) {
        if (window == null) {
            return children().stream().noneMatch(button -> button.isMouseOver(mouseX, mouseY));
        }
        return !window.childrenContainsElement(e -> e.isMouseOver(mouseX, mouseY));
    }

    private boolean isVirtualSlotAvailable(VirtualSlot virtualSlot) {
        return !(virtualSlot.getLinkedWindow() instanceof Window linked) || windows.contains(linked);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return super.isHovering(x, y, width, height, mouseX, mouseY) && getWindowHovering(mouseX, mouseY) == null &&
                overNoButtons(null, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@Nonnull PoseStack matrix, float partialTick, int mouseX, int mouseY) {
        ClientUtil.resetColour();
        if (width < 8 || height < 8) {
            Utils.LOGGER.warn("Gui: {}, was too small to draw the background of. Unable to draw a background for a gui smaller than 8 by 8.", getClass());
            return;
        }
        renderBackgroundTexture(matrix, partialTick, mouseX, mouseY);
    }

    protected void renderBackgroundTexture(PoseStack matrix, float partialTicks, int mouseX, int mouseY) {
        ClientUtil.renderBackgroundTexture(matrix, BASE_BACKGROUND, 4, 4, leftPos, topPos, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void render(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(0, 0, -500);
        RenderSystem.applyModelViewMatrix();
        matrix.pushPose();
        renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        matrix.popPose();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public void renderTooltip(@Nonnull PoseStack poseStack, @Nonnull List<Component> tooltips, @Nonnull Optional<TooltipComponent> visualTooltipComponent, int mouseX,
                              int mouseY) {
        adjustTooltipZ(poseStack, pose -> super.renderTooltip(pose, tooltips, visualTooltipComponent, mouseX, mouseY));
    }

    @Override
    public void renderComponentTooltip(@Nonnull PoseStack poseStack, @Nonnull List<Component> tooltips, int mouseX, int mouseY) {
        adjustTooltipZ(poseStack, pose -> super.renderComponentTooltip(pose, tooltips, mouseX, mouseY));
    }

    @Override
    public void renderComponentTooltip(@Nonnull PoseStack poseStack, @Nonnull List<? extends FormattedText> tooltips, int mouseX, int mouseY, @Nullable Font font,
                                       @Nonnull ItemStack stack) {
        adjustTooltipZ(poseStack, pose -> super.renderComponentTooltip(pose, tooltips, mouseX, mouseY, font, stack));
    }

    @Override
    public void renderTooltip(@Nonnull PoseStack poseStack, @Nonnull List<? extends FormattedCharSequence> tooltips, int mouseX, int mouseY) {
        adjustTooltipZ(poseStack, pose -> super.renderTooltip(pose, tooltips, mouseX, mouseY));
    }

    private void adjustTooltipZ(@Nonnull PoseStack poseStack, @Nonnull Consumer<PoseStack> tooltipRender) {
        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.translate(0, 0, -maxZOffsetNoWindows - 1);
        modelViewStack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();
        tooltipRender.accept(new PoseStack());
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public void renderItemTooltip(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis) {
        renderTooltip(matrix, stack, xAxis, yAxis);
    }

    @Override
    public void renderItemTooltipWithExtra(PoseStack matrix, @Nonnull ItemStack stack, int xAxis, int yAxis, List<Component> toAppend) {
        if (toAppend.isEmpty()) {
            renderItemTooltip(matrix, stack, xAxis, yAxis);
        } else {
            final var tooltip = new ArrayList<>(getTooltipFromItem(stack));
            tooltip.addAll(toAppend);
            renderTooltip(matrix, tooltip, stack.getTooltipImage(), xAxis, yAxis, stack);
        }
    }

    @Override
    public @NotNull ItemRenderer getItemRenderer() {
        return itemRenderer;
    }

    @Override
    public boolean currentlyQuickCrafting() {
        return isQuickCrafting && !quickCraftSlots.isEmpty();
    }

    @Override
    public void addWindow(final Window window) {
        final var top = windows.isEmpty() ? null : windows.iterator().next();
        if (top != null) {
            top.onFocusLost();
        }
        windows.add(window);
        window.init();
        window.onFocused();
    }

    @Override
    public void removeWindow(final Window window) {
        if (!windows.isEmpty()) {
            final var top = windows.iterator().next();
            windows.remove(window);
            if (window == top) {
                window.onFocusLost();
                final var newTop = windows.isEmpty() ? null : windows.iterator().next();
                if (newTop == null) {
                    lastWindowRemoved();
                } else {
                    newTop.onFocused();
                }
                setFocused(newTop);
            }
        }
    }

    protected void lastWindowRemoved() {
        if (menu instanceof SimpleMenu container) {
            container.setSelectedWindow(null);
        }
    }

    @Override
    public void setSelectedWindow(SelectedWindowData selectedWindow) {
        if (menu instanceof SimpleMenu container) {
            container.setSelectedWindow(selectedWindow);
        }
    }

    @Nullable
    @Override
    public Window getWindowHovering(double mouseX, double mouseY) {
        return windows.stream().filter(w -> w.isMouseOver(mouseX, mouseY)).findFirst().orElse(null);
    }

    public Collection<Window> getWindows() {
        return windows;
    }
}
