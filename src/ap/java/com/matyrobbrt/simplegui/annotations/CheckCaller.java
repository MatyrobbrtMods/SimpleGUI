package com.matyrobbrt.simplegui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method will check callers, and if called by an invalid caller, an
 * {@link IllegalCallerException exception} will be thrown. <br>
 * The contract is assured by runtime transformation.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CheckCaller {
    /**
     * A list of classes that will be checked for valid callers.
     */
    Class<?>[] value() default {};

    /**
     * A list of class names that will be checked for valid callers. <br>
     * These values will be merged with {@link #value()}.
     */
    String[] named() default {};

    /**
     * A custom exception message.
     */
    String exception() default "Method was called by an invalid caller!";

    /**
     * If the check should blacklist the defined callers. ({@link #value()} and {@link #named()}).
     */
    boolean blacklist() default false;
}
