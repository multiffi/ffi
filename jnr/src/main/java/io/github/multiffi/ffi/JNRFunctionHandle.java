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
    private final CallContext context;
    private final CallingConvention convention;
    private final JNRInvoker invoker;
    private final int firstVarArgIndex;
    private final boolean dyncall;
    private final boolean saveErrno;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;

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
        this.context = dyncall ? null : CallContext.getCallContext(JNRUtil.toFFIType(returnType), JNRUtil.toFFITypes(parameterTypes), convention, saveErrno);
        this.invoker = dyncall ? null : JNRInvoker.getSupportedInvoker(context, returnType, this.parameterTypes, convention);
        this.address = address;
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
        invoke(args);
    }

    @Override
    public boolean invokeBoolean(Object... args) {
        return (boolean) invoke(args);
    }

    @Override
    public byte invokeInt8(Object... args) {
        return ((Number) invoke(args)).byteValue();
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) invoke(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return ((Number) invoke(args)).shortValue();
    }

    @Override
    public int invokeInt32(Object... args) {
        return ((Number) invoke(args)).intValue();
    }

    @Override
    public long invokeInt64(Object... args) {
        return ((Number) invoke(args)).longValue();
    }

    @Override
    public float invokeFloat(Object... args) {
        return ((Number) invoke(args)).floatValue();
    }

    @Override
    public double invokeDouble(Object... args) {
        return ((Number) invoke(args)).doubleValue();
    }

    @Override
    public long invokeAddress(Object... args) {
        return ((Number) invoke(args)).longValue();
    }

    @Override
    public MemoryHandle invokeCompound(Object... args) {
        return (MemoryHandle) invoke(args);
    }

    private static Type[] toFFITypes(List<ForeignType> foreignTypes) {
        Type[] types = new Type[foreignTypes.size()];
        for (int i = 0; i < foreignTypes.size(); i ++) {
            types[i] = JNRUtil.toFFIType(Objects.requireNonNull(foreignTypes.get(i)));
        }
        return types;
    }

    @Override
    public Object invoke(Object... args) {
        try {
            if (dyncall) {
                Object varargs = args[args.length - 1];
                if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
                int varargsLength = Array.getLength(varargs);
                Object[] arguments = new Object[args.length - 1 + varargsLength];
                System.arraycopy(args, 0, arguments, 0, args.length - 1);
                System.arraycopy(varargs, 0, arguments, args.length - 1, varargsLength);
                args = arguments;
                boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
                List<ForeignType> parameterTypes = new ArrayList<>(args.length);
                parameterTypes.addAll(this.parameterTypes);
                if (addReturnMemoryParameter) parameterTypes.remove(0);
                for (int i = this.parameterTypes.size(); i < args.length; i ++) {
                    Object arg = args[i];
                    if (arg instanceof Boolean) parameterTypes.add(ScalarType.BOOLEAN);
                    else if (arg instanceof Character) parameterTypes.add(ScalarType.UTF16);
                    else if (arg instanceof Byte) parameterTypes.add(ScalarType.INT8);
                    else if (arg instanceof Short) parameterTypes.add(ScalarType.INT16);
                    else if (arg instanceof Integer) parameterTypes.add(ScalarType.INT32);
                    else if (arg instanceof Long) parameterTypes.add(ScalarType.INT64);
                    else if (arg instanceof Float) parameterTypes.add(ScalarType.FLOAT);
                    else if (arg instanceof Double) parameterTypes.add(ScalarType.DOUBLE);
                    else {
                        MemoryHandle memoryHandle = (MemoryHandle) arg;
                        long size = memoryHandle.size();
                        if (size < 0 || size > (Integer.MAX_VALUE - 8)) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
                        parameterTypes.add(CompoundType.ofArray(ScalarType.INT8, size));
                    }
                }
                CallContext context = CallContext.getCallContext(JNRUtil.toFFIType(returnType), toFFITypes(parameterTypes), convention, saveErrno);
                parameterTypes.add(0, returnType);
                JNRInvoker invoker = JNRInvoker.getSupportedInvoker(context, returnType, parameterTypes, convention);
                return invoker.invoke(context, returnType, parameterTypes, address, args);
            }
            else return invoker.invoke(context, returnType, parameterTypes, address, args.clone());
        }
        finally {
            if (saveErrno) JNRLastErrno.dump();
        }
    }

}
