package com.matyrobbrt.simplegui.inventory.slot.impl;

import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import com.matyrobbrt.simplegui.inventory.slot.InventorySlot;
import com.matyrobbrt.simplegui.util.Action;
import com.matyrobbrt.simplegui.util.InteractionType;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class InventoryContainerSlot extends Slot implements InsertableSlot {

    private static final Container EMPTY = new SimpleContainer(0);
    private final Consumer<ItemStack> uncheckedSetter;
    private final BasicInventorySlot slot;

    public InventoryContainerSlot(BasicInventorySlot slot, int x, int y,
                                  Consumer<ItemStack> uncheckedSetter) {
        super(EMPTY, 0, x, y);
        this.slot = slot;
        this.uncheckedSetter = uncheckedSetter;
    }

    public InventorySlot getInventorySlot() {
        return slot;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(@Nonnull final ItemStack stack, final Action action) {
        final var remainder = slot.insertItem(stack, action, InteractionType.MANUAL);
        if (action == Action.EXECUTE && stack.getCount() != remainder.getCount()) {
            setChanged();
        }
        return remainder;
    }

    @Override
    public boolean mayPlace(@Nonnull final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot.isEmpty()) {
            // If the slot is currently empty, try simulating insertion so
            // predicates are checked
            return insertItem(stack, Action.SIMULATE).getCount() < stack.getCount();
        }
        // If it isn't empty, check if an item can be extracted
        if (slot.extractItem(1, Action.SIMULATE, InteractionType.MANUAL).isEmpty()) {
            return false;
        }
        // Check if the item can be inserted ignoring the current contents
        return slot.isItemValidForInsertion(stack, InteractionType.MANUAL);
    }

    @Nonnull
    @Override
    public ItemStack getItem() {
        return slot.getStack();
    }

    @Override
    public boolean hasItem() {
        return !slot.isEmpty();
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        uncheckedSetter.accept(stack);
        setChanged();
    }

    @Override
    public void initialize(@NotNull ItemStack stack) {
        uncheckedSetter.accept(stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        slot.onContentsChanged();
    }

    @Override
    public void onQuickCraft(@Nonnull final ItemStack current, @Nonnull final ItemStack newStack) {
        final var change = newStack.getCount() - current.getCount();
        if (change > 0) {
            slot.onContentsChanged();
            onQuickCraft(newStack, change);
        }
    }

    @Override
    public int getMaxStackSize() {
        return slot.getLimit(ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        return slot.getLimit(stack);
    }

    @Override
    public boolean mayPickup(@Nonnull Player player) {
        return !slot.extractItem(1, Action.SIMULATE, InteractionType.MANUAL).isEmpty();
    }

    @Nonnull
    @Override
    public ItemStack remove(int amount) {
        return slot.extractItem(amount, Action.EXECUTE, InteractionType.MANUAL);
    }
}
