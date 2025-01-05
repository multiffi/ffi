package io.github.multiffi.ffi;

import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallingConvention;
import jnr.ffi.LastError;
import multiffi.ffi.CallOption;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.StandardCallOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        this.context = CallContext.getCallContext(JNRUtil.toFFIType(returnType), JNRUtil.toFFITypes(parameterTypes), convention, saveErrno);
        JNRInvoker supportedInvoker = null;
        for (JNRInvoker invoker : JNRUtil.InvokerHolder.FAST_INVOKERS) {
            if (invoker.isSupported(context, this.returnType, this.parameterTypes, convention)) {
                supportedInvoker = invoker;
                break;
            }
        }
        if (supportedInvoker == null) {
            if (returnType == null) supportedInvoker = JNRInvoker.Buffer.VOID;
            else if (returnType == ScalarType.INT8 || returnType == ScalarType.CHAR) supportedInvoker = JNRInvoker.Buffer.BYTE;
            else if (returnType == ScalarType.INT16 || returnType == ScalarType.SHORT) supportedInvoker = JNRInvoker.Buffer.SHORT;
            else if (returnType == ScalarType.INT32 || returnType == ScalarType.INT) supportedInvoker = JNRInvoker.Buffer.INT;
            else if (returnType == ScalarType.INT64) supportedInvoker = JNRInvoker.Buffer.LONG;
            else if (returnType == ScalarType.FLOAT) supportedInvoker = JNRInvoker.Buffer.FLOAT;
            else if (returnType == ScalarType.DOUBLE) supportedInvoker = JNRInvoker.Buffer.DOUBLE;
            else if (returnType == ScalarType.LONG) supportedInvoker = JNRInvoker.Buffer.NATIVE_LONG;
            else if (returnType == ScalarType.WCHAR) supportedInvoker = JNRInvoker.Buffer.WCHAR;
            else if (returnType == ScalarType.ADDRESS || returnType == ScalarType.SIZE) supportedInvoker = JNRInvoker.Buffer.ADDRESS;
            else if (returnType == ScalarType.BOOLEAN) supportedInvoker = JNRInvoker.Buffer.BOOLEAN;
            else if (returnType == ScalarType.UTF16) supportedInvoker = JNRInvoker.Buffer.CHAR;
            else supportedInvoker = JNRInvoker.Buffer.STRUCT;
        }
        this.invoker = supportedInvoker;
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

    @Override
    public Object invoke(Object... args) {
        try {
            return invoker.invoke(context, returnType, parameterTypes, address, args);
        }
        finally {
            if (saveErrno) JNRUtil.LastErrnoHolder.ERRNO_THREAD_LOCAL.set(LastError.getLastError(JNRUtil.UnsafeHolder.RUNTIME));
        }
    }

}
