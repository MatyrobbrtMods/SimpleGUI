package com.matyrobbrt.simplegui.util;

import com.matyrobbrt.simplegui.SimpleGUIMod;
import com.matyrobbrt.simplegui.util.col.ReverseIterator;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

public class Utils {

    public static final Logger LOGGER = LoggerFactory.getLogger("SimpleGui");

    /**
     * Helper to call the constructor for string text components and also convert any non-breaking spaces to spaces so that they render properly.
     */
    public static MutableComponent getString(String component) {
        return Component.literal(cleanString(component));
    }

    /**
     * Helper to clean up strings and convert any non-breaking spaces to spaces so that they render properly.
     *
     * @param component String
     *
     * @return Cleaned string
     */
    private static String cleanString(String component) {
        return component.replace("\u00A0", " ")//non-breaking space
                .replace("\u202f", " ");//narrow non-breaking space
    }

    @Nullable
    public static String getRLOrNull(@Nullable ResourceLocation rl) {
        return rl == null ? null : rl.toString();
    }

    public static Component emptyComponent() {
        return Component.empty();
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * Represents which direction our tiling is done when extending past the max size.
     */
    public enum TilingDirection {
        /**
         * Textures are being tiled/filled from top left to bottom right.
         */
        DOWN_RIGHT(true, true),
        /**
         * Textures are being tiled/filled from top right to bottom left.
         */
        DOWN_LEFT(true, false),
        /**
         * Textures are being tiled/filled from bottom left to top right.
         */
        UP_RIGHT(false, true),
        /**
         * Textures are being tiled/filled from bottom right to top left.
         */
        UP_LEFT(false, false);

        public final boolean down;
        public final boolean right;

        TilingDirection(boolean down, boolean right) {
            this.down = down;
            this.right = right;
        }
    }

    public static ResourceLocation getResource(String type, String location) {
        return new ResourceLocation(SimpleGUIMod.MOD_ID, type + "/" + location + ".png");
    }

    public static float getRed(int color) {
        return (color >> 16 & 0xFF) / 255.0F;
    }

    public static float getGreen(int color) {
        return (color >> 8 & 0xFF) / 255.0F;
    }

    public static float getBlue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    public static float getAlpha(int color) {
        return (color >> 24 & 0xFF) / 255.0F;
    }

    public static ResourceLocation getRegistryName(Item item) {
        //noinspection deprecation
        return Registry.ITEM.getKey(item);
    }

    public static <T> Iterable<T> reverseIterator(List<T> list) {
        return new ReverseIterator<>(list);
    }

}
