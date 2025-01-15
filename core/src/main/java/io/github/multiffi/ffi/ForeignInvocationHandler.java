package io.github.multiffi.ffi;

import multiffi.ffi.CallOption;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.FunctionOptionVisitor;
import multiffi.ffi.spi.ForeignProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ForeignInvocationHandler implements InvocationHandler {

    private final Map<Method, InvocationHandlerDelegate> delegateMap;
    public ForeignInvocationHandler(ForeignProvider foreign, Class<?>[] classes, FunctionOptionVisitor functionOptionVisitor) {
        Map<Method, InvocationHandlerDelegate> delegateMap = new HashMap<>();
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                String methodName = method.getName();
                Class<?> returnType = method.getReturnType();
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (methodName.equals("hashCode") && returnType == int.class && parameterTypes.length == 0) delegateMap.put(method,
                        (proxy, handler, args) -> handler.hashCode());
                else if (methodName.equals("equals") && returnType == boolean.class && parameterTypes.length == 1) delegateMap.put(method,
                        (proxy, handler, args) -> {
                    Object other = args[0];
                    if (other == null) return false;
                    if (Proxy.isProxyClass(other.getClass())) return handler == Proxy.getInvocationHandler(other);
                    else return handler == other;
                });
                else if (methodName.equals("toString") && returnType == String.class && parameterTypes.length == 0) delegateMap.put(method,
                        (proxy, handler, args) -> proxy.getClass().getName() + "@" + Integer.toHexString(hashCode()));
                else {
                    long address = functionOptionVisitor.visitAddress(method);
                    int firstVarArgIndex = functionOptionVisitor.visitFirstVarArgIndex(method);
                    ForeignType returnForeignType = functionOptionVisitor.visitReturnType(method);
                    ForeignType[] parameterForeignTypes = functionOptionVisitor.visitParameterTypes(method);
                    CallOption[] callOptions = functionOptionVisitor.visitCallOptions(method);
                    FunctionHandle functionHandle = foreign.downcallHandle(address, firstVarArgIndex, returnForeignType, parameterForeignTypes, callOptions);
                    InvocationHandlerDelegate delegate;
                    if (returnType == void.class) delegate = (proxy, handler, args) -> {
                        functionHandle.invokeVoid(args);
                        return null;
                    };
                    else if (returnType == boolean.class) delegate = (proxy, handler, args) -> (Boolean) functionHandle.invoke(args);
                    else if (returnType == char.class) delegate = (proxy, handler, args) -> (Character) functionHandle.invoke(args);
                    else if (returnType == byte.class) delegate = (proxy, handler, args) -> ((Number) functionHandle.invoke(args)).byteValue();
                    else if (returnType == short.class) delegate = (proxy, handler, args) -> ((Number) functionHandle.invoke(args)).shortValue();
                    else if (returnType == int.class) delegate = (proxy, handler, args) -> ((Number) functionHandle.invoke(args)).intValue();
                    else if (returnType == long.class) delegate = (proxy, handler, args) -> ((Number) functionHandle.invoke(args)).longValue();
                    else if (returnType == float.class) delegate = (proxy, handler, args) -> ((Number) functionHandle.invoke(args)).floatValue();
                    else if (returnType == double.class) delegate = (proxy, handler, args) -> ((Number) functionHandle.invoke(args)).doubleValue();
                    else delegate = (proxy, handler, args) -> (MemoryHandle) functionHandle.invoke(args);
                    delegateMap.put(method, delegate);
                }
            }
        }
        this.delegateMap = Collections.unmodifiableMap(delegateMap);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return delegateMap.get(method).invoke(proxy, this, args);
    }

}