package com.matyrobbrt.simplegui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method marked with this annotation can only be called on the specified logical / physical side. <br>
 * The contract will be assured by runtime transformation.
 */
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface CallOnlyOn {
    /**
     * The side this method can be called on.
     */
    Side value();

    /**
     * If the {@linkplain #value() side} should be a logical, or a physical one: <br>
     * {@code true} -> logical <br>
     * {@code false} -> physical (default)
     */
    boolean logical() default false;

    enum Side {
        /**
         * The server side. The physical equivalent is {@link net.minecraftforge.api.distmarker.Dist#DEDICATED_SERVER dedicated server}.
         */
        SERVER,
        /**
         * The client side.
         */
        CLIENT
    }
}
