package com.gradle.cuud.adapters;

import org.gradle.api.Action;
import org.mockito.stubbing.Stubber;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.doAnswer;

public final class ActionMockFixtures {

    private ActionMockFixtures() {
    }

    public static <T> Stubber doExecuteActionWith(T obj) {
        return doAnswer(invocation -> {
            Action<? super T> action = invocation.getArgument(0);
            action.execute(obj);
            return null;
        });
    }

    public static class ArgCapturingAction<T> implements Action<T> {

        private final AtomicReference<T> arg = new AtomicReference<>();

        @Override
        public void execute(T t) {
            arg.set(t);
        }

        public T getValue() {
            return arg.get();
        }
    }

}
