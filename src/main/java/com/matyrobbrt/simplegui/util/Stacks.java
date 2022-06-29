package com.matyrobbrt.simplegui.util;

import com.matyrobbrt.simplegui.inventory.SelectedWindowData;
import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Stacks {
    public static ItemStack withSize(ItemStack stack, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemHandlerHelper.copyStackWithSize(stack, size);
    }

    // quickMoveStack utils

    public static <S extends Slot & InsertableSlot> ItemStack insertItem(List<S> slots, @Nonnull ItemStack stack, @Nullable SelectedWindowData selectedWindow) {
        stack = insertItem(slots, stack, true, selectedWindow);
        return insertItem(slots, stack, false, selectedWindow);
    }

    public static <S extends Slot & InsertableSlot> ItemStack insertItem(List<S> slots, @Nonnull ItemStack stack, boolean ignoreEmpty,
                                                                         @Nullable SelectedWindowData selectedWindow) {
        return insertItem(slots, stack, ignoreEmpty, selectedWindow, Action.EXECUTE);
    }

    public static <S extends Slot & InsertableSlot> ItemStack insertItem(List<S> slots, @Nonnull ItemStack stack, boolean ignoreEmpty,
                                                                         @Nullable SelectedWindowData selectedWindow, Action action) {
        return insertItem(slots, stack, ignoreEmpty, false, selectedWindow, action);
    }

    @Nonnull
    public static <S extends Slot & InsertableSlot> ItemStack insertItem(List<S> slots, @Nonnull ItemStack stack, boolean ignoreEmpty, boolean checkAll,
                                                                         @Nullable SelectedWindowData selectedWindow, Action action) {
        if (stack.isEmpty()) {
            // Stack is empty, don't waste time on doing checks
            return stack;
        }
        for (final var slot : slots) {
            if ((!checkAll && ignoreEmpty != slot.hasItem()) || !slot.exists(selectedWindow)) continue;
            stack = slot.insertItem(stack, action);
            if (stack.isEmpty()) break;
        }
        return stack;
    }
}
