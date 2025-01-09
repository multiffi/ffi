package io.github.multiffi.ffi;

import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Type;
import multiffi.ffi.CallOption;
import multiffi.ffi.CompoundType;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.StandardCallOption;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JNRFunctionHandle extends FunctionHandle {

    private final long address;
    private final CallingConvention convention;
    private final int firstVarArgIndex;
    private final boolean dyncall;
    private final boolean saveErrno;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final InvokeFunction invokeFunction;

    private interface InvokeFunction {
        Object invoke(Object[] args);
    }

    private static final Object[] EMPTY_ARGUMENTS = new Object[0];
    public JNRFunctionHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        boolean dyncall = false;
        boolean stdcall = false;
        boolean saveErrno = false;
        for (CallOption option : options) {
            if (option.equals(StandardCallOption.DYNCALL)) dyncall = true;
            else if (option.equals(StandardCallOption.SAVE_ERRNO)) saveErrno = true;
            else if (option.equals(StandardCallOption.STDCALL)) stdcall = true;
            else if (!option.equals(StandardCallOption.TRIVIAL) && !option.equals(StandardCallOption.CRITICAL))
                throw new IllegalArgumentException(option + " not supported");
        }
        if (!JNRUtil.STDCALL_AVAILABLE) stdcall = false;
        this.dyncall = dyncall;
        this.saveErrno = saveErrno;
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        List<ForeignType> parameterTypeList = new ArrayList<>(parameterTypes.length + (addReturnMemoryParameter ? 1 : 0));
        parameterTypeList.addAll(Arrays.asList(parameterTypes));
        if (addReturnMemoryParameter) parameterTypeList.add(0, returnType);
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypeList);
        this.firstVarArgIndex = firstVararg >= 0 ? firstVararg : (dyncall ? parameterTypes.length : -1);
        this.convention = stdcall ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
        this.address = address;
        InvokeFunction function;
        if (dyncall) function = args -> {
            Object varargs = args[args.length - 1];
            if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
            int varargsLength = Array.getLength(varargs);
            Object[] arguments = new Object[args.length - 1 + varargsLength];
            System.arraycopy(args, 0, arguments, 0, args.length - 1);
            System.arraycopy(varargs, 0, arguments, args.length - 1, varargsLength);
            args = arguments;
            List<ForeignType> parameterForeignTypes = new ArrayList<>(args.length);
            parameterForeignTypes.addAll(JNRFunctionHandle.this.parameterTypes);
            if (addReturnMemoryParameter) parameterForeignTypes.remove(0);
            for (int i = JNRFunctionHandle.this.parameterTypes.size(); i < args.length; i ++) {
                Object arg = args[i];
                if (arg instanceof Boolean) parameterForeignTypes.add(ScalarType.BOOLEAN);
                else if (arg instanceof Character) parameterForeignTypes.add(ScalarType.UTF16);
                else if (arg instanceof Byte) parameterForeignTypes.add(ScalarType.INT8);
                else if (arg instanceof Short) parameterForeignTypes.add(ScalarType.INT16);
                else if (arg instanceof Integer) parameterForeignTypes.add(ScalarType.INT32);
                else if (arg instanceof Long) parameterForeignTypes.add(ScalarType.INT64);
                else if (arg instanceof Float) parameterForeignTypes.add(ScalarType.FLOAT);
                else if (arg instanceof Double) parameterForeignTypes.add(ScalarType.DOUBLE);
                else {
                    MemoryHandle memoryHandle = (MemoryHandle) arg;
                    long size = memoryHandle.size();
                    if (size < 0 || size > (Integer.MAX_VALUE - 8)) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
                    parameterForeignTypes.add(CompoundType.ofArray(ScalarType.INT8, size));
                }
            }
            CallContext context = CallContext.getCallContext(JNRUtil.toFFIType(returnType), toFFITypes(parameterForeignTypes), convention, JNRFunctionHandle.this.saveErrno);
            parameterForeignTypes.add(0, returnType);
            JNRInvoker invoker = JNRInvoker.getSupportedInvoker(context, returnType, parameterForeignTypes, convention);
            return invoker.invoke(context, returnType, parameterForeignTypes, address, args);
        };
        else {
            CallContext context = CallContext.getCallContext(JNRUtil.toFFIType(returnType), JNRUtil.toFFITypes(parameterTypes), convention, saveErrno);
            JNRInvoker invoker = JNRInvoker.getSupportedInvoker(context, returnType, this.parameterTypes, convention);
            function = args -> invoker.invoke(context, returnType, JNRFunctionHandle.this.parameterTypes, address, args == null ? EMPTY_ARGUMENTS : args.clone());
        }
        if (saveErrno) invokeFunction = args -> {
            try {
                return function.invoke(args);
            }
            finally {
                JNRLastErrno.dump();
            }
        };
        else invokeFunction = function;
    }

    private static Type[] toFFITypes(List<ForeignType> foreignTypes) {
        Type[] types = new Type[foreignTypes.size()];
        for (int i = 0; i < foreignTypes.size(); i ++) {
            types[i] = JNRUtil.toFFIType(Objects.requireNonNull(foreignTypes.get(i)));
        }
        return types;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public List<ForeignType> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public ForeignType getReturnType() {
        return returnType;
    }

    @Override
    public int getFirstVarArgIndex() {
        return firstVarArgIndex;
    }

    @Override
    public boolean isDynCall() {
        return dyncall;
    }

    @Override
    public boolean isStdCall() {
        return convention == CallingConvention.STDCALL;
    }

    @Override
    public boolean isCritical() {
        return false;
    }

    @Override
    public boolean isTrivial() {
        return false;
    }

    @Override
    public boolean isSaveErrno() {
        return saveErrno;
    }

    @Override
    public void invokeVoid(Object... args) {
        invokeFunction.invoke(args);
    }

    @Override
    public boolean invokeBoolean(Object... args) {
        return (boolean) invokeFunction.invoke(args);
    }

    @Override
    public byte invokeInt8(Object... args) {
        return ((Number) invokeFunction.invoke(args)).byteValue();
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) invokeFunction.invoke(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return ((Number) invokeFunction.invoke(args)).shortValue();
    }

    @Override
    public int invokeInt32(Object... args) {
        return ((Number) invokeFunction.invoke(args)).intValue();
    }

    @Override
    public long invokeInt64(Object... args) {
        return ((Number) invokeFunction.invoke(args)).longValue();
    }

    @Override
    public float invokeFloat(Object... args) {
        return ((Number) invokeFunction.invoke(args)).floatValue();
    }

    @Override
    public double invokeDouble(Object... args) {
        return ((Number) invokeFunction.invoke(args)).doubleValue();
    }

    @Override
    public long invokeAddress(Object... args) {
        return ((Number) invokeFunction.invoke(args)).longValue();
    }

    @Override
    public MemoryHandle invokeCompound(Object... args) {
        return (MemoryHandle) invokeFunction.invoke(args);
    }

    @Override
    public Object invoke(Object... args) {
        return invokeFunction.invoke(args);
    }

}
