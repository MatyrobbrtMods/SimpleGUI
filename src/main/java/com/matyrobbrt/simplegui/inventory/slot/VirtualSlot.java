package com.matyrobbrt.simplegui.inventory.slot;

import com.matyrobbrt.simplegui.annotations.CallOnlyOn;
import com.matyrobbrt.simplegui.client.element.window.Window;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.IntSupplier;

public interface VirtualSlot {

    @Nullable
    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    GuiWindow getLinkedWindow();

    int getActualX();

    int getActualY();

    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    void updatePosition(@Nullable Window window, IntSupplier xPositionSupplier, IntSupplier yPositionSupplier);

    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    void updateRenderInfo(@Nonnull ItemStack stackToRender, boolean shouldDrawOverlay, @Nullable String tooltipOverride);

    @Nonnull
    ItemStack getStackToRender();

    boolean shouldDrawOverlay();

    @Nullable
    String getTooltipOverride();

    Slot getSlot();

    /**
     * Marker to avoid loading client classes on the server.
     */
    interface GuiWindow {

    }
}
