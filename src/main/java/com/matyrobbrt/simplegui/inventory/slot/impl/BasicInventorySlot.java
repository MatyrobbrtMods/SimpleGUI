package com.matyrobbrt.simplegui.inventory.slot.impl;

import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.matyrobbrt.simplegui.inventory.ContentsListener;
import com.matyrobbrt.simplegui.inventory.slot.InventorySlot;
import com.matyrobbrt.simplegui.util.Action;
import com.matyrobbrt.simplegui.util.InteractionType;
import com.matyrobbrt.simplegui.util.Stacks;
import com.matyrobbrt.simplegui.util.Utils;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BasicInventorySlot implements InventorySlot {

    public static final int DEFAULT_LIMIT = 64;
    public static final String TAG_ITEM = "Item";
    public static final String TAG_COUNT = "CountOverride";

    public static BasicInventorySlot at(@Nullable ContentsListener listener, int x, int y) {
        return at(e -> true, listener, x, y);
    }

    public static BasicInventorySlot at(Predicate<@NonNull ItemStack> validator, @Nullable ContentsListener listener, int x, int y) {
        Objects.requireNonNull(validator, "Item validity check cannot be null");
        return new BasicInventorySlot(InteractionType.Predicate.TRUE, InteractionType.Predicate.TRUE, validator, listener, x, y);
    }

    public static BasicInventorySlot at(Predicate<@NonNull ItemStack> canExtract, Predicate<@NonNull ItemStack> canInsert, @Nullable ContentsListener listener, int x, int y) {
        Objects.requireNonNull(canExtract, "Extraction validity check cannot be null");
        Objects.requireNonNull(canInsert, "Insertion validity check cannot be null");
        return new BasicInventorySlot(canExtract, canInsert, e -> true, listener, x, y);
    }

    public static BasicInventorySlot at(InteractionType.Predicate canExtract,
                                        InteractionType.Predicate canInsert, @Nullable ContentsListener listener, int x, int y) {
        Objects.requireNonNull(canExtract, "Extraction validity check cannot be null");
        Objects.requireNonNull(canInsert, "Insertion validity check cannot be null");
        return new BasicInventorySlot(canExtract, canInsert, e -> true, listener, x, y);
    }

    protected ItemStack current = ItemStack.EMPTY;
    private final InteractionType.Predicate canExtract;
    private final InteractionType.Predicate canInsert;
    private final Predicate<@NonNull ItemStack> validator;
    private final int limit;
    @Nullable
    private final ContentsListener listener;
    public final int x;
    public final int y;
    protected boolean obeyStackLimit = false;

    protected BasicInventorySlot(Predicate<@NonNull ItemStack> canExtract, Predicate<@NonNull ItemStack> canInsert, Predicate<@NonNull ItemStack> validator,
                                 @Nullable ContentsListener listener, int x, int y) {
        this((stack, type) -> type == InteractionType.MANUAL || canExtract.test(stack), (stack, InteractionType) -> canInsert.test(stack),
                validator, listener, x, y);
    }

    protected BasicInventorySlot(InteractionType.Predicate canExtract, InteractionType.Predicate canInsert,
                                 Predicate<@NonNull ItemStack> validator, @Nullable ContentsListener listener, int x, int y) {
        this(DEFAULT_LIMIT, canExtract, canInsert, validator, listener, x, y);
    }

    protected BasicInventorySlot(int limit, InteractionType.Predicate canExtract,
                                 InteractionType.Predicate canInsert, Predicate<@NonNull ItemStack> validator, @Nullable ContentsListener listener, int x, int y) {
        this.limit = limit;
        this.canExtract = canExtract;
        this.canInsert = canInsert;
        this.validator = validator;
        this.listener = listener;
        this.x = x;
        this.y = y;
    }

    @Override
    public ItemStack getStack() {
        return current;
    }

    @Override
    public void setStack(ItemStack stack) {
        setStack(stack, true);
    }

    protected void setStackUnchecked(ItemStack stack) {
        setStack(stack, false);
    }

    private void setStack(ItemStack stack, boolean validateStack) {
        if (stack.isEmpty()) {
            if (current.isEmpty()) {
                return;
            }
            current = ItemStack.EMPTY;
        } else if (!validateStack || isItemValid(stack)) {
            current = stack.copy();
        } else {
            throw new RuntimeException("Invalid stack for slot: " + Utils.getRegistryName(stack.getItem()) + " " + stack.getCount() + " " + stack.getTag());
        }
        onContentsChanged();
    }

    @Override
    public ItemStack insertItem(ItemStack stack, Action action, InteractionType InteractionType) {
        if (stack.isEmpty() || !isItemValid(stack) || !canInsert.canExecute(stack, InteractionType)) {
            return stack;
        }
        final var needed = getLimit(stack) - getCount();
        if (needed <= 0) {
            return stack;
        }
        boolean sameType = false;
        if (isEmpty() || (sameType = ItemHandlerHelper.canItemStacksStack(current, stack))) {
            final var toAdd = Math.min(stack.getCount(), needed);
            if (action == Action.EXECUTE) {
                if (sameType) {
                    current.grow(toAdd);
                    onContentsChanged();
                } else {
                    setStackUnchecked(Stacks.withSize(stack, toAdd));
                }
            }
            return Stacks.withSize(stack, stack.getCount() - toAdd);
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int amount, Action action, InteractionType InteractionType) {
        if (isEmpty() || amount < 1 || !canExtract.canExecute(current, InteractionType)) {
            return ItemStack.EMPTY;
        }
        final var currentAmount = Math.min(getCount(), current.getMaxStackSize());
        if (currentAmount < amount) {
            amount = currentAmount;
        }
        final var toReturn = Stacks.withSize(current, amount);
        if (action == Action.EXECUTE) {
            current.shrink(amount);
            onContentsChanged();
        }
        return toReturn;
    }

    @Override
    public int getLimit(ItemStack stack) {
        return obeyStackLimit && !stack.isEmpty() ? Math.min(limit, stack.getMaxStackSize()) : limit;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return validator.test(stack);
    }

    public boolean isItemValidForInsertion(ItemStack stack, InteractionType InteractionType) {
        return validator.test(stack) && canInsert.canExecute(stack, InteractionType);
    }

    @Override
    public void onContentsChanged() {
        if (listener != null) {
            listener.onContentsChanged();
        }
    }

    @Nonnull
    @Override
    public Slot createContainerSlot() {
        return new InventoryContainerSlot(this, x, y, this::setStackUnchecked);
    }

    @Override
    public int setStackSize(int amount, final Action action) {
        if (isEmpty()) {
            return 0;
        } else if (amount <= 0) {
            if (action == Action.EXECUTE) {
                empty();
            }
            return 0;
        }
        final var maxStackSize = getLimit(current);
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }
        if (getCount() == amount || action == Action.SIMULATE) {
            return amount;
        }
        current.setCount(amount);
        onContentsChanged();
        return amount;
    }

    @Override
    public int growStack(int amount, final Action action) {
        int current = getCount();
        if (amount > 0) {
            amount = Math.min(amount, getLimit(this.current));
        }
        final var newSize = setStackSize(current + amount, action);
        return newSize - current;
    }

    @Override
    public boolean isEmpty() {
        return current.isEmpty();
    }

    @Override
    public int getCount() {
        return current.getCount();
    }

    @Override
    public CompoundTag serializeNBT() {
        final var nbt = new CompoundTag();
        if (!isEmpty()) {
            nbt.put(TAG_ITEM, current.save(new CompoundTag()));
            if (getCount() > current.getMaxStackSize()) {
                nbt.putInt(TAG_COUNT, getCount());
            }
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        var stack = ItemStack.EMPTY;
        if (nbt.contains(TAG_ITEM, Tag.TAG_COMPOUND)) {
            stack = ItemStack.of(nbt.getCompound(TAG_ITEM));
            if (nbt.contains(TAG_COUNT, Tag.TAG_INT))
                stack.setCount(nbt.getInt(TAG_COUNT));
        }
        setStackUnchecked(stack);
    }
}