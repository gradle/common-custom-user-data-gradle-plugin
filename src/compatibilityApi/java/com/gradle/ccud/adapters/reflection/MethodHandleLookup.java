package com.gradle.ccud.adapters.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

interface MethodHandleLookup {

    MethodHandleLookup INSTANCE = isAtLeastJava9()
        ? new Java9MethodHandleLookup()
        : new Java8MethodHandleLookup();

    MethodHandle getMethodHandle(Object proxy, Method method);

    static boolean isAtLeastJava9() {
        try {
            // the method was added in Java 9
            Process.class.getMethod("pid");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
