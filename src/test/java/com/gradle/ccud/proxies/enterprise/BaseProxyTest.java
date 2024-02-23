package com.gradle.ccud.proxies.enterprise;

import org.gradle.api.Action;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Stubber;

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

}
