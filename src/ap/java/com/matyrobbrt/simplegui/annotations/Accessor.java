package com.matyrobbrt.simplegui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotated with this annotation in a class annotated with {@link Holder}. <br>
 * Accessors may be used to access private fields or methods.
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Accessor {
    /**
     * The name of the targeted field / method. <br>
     * By default, depending on the type {@link #type() type}, the target will be computed.
     *
     * @return the target name
     */
    String value() default "";

    /**
     * The type of this accessor.
     *
     * @return the type
     */
    Type type() default Type.COMPUTE;

    /**
     * Indicates an accessor's type.
     */
    enum Type {
        /**
         * Indicates field accessors. A field accessor may be either a getter or a setter depending on the
         * accessor's parameters and return types:
         * <dl>
         *   <dt>Getters</dt>
         *   <dd>- have no parameters and a non-void return type representing the value of the field</dd>
         *   <dt>Setters</dt>
         *   <dd>- have a void return type and <strong>only one</strong> parameter representing the new field value</dd>
         * </dl> <br>
         * Setter accessors will make the target field mutable, if final. <br> <br>
         * If the accessor does not specify a {@link #value() target}, it will be computed as follows: <br>
         * The 'set' or 'get' prefixes will be stripped, and the remainder of the name will have the
         * first letter lowercased. Example: <br>
         * 'setMyField' -> targets field 'myField'
         * 'getThatField' -> targets field 'thatField'
         */
        FIELD,
        /**
         * Indicates an accessor for methods (invoker). A method invoker needs to have the exact descriptor of its target. <br>
         * If the accessor does not specify a {@link #value() target}, it will be computed as follows: <br>
         * The 'call' or 'invoke' prefixes will be stripped, and the remainder of the name will have the
         * first letter lowercased. Example: <br>
         * 'callThatMethod' -> targets method 'thatMethod'
         */
        METHOD,
        /**
         * Indicates that the accessor's type will be computed. That is done as follows: <br>
         * First, it is checked if the accessor complies with the conditions for being a {@link #FIELD field accessor}.
         * If so, the computed type will be {@link #FIELD}, otherwise {@link #METHOD} will be the computed type.
         */
        COMPUTE
    }

    /**
     * A class marked with this annotation holds {@link Accessor accessors}. <br>
     * Note: if an interface is marked with this annotation, all non-default methods
     * <strong>must</strong> be accessors. <br>
     * Interface accessor holders will be implemented onto the target, so casts are
     * a safe way of using the accessors.
     */
    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Holder {
        /**
         * The target of the accessors. If the holder is an interface, it may be any class, but if not,
         * only a superclass of the holder can be targeted.
         */
        Class<?> value() default String.class; // Dummy value, as of not specified, the target will be computed.
    }
}
