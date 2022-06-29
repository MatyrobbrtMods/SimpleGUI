package com.matyrobbrt.simplegui.annotations.process;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraftforge.srgutils.IMappingFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

// TODO at some point we should maybe also map classes, but currently forge only uses Mojmaps for classes
@MethodsReturnNonnullByDefault
public record MappingHelper(IMappingFile file) {
    public static MappingHelper from(InputStream is) throws IOException {
        return new MappingHelper(IMappingFile.load(is));
    }

    public Optional<String> getSrgNameOpt(String clazz, String method, String desc) {
        return Optional.ofNullable(getClass(clazz))
                .flatMap(c -> Optional.ofNullable(c.getMethod(method, desc)))
                .map(IMappingFile.IMethod::getMapped);
    }
    public String getSrgName(String clazz, String method, String desc) {
        return getSrgNameOpt(clazz, method, desc).orElse(method);
    }

    public Optional<String> getSrgNameOpt(String clazz, String field) {
        return Optional.ofNullable(getClass(clazz))
                .flatMap(c -> Optional.ofNullable(c.getField(field)))
                .map(IMappingFile.IField::getMapped);
    }
    public String getSrgName(String clazz, String field) {
        return getSrgNameOpt(clazz, field).orElse(field);
    }

    @Nullable
    public IMappingFile.IClass getClass(String qualifiedName) {
        return file.getClass(qualifiedName.replace('.', '/'));
    }
}
