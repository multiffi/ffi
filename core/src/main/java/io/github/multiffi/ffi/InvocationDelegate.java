package io.github.multiffi.ffi;

@FunctionalInterface
public interface InvocationDelegate<T, R> {
    R invoke(T t);
}
