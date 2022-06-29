package com.matyrobbrt.simplegui.client.element.slot;

import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.client.VirtualSlotScreen;
import com.matyrobbrt.simplegui.client.element.window.Window;
import com.matyrobbrt.simplegui.inventory.slot.VirtualSlot;
import com.mojang.blaze3d.vertex.PoseStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleGuiVirtualSlot extends GuiSlot implements GuiVirtualSlot {

    private VirtualSlot virtualSlot;

    public SimpleGuiVirtualSlot(@Nullable Window window, Gui gui, int x, int y, VirtualSlot containerSlot) {
        this(gui, x, y);
        if (containerSlot != null) {
            updateVirtualSlot(window, containerSlot);
        }
    }

    public SimpleGuiVirtualSlot(Gui gui, int x, int y) {
        super(gui, x, y);
        setRenderHover(true);
    }

    @Override
    public boolean isChild(VirtualSlot virtualSlot) {
        return this.virtualSlot == virtualSlot;
    }

    public void updateVirtualSlot(@Nullable Window window, @Nonnull VirtualSlot virtualSlot) {
        this.virtualSlot = virtualSlot;
        this.virtualSlot.updatePosition(window, () -> relativeX + 1, () -> relativeY + 1);
    }

    @Override
    protected void drawContents(@Nonnull PoseStack matrix) {
        if (virtualSlot != null) {
            final var stack = virtualSlot.getStackToRender();
            if (!stack.isEmpty()) {
                final int xPos = x + borderSize(), yPos = y + borderSize();
                if (virtualSlot.shouldDrawOverlay()) {
                    fill(matrix, xPos, yPos, xPos + 16, yPos + 16, DEFAULT_HOVER_COLOR);
                }
                gui().renderItemWithOverlay(matrix, stack, xPos, yPos, 1, virtualSlot.getTooltipOverride());
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
            final var gui = gui();
            if (gui instanceof VirtualSlotScreen<?> screen && virtualSlot != null) {
                return screen.virtualSlotClicked(virtualSlot.getSlot(), button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}