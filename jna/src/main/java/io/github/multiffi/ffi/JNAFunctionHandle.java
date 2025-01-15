package io.github.multiffi.ffi;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import multiffi.ffi.CallOption;
import multiffi.ffi.Foreign;
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

public class JNAFunctionHandle extends FunctionHandle {

    private final Function function;
    private final int firstVarArgIndex;
    private final boolean dyncall;
    private final boolean saveErrno;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final InvocationDelegate<Object[], Object> delegate;

    public JNAFunctionHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        boolean dyncall = false;
        boolean stdcall = false;
        boolean saveErrno = false;
        if (options != null) {
            for (CallOption option : options) {
                if (option.equals(StandardCallOption.DYNCALL)) dyncall = true;
                else if (option.equals(StandardCallOption.SAVE_ERRNO)) saveErrno = true;
                else if (option.equals(StandardCallOption.STDCALL)) stdcall = true;
                else if (!option.equals(StandardCallOption.TRIVIAL) && !option.equals(StandardCallOption.CRITICAL))
                    throw new IllegalArgumentException(option + " not supported");
            }
            if (!JNAUtil.STDCALL_SUPPORTED) stdcall = false;
        }
        this.dyncall = dyncall;
        this.saveErrno = saveErrno;
        int parameterCount = parameterTypes == null || parameterTypes.length == 1 && parameterTypes[0] == null ? 0 : parameterTypes.length;
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        this.returnType = returnType;
        if (parameterCount == 0) this.parameterTypes = addReturnMemoryParameter ? Collections.singletonList(returnType) : Collections.emptyList();
        else {
            List<ForeignType> list = new ArrayList<>(parameterCount + (addReturnMemoryParameter ? 1 : 0));
            if (addReturnMemoryParameter) list.add(returnType);
            list.addAll(Arrays.asList(parameterTypes));
            this.parameterTypes = Collections.unmodifiableList(list);
        }
        this.firstVarArgIndex = firstVarArgIndex >= 0 ? Math.min(firstVarArgIndex, parameterCount) : (dyncall ? parameterCount : -1);
        this.function = Function.getFunction(new Pointer(address));
        int callFlags = (stdcall ? Function.ALT_CONVENTION : Function.C_CONVENTION) | (this.firstVarArgIndex == -1 ? 0 : (this.firstVarArgIndex & 0xFF) << 7);
        InvocationDelegate<Object[], Object> function;
        if (returnType == null) function = args -> {
            if (args == null) args = Util.EMPTY_OBJECT_ARRAY;
            else {
                Object[] arguments = new Object[args.length];
                for (int i = 0; i < arguments.length; i ++) {
                    arguments[i] = i < JNAFunctionHandle.this.parameterTypes.size() ?
                            checkArgument(JNAFunctionHandle.this.parameterTypes.get(i), args[i]) : checkCompound(args[i]);
                }
                args = arguments;
            }
            JNAUtil.invoke(null, JNAFunctionHandle.this.function, callFlags, args);
            return null;
        };
        else if (returnType.isCompound()) function = args -> {
            MemoryHandle memoryHandle = (MemoryHandle) args[0];
            Object[] arguments = new Object[args.length];
            for (int i = 1; i < arguments.length; i ++) {
                arguments[i - 1] = i < JNAFunctionHandle.this.parameterTypes.size() ?
                        checkArgument(JNAFunctionHandle.this.parameterTypes.get(i), args[i]) : checkCompound(args[i]);
            }
            JNACompound compound = JNACompound.getInstance(memoryHandle);
            JNAUtil.invoke(compound, JNAFunctionHandle.this.function, callFlags, arguments);
            compound.autoRead();
            return memoryHandle;
        };
        else function = args -> {
            if (args == null) args = Util.EMPTY_OBJECT_ARRAY;
            else {
                Object[] arguments = new Object[args.length];
                for (int i = 0; i < arguments.length; i ++) {
                    arguments[i] = i < JNAFunctionHandle.this.parameterTypes.size() ?
                            checkArgument(JNAFunctionHandle.this.parameterTypes.get(i), args[i]) : checkCompound(args[i]);
                }
                args = arguments;
            }
            return JNAUtil.invoke(returnType == ScalarType.ADDRESS ? Pointer.class : returnType.carrier(),
                    JNAFunctionHandle.this.function, callFlags, args);
        };
        InvocationDelegate<Object[], Object> delegate;
        if (dyncall) delegate = args -> {
            Object varargs = args[args.length - 1];
            if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
            int varargsLength = Array.getLength(varargs);
            Object[] arguments = new Object[args.length - 1 + varargsLength];
            System.arraycopy(args, 0, arguments, 0, args.length - 1);
            System.arraycopy(varargs, 0, arguments, args.length - 1, varargsLength);
            return function.invoke(arguments);
        };
        else delegate = function;
        if (saveErrno) this.delegate = args -> {
            try {
                return delegate.invoke(args);
            }
            finally {
                JNALastErrno.dump();
            }
        };
        else this.delegate = delegate;
    }

    private static Object checkArgument(ForeignType type, Object argument) {
        if (type == ScalarType.BOOLEAN) return (Boolean) argument;
        else if (type == ScalarType.UTF16) return (Character) argument;
        else if (type == ScalarType.INT8 || type == ScalarType.CHAR) return ((Number) argument).byteValue();
        else if (type == ScalarType.INT16
                || (type == ScalarType.SHORT && Foreign.shortSize() == 2) || (type == ScalarType.WCHAR && Foreign.wcharSize() == 2))
            return ((Number) argument).shortValue();
        else if (type == ScalarType.INT32
                || (type == ScalarType.INT && Foreign.intSize() == 4)
                || (type == ScalarType.LONG && Foreign.longSize() == 4)
                || (type == ScalarType.SIZE && Foreign.addressSize() == 4)
                || (type == ScalarType.WCHAR && Foreign.wcharSize() == 4))
            return ((Number) argument).intValue();
        else if (type == ScalarType.INT64
                || (type == ScalarType.SHORT && Foreign.shortSize() == 8)
                || (type == ScalarType.INT && Foreign.intSize() == 8)
                || (type == ScalarType.LONG && Foreign.longSize() == 8)
                || (type == ScalarType.SIZE && Foreign.addressSize() == 8))
            return ((Number) argument).longValue();
        else if (type == ScalarType.FLOAT) return ((Number) argument).floatValue();
        else if (type == ScalarType.DOUBLE) return ((Number) argument).doubleValue();
        else if (type == ScalarType.ADDRESS) return new Pointer(((Number) argument).longValue());
        else {
            JNACompound compound = JNACompound.getInstance((MemoryHandle) argument);
            compound.autoWrite();
            return compound;
        }
    }

    private static Object checkCompound(Object argument) {
        if (argument instanceof Boolean || argument instanceof Character || argument instanceof Number) return argument;
        else {
            JNACompound compound = JNACompound.getInstance((MemoryHandle) argument);
            compound.autoWrite();
            return compound;
        }
    }

    @Override
    public long address() {
        return Pointer.nativeValue(function);
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
        return function.getCallingConvention() == Function.ALT_CONVENTION;
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
        delegate.invoke(args);
    }

    @Override
    public boolean invokeBoolean(Object... args) {
        return (boolean) delegate.invoke(args);
    }

    @Override
    public byte invokeInt8(Object... args) {
        return ((Number) delegate.invoke(args)).byteValue();
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) delegate.invoke(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return ((Number) delegate.invoke(args)).shortValue();
    }

    @Override
    public int invokeInt32(Object... args) {
        return ((Number) delegate.invoke(args)).intValue();
    }

    @Override
    public long invokeInt64(Object... args) {
        return ((Number) delegate.invoke(args)).longValue();
    }

    @Override
    public float invokeFloat(Object... args) {
        return ((Number) delegate.invoke(args)).floatValue();
    }

    @Override
    public double invokeDouble(Object... args) {
        return ((Number) delegate.invoke(args)).doubleValue();
    }

    @Override
    public long invokeAddress(Object... args) {
        return ((Number) delegate.invoke(args)).longValue();
    }

    @Override
    public MemoryHandle invokeCompound(Object... args) {
        return (MemoryHandle) delegate.invoke(args);
    }

    @Override
    public Object invoke(Object... args) {
        return delegate.invoke(args);
    }

}
