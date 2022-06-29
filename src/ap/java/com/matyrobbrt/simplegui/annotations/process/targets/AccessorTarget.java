package com.matyrobbrt.simplegui.annotations.process.targets;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public record AccessorTarget(Type type, String target, String intermediary, List<Accessor> accessors) {
    public enum Type {
        INTERFACE,
        ABSTRACT
    }
    public record FieldAccessor(String type, Field field, String name, Method method,
                                @Nullable String actualOwner,
                                @SerializedName("static") boolean isStatic) implements Accessor {
        public enum Method {
            SET, GET
        }
    }

    public record MethodInvoker(String type, Method method, String name,
                                @Nullable String actualOwner,
                                @SerializedName("static") boolean isStatic) implements Accessor {}

    public sealed interface Accessor {}
}
