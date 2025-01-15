package io.github.multiffi.ffi;

import java.lang.reflect.InvocationHandler;

public interface InvocationHandlerDelegate {
    Object invoke(Object proxy, InvocationHandler handler, Object[] args);
}
