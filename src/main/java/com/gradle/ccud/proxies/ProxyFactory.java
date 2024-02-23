package com.gradle.ccud.proxies;

import org.gradle.api.Action;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public final class ProxyFactory {

    public static <T> T createProxy(Object target, Class<T> targetInterface) {
        return newProxyInstance(targetInterface, new ProxyingInvocationHandler(target));
    }

    @SuppressWarnings("unchecked")
    private static <T> T newProxyInstance(Class<T> targetInterface, InvocationHandler invocationHandler) {
        return (T) Proxy.newProxyInstance(targetInterface.getClassLoader(), new Class[]{targetInterface}, invocationHandler);
    }

    private static final class ProxyingInvocationHandler implements InvocationHandler {

        private final Object target;

        private ProxyingInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            try {
                Object result = method.isDefault()
                    ? invokeDefaultMethod(proxy, method, args)
                    : invokeDelegateMethod(method, args);

                if (result == null || isJdkType(result.getClass())) {
                    return result;
                }
                return createProxy(result, method.getReturnType());
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke " + method + " on " + target + " with args " + Arrays.toString(args), e);
            }
        }

        private static Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
            return MethodHandleLookup.INSTANCE.getMethodHandle(proxy, method).invokeWithArguments(args);
        }

        private Object invokeDelegateMethod(Method method, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            Method targetMethod = target.getClass().getMethod(method.getName(), convertTypes(method.getParameterTypes(), target.getClass().getClassLoader()));
            Object[] targetArgs = toTargetArgs(method, args);
            return targetMethod.invoke(target, targetArgs);
        }

        private static Object[] toTargetArgs(Method method, Object[] args) {
            if (args == null || args.length == 0) {
                return args;
            }
            if (args.length == 1 && args[0] instanceof Action) {
                return new Object[]{adaptActionArg(method, (Action<?>) args[0])};
            }
            if (args.length == 1 && args[0] instanceof Function) {
                return new Object[]{adaptFunctionArg((Function<?, ?>) args[0])};
            }
            if (Arrays.stream(args).allMatch(it -> isJdkType(it.getClass()))) {
                return args;
            }
            throw new RuntimeException("Unsupported argument types in " + Arrays.toString(args));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Action<Object> adaptActionArg(Method method, Action action) {
            Parameter actionParam = method.getParameters()[0];
            ProxyAction proxyAction = actionParam.getAnnotation(ProxyAction.class);
            if (proxyAction != null) {
                return arg -> action.execute(ProxyFactory.createProxy(arg, proxyAction.value()));
            }

            return arg -> action.execute(createLocalProxy(arg));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Function<Object, Object> adaptFunctionArg(Function func) {
            return arg -> func.apply(createLocalProxy(arg));
        }

        private static Object createLocalProxy(Object target) {
            if (isJdkType(target.getClass())) {
                return target;
            }

            ClassLoader localClassLoader = ProxyFactory.class.getClassLoader();
            return Proxy.newProxyInstance(
                localClassLoader,
                convertTypes(collectInterfaces(target.getClass()), localClassLoader),
                new ProxyingInvocationHandler(target)
            );
        }

        public static Class<?>[] collectInterfaces(final Class<?> type) {
            Set<Class<?>> result = new LinkedHashSet<>();
            collectInterfaces(type, result);
            return result.toArray(new Class<?>[0]);
        }

        private static void collectInterfaces(Class<?> type, Set<Class<?>> result) {
            for (Class<?> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
                for (Class<?> i : candidate.getInterfaces()) {
                    if (result.add(i)) {
                        collectInterfaces(i, result);
                    }
                }
            }
        }

        private static Class<?>[] convertTypes(Class<?>[] parameterTypes, ClassLoader classLoader) {
            if (parameterTypes.length == 0) {
                return parameterTypes;
            }
            return Arrays.stream(parameterTypes)
                .map(type -> {
                    if (isJdkType(type)) {
                        return type;
                    }

                    try {
                        return classLoader.loadClass(type.getName());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load class: " + type.getName(), e);
                    }
                })
                .toArray(Class<?>[]::new);
        }

        private static boolean isJdkType(Class<?> type) {
            ClassLoader typeClassLoader = type.getClassLoader();
            return typeClassLoader == null || typeClassLoader.equals(Object.class.getClassLoader());
        }

    }

}
