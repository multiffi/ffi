package multiffi;

final class Util {

    private Util() {
        throw new AssertionError("No multiffi.Util instances for you!");
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

}
