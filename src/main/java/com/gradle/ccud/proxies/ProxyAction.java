package com.gradle.ccud.proxies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When trying to proxy a method applied to an {@link org.gradle.api.Action} type, we need to ensure that the inputs for the action
 * are correctly proxied during the action invocation. This annotation applied to a method param declares which proxy type should be used
 * to establish this proxy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ProxyAction {

    Class<? extends ProxyType> value();

}
