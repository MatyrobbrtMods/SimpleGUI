package com.matyrobbrt.simplegui.inventory;

import com.matyrobbrt.simplegui.util.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a type of window in order to store last pos information.
 */
public final class WindowType {

    private static final List<WindowType> LOOKUP = new ObjectArrayList<>();

    /**
     * Gets a window type by its id. Useful for network transportations.
     *
     * @param id the id of the window
     * @return the window, or {@code null} if it doesn't exist
     */
    @Nullable
    public static WindowType byId(int id) {
        try {
            return LOOKUP.get(id);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * The 'unknown' window type.
     */
    public static final WindowType UNSPECIFIED = new WindowType(null);

    private final @Nullable ResourceLocation name;
    private final short maxData;
    private final int id;

    /**
     * Creates a new window type.
     * @param name the type's name
     * @param maxData the maximum amount of variations this window may have
     */
    public WindowType(@Nullable ResourceLocation name, short maxData) {
        this.name = name;
        this.maxData = maxData;
        final var computed = LOOKUP.indexOf(this);
        if (computed == -1) {
            this.id = LOOKUP.size();
            LOOKUP.add(this);
        } else {
            this.id = computed;
        }
    }

    public WindowType(@Nullable ResourceLocation saveName) {
        this(saveName, (short) 1);
    }

    public int getId() {
        return id;
    }

    @Nullable
    public String saveName(short extraData) {
        return maxData == 1 ? Utils.getRLOrNull(name) : Utils.getRLOrNull(name) + "$" + extraData;
    }

    public boolean isValid(short extraData) {
        return extraData >= 0 && extraData < maxData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final var that = (WindowType) o;
        return maxData == that.maxData && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, maxData);
    }
}
