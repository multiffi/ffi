package io.github.multiffi.ffi;

import com.kenai.jffi.CallContext;
import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.HeapInvocationBuffer;
import com.kenai.jffi.Invoker;
import com.kenai.jffi.Type;
import jnr.ffi.Platform;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;

import java.util.List;

public abstract class JNRInvoker {
    
    private static final Invoker INVOKER = Invoker.getInstance();
    
    public static abstract class FastInt extends JNRInvoker {
        
        public static final JNRInvoker I0 = new FastInt(0) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI0(context, function);
            }
        };
        public static final JNRInvoker I1 = new FastInt(1) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI1(context, function, intValue((Number) args[0]));
            }
        };
        public static final JNRInvoker I2 = new FastInt(2) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI2(context, function, intValue((Number) args[0]), intValue((Number) args[1]));
            }
        };
        public static final JNRInvoker I3 = new FastInt(3) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI3(context, function, intValue((Number) args[0]), intValue((Number) args[1]), intValue((Number) args[2]));
            }
        };
        public static final JNRInvoker I4 = new FastInt(4) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI4(context, function, intValue((Number) args[0]), intValue((Number) args[1]), intValue((Number) args[2]), 
                        intValue((Number) args[3]));
            }
        };
        public static final JNRInvoker I5 = new FastInt(5) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI5(context, function, intValue((Number) args[0]), intValue((Number) args[1]), intValue((Number) args[2]),
                        intValue((Number) args[3]), intValue((Number) args[4]));
            }
        };
        public static final JNRInvoker I6 = new FastInt(6) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeI6(context, function, intValue((Number) args[0]), intValue((Number) args[1]), intValue((Number) args[2]),
                        intValue((Number) args[3]), intValue((Number) args[4]), intValue((Number) args[5]));
            }
        };
        
        private static final boolean ENABLED = JNRUtil.getBooleanProperty("jnr.ffi.fast-int.enabled", true);
        private final int parameters;
        private FastInt(int parameters) {
            this.parameters = parameters;
        }
        @Override
        public boolean isSupported(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, CallingConvention convention) {
            if (!ENABLED) return false;
            if (!convention.equals(CallingConvention.DEFAULT) || context.getParameterCount() > parameters) return false;
            if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) return false;
            if (JNRUtil.PLATFORM.getCPU() != Platform.CPU.I386 && JNRUtil.PLATFORM.getCPU() != Platform.CPU.X86_64) return false;
            for (int i = 0; i < context.getParameterCount(); i ++) {
                if (!isFastIntType(context.getParameterType(i))) return false;
            }
            return isFastIntType(context.getReturnType()) || context.getReturnType() == Type.VOID;
        }
        private static boolean isFastIntType(Type type) {
            return type.size() <= 4 && (type == Type.SCHAR || type == Type.UCHAR || type == Type.SSHORT || type == Type.USHORT
                    || type == Type.SINT || type == Type.UINT || type == Type.SLONG || type == Type.ULONG || type == Type.POINTER);
        }
        private static int intValue(Number number) {
            if (number instanceof Byte) return number.byteValue() & 0xFF;
            else if (number instanceof Short) return number.shortValue() & 0xFFFF;
            else return number.intValue();
        }
        
    }

    public static abstract class FastLong extends JNRInvoker {

        public static final JNRInvoker L0 = new FastLong(0) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL0(context, function);
            }
        };
        public static final JNRInvoker L1 = new FastLong(1) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL1(context, function, longValue((Number) args[0]));
            }
        };
        public static final JNRInvoker L2 = new FastLong(2) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL2(context, function, longValue((Number) args[0]), longValue((Number) args[1]));
            }
        };
        public static final JNRInvoker L3 = new FastLong(3) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL3(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]));
            }
        };
        public static final JNRInvoker L4 = new FastLong(4) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL4(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]),
                        longValue((Number) args[3]));
            }
        };
        public static final JNRInvoker L5 = new FastLong(5) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL5(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]),
                        longValue((Number) args[3]), longValue((Number) args[4]));
            }
        };
        public static final JNRInvoker L6 = new FastLong(6) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return INVOKER.invokeL6(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]),
                        longValue((Number) args[3]), longValue((Number) args[4]), longValue((Number) args[5]));
            }
        };

        private static final boolean ENABLED = JNRUtil.getBooleanProperty("jnr.ffi.fast-long.enabled", true);
        private final int parameters;
        private FastLong(int parameters) {
            this.parameters = parameters;
        }
        @Override
        public boolean isSupported(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, CallingConvention convention) {
            if (!ENABLED) return false;
            if (!convention.equals(CallingConvention.DEFAULT) || context.getParameterCount() > parameters) return false;
            if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) return false;
            if (JNRUtil.PLATFORM.getCPU() != Platform.CPU.X86_64) return false;
            for (int i = 0; i < context.getParameterCount(); i ++) {
                if (!isFastLongType(context.getParameterType(i))) return false;
            }
            return isFastLongType(context.getReturnType()) || context.getReturnType() == Type.VOID;
        }
        private static boolean isFastLongType(Type type) {
            return type == Type.SCHAR || type == Type.UCHAR || type == Type.SSHORT || type == Type.USHORT
                    || type == Type.SINT || type == Type.UINT || type == Type.SLONG || type == Type.ULONG || type == Type.POINTER
                    || type == Type.SLONG_LONG || type == Type.ULONG_LONG;
        }
        private static long longValue(Number number) {
            if (number instanceof Byte) return number.byteValue() & 0xFFL;
            else if (number instanceof Short) return number.shortValue() & 0xFFFFL;
            else if (number instanceof Integer) return number.intValue() & 0xFFFFFFFFL;
            else return number.longValue();
        }

    }

    public static abstract class FastNumeric extends JNRInvoker {

        public static final JNRInvoker N0 = new FastNumeric(0) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN0(context, function));
            }
        };
        public static final JNRInvoker N1 = new FastNumeric(1) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN1(context, function, longValue((Number) args[0])));
            }
        };
        public static final JNRInvoker N2 = new FastNumeric(2) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN2(context, function, longValue((Number) args[0]), longValue((Number) args[1])));
            }
        };
        public static final JNRInvoker N3 = new FastNumeric(3) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN3(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2])));
            }
        };
        public static final JNRInvoker N4 = new FastNumeric(4) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN4(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]),
                        longValue((Number) args[3])));
            }
        };
        public static final JNRInvoker N5 = new FastNumeric(5) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN5(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]),
                        longValue((Number) args[3]), longValue((Number) args[4])));
            }
        };
        public static final JNRInvoker N6 = new FastNumeric(6) {
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                return objectValue(returnType, INVOKER.invokeN6(context, function, longValue((Number) args[0]), longValue((Number) args[1]), longValue((Number) args[2]),
                        longValue((Number) args[3]), longValue((Number) args[4]), longValue((Number) args[5])));
            }
        };

        private static final boolean ENABLED = JNRUtil.getBooleanProperty("jnr.ffi.fast-numeric.enabled", true);
        private final int parameters;
        private FastNumeric(int parameters) {
            this.parameters = parameters;
        }
        @Override
        public boolean isSupported(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, CallingConvention convention) {
            if (!ENABLED) return false;
            if (!convention.equals(CallingConvention.DEFAULT) || context.getParameterCount() > parameters) return false;
            if (JNRUtil.PLATFORM.getOS() == Platform.OS.WINDOWS) return false;
            if (JNRUtil.PLATFORM.getCPU() != Platform.CPU.I386 && JNRUtil.PLATFORM.getCPU() != Platform.CPU.X86_64) return false;
            for (int i = 0; i < context.getParameterCount(); i ++) {
                if (!isFastNumericType(context.getParameterType(i))) return false;
            }
            return isFastNumericType(context.getReturnType()) || context.getReturnType() == Type.VOID;
        }
        private static boolean isFastNumericType(Type type) {
            return type == Type.SCHAR || type == Type.UCHAR || type == Type.SSHORT || type == Type.USHORT
                    || type == Type.SINT || type == Type.UINT || type == Type.SLONG || type == Type.ULONG || type == Type.POINTER
                    || type == Type.SLONG_LONG || type == Type.ULONG_LONG || type == Type.FLOAT || type == Type.DOUBLE;
        }
        private static long longValue(Number number) {
            if (number instanceof Byte) return number.byteValue() & 0xFFL;
            else if (number instanceof Short) return number.shortValue() & 0xFFFFL;
            else if (number instanceof Integer) return number.intValue() & 0xFFFFFFFFL;
            else if (number instanceof Float) return Float.floatToRawIntBits(number.floatValue()) & 0xFFFFFFFFL;
            else if (number instanceof Double) return Double.doubleToRawLongBits(number.doubleValue());
            else return number.longValue();
        }
        private static Object objectValue(ForeignType returnType, long value) {
            if (returnType == null) return null;
            else if (returnType == ScalarType.FLOAT) return Float.intBitsToFloat((int) value);
            else if (returnType == ScalarType.DOUBLE) return Double.longBitsToDouble(value);
            else return value;
        }

    }

    public static abstract class Buffer extends JNRInvoker {

        public static final JNRInvoker VOID = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                INVOKER.invokeInt(context, function, heapInvocationBuffer);
                return null;
            }
        };
        public static final JNRInvoker BOOLEAN = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeInt(context, function, heapInvocationBuffer) != 0;
            }
        };
        public static final JNRInvoker BYTE = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return (byte) INVOKER.invokeInt(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker SHORT = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return (short) INVOKER.invokeInt(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker CHAR = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return (char) INVOKER.invokeInt(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker INT = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeInt(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker LONG = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeLong(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker FLOAT = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeFloat(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker DOUBLE = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeDouble(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker ADDRESS = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeAddress(context, function, heapInvocationBuffer);
            }
        };
        public static final JNRInvoker NATIVE_LONG = JNRUtil.UnsafeHolder.RUNTIME.longSize() == 8 ? LONG : new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeInt(context, function, heapInvocationBuffer) & 0xFFFFFFFFL;
            }
        };
        public static final JNRInvoker WCHAR = JNRUtil.WCHAR_SIZE == 4 ? INT : new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return (short) INVOKER.invokeInt(context, function, heapInvocationBuffer) & 0xFFFF;
            }
        };
        public static final JNRInvoker STRUCT = new Buffer() {
            @Override
            protected Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer) {
                return INVOKER.invokeStruct(context, function, heapInvocationBuffer);
            }
            @Override
            public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
                if (returnType != null && returnType.isCompound()) {
                    MemoryHandle memoryHandle = (MemoryHandle) args[0];
                    memoryHandle.setInt8Array(0, (byte[]) super.invoke(context, returnType, parameterTypes, function, args));
                    return memoryHandle;
                }
                else return super.invoke(context, returnType, parameterTypes, function, args);
            }
        };

        @Override
        public boolean isSupported(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, CallingConvention convention) {
            return true;
        }
        protected abstract Object invoke(CallContext context, long function, HeapInvocationBuffer heapInvocationBuffer);

        @Override
        public Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args) {
            HeapInvocationBuffer heapInvocationBuffer = new HeapInvocationBuffer(context);
            for (int i = (returnType != null && returnType.isCompound()) ? 1 : 0; i < parameterTypes.size(); i ++) {
                ForeignType parameterType = parameterTypes.get(i);
                Object arg = args[i];
                if (parameterType == ScalarType.BOOLEAN) heapInvocationBuffer.putByte(((boolean) arg) ? 1 : 0);
                else if (parameterType == ScalarType.INT8 || parameterType == ScalarType.CHAR)
                    heapInvocationBuffer.putByte(((Number) arg).byteValue() & 0xFF);
                else if (parameterType == ScalarType.INT16 || parameterType == ScalarType.SHORT)
                    heapInvocationBuffer.putShort(((Number) arg).shortValue() & 0xFFFF);
                else if (parameterType == ScalarType.INT32 || parameterType == ScalarType.INT
                        || (parameterType == ScalarType.LONG && Foreign.longSize() == 4L))
                    heapInvocationBuffer.putInt(((Number) arg).intValue());
                else if (parameterType == ScalarType.INT64 || (parameterType == ScalarType.LONG && Foreign.longSize() == 8L))
                    heapInvocationBuffer.putLong(((Number) arg).longValue());
                else if (parameterType == ScalarType.FLOAT) heapInvocationBuffer.putFloat(((Number) arg).floatValue());
                else if (parameterType == ScalarType.DOUBLE) heapInvocationBuffer.putDouble(((Number) arg).doubleValue());
                else if (parameterType == ScalarType.ADDRESS || parameterType == ScalarType.SIZE)
                    heapInvocationBuffer.putAddress(((Number) arg).longValue());
                else {
                    MemoryHandle memoryHandle = (MemoryHandle) arg;
                    if (memoryHandle.isDirect()) heapInvocationBuffer.putStruct(memoryHandle.address());
                    else if (memoryHandle.array() instanceof byte[] && memoryHandle.arrayOffset() >= 0
                            && memoryHandle.arrayOffset() < (Integer.MAX_VALUE - 8))
                        heapInvocationBuffer.putStruct((byte[]) memoryHandle.array(), (int) memoryHandle.arrayOffset());
                    else {
                        byte[] array = new byte[(int) parameterType.size()];
                        memoryHandle.getInt8Array(0, array);
                        heapInvocationBuffer.putStruct(array, 0);
                    }
                }
            }
            return invoke(context, function, heapInvocationBuffer);
        }

    }

    public abstract boolean isSupported(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, CallingConvention convention);
    public abstract Object invoke(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, long function, Object... args);

    public static JNRInvoker getSupportedInvoker(CallContext context, ForeignType returnType, List<ForeignType> parameterTypes, CallingConvention convention) {
        for (JNRInvoker invoker : JNRUtil.InvokerHolder.FAST_INVOKERS) {
            if (invoker.isSupported(context, returnType, parameterTypes, convention)) return invoker;
        }
        if (returnType == null) return JNRInvoker.Buffer.VOID;
        else if (returnType == ScalarType.INT8 || returnType == ScalarType.CHAR) return JNRInvoker.Buffer.BYTE;
        else if (returnType == ScalarType.INT16 || returnType == ScalarType.SHORT) return JNRInvoker.Buffer.SHORT;
        else if (returnType == ScalarType.INT32 || returnType == ScalarType.INT) return JNRInvoker.Buffer.INT;
        else if (returnType == ScalarType.INT64) return JNRInvoker.Buffer.LONG;
        else if (returnType == ScalarType.FLOAT) return JNRInvoker.Buffer.FLOAT;
        else if (returnType == ScalarType.DOUBLE) return JNRInvoker.Buffer.DOUBLE;
        else if (returnType == ScalarType.LONG) return JNRInvoker.Buffer.NATIVE_LONG;
        else if (returnType == ScalarType.WCHAR) return JNRInvoker.Buffer.WCHAR;
        else if (returnType == ScalarType.ADDRESS || returnType == ScalarType.SIZE) return JNRInvoker.Buffer.ADDRESS;
        else if (returnType == ScalarType.BOOLEAN) return JNRInvoker.Buffer.BOOLEAN;
        else if (returnType == ScalarType.UTF16) return JNRInvoker.Buffer.CHAR;
        else return JNRInvoker.Buffer.STRUCT;
    }
    
}
