package io.github.multiffi;

import multiffi.Foreign;
import multiffi.MemoryHandle;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class FFMMethodFilters {

    private FFMMethodFilters() {
        throw new AssertionError("No io.github.multiffi.FFMMethodFilters instances for you!");
    }

    private static MemorySegment handleToSegment(MemoryHandle handle) {
        if (handle == null || handle.isNil()) throw new NullPointerException();
        else if (handle.isDirect()) return MemorySegment.ofAddress(handle.address())
                .reinterpret(handle.isBounded() ? handle.size() : Foreign.addressSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE);
        else {
            Object array = handle.array();
            return switch (array) {
                case byte[] byteArray -> MemorySegment.ofArray(byteArray);
                case short[] shortArray -> MemorySegment.ofArray(shortArray);
                case int[] intArray -> MemorySegment.ofArray(intArray);
                case long[] longArray -> MemorySegment.ofArray(longArray);
                case float[] floatArray -> MemorySegment.ofArray(floatArray);
                case double[] doubleArray -> MemorySegment.ofArray(doubleArray);
                case char[] charArray -> MemorySegment.ofArray(charArray);
                case null, default -> throw new IllegalStateException("Unexpected exception");
            };
        }
    }
    private static MemoryHandle segmentToHandle(MemorySegment segment) {
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
                case null, default -> throw new IllegalStateException("Unexpected exception");
            };
        }
    }
    private static short longToShort(long x) {
        return (short) x;
    }
    private static long shortToLong(short x) {
        return x & 0xFFFFL;
    }
    private static int longToInt(long x) {
        return (int) x;
    }
    private static long intToLong(int x) {
        return x & 0xFFFFFFFFL;
    }

    public static final MethodHandle HANDLE_TO_SEGMENT;
    public static final MethodHandle SEGMENT_TO_HANDLE;
    public static final MethodHandle SHORT_TO_LONG;
    public static final MethodHandle LONG_TO_SHORT;
    public static final MethodHandle INT_TO_LONG;
    public static final MethodHandle LONG_TO_INT;
    static {
        try {
            HANDLE_TO_SEGMENT = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "handleToSegment",
                    MethodType.methodType(MemorySegment.class, MemoryHandle.class));
            SEGMENT_TO_HANDLE = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "segmentToHandle",
                    MethodType.methodType(MemoryHandle.class, MemorySegment.class));
            SHORT_TO_LONG = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "shortToLong",
                    MethodType.methodType(long.class, short.class));
            LONG_TO_SHORT = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "longToShort",
                    MethodType.methodType(short.class, long.class));
            INT_TO_LONG = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "intToLong",
                    MethodType.methodType(long.class, int.class));
            LONG_TO_INT = MethodHandles.lookup().findStatic(FFMMethodFilters.class, "longToInt",
                    MethodType.methodType(int.class, long.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception");
        }
    }

}
