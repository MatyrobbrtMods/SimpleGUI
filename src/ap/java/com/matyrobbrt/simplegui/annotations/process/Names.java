package com.matyrobbrt.simplegui.annotations.process;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public record Names(Types types, Elements elements) {
    public String getDesc(ExecutableElement method) {
        final var signatureBuilder = new StringBuilder("(");
        for (final var parameter : method.getParameters()) {
            signatureBuilder.append(getParamDescriptor(parameter.asType()));
        }
        return signatureBuilder + ")" + getParamDescriptor(method.getReturnType());
    }

    public String getParamDescriptor(TypeMirror type) {
        // Erase the type as we don't have it during reflection
        type = types.erasure(type);
        return switch (type.getKind()) {
            case ARRAY -> "[" + getParamDescriptor(((ArrayType) type).getComponentType());
            case BOOLEAN -> "Z";
            case BYTE -> "B";
            case CHAR -> "C";
            case DOUBLE -> "D";
            case FLOAT -> "F";
            case INT -> "I";
            case LONG -> "J";
            case SHORT -> "S";
            case VOID -> "V";
            case DECLARED -> "L" + getClassDescriptor(type) + ";";
            default -> throw new IllegalStateException("Unexpected value: " + type.getKind());
        };
    }

    public String getClassDescriptor(TypeMirror type) {
        return getTypeName(type).replace('.', '/');
    }

    public String getTypeName(TypeMirror type) {
        return getTypeName(types.asElement(type));
    }

    public String getTypeName(Element el) {
        if (!(el instanceof TypeElement element))
            throw new IllegalArgumentException();
        final var pkg = elements.getPackageOf(element);
        final var packageName = pkg.getQualifiedName().toString();
        final var subClasses = String.join("$", Hierarchy.walkEnclosingClasses(element)
                .map(e -> e.getSimpleName().toString())
                .toList());
        final var className = (subClasses.isEmpty() ? "" : subClasses + "$") + element.getSimpleName().toString();
        if (packageName.isEmpty()) {
            return className.replace('.', '$');
        } else {
            return packageName + "." + className.replace('.', '$');
        }
    }
}
