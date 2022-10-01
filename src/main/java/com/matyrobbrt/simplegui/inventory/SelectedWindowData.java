package com.matyrobbrt.simplegui.inventory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.matyrobbrt.simplegui.annotations.CallOnlyOn;
import com.matyrobbrt.simplegui.util.IntHandler;
import com.matyrobbrt.simplegui.util.Utils;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class SelectedWindowData {

    public static final SelectedWindowData UNSPECIFIED = new SelectedWindowData(WindowType.UNSPECIFIED);

    @Nonnull
    public final WindowType type;
    public final short extraData;

    public SelectedWindowData(@Nonnull WindowType type) {
        this(type, (short) 0);
    }

    /**
     * It is expected to only call this with a piece of extra data that is valid. If it is not valid this end up treating it as zero instead.
     */
    public SelectedWindowData(@Nonnull WindowType type, short extraData) {
        this.type = Objects.requireNonNull(type);
        this.extraData = this.type.isValid(extraData) ? extraData : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        else if (o == null || getClass() != o.getClass()) return false;
        final var other = (SelectedWindowData) o;
        return extraData == other.extraData && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, extraData);
    }

    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    public void updateLastPosition(int x, int y) {
        final var saveName = type.saveName(extraData);
        if (saveName != null) {
            final var cachedPosition = Handler.get(saveName);
            if (cachedPosition != null) {
                final var cachedX = cachedPosition.x();
                if (cachedX.getAsInt() != x) {
                    cachedX.accept(x);
                }
                final var cachedY = cachedPosition.y();
                if (cachedY.getAsInt() != y) {
                    cachedY.accept(y);
                }
            }
        }
    }

    @CallOnlyOn(CallOnlyOn.Side.CLIENT)
    public WindowPosition getLastPosition() {
        final var saveName = type.saveName(extraData);
        if (saveName != null) {
            final var cachedPosition = Handler.get(saveName);
            if (cachedPosition != null) {
                return new WindowPosition(cachedPosition.x().getAsInt(), cachedPosition.y().getAsInt());
            }
        }
        return new WindowPosition(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public record WritablePosition(IntHandler x, IntHandler y) {
    }

    public record WindowPosition(int x, int y) {

        public static final class Serializer implements JsonSerializer<WindowPosition>, JsonDeserializer<WindowPosition> {

            @Override
            public WindowPosition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                final var obj = (JsonObject) json;
                return new WindowPosition(getInt(obj.get("x")), getInt(obj.get("y")));
            }

            private static int getInt(@Nullable JsonElement json) {
                return json == null ? Integer.MAX_VALUE : json.getAsInt();
            }

            @Override
            public JsonElement serialize(WindowPosition src, Type typeOfSrc, JsonSerializationContext context) {
                final var json = new JsonObject();
                json.addProperty("x", src.x());
                json.addProperty("y", src.y());
                return json;
            }
        }

    }

    private static final class Handler {
        public static final Path CFG_PATH = FMLPaths.CONFIGDIR.get().resolve("simplegui_positions.json").toAbsolutePath();
        public static final Gson GSON = new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(WindowPosition.class, new WindowPosition.Serializer())
                .create();
        public static final Type TYPE = new TypeToken<Map<String, WindowPosition>>() {}.getType();

        @Nullable
        public static WritablePosition get(String name) {
            try {
                if (!Files.exists(CFG_PATH)) {
                    try (final var writer = Files.newBufferedWriter(CFG_PATH)) {
                        GSON.toJson(new JsonObject(), writer);
                    }
                }
                try (final var reader = Files.newBufferedReader(CFG_PATH)) {
                    final Map<String, WindowPosition> data = GSON.fromJson(reader, TYPE);
                    final var pos = data.computeIfAbsent(name, k -> new WindowPosition(Integer.MAX_VALUE, Integer.MAX_VALUE));
                    return new WritablePosition(
                            new IntHandler(pos::x, n -> write(name, data, new WindowPosition(n, get(name).y().getAsInt()))),
                            new IntHandler(pos::y, n -> write(name, data, new WindowPosition(get(name).x.getAsInt(), n)))
                    );
                }
            } catch (IOException e) {
                Utils.LOGGER.error("Exception trying to read SimpleGui positions config!", e);
            }
            return null;
        }

        private static void write(String name, Map<String, WindowPosition> old, WindowPosition pos) {
            try (final var writer = Files.newBufferedWriter(CFG_PATH)) {
                old.put(name, pos);
                GSON.toJson(old, TYPE, writer);
            } catch (IOException e) {
                Utils.LOGGER.error("Exception trying to write SimpleGui position configs!", e);
            }
        }
    }
}
