package com.matyrobbrt.simplegui.client.element.slot;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.matyrobbrt.simplegui.client.Gui;
import com.matyrobbrt.simplegui.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface SlotFactory<S extends Slot> {

    SlotFactory<Slot> DEFAULT = (invSlot, gui, x, y) -> new GuiSlot(gui, x, y);

    @Nullable
    GuiSlot createSlot(S invSlot, Gui gui, int x, int y);

    @CanIgnoreReturnValue
    static <S extends Slot> SlotFactory<S> register(Class<S> clazz, SlotFactory<S> factory) {
        return Registry.register(clazz, factory);
    }
    @CanIgnoreReturnValue
    static <S extends Slot> SlotFactory<S> registerNop(Class<S> clazz) {
        return register(clazz, (invSlot, gui, x, y) -> null);
    }

    class Registry {
        private static final Map<Class<?>, SlotFactory<?>> REGISTRY = new HashMap<>();
        @CanIgnoreReturnValue
        public static <S extends Slot> SlotFactory<S> register(Class<S> clazz, SlotFactory<S> factory) {
            REGISTRY.put(clazz, factory);
            return factory;
        }

        @Nullable
        public static GuiSlot create(Slot slot, Gui gui, int x, int y) {
            return REGISTRY.getOrDefault(slot.getClass(), DEFAULT).createSlot(Utils.cast(slot), gui, x, y);
        }
    }
}
