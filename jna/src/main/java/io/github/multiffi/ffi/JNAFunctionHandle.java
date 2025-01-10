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
    private final InvokeFunction invokeFunction;

    private interface InvokeFunction {
        Object invoke(Object[] args);
    }

    private static final Object[] EMPTY_ARGUMENTS = new Object[0];
    public JNAFunctionHandle(long address, int firstVararg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
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
        if (!JNAUtil.STDCALL_AVAILABLE) stdcall = false;
        this.dyncall = dyncall;
        this.saveErrno = saveErrno;
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        List<ForeignType> parameterTypeList = new ArrayList<>(parameterTypes.length + (addReturnMemoryParameter ? 1 : 0));
        parameterTypeList.addAll(Arrays.asList(parameterTypes));
        if (addReturnMemoryParameter) parameterTypeList.add(0, returnType);
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypeList);
        this.firstVarArgIndex = firstVararg >= 0 ? firstVararg : (dyncall ? parameterTypes.length : -1);
        this.function = Function.getFunction(new Pointer(address));
        int callFlags = (stdcall ? Function.ALT_CONVENTION : Function.C_CONVENTION) | (firstVarArgIndex == -1 ? 0 : (firstVarArgIndex & 0xFF) << 7);
        InvokeFunction invoker;
        InvokeFunction function;
        if (returnType == null) invoker = args -> {
            if (args == null) args = EMPTY_ARGUMENTS;
            else {
                Object[] arguments = new Object[args.length];
                for (int i = 0; i < arguments.length; i ++) {
                    arguments[i] = i < JNAFunctionHandle.this.parameterTypes.size() ?
                            checkArgumentType(JNAFunctionHandle.this.parameterTypes.get(i), args[i]) : checkCompound(args[i]);
                }
                args = arguments;
            }
            JNAUtil.invoke(null, JNAFunctionHandle.this.function, callFlags, args);
            return null;
        };
        else if (returnType.isCompound()) invoker = args -> {
            MemoryHandle memoryHandle = (MemoryHandle) args[0];
            Object[] arguments = new Object[args.length];
            for (int i = 1; i < arguments.length; i ++) {
                arguments[i - 1] = i < JNAFunctionHandle.this.parameterTypes.size() ?
                        checkArgumentType(JNAFunctionHandle.this.parameterTypes.get(i), args[i]) : checkCompound(args[i]);
            }
            JNACompound compound = JNACompound.getInstance(memoryHandle);
            JNAUtil.invoke(compound, JNAFunctionHandle.this.function, callFlags, arguments);
            compound.autoRead();
            return memoryHandle;
        };
        else invoker = args -> {
            if (args == null) args = EMPTY_ARGUMENTS;
            else {
                Object[] arguments = new Object[args.length];
                for (int i = 0; i < arguments.length; i ++) {
                    arguments[i] = i < JNAFunctionHandle.this.parameterTypes.size() ?
                            checkArgumentType(JNAFunctionHandle.this.parameterTypes.get(i), args[i]) : checkCompound(args[i]);
                }
                args = arguments;
            }
            return JNAUtil.invoke(returnType == ScalarType.ADDRESS ? Pointer.class : returnType.carrier(),
                    JNAFunctionHandle.this.function, callFlags, args);
        };
        if (dyncall) function = args -> {
            Object varargs = args[args.length - 1];
            if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
            int varargsLength = Array.getLength(varargs);
            Object[] arguments = new Object[args.length - 1 + varargsLength];
            System.arraycopy(args, 0, arguments, 0, args.length - 1);
            System.arraycopy(varargs, 0, arguments, args.length - 1, varargsLength);
            return invoker.invoke(arguments);
        };
        else function = invoker;
        if (saveErrno) invokeFunction = args -> {
            try {
                return function.invoke(args);
            }
            finally {
                JNALastErrno.dump();
            }
        };
        else invokeFunction = function;
    }

    private static Object checkArgumentType(ForeignType type, Object argument) {
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
