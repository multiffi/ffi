package multiffi;

import java.util.List;

public abstract class FunctionPointer {

    public abstract long address();

    public abstract List<ForeignType> getParameterTypes();
    public abstract ForeignType getReturnType();
    public abstract boolean isStdCall();
    public abstract boolean isCritical();
    public abstract boolean isTrivial();
    public abstract boolean isSaveErrno();

    public abstract void invokeVoid(Object... args);
    public abstract boolean invokeBoolean(Object... args);
    public abstract byte invokeInt8(Object... args);
    public abstract char invokeUTF16(Object... args);
    public abstract short invokeInt16(Object... args);
    public abstract int invokeInt32(Object... args);
    public abstract long invokeInt64(Object... args);

    public abstract float invokeFloat(Object... args);
    public abstract double invokeDouble(Object... args);
    public abstract long invokeAddress(Object... args);
    public abstract MemoryHandle invokeCompound(Object... args);
    public abstract Object invoke(Object... args);

    private abstract static class InvokeAdapter {

        public abstract long invoke(FunctionPointer function, Object... args);

        public static final InvokeAdapter SIZE64 = new InvokeAdapter() {
            @Override
            public long invoke(FunctionPointer function, Object... args) {
                return function.invokeInt64(args);
            }
        };

        public static final InvokeAdapter SIZE32 = new InvokeAdapter() {
            @Override
            public long invoke(FunctionPointer function, Object... args) {
                return function.invokeInt32(args) & 0xFFFFFFFFL;
            }
        };

        public static final InvokeAdapter SIZE16 = new InvokeAdapter() {
            @Override
            public long invoke(FunctionPointer function, Object... args) {
                return function.invokeInt16(args) & 0xFFFFL;
            }
        };

        public static final InvokeAdapter SHORT = Foreign.shortSize() == 8 ? SIZE64 : SIZE16;
        public static final InvokeAdapter INT = Foreign.intSize() == 8 ? SIZE64 : SIZE32;
        public static final InvokeAdapter LONG = Foreign.longSize() == 8 ? SIZE64 : SIZE32;
        public static final InvokeAdapter ADDRESS = Foreign.addressSize() == 8 ? SIZE64 : SIZE32;

    }

    public byte invokeChar(Object... args) {
        return invokeInt8(args);
    }
    public long invokeShort(Object... args) {
        return InvokeAdapter.SHORT.invoke(this, args);
    }
    public long invokeInt(Object... args) {
        return InvokeAdapter.INT.invoke(this, args);
    }
    public long invokeLong(Object... args) {
        return InvokeAdapter.LONG.invoke(this, args);
    }

}
