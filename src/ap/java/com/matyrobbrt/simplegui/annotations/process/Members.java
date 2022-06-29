package com.matyrobbrt.simplegui.annotations.process;

import com.matyrobbrt.simplegui.annotations.process.targets.Method;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public record Members(Names names, MappingHelper mappings, Hierarchy hierarchy) {
    public String getMappedMethod(ExecutableElement method) {
        return getMappedMethod((TypeElement) method.getEnclosingElement(), method.getSimpleName().toString(), names.getDesc(method));
    }

    public String getMappedMethod(TypeElement type, String name, String desc) {
        return maybeGetMappedMethod(type.asType(), name, desc).orElse(name);
    }

    private Optional<String> maybeGetMappedMethod(TypeMirror type, String name, String desc) {
        return mappings.getSrgNameOpt(names.getTypeName(type), name, desc)
                .or(() -> names().types().directSupertypes(type)
                        .stream()
                        .flatMap(superType -> maybeGetMappedMethod(superType, name, desc).stream())
                        .findFirst());
    }

    public String getMappedField(TypeElement type, String field) {
        return maybeGetMappedField(type.asType(), field).orElse(field);
    }

    private Optional<String> maybeGetMappedField(TypeMirror type, String field) {
        return mappings.getSrgNameOpt(names.getTypeName(type), field)
                .or(() -> names().types().directSupertypes(type)
                        .stream()
                        .flatMap(superType -> maybeGetMappedField(superType, field).stream())
                        .findFirst());
    }

    public Method method(ExecutableElement method) {
        return new Method(
                getMappedMethod(method),
                names.getDesc(method),
                method.getParameters().stream().map(v -> names.getParamDescriptor(v.asType())).toList(),
                names.getParamDescriptor(method.getReturnType())
        );
    }
}
