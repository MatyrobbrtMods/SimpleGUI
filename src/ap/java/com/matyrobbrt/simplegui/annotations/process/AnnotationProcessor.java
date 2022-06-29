package com.matyrobbrt.simplegui.annotations.process;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.matyrobbrt.simplegui.annotations.Accessor;
import com.matyrobbrt.simplegui.annotations.CallOnlyOn;
import com.matyrobbrt.simplegui.annotations.CheckCaller;
import com.matyrobbrt.simplegui.annotations.PrivateOverride;
import com.matyrobbrt.simplegui.annotations.process.targets.AccessorTarget;
import com.matyrobbrt.simplegui.annotations.process.targets.CallOnlyOnTarget;
import com.matyrobbrt.simplegui.annotations.process.targets.CheckCallerTarget;
import com.matyrobbrt.simplegui.annotations.process.targets.Field;
import com.matyrobbrt.simplegui.annotations.process.targets.PrivateOverrideTarget;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class AnnotationProcessor extends AbstractProcessor {
    public static final Gson GSON = new GsonBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .create();
    public static final String MAPPINGS_LOCATION_OPTION = "mappingsLocation";

    private MappingHelper mappings;
    private Names names;
    private Hierarchy hierarchy;
    private Members members;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        final var mappingsPath = Path.of(processingEnv.getOptions().get(MAPPINGS_LOCATION_OPTION));
        if (!Files.exists(mappingsPath)) {
            throw new RuntimeException("Provided mappings file (" + mappingsPath + ") does not exist!");
        }
        try (final var is = Files.newInputStream(mappingsPath)) {
            this.mappings = MappingHelper.from(is);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Using mappings file: " + mappingsPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        this.names = new Names(processingEnv.getTypeUtils(), processingEnv.getElementUtils());
        this.hierarchy = new Hierarchy(processingEnv.getTypeUtils(), names);
        this.members = new Members(names, mappings, hierarchy);
    }

    @Override
    public boolean process(Set<? extends TypeElement> $annotations, RoundEnvironment roundEnv) {
        if ($annotations.isEmpty()) return false;

        final var callOnlyOn = callOnlyOn(roundEnv);
        write(callOnlyOn, "coremods/only_call_on.json");

        {
            final var checkCaller = checkCaller(roundEnv);
            final var json = new JsonArray();
            checkCaller.asMap().forEach((clazz, methods) -> {
                final var obj = new JsonObject();
                obj.addProperty("clazz", clazz);
                obj.add("methods", GSON.toJsonTree(methods));
                json.add(obj);
            });
            write(json, "coremods/check_caller.json");
        }

        final var privateOverride = privateOverride(roundEnv);
        write(privateOverride, "coremods/override_method.json");

        write(accessor(roundEnv), "coremods/accessors.json");

        return false;
    }

    private void write(Object object, String location) {
        try {
            final var resource = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", location);
            try (final var writer = new BufferedWriter(new OutputStreamWriter(resource.openOutputStream(), StandardCharsets.UTF_8))) {
                GSON.toJson(object, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<AccessorTarget> accessor(RoundEnvironment roundEnv) {
        final var data = new ArrayList<AccessorTarget>();
        for (final var accessorHolder : roundEnv.getElementsAnnotatedWith(Accessor.Holder.class)) {
            final var accessors = new ArrayList<AccessorTarget.Accessor>();
            final var target = getClassOpt(getAnnotation(accessorHolder, Accessor.Holder.class), "value")
                    .orElse(processingEnv.getTypeUtils().directSupertypes(accessorHolder.asType()).get(0));
            final var targetElement = (TypeElement) processingEnv.getTypeUtils().asElement(target);
            if (targetElement.getKind() == ElementKind.INTERFACE)
                throw new IllegalStateException("Interfaces cannot be targetted by accessors: " + names.getTypeName(accessorHolder));
            final var isInterface = accessorHolder.getKind().isInterface();
            if (!isInterface && !isSubClass(accessorHolder.asType(), target))
                throw new UnsupportedOperationException("Cannot use a non-interface accessor on target '%s' for non-subclass: '%s'"
                        .formatted(names.getTypeName(target), names.getTypeName(accessorHolder)));
            for (final var mthd : hierarchy.<ExecutableElement>findChildrenOfType(accessorHolder, ElementKind.METHOD)) {
                final var ann = mthd.getAnnotation(Accessor.class);
                if (ann == null) {
                    if (isInterface && (!mthd.getModifiers().contains(Modifier.STATIC)) && !mthd.getModifiers().contains(Modifier.DEFAULT)) {
                        throw new IllegalStateException("@Accessor interface '%s' contains non-static / non-default method '%s%s'"
                                .formatted(names.getTypeName(accessorHolder), mthd.getSimpleName(), names.getDesc(mthd)));
                    } else {
                        continue;
                    }
                }
                final var computeType = ann.type() == Accessor.Type.COMPUTE;
                final Accessor.Type type;
                if (computeType)
                    type = computeAccessorType(mthd);
                else
                    type = ann.type();
                if (type == Accessor.Type.FIELD) {
                    if ((mthd.getParameters().size() != 1 && mthd.getReturnType().getKind() == TypeKind.VOID) || (mthd.getReturnType().getKind() != TypeKind.VOID && !mthd.getParameters().isEmpty()))
                        throw new IllegalArgumentException("@Accessor: Field accessor method %s in %s must have either no parameters and a non-void return type (for getters) or a void return type and one parameter (for setters)"
                                .formatted(mthd.getSimpleName(), names.getTypeName(accessorHolder)));
                    final var desc = mthd.getParameters().isEmpty() ? names.getParamDescriptor(mthd.getReturnType()) : names.getParamDescriptor(mthd.getParameters().get(0).asType());
                    final var method = mthd.getParameters().isEmpty() ? AccessorTarget.FieldAccessor.Method.GET : AccessorTarget.FieldAccessor.Method.SET;
                    final var fieldName = ann.value().isEmpty() ? Utils.stripMarkers(mthd.getSimpleName().toString(), true, "get", "set")
                            : ann.value();
                    final var field = hierarchy.findField(targetElement, fieldName, desc);
                    if (field.isEmpty()) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Accessor type has been computed to " + type);
                        throw new IllegalArgumentException("@Accessor: Cannot find target field " + fieldName + "(" + desc + ") in target class " + target + ": " + names.getTypeName(accessorHolder));
                    }
                    final var isStatic = mthd.getModifiers().contains(Modifier.STATIC);
                    if (!isStatic && field.get().getModifiers().contains(Modifier.STATIC))
                        throw new IllegalArgumentException("Non-static @Accessor '%s#%s%s' targets static field %s"
                                .formatted(names.getTypeName(accessorHolder), mthd.getSimpleName(), names.getDesc(mthd), field.get().getSimpleName()));
                    else if (isStatic && !field.get().getModifiers().contains(Modifier.STATIC))
                        throw new IllegalArgumentException("Static @Accessor '%s#%s%s' targets non-static field %s"
                                .formatted(names.getTypeName(accessorHolder), mthd.getSimpleName(), names.getDesc(mthd), field.get().getSimpleName()));
                    accessors.add(new AccessorTarget.FieldAccessor(
                            "field",
                            new Field(
                                    members.getMappedField(targetElement, fieldName),
                                    method == AccessorTarget.FieldAccessor.Method.GET ?
                                            names.getParamDescriptor(mthd.getReturnType()) :
                                            names.getParamDescriptor(mthd.getParameters().get(0).asType())
                            ),
                            mthd.getSimpleName().toString(), method,
                            field.get().getEnclosingElement().equals(targetElement) ? null : names.getTypeName(field.get().getEnclosingElement()),
                            isStatic
                    ));
                } else {
                    final var desc = names.getDesc(mthd);
                    final var methodName = ann.value().isEmpty() ? Utils.stripMarkers(mthd.getSimpleName().toString(), true, "call", "invoke")
                            : ann.value();
                    final var methodOpt = hierarchy.findMethod(targetElement, methodName, desc, true);
                    if (methodOpt.isEmpty()) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Accessor type has been computed to " + type);
                        throw new IllegalArgumentException("@Accessor: Cannot find target method " + methodName + "(" + desc + ") in target class " + target + ": " + names.getTypeName(accessorHolder));
                    }
                    final var method = methodOpt.get();
                    final var isStatic = mthd.getModifiers().contains(Modifier.STATIC);
                    if (!isStatic && method.getModifiers().contains(Modifier.STATIC))
                        throw new IllegalArgumentException("Non-static @Accessor '%s#%s%s' targets static method %s%s"
                                .formatted(names.getTypeName(accessorHolder), mthd.getSimpleName(), names.getDesc(mthd),
                                        method.getSimpleName(), names.getDesc(method)));
                    else if (isStatic && !method.getModifiers().contains(Modifier.STATIC))
                        throw new IllegalArgumentException("Static @Accessor '%s#%s%s' targets non-static method %s%s"
                                .formatted(names.getTypeName(accessorHolder), mthd.getSimpleName(), names.getDesc(mthd),
                                        method.getSimpleName(), names.getDesc(method)));
                    accessors.add(new AccessorTarget.MethodInvoker(
                            "method",
                            members.method(method),
                            mthd.getSimpleName().toString(),
                            method.getEnclosingElement().equals(targetElement) ? null : names.getTypeName(method.getEnclosingElement()),
                            isStatic
                    ));
                }
            }
            if (accessors.isEmpty()) continue;
            data.add(new AccessorTarget(
                    isInterface ? AccessorTarget.Type.INTERFACE : AccessorTarget.Type.ABSTRACT,
                    names.getTypeName(target),
                    names.getTypeName(accessorHolder),
                    accessors
            ));
        }
        return data;
    }

    private Accessor.Type computeAccessorType(ExecutableElement method) {
        if (method.getParameters().size() == 1 && method.getReturnType().getKind() == TypeKind.VOID)
            return Accessor.Type.FIELD;
        else if (method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID)
            return Accessor.Type.FIELD;
        return Accessor.Type.METHOD;
    }

    private List<PrivateOverrideTarget> privateOverride(RoundEnvironment roundEnv) {
        final var data = new ArrayList<PrivateOverrideTarget>();
        final var alreadyOverrode = new ArrayList<String>();
        for (final var method$ : roundEnv.getElementsAnnotatedWith(PrivateOverride.class)) {
            if (method$.getKind() != ElementKind.METHOD || method$.getModifiers().contains(Modifier.STATIC) || !isValidCoremodTarget(method$))
                continue;
            final var method = (ExecutableElement) method$;
            final var ann = method.getAnnotation(PrivateOverride.class);
            final var desc = names.getDesc(method);
            final var fullName = names.getTypeName(method.getEnclosingElement()) + "#" + method.getSimpleName() + desc;
            {
                if (alreadyOverrode.contains(fullName)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Found 2 " + fullName + " @PrivateOverride targets!", method);
                    continue;
                }
                alreadyOverrode.add(fullName);
            }
            final TypeMirror proposedSuper;
            {
                final var supers = processingEnv.getTypeUtils().directSupertypes(method.getEnclosingElement().asType());
                final var opt = getClassOpt(getAnnotation(method, PrivateOverride.class), "superClass");
                if (opt.isEmpty()) {
                    proposedSuper = supers.get(0);
                } else {
                    proposedSuper = opt.get();
                    if (!isSubClass(method.getEnclosingElement().asType(), proposedSuper))
                        throw new IllegalArgumentException("Tried to override a method in a non-subclass '%s' using @PrivateOverride: %s"
                                .formatted(names.getTypeName(proposedSuper), fullName));
                }
            }
            final var proposedSuperClass = (TypeElement) processingEnv.getTypeUtils().asElement(proposedSuper);
            final var targetMethod = hierarchy.onAllChildren(proposedSuperClass)
                    .filter(e -> e.getKind() == ElementKind.METHOD)
                    .filter(e -> e.getSimpleName().toString().equals(ann.value()))
                    .filter(e -> names.getDesc((ExecutableElement) e).equals(desc))
                    .findFirst();
            if (targetMethod.isEmpty()) {
                throw new IllegalArgumentException("Tried to override unknown method '%s%s' in super class '%s' using @PrivateOverride: %s"
                        .formatted(ann.value(), desc, names.getTypeName(proposedSuper), fullName));
            }
            if (targetMethod.get().getSimpleName().equals(method.getSimpleName()))
                throw new IllegalArgumentException("@PrivateOverride target method cannot have the same name as the input method: " + fullName);
            data.add(new PrivateOverrideTarget(
                    members.method(method).excludeName(),
                    new PrivateOverrideTarget.Target(names.getTypeName(targetMethod.get().getEnclosingElement()), members.getMappedMethod(
                            proposedSuperClass, ann.value(), desc
                    )),
                    new PrivateOverrideTarget.Input(names.getTypeName(method.getEnclosingElement()), method.getSimpleName().toString(), method.getEnclosingElement().getKind().isInterface())
            ));
        }
        return data;
    }

    private ListMultimap<String, CheckCallerTarget> checkCaller(RoundEnvironment roundEnv) {
        final var data = Multimaps.<String, CheckCallerTarget>newListMultimap(new HashMap<>(), ArrayList::new);
        for (final var method : roundEnv.getElementsAnnotatedWith(CheckCaller.class)) {
            if (method.getKind() != ElementKind.METHOD && method.getKind() != ElementKind.CONSTRUCTOR)
                continue;
            if (!isValidCoremodTarget(method))
                continue;
            final var ann = method.getAnnotation(CheckCaller.class);
            final var mirror = getAnnotation(method, CheckCaller.class);
            final var owner = names.getTypeName(method.getEnclosingElement());
            final var target = new CheckCallerTarget(
                    members.getMappedMethod((ExecutableElement) method),
                    names.getDesc((ExecutableElement) method),
                    Stream.concat(
                            mirror.getElementValues().entrySet()
                                    .stream()
                                    .filter(e -> e.getKey().toString().equals("value()"))
                                    .map(Map.Entry::getValue)
                                    .flatMap(a -> getArray(a, this::qualifiedName)),
                            Stream.of(ann.named())
                    ).toList(),
                    ann.exception(),
                    ann.blacklist()
            );
            target.verify(method.getSimpleName().toString(), owner);
            data.put(owner, target);
        }
        return data;
    }

    private List<CallOnlyOnTarget> callOnlyOn(RoundEnvironment roundEnv) {
        final var data = new ArrayList<CallOnlyOnTarget>();
        for (final var method : roundEnv.getElementsAnnotatedWith(CallOnlyOn.class)) {
            if (method.getKind() != ElementKind.METHOD && method.getKind() != ElementKind.CONSTRUCTOR)
                continue;
            if (!isValidCoremodTarget(method))
                continue;
            final var ann = method.getAnnotation(CallOnlyOn.class);
            data.add(new CallOnlyOnTarget(
                    names.getTypeName(method.getEnclosingElement()),
                    members.method((ExecutableElement) method).excludeReturnAndParams(),
                    ann.value().name(),
                    ann.logical()
            ));
        }
        return data;
    }

    private String qualifiedName(AnnotationValue val) {
        if (val.getValue() instanceof TypeMirror typeMirror) {
            return names.getTypeName(typeMirror);
        }
        throw new NullPointerException();
    }

    @SuppressWarnings("unchecked")
    private static <T> Stream<T> getArray(AnnotationValue value, Function<AnnotationValue, T> fun) {
        final var values = (List<? extends AnnotationValue>) value.getValue();
        return values.stream().map(fun);
    }

    private static boolean isValidCoremodTarget(Element element) {
        if (element.getEnclosingElement().getKind().isInterface())
            return false;
        else return !(element instanceof ExecutableElement exec) || !exec.getModifiers().contains(Modifier.ABSTRACT);
    }

    private <A extends Annotation> AnnotationMirror getAnnotation(Element element, Class<A> clazz) {
        return element.getAnnotationMirrors()
                .stream()
                .filter(m -> names.getTypeName(m.getAnnotationType()).equals(clazz.getName()))
                .findFirst()
                .orElseThrow(NullPointerException::new);
    }

    private boolean isSubClass(TypeMirror type, TypeMirror superType) {
        final var superName = names.getTypeName(superType);
        return processingEnv.getTypeUtils().directSupertypes(type)
                .stream()
                .map(names::getTypeName)
                .anyMatch(superName::equals);
    }

    private static TypeMirror getClass(AnnotationMirror annotationMirror, String name) {
        return getClassOpt(annotationMirror, name).orElseThrow();
    }

    private static Optional<TypeMirror> getClassOpt(AnnotationMirror annotationMirror, String name) {
        return annotationMirror.getElementValues().entrySet()
                .stream()
                .filter(e -> e.getKey().toString().equals(name + "()"))
                .map(Map.Entry::getValue)
                .map(v -> (TypeMirror) v.getValue())
                .findFirst();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                CallOnlyOn.class.getCanonicalName(),
                CheckCaller.class.getCanonicalName()
        );
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(MAPPINGS_LOCATION_OPTION);
    }

}
