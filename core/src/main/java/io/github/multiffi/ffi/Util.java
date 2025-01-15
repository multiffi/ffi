package io.github.multiffi.ffi;

import multiffi.ffi.CallOption;
import multiffi.ffi.CompoundElement;
import multiffi.ffi.ForeignType;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.SimpleFunctionOptionVisitor;

public final class Util {

    private Util() {
        throw new AssertionError("No io.github.multiffi.ffi.Util instances for you!");
    }

    public static final SimpleFunctionOptionVisitor DEFAULT_SIGNATURE_VISITOR = new SimpleFunctionOptionVisitor();

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final CallOption[] EMPTY_CALL_OPTION_ARRAY = new CallOption[0];
    public static final CompoundElement[] EMPTY_COMPOUND_ELEMENT_ARRAY = new CompoundElement[0];
    public static final ForeignType[] EMPTY_FOREIGN_TYPE_ARRAY = new ForeignType[0];

    public static long getArrayContentSize(Object array) {
        if (array instanceof byte[]) return ((byte[]) array).length;
        else if (array instanceof char[]) return (long) ((char[]) array).length << 1;
        else if (array instanceof short[]) return (long) ((short[]) array).length << 1;
        else if (array instanceof int[]) return (long) ((int[]) array).length << 2;
        else if (array instanceof long[]) return (long) ((long[]) array).length << 3;
        else if (array instanceof float[]) return (long) ((float[]) array).length << 2;
        else if (array instanceof double[]) return (long) ((double[]) array).length << 3;
        else throw new IllegalArgumentException("Unsupported array");
    }

    public static int compareUnsigned(byte a, byte b) {
        return Byte.toUnsignedInt(a) - Byte.toUnsignedInt(b);
    }

    public static long unsignedAddExact(long x, long y) {
        long sum = x + y;
        if (Long.compareUnsigned(x, sum) > 0) throw new ArithmeticException("long overflow");
        return sum;
    }

    public static long unsignedMin(long a, long b) {
        if (a == 0) return a;
        else if (b == 0) return b;
        else {
            int s1 = Long.signum(a);
            int s2 = Long.signum(b);
            if (s1 == s2) return Math.min(a, b);
            else if (s1 < s2) return b;
            else return a;
        }
    }

    public static long unsignedMax(long a, long b) {
        if (a == 0) return b;
        else if (b == 0) return a;
        else {
            int s1 = Long.signum(a);
            int s2 = Long.signum(b);
            if (s1 == s2) return Math.max(a, b);
            else if (s1 < s2) return a;
            else return b;
        }
    }

    private static long multiplyHigh(long x, long y) {
        // Use technique from section 8-2 of Henry S. Warren, Jr.,
        // Hacker's Delight (2nd ed.) (Addison Wesley, 2013), 173-174.
        long x1 = x >> 32;
        long x2 = x & 0xFFFFFFFFL;
        long y1 = y >> 32;
        long y2 = y & 0xFFFFFFFFL;

        long z2 = x2 * y2;
        long t = x1 * y2 + (z2 >>> 32);
        long z1 = t & 0xFFFFFFFFL;
        long z0 = t >> 32;
        z1 += x2 * y1;

        return x1 * y1 + z0 + (z1 >> 32);
    }

    private static long unsignedMultiplyHigh(long x, long y) {
        long result = multiplyHigh(x, y);
        result += (y & (x >> 63)); // equivalent to `if (x < 0) result += y;`
        result += (x & (y >> 63)); // equivalent to `if (y < 0) result += x;`
        return result;
    }

    public static long unsignedMultiplyExact(long x, long y) {
        long hi = unsignedMultiplyHigh(x, y);
        if (hi != 0) throw new ArithmeticException("long overflow");
        return x * y;
    }

    public static boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(System.getProperty(propertyName, Boolean.valueOf(defaultValue).toString()));
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public static void checkType(ForeignType type, Class<?> clazz) {
        Class<?> expected;
        if (type == ScalarType.BOOLEAN) expected = boolean.class;
        else if (type == ScalarType.UTF16) expected = char.class;
        else if (type == ScalarType.INT8 || type == ScalarType.CHAR) expected = byte.class;
        else if (type == ScalarType.INT16) expected = short.class;
        else if (type == ScalarType.INT32 || type == ScalarType.WCHAR) expected = int.class;
        else if (type == ScalarType.INT64 || type == ScalarType.SHORT || type == ScalarType.INT
                || type == ScalarType.LONG || type == ScalarType.SIZE || type == ScalarType.ADDRESS)
            expected = long.class;
        else if (type == ScalarType.FLOAT) expected = float.class;
        else if (type == ScalarType.DOUBLE) expected = double.class;
        else expected = MemoryHandle.class;
        if (clazz != expected) throw new IllegalArgumentException("Illegal mapping type; expected " + expected);
    }

}
