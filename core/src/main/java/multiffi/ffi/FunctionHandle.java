package multiffi.ffi;

import java.util.List;

public abstract class FunctionHandle {

    public abstract long address();

    public abstract List<ForeignType> getParameterTypes();
    public abstract ForeignType getReturnType();
    public abstract int getFirstVarArgIndex();
    public abstract boolean isDynCall();
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

    private abstract static class InvokeAdapter64 {

        public abstract long invoke(FunctionHandle function, Object... args);

        public static final InvokeAdapter64 SIZE64 = new InvokeAdapter64() {
            @Override
            public long invoke(FunctionHandle function, Object... args) {
                return function.invokeInt64(args);
            }
        };
        public static final InvokeAdapter64 SIZE32 = new InvokeAdapter64() {
            @Override
            public long invoke(FunctionHandle function, Object... args) {
                return function.invokeInt32(args) & 0xFFFFFFFFL;
            }
        };
        public static final InvokeAdapter64 SIZE16 = new InvokeAdapter64() {
            @Override
            public long invoke(FunctionHandle function, Object... args) {
                return function.invokeInt16(args) & 0xFFFFL;
            }
        };

        public static final InvokeAdapter64 SHORT = Foreign.shortSize() == 8L ? SIZE64 : SIZE16;
        public static final InvokeAdapter64 INT = Foreign.intSize() == 8L ? SIZE64 : SIZE32;
        public static final InvokeAdapter64 LONG = Foreign.longSize() == 8L ? SIZE64 : SIZE32;
        public static final InvokeAdapter64 SIZE = Foreign.addressSize() == 8L ? SIZE64 : SIZE32;

    }
    public abstract static class InvokeAdapter32 {

        public abstract int invoke(FunctionHandle function, Object... args);

        public static final InvokeAdapter32 SIZE32 = new InvokeAdapter32() {
            @Override
            public int invoke(FunctionHandle function, Object... args) {
                return function.invokeInt32(args);
            }
        };
        public static final InvokeAdapter32 SIZE16 = new InvokeAdapter32() {
            @Override
            public int invoke(FunctionHandle function, Object... args) {
                return function.invokeInt16(args) & 0xFFFF;
            }
        };

        public static final InvokeAdapter32 WCHAR = Foreign.wcharSize() == 4L ? SIZE32 : SIZE16;

    }

    public byte invokeChar(Object... args) {
        return invokeInt8(args);
    }
    public int invokeWChar(Object... args) {
        return InvokeAdapter32.WCHAR.invoke(this, args);
    }
    public long invokeShort(Object... args) {
        return InvokeAdapter64.SHORT.invoke(this, args);
    }
    public long invokeInt(Object... args) {
        return InvokeAdapter64.INT.invoke(this, args);
    }
    public long invokeLong(Object... args) {
        return InvokeAdapter64.LONG.invoke(this, args);
    }
    public long invokeSize(Object... args) {
        return InvokeAdapter64.SIZE.invoke(this, args);
    }

}
