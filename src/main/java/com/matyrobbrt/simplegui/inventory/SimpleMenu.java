package com.matyrobbrt.simplegui.inventory;

import com.matyrobbrt.simplegui.annotations.CallOnlyOn;
import com.matyrobbrt.simplegui.inventory.slot.InsertableSlot;
import com.matyrobbrt.simplegui.inventory.slot.impl.ArmorSlot;
import com.matyrobbrt.simplegui.inventory.slot.impl.HotbarSlot;
import com.matyrobbrt.simplegui.inventory.slot.impl.InventoryContainerSlot;
import com.matyrobbrt.simplegui.inventory.slot.impl.MainInvSlot;
import com.matyrobbrt.simplegui.inventory.slot.impl.OffhandSlot;
import com.matyrobbrt.simplegui.network.SelectWindowPacket;
import com.matyrobbrt.simplegui.network.SimpleGuiNetwork;
import com.matyrobbrt.simplegui.util.Stacks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class SimpleMenu extends AbstractContainerMenu {

    protected final Inventory inv;
    protected final List<InventoryContainerSlot> inventoryContainerSlots = new ArrayList<>();
    protected final List<ArmorSlot> armorSlots = new ArrayList<>();
    protected final List<MainInvSlot> mainInventorySlots = new ArrayList<>();
    protected final List<HotbarSlot> hotBarSlots = new ArrayList<>();
    protected final List<OffhandSlot> offhandSlots = new ArrayList<>();

    /**
     * Keeps track of which window the player has open. Only used on the client, so doesn't need to keep track of other players.
     */
    @Nullable
    private SelectedWindowData selectedWindow;

    /**
     * Only used on the server
     */
    private Map<UUID, SelectedWindowData> selectedWindows;

    protected SimpleMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory inv) {
        super(pMenuType, pContainerId);
        this.inv = inv;
        if (!isClientSide())
            selectedWindows = new HashMap<>(1);
    }

    public boolean isClientSide() {
        return inv.player.level.isClientSide;
    }

    @Override
    public boolean canTakeItemForPickAll(@Nonnull ItemStack stack, @Nonnull Slot slot) {
        if (slot instanceof final InsertableSlot insertableSlot) {
            if (!insertableSlot.canMergeWith(stack)) {
                return false;
            }
            final var selectedWindow = isClientSide() ? getSelectedWindow() : getSelectedWindow(inv.player.getUUID());
            return insertableSlot.exists(selectedWindow) && super.canTakeItemForPickAll(stack, slot);
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Nonnull
    @Override
    protected Slot addSlot(@Nonnull Slot slot) {
        super.addSlot(slot);
        if (slot instanceof final InventoryContainerSlot inventorySlot) {
            inventoryContainerSlots.add(inventorySlot);
        } else if (slot instanceof final ArmorSlot armorSlot) {
            armorSlots.add(armorSlot);
        } else if (slot instanceof final MainInvSlot inventorySlot) {
            mainInventorySlots.add(inventorySlot);
        } else if (slot instanceof final HotbarSlot hotBarSlot) {
            hotBarSlots.add(hotBarSlot);
        } else if (slot instanceof final OffhandSlot offhandSlot) {
            offhandSlots.add(offhandSlot);
        }
        return slot;
    }

    protected int getInventoryYOffset() {
        return 84;
    }

    protected int getInventoryXOffset() {
        return 8;
    }

    protected void addPlayerInventory(@Nonnull Inventory inv) {
        if (this instanceof EmptyContainer) {
            return;
        }
        var yOffset = getInventoryYOffset();
        final var xOffset = getInventoryXOffset();
        for (var slotY = 0; slotY < 3; slotY++) {
            for (int slotX = 0; slotX < 9; slotX++) {
                addSlot(new MainInvSlot(inv, Inventory.getSelectionSize() + slotX + slotY * 9, xOffset + slotX * 18, yOffset + slotY * 18));
            }
        }
        yOffset += 58;
        for (var slotX = 0; slotX < Inventory.getSelectionSize(); slotX++) {
            addSlot(new HotbarSlot(inv, slotX, xOffset + slotX * 18, yOffset));
        }
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int slotID) {
        final var currentSlot = slots.get(slotID);
        if (!currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final var selectedWindow = player.level.isClientSide ? getSelectedWindow() : getSelectedWindow(player.getUUID());
        if (currentSlot instanceof InsertableSlot insertableSlot && !insertableSlot.exists(selectedWindow)) {
            return ItemStack.EMPTY;
        }
        final var slotStack = currentSlot.getItem();
        var stackToInsert = slotStack;
        // Start by inserting into slots with items already
        // hot bar -> main inv
        if (currentSlot instanceof InventoryContainerSlot) {
            stackToInsert = Stacks.insertItem(armorSlots, stackToInsert, true, selectedWindow);
            stackToInsert = Stacks.insertItem(hotBarSlots, stackToInsert, true, selectedWindow);
            stackToInsert = Stacks.insertItem(mainInventorySlots, stackToInsert, true, selectedWindow);
            // Now we go backwards if we have a remainder
            stackToInsert = Stacks.insertItem(armorSlots, stackToInsert, false, selectedWindow);
            stackToInsert = Stacks.insertItem(hotBarSlots, stackToInsert, false, selectedWindow);
            stackToInsert = Stacks.insertItem(mainInventorySlots, stackToInsert, false, selectedWindow);
        } else {
            // First try to insert into the tile's slots
            stackToInsert = Stacks.insertItem(inventoryContainerSlots, stackToInsert, true, selectedWindow);
            if (slotStack.getCount() == stackToInsert.getCount()) {
                stackToInsert = Stacks.insertItem(inventoryContainerSlots, stackToInsert, false, selectedWindow);
                if (slotStack.getCount() == stackToInsert.getCount()) {
                    // So, tile slots are all full, let's try inserting into armour, offhand, main inv or hot bar
                    if (currentSlot instanceof ArmorSlot || currentSlot instanceof OffhandSlot) {
                        stackToInsert = Stacks.insertItem(hotBarSlots, stackToInsert, true, selectedWindow);
                        stackToInsert = Stacks.insertItem(mainInventorySlots, stackToInsert, true, selectedWindow);
                        stackToInsert = Stacks.insertItem(hotBarSlots, stackToInsert, false, selectedWindow);
                        stackToInsert = Stacks.insertItem(mainInventorySlots, stackToInsert, false, selectedWindow);
                    } else if (currentSlot instanceof MainInvSlot) {
                        stackToInsert = Stacks.insertItem(armorSlots, stackToInsert, false, selectedWindow);
                        stackToInsert = Stacks.insertItem(hotBarSlots, stackToInsert, selectedWindow);
                    } else if (currentSlot instanceof HotbarSlot) {
                        stackToInsert = Stacks.insertItem(armorSlots, stackToInsert, false, selectedWindow);
                        stackToInsert = Stacks.insertItem(mainInventorySlots, stackToInsert, selectedWindow);
                    }
                }
            }
        }
        if (stackToInsert.getCount() == slotStack.getCount()) {
            // Everything was inserted, nothing remains
            return ItemStack.EMPTY;
        }

        // Otherwise, decrease the stack by the amount we inserted, and return it as a new stack for what is now in the slot
        final var diff = slotStack.getCount() - stackToInsert.getCount();
        final var newStack = currentSlot.remove(diff);
        currentSlot.onTake(player, newStack);
        return newStack;
    }

    @Nullable
    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    public SelectedWindowData getSelectedWindow() {
        return selectedWindow;
    }

    @Nullable
    @CallOnlyOn(value = CallOnlyOn.Side.SERVER, logical = true)
    public SelectedWindowData getSelectedWindow(UUID player) {
        return selectedWindows.get(player);
    }

    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    public void setSelectedWindow(@Nullable SelectedWindowData selectedWindow) {
        if (!Objects.equals(this.selectedWindow, selectedWindow)) {
            this.selectedWindow = selectedWindow;
            SimpleGuiNetwork.sendToServer(new SelectWindowPacket(this.selectedWindow));
        }
    }

    @CallOnlyOn(value = CallOnlyOn.Side.SERVER, logical = true)
    public void setSelectedWindow(UUID player, @Nullable SelectedWindowData selectedWindow) {
        if (selectedWindow == null) {
            clearSelectedWindow(player);
        } else {
            selectedWindows.put(player, selectedWindow);
        }
    }

    @CallOnlyOn(value = CallOnlyOn.Side.SERVER, logical = true)
    private void clearSelectedWindow(UUID player) {
        selectedWindows.remove(player);
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        closeInventory(player);
    }

    protected void closeInventory(@Nonnull Player player) {
        if (!player.level.isClientSide()) {
            clearSelectedWindow(player.getUUID());
        }
    }
}
