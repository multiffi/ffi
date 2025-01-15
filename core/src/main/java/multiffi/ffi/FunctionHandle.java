package multiffi.ffi;

import java.util.Iterator;
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

    @FunctionalInterface
    private interface InvokeAdapter64 {
        long invoke(FunctionHandle function, Object... args);

        InvokeAdapter64 SIZE64 = FunctionHandle::invokeInt64;
        InvokeAdapter64 SIZE32 = (function, args) -> function.invokeInt32(args) & 0xFFFFFFFFL;
        InvokeAdapter64 SIZE16 = (function, args) -> function.invokeInt16(args) & 0xFFFFL;

        InvokeAdapter64 SHORT = Foreign.shortSize() == 8L ? SIZE64 : SIZE16;
        InvokeAdapter64 INT = Foreign.intSize() == 8L ? SIZE64 : SIZE32;
        InvokeAdapter64 LONG = Foreign.longSize() == 8L ? SIZE64 : SIZE32;
        InvokeAdapter64 SIZE = Foreign.addressSize() == 8L ? SIZE64 : SIZE32;
    }

    @FunctionalInterface
    private interface InvokeAdapter32 {
        int invoke(FunctionHandle function, Object... args);

        InvokeAdapter32 SIZE32 = FunctionHandle::invokeInt32;
        InvokeAdapter32 SIZE16 = (function, args) -> function.invokeInt16(args) & 0xFFFF;

        InvokeAdapter32 WCHAR = Foreign.wcharSize() == 4L ? SIZE32 : SIZE16;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isStdCall()) {
            builder.append("stdcall");
            builder.append(' ');
        }
        else {
            builder.append("cdecl");
            builder.append(' ');
        }
        if (isCritical()) {
            builder.append("critical");
            builder.append(' ');
        }
        else if (isTrivial()) {
            builder.append("trivial");
            builder.append(' ');
        }
        else if (isSaveErrno()) {
            builder.append("capture");
            builder.append(' ');
        }
        ForeignType returnType = getReturnType();
        builder.append(returnType == null ? "void" : returnType);
        builder.append(' ');
        builder.append(String.format("0x%08X", address()));
        builder.append('(');
        Iterator<ForeignType> iterator = getParameterTypes().iterator();
        if (iterator.hasNext()) {
            while (iterator.hasNext()) {
                builder.append(iterator.next());
                if (iterator.hasNext()) builder.append(',').append(' ');
                else {
                    if (isDynCall()) builder.append(',').append(' ').append("...");
                    builder.append(')');
                }
            }
        }
        else {
            if (isDynCall()) builder.append("...");
            builder.append(')');
        }
        return builder.toString();
    }

}
