package com.matyrobbrt.simplegui.inventory.slot;

import com.matyrobbrt.simplegui.inventory.SelectedWindowData;
import com.matyrobbrt.simplegui.util.Action;
import com.matyrobbrt.simplegui.util.Stacks;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface InsertableSlot {
    @Nonnull
    ItemStack insertItem(@Nonnull ItemStack stack, Action action);

    /**
     * Used for determining if this slot can merge with the given stack when the stack is double-clicked.
     */
    default boolean canMergeWith(@Nonnull ItemStack stack) {
        return true;
    }

    /**
     * Used for determining if this slot "exists" when a given window is selected.
     *
     * @param windowData Data for currently selected "popup" window or null if there is no window visible.
     */
    default boolean exists(@Nullable SelectedWindowData windowData) {
        return true;
    }

    class Impl extends Slot implements InsertableSlot {
        public Impl(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }

        @Nonnull
        @Override
        public ItemStack insertItem(@Nonnull ItemStack stack, Action action) {
            if (stack.isEmpty() || !mayPlace(stack)) {
                //TODO: Should we even be checking isItemValid
                //"Fail quick" if the given stack is empty or we are not valid for the slot
                return stack;
            }
            ItemStack current = getItem();
            int needed = getMaxStackSize(stack) - current.getCount();
            if (needed <= 0) {
                //Fail if we are a full slot
                return stack;
            }
            if (current.isEmpty() || ItemHandlerHelper.canItemStacksStack(current, stack)) {
                int toAdd = Math.min(stack.getCount(), needed);
                if (action == Action.EXECUTE) {
                    //If we want to actually insert the item, then update the current item
                    //Set the stack to our new stack (we have no simple way to increment the stack size) so we have to set it instead of being able to just grow it
                    set(Stacks.withSize(stack, current.getCount() + toAdd));
                }
                return Stacks.withSize(stack, stack.getCount() - toAdd);
            }
            //If we didn't accept this item, then just return the given stack
            return stack;
        }
    }
}
