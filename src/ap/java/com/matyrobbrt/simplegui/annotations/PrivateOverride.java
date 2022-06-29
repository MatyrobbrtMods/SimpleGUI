package com.matyrobbrt.simplegui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method with this annotation in order to override a private method in a super class. <br>
 * The created override will call the source method annotated. <br>
 * Note: the source method <strong>cannot</strong> have the same name as the {@link #value() targeted} method.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface PrivateOverride {
    /**
     * Gets the class of the method to override. <br>
     * By default, this is the primary super class.
     *
     * @return the super class to override the method in
     */
    Class<?> superClass() default String.class; // String is a dummy class, not that it matters

    /**
     * The name of the method to override.
     *
     * @return the name of the method to override
     */
    String value();
}
