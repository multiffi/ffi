package io.github.multiffi;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import multiffi.CallOption;
import multiffi.Foreign;
import multiffi.ForeignType;
import multiffi.FunctionHandle;
import multiffi.MemoryHandle;
import multiffi.ScalarType;
import multiffi.StandardCallOption;

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
    private final int callFlags;
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
        function = Function.getFunction(new Pointer(address));
        callFlags = (stdcall ? Function.ALT_CONVENTION : Function.C_CONVENTION) | (firstVarArgIndex == -1 ? 0 : (firstVarArgIndex & 0xFF) << 7);
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
        return Pointer.nativeValue((Pointer) invoke(args));
    }

    @Override
    public MemoryHandle invokeCompound(Object... args) {
        return (MemoryHandle) invoke(args);
    }

    private static Object checkArgument(ForeignType type, Object argument) {
        if (type == ScalarType.BOOLEAN) return (Boolean) argument;
        else if (type == ScalarType.UTF16 || (type == ScalarType.WCHAR && Foreign.wcharSize() == 2)) return (Character) argument;
        else if (type == ScalarType.INT8 || type == ScalarType.CHAR) return ((Number) argument).byteValue();
        else if (type == ScalarType.INT16 || (type == ScalarType.SHORT && Foreign.shortSize() == 2)) return ((Number) argument).shortValue();
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

    private static final Object[] EMPTY_ARGUMENTS = new Object[0];
    @Override
    public Object invoke(Object... args) {
        if (args == null) args = EMPTY_ARGUMENTS;
        Object result;
        try {
            if (returnType == null) {
                JNAUtil.invoke(null, function, Pointer.nativeValue(function), callFlags);
                result = null;
            }
            else if (returnType.isCompound()) {
                MemoryHandle memoryHandle = (MemoryHandle) args[0];
                Object[] arguments = new Object[parameterTypes.size() - 1];
                for (int i = 1; i < parameterTypes.size(); i ++) {
                    arguments[i - 1] = checkArgument(parameterTypes.get(i), args[i]);
                }
                JNACompound compound = JNACompound.getInstance(memoryHandle);
                JNAUtil.invoke(compound, function, Pointer.nativeValue(function), callFlags, arguments);
                compound.autoRead();
                result = memoryHandle;
            }
            else {
                Object[] arguments = new Object[args.length];
                for (int i = 0; i < arguments.length; i ++) {
                    arguments[i] = checkArgument(parameterTypes.get(i), args[i]);
                }
                result = JNAUtil.invoke(returnType.carrier(), function, Pointer.nativeValue(function), callFlags, arguments);
            }
        }
        finally {
            if (saveErrno) JNAUtil.LastErrnoHolder.ERRNO_THREAD_LOCAL.set(Native.getLastError());
        }
        return result;
    }

}
