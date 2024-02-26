package com.gradle.ccud.adapters.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Java9MethodHandleLookup implements MethodHandleLookup {

    private static final Method LOOKUP_METHOD;

    static {
        try {
            LOOKUP_METHOD = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public MethodHandle getMethodHandle(Object proxy, Method method) {
        try {
            Class<?> declaringClass = method.getDeclaringClass();
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) LOOKUP_METHOD.invoke(null, declaringClass, MethodHandles.lookup());
            return lookup.findSpecial(
                    declaringClass,
                    method.getName(),
                    MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                    declaringClass
                )
                .bindTo(proxy);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
