package com.matyrobbrt.simplegui.client;

import com.matyrobbrt.simplegui.annotations.Accessor;
import com.matyrobbrt.simplegui.annotations.PrivateInSuper;
import com.matyrobbrt.simplegui.annotations.PrivateOverride;
import com.matyrobbrt.simplegui.inventory.slot.VirtualSlot;
import com.matyrobbrt.simplegui.util.Stacks;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Accessor.Holder(AbstractContainerScreen.class)
public abstract class VirtualSlotScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public VirtualSlotScreen(T container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    protected abstract boolean isMouseOverSlot(@Nonnull Slot slot, double mouseX, double mouseY);

    @Accessor
    protected ItemStack getDraggingItem() {
        return ItemStack.EMPTY;
    }
    @Accessor
    protected void setDraggingItem(ItemStack draggingItem) {}

    @Nullable
    @PrivateOverride("findSlot")
    protected Slot findActualSlot(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            // Make sure to only return the slot if the mouse is over the slot
            if (slot.isActive() && isMouseOverSlot(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    @PrivateOverride("isHovering")
    private boolean isActuallyHovering(@Nonnull Slot slot, double mouseX, double mouseY) {
        final var mouseOver = isMouseOverSlot(slot, mouseX, mouseY);
        if (mouseOver && slot instanceof VirtualSlot) {
            if (hoveredSlot == null && slot.isActive()) {
                hoveredSlot = slot;
            }
            return false;
        }
        return mouseOver;
    }

    @Override
    @PrivateInSuper
    protected final void renderFloatingItem(@Nonnull ItemStack stack, int x, int y, @Nullable String altText) {
        if (!stack.isEmpty()) {
            if (stack == this.snapbackItem && this.snapbackEnd instanceof VirtualSlot returningVirtualSlot) {
                final float f = (float) (Util.getMillis() - this.snapbackTime) / 100.0F;
                if (f >= 1.0F) {
                    this.snapbackItem = ItemStack.EMPTY;
                    return;
                }
                final int xOffset = returningVirtualSlot.getActualX() - this.snapbackStartX,
                        yOffset = returningVirtualSlot.getActualY() - this.snapbackStartY;
                x = this.snapbackStartX + (int) (xOffset * f);
                y = this.snapbackStartY + (int) (yOffset * f);
            }
            // noinspection ConstantConditions
            super.renderFloatingItem(stack, x, y, altText);
        }
    }

    @Override
    @PrivateInSuper
    protected final void renderSlot(@Nonnull PoseStack matrixStack, @Nonnull Slot slot) {
        if (!(slot instanceof VirtualSlot virtualSlot)) {
            super.renderSlot(matrixStack, slot);
            return;
        }
        var currentStack = slot.getItem();
        var shouldDrawOverlay = false;
        final var skipStackRendering = slot == this.clickedSlot && !getDraggingItem().isEmpty() && !this.isSplittingStack;
        // noinspection ConstantConditions
        final var heldStack = getMinecraft().player.containerMenu.getCarried();
        String s = null;
        if (slot == this.clickedSlot && !getDraggingItem().isEmpty() && this.isSplittingStack && !currentStack.isEmpty()) {
            currentStack = Stacks.withSize(currentStack, currentStack.getCount() / 2);
        } else if (isQuickCrafting && quickCraftSlots.contains(slot) && !heldStack.isEmpty()) {
            if (quickCraftSlots.size() == 1) {
                return;
            }
            if (AbstractContainerMenu.canItemQuickReplace(slot, heldStack, true) && this.menu.canDragTo(slot)) {
                currentStack = heldStack.copy();
                shouldDrawOverlay = true;
                AbstractContainerMenu.getQuickCraftSlotCount(quickCraftSlots, this.quickCraftingType, currentStack, slot.getItem().isEmpty() ? 0 : slot.getItem().getCount());
                int k = Math.min(currentStack.getMaxStackSize(), slot.getMaxStackSize(currentStack));
                if (currentStack.getCount() > k) {
                    s = ChatFormatting.YELLOW.toString() + k;
                    currentStack.setCount(k);
                }
            } else {
                quickCraftSlots.remove(slot);
                recalculateQuickCraftRemaining();
            }
        }
        // Let the slot render itself
        virtualSlot.updateRenderInfo(skipStackRendering ? ItemStack.EMPTY : currentStack, shouldDrawOverlay, s);
    }

    public boolean virtualSlotClicked(@Nonnull Slot slot, int button) {
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
        // noinspection ConstantConditions
        boolean pickBlockButton = minecraft.options.keyPickItem.isActiveAndMatches(mouseKey);
        long time = Util.getMillis();
        this.doubleclick = this.lastClickSlot == slot && time - this.lastClickTime < 250L && this.lastClickButton == button;
        this.skipNextRelease = false;
        if (button != 0 && button != 1 && !pickBlockButton) {
            checkHotbarMouseClicked(button);
        } else if (slot.index != -1) {
            if (minecraft.options.touchscreen().get()) {
                if (slot.hasItem()) {
                    this.clickedSlot = slot;
                    setDraggingItem(ItemStack.EMPTY);
                    this.isSplittingStack = button == 1;
                } else {
                    this.clickedSlot = null;
                }
            } else if (!this.isQuickCrafting) {
                // noinspection ConstantConditions
                if (minecraft.player.containerMenu.getCarried().isEmpty()) {
                    if (pickBlockButton) {
                        this.slotClicked(slot, slot.index, button, ClickType.CLONE);
                    } else {
                        ClickType clicktype = ClickType.PICKUP;
                        if (Screen.hasShiftDown()) {
                            this.lastQuickMoved = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                            clicktype = ClickType.QUICK_MOVE;
                        }
                        this.slotClicked(slot, slot.index, button, clicktype);
                    }
                    this.skipNextRelease = true;
                } else {
                    this.isQuickCrafting = true;
                    this.quickCraftingButton = button;
                    this.quickCraftSlots.clear();
                    if (button == 0) {
                        this.quickCraftingType = 0;
                    } else if (button == 1) {
                        this.quickCraftingType = 1;
                    } else {
                        this.quickCraftingType = 2;
                    }
                }
            }
        }
        this.lastClickSlot = slot;
        this.lastClickTime = time;
        this.lastClickButton = button;
        return true;
    }
}
