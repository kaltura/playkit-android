package com.kaltura.playkit;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Marker annotation, specifying that a class is part of the public API.
 * There are classes that are declared public just because of Java's cross-package access rules.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(value={TYPE})
public @interface PKPublicAPI {
}
