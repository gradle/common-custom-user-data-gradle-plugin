package com.gradle.ccud.adapters.enterprise;

import org.gradle.api.Action;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Stubber;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
abstract class BaseProxyTest {

    protected static <T> Stubber doExecuteActionWith(T obj) {
        return doAnswer(invocation -> {
            Action<? super T> action = invocation.getArgument(0);
            action.execute(obj);
            return null;
        });
    }

    protected static class ArgCapturingAction<T> implements Action<T> {

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
