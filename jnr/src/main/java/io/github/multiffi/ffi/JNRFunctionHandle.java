package io.github.multiffi.ffi;

import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallingConvention;
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

public class JNRFunctionHandle extends FunctionHandle {

    private final long address;
    private final CallingConvention convention;
    private final int firstVarArgIndex;
    private final boolean dyncall;
    private final boolean saveErrno;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final InvocationDelegate<Object[], Object> delegate;

    public JNRFunctionHandle(long address, int firstVarArgIndex, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
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
            if (!JNRUtil.STDCALL_SUPPORTED) stdcall = false;
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
        this.convention = stdcall ? CallingConvention.STDCALL : CallingConvention.DEFAULT;
        this.address = address;
        InvocationDelegate<Object[], Object> delegate;
        if (dyncall) delegate = args -> {
            if (args == null || args.length != this.parameterTypes.size() + 1) throw new ArrayIndexOutOfBoundsException("length mismatch");
            int fixedArgsLength = args.length - 1;
            Object varargs = args[fixedArgsLength];
            if (!varargs.getClass().isArray()) throw new IllegalArgumentException("Last argument must be array as variadic arguments");
            int varargsLength = Array.getLength(varargs);
            Object[] arguments = new Object[fixedArgsLength + varargsLength];
            System.arraycopy(args, 0, arguments, 0, fixedArgsLength);
            System.arraycopy(varargs, 0, arguments, fixedArgsLength, varargsLength);
            List<ForeignType> parameterForeignTypes = new ArrayList<>(arguments.length);
            parameterForeignTypes.addAll(JNRFunctionHandle.this.parameterTypes);
            if (addReturnMemoryParameter) parameterForeignTypes.remove(0);
            for (int i = JNRFunctionHandle.this.parameterTypes.size(); i < arguments.length; i ++) {
                Object arg = arguments[i];
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
            CallContext context = CallContext.getCallContext(JNRUtil.toFFIType(returnType), JNRUtil.toFFITypes(parameterForeignTypes), convention, JNRFunctionHandle.this.saveErrno);
            parameterForeignTypes.add(0, returnType);
            JNRInvoker invoker = JNRInvoker.getSupportedInvoker(context, returnType, parameterForeignTypes, convention);
            return invoker.invoke(context, returnType, parameterForeignTypes, address, arguments);
        };
        else {
            CallContext context = CallContext.getCallContext(JNRUtil.toFFIType(returnType), parameterTypes == null ? JNRUtil.EMPTY_TYPE_ARRAY : JNRUtil.toFFITypes(parameterTypes), convention, saveErrno);
            JNRInvoker invoker = JNRInvoker.getSupportedInvoker(context, returnType, this.parameterTypes, convention);
            delegate = args -> invoker.invoke(context, returnType, JNRFunctionHandle.this.parameterTypes, address, args == null ? Util.EMPTY_OBJECT_ARRAY : args.clone());
        }
        if (saveErrno) this.delegate = args -> {
            try {
                return delegate.invoke(args);
            }
            finally {
                JNRLastErrno.dump();
            }
        };
        else this.delegate = delegate;
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
