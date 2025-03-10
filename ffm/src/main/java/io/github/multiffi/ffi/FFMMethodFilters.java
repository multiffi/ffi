package io.github.multiffi.ffi;

import multiffi.ffi.Foreign;
import multiffi.ffi.MemoryHandle;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class FFMMethodFilters {

    private FFMMethodFilters() {
        throw new AssertionError("No io.github.multiffi.ffi.FFMMethodFilters instances for you!");
    }

    public static MemorySegment handleToSegment(MemoryHandle handle) {
        if (handle == null || handle.isNil()) throw new NullPointerException();
        else if (handle.isDirect()) return MemorySegment.ofAddress(handle.address())
                .reinterpret(handle.isBounded() ? handle.size() : Foreign.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE);
        else {
            Object array = handle.array();
            MemorySegment segment = switch (array) {
                case byte[] byteArray -> MemorySegment.ofArray(byteArray);
                case short[] shortArray -> MemorySegment.ofArray(shortArray);
                case int[] intArray -> MemorySegment.ofArray(intArray);
                case long[] longArray -> MemorySegment.ofArray(longArray);
                case float[] floatArray -> MemorySegment.ofArray(floatArray);
                case double[] doubleArray -> MemorySegment.ofArray(doubleArray);
                case char[] charArray -> MemorySegment.ofArray(charArray);
                case null, default -> throw new IllegalArgumentException("Unsupported array");
            };
            return handle.arrayOffset() == 0L ? segment : segment.asSlice(handle.arrayOffset());
        }
    }
    public static MemoryHandle segmentToHandle(MemorySegment segment) {
        if (segment == null || MemorySegment.NULL.equals(segment)) throw new NullPointerException();
        else if (segment.isNative()) return MemoryHandle.wrap(segment.address(), segment.byteSize());
        else {
            Object array = segment.heapBase().orElse(null);
            return switch (array) {
                case byte[] byteArray -> MemoryHandle.wrap(byteArray);
                case short[] shortArray -> MemoryHandle.wrap(shortArray);
                case int[] intArray -> MemoryHandle.wrap(intArray);
                case long[] longArray -> MemoryHandle.wrap(longArray);
                case float[] floatArray -> MemoryHandle.wrap(floatArray);
                case double[] doubleArray -> MemoryHandle.wrap(doubleArray);
                case char[] charArray -> MemoryHandle.wrap(charArray);
                case null, default -> throw new IllegalArgumentException("Unsupported array");
            };
        }
    }
    public static long segmentToInt64(MemorySegment segment) {
        return segment == null ? 0L : segment.address();
    }
    public static MemorySegment int64ToSegment(long address) {
        return address == 0L ? MemorySegment.NULL : MemorySegment.ofAddress(address);
    }
    public static long int16ToInt64(short value) {
        return (long) value & 0xFFFFL;
    }
    public static long int32ToInt64(int value) {
        return (long) value & 0xFFFFFFFFL;
    }
    public static int utf16ToInt32(char value) {
        return (int) value & 0xFFFF;
    }
    public static short shortToInt16(long value) {
        if (FFMUtil.SHORT_SIZE == 2 && (value > 65535 || value < 0))
            throw new ArithmeticException("integer overflow");
        else return (short) value;
    }
    public static int intToInt32(long value) {
        if (FFMUtil.INT_SIZE == 4 && (((value >> 32) + 1) & ~1) != 0)
            throw new ArithmeticException("integer overflow");
        else return (int) value;
    }
    public static int longToInt32(long value) {
        if (FFMUtil.LONG_SIZE == 4 && (((value >> 32) + 1) & ~1) != 0)
            throw new ArithmeticException("integer overflow");
        else return (int) value;
    }
    public static int sizeToInt32(long value) {
        if (FFMUtil.LONG_SIZE == 4 && (((value >> 32) + 1) & ~1) != 0)
            throw new ArithmeticException("integer overflow");
        else return (int) value;
    }
    public static char wcharToUTF16(int value) {
        if (FFMUtil.WCHAR_SIZE == 2 && (value > 65535 || value < 0))
            throw new ArithmeticException("integer overflow");
        else return (char) value;
    }
    public static int booleanToInt32(boolean value) {
        return value ? 1 : 0;
    }
    public static boolean int32ToBoolean(int value) {
        return value != 0;
    }
    public static long booleanToInt64(boolean value) {
        return value ? 1L : 0L;
    }
    public static boolean int64ToBoolean(long value) {
        return value != 0L;
    }
    public static MethodHandle filterShortArgument(MethodHandle target, int pos, boolean upcall) {
        if (target != null && FFMUtil.SHORT_SIZE == 2) return MethodHandles.filterArguments(target, pos,
                upcall ? INT16_TO_INT64 : SHORT_TO_INT16);
        else return target;
    }
    public static MethodHandle filterIntArgument(MethodHandle target, int pos, boolean upcall) {
        if (target != null && FFMUtil.INT_SIZE == 4) return MethodHandles.filterArguments(target, pos,
                upcall ? INT32_TO_INT64 : INT_TO_INT32);
        else return target;
    }
    public static MethodHandle filterLongArgument(MethodHandle target, int pos, boolean upcall) {
        if (target != null && FFMUtil.LONG_SIZE == 4) return MethodHandles.filterArguments(target, pos,
                upcall ? INT32_TO_INT64 : LONG_TO_INT32);
        else return target;
    }
    public static MethodHandle filterSizeArgument(MethodHandle target, int pos, boolean upcall) {
        if (target != null && FFMUtil.DIFF_SIZE == 4) return MethodHandles.filterArguments(target, pos,
                upcall ? INT32_TO_INT64 : SIZE_TO_INT32);
        else return target;
    }
    public static MethodHandle filterWCharArgument(MethodHandle target, int pos, boolean upcall) {
        if (target != null && FFMUtil.WCHAR_SIZE == 2) return MethodHandles.filterArguments(target, pos,
                upcall ? UTF16_TO_INT32 : WCHAR_TO_UTF16);
        else return target;
    }
    public static MethodHandle filterBooleanArgument(MethodHandle target, int pos, boolean upcall) {
        if (target == null) return target;
        else if (FFMUtil.ADDRESS_SIZE == 8) return MethodHandles.filterArguments(target, pos,
                upcall ? BOOLEAN_TO_INT64 : INT64_TO_BOOLEAN);
        else return MethodHandles.filterArguments(target, pos, upcall ? BOOLEAN_TO_INT32 : INT32_TO_BOOLEAN);
    }
    public static MethodHandle filterShortReturnValue(MethodHandle target, boolean upcall) {
        if (target != null && FFMUtil.SHORT_SIZE == 2) return MethodHandles.filterReturnValue(target,
                upcall ? SHORT_TO_INT16 : INT16_TO_INT64);
        else return target;
    }
    public static MethodHandle filterIntReturnValue(MethodHandle target, boolean upcall) {
        if (target != null && FFMUtil.INT_SIZE == 4) return MethodHandles.filterReturnValue(target,
                upcall ? INT_TO_INT32 : INT32_TO_INT64);
        else return target;
    }
    public static MethodHandle filterLongReturnValue(MethodHandle target, boolean upcall) {
        if (target != null && FFMUtil.LONG_SIZE == 4) return MethodHandles.filterReturnValue(target,
                upcall ? LONG_TO_INT32 : INT32_TO_INT64);
        else return target;
    }
    public static MethodHandle filterSizeReturnValue(MethodHandle target, boolean upcall) {
        if (target != null && FFMUtil.DIFF_SIZE == 4) return MethodHandles.filterReturnValue(target,
                upcall ? SIZE_TO_INT32 : INT32_TO_INT64);
        else return target;
    }
    public static MethodHandle filterWCharReturnValue(MethodHandle target, boolean upcall) {
        if (target != null && FFMUtil.WCHAR_SIZE == 2) return MethodHandles.filterReturnValue(target,
                upcall ? WCHAR_TO_UTF16 : UTF16_TO_INT32);
        else return target;
    }
    public static MethodHandle filterBooleanReturnValue(MethodHandle target, boolean upcall) {
        if (target == null) return null;
        else if (FFMUtil.ADDRESS_SIZE == 8) return MethodHandles.filterReturnValue(target,
                upcall ? INT64_TO_BOOLEAN : BOOLEAN_TO_INT64);
        else return MethodHandles.filterReturnValue(target, upcall ? INT32_TO_BOOLEAN : BOOLEAN_TO_INT32);
    }

    public static final MethodHandle HANDLE_TO_SEGMENT;
    public static final MethodHandle SEGMENT_TO_HANDLE;
    public static final MethodHandle INT16_TO_INT64;
    public static final MethodHandle INT32_TO_INT64;
    public static final MethodHandle SHORT_TO_INT16;
    public static final MethodHandle INT_TO_INT32;
    public static final MethodHandle LONG_TO_INT32;
    public static final MethodHandle SIZE_TO_INT32;
    public static final MethodHandle SEGMENT_TO_INT64;
    public static final MethodHandle INT64_TO_SEGMENT;
    public static final MethodHandle UTF16_TO_INT32;
    public static final MethodHandle WCHAR_TO_UTF16;
    public static final MethodHandle INT64_TO_BOOLEAN;
    public static final MethodHandle BOOLEAN_TO_INT64;
    public static final MethodHandle INT32_TO_BOOLEAN;
    public static final MethodHandle BOOLEAN_TO_INT32;
    static {
        try {
            HANDLE_TO_SEGMENT = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "handleToSegment",
                    MethodType.methodType(MemorySegment.class, MemoryHandle.class));
            SEGMENT_TO_HANDLE = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "segmentToHandle",
                    MethodType.methodType(MemoryHandle.class, MemorySegment.class));
            INT16_TO_INT64 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "int16ToInt64",
                    MethodType.methodType(long.class, short.class));
            INT32_TO_INT64 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "int32ToInt64",
                    MethodType.methodType(long.class, int.class));
            SHORT_TO_INT16 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "shortToInt16",
                    MethodType.methodType(short.class, long.class));
            INT_TO_INT32 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "intToInt32",
                    MethodType.methodType(int.class, long.class));
            LONG_TO_INT32 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "longToInt32",
                    MethodType.methodType(int.class, long.class));
            SIZE_TO_INT32 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "sizeToInt32",
                    MethodType.methodType(int.class, long.class));
            SEGMENT_TO_INT64 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "segmentToInt64",
                    MethodType.methodType(long.class, MemorySegment.class));
            INT64_TO_SEGMENT = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "int64ToSegment",
                    MethodType.methodType(MemorySegment.class, long.class));
            UTF16_TO_INT32 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "utf16ToInt32",
                    MethodType.methodType(int.class, char.class));
            WCHAR_TO_UTF16 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "wcharToUTF16",
                    MethodType.methodType(char.class, int.class));
            INT64_TO_BOOLEAN = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "int64ToBoolean",
                    MethodType.methodType(boolean.class, long.class));
            BOOLEAN_TO_INT64 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "booleanToInt64",
                    MethodType.methodType(long.class, boolean.class));
            INT32_TO_BOOLEAN = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "int32ToBoolean",
                    MethodType.methodType(boolean.class, int.class));
            BOOLEAN_TO_INT32 = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "booleanToInt32",
                    MethodType.methodType(int.class, boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

}
