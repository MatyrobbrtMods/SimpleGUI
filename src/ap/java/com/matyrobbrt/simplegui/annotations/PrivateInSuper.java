package com.matyrobbrt.simplegui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an element is private in the super class, and as such was AT'd to change functionality. <br>
 * Treat any members annotated as such as deprecated as they shouldn't be called directly.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface PrivateInSuper {
}
