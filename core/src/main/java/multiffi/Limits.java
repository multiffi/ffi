package multiffi;

public final class Limits {

    public static final char  UTF16_MIN   = Character.MIN_VALUE;
    public static final char  UTF16_MAX   = Character.MAX_VALUE;
    public static final byte  INT8_MIN    = Byte.MIN_VALUE;
    public static final byte  INT8_MAX    = Byte.MAX_VALUE;
    public static final byte  UINT8_MIN   = 0;
    public static final byte  UINT8_MAX   = -1;
    public static final short INT16_MIN   = Short.MIN_VALUE;
    public static final short INT16_MAX   = Short.MAX_VALUE;
    public static final short UINT16_MIN  = 0;
    public static final short UINT16_MAX  = -1;
    public static final int   INT32_MIN   = Integer.MIN_VALUE;
    public static final int   INT32_MAX   = Integer.MAX_VALUE;
    public static final int   UINT32_MIN  = 0;
    public static final int   UINT32_MAX  = -1;
    public static final long  INT64_MIN   = Long.MIN_VALUE;
    public static final long  INT64_MAX   = Long.MAX_VALUE;
    public static final long  UINT64_MIN  = 0;
    public static final long  UINT64_MAX  = -1L;
    public static final long  USHORT_MAX  = Foreign.shortSize() == 8 ? UINT64_MAX : Short.toUnsignedLong(UINT16_MAX);
    public static final long  UINT_MAX    = Foreign.intSize() == 8 ? UINT64_MAX : Integer.toUnsignedLong(UINT32_MAX);
    public static final long  ULONG_MAX   = Foreign.longSize() == 8 ? UINT64_MAX : Integer.toUnsignedLong(UINT32_MAX);
    public static final long  ADDRESS_MAX = Foreign.addressSize() == 8 ? UINT64_MAX : Integer.toUnsignedLong(UINT32_MAX);
    public static final byte  CHAR_MIN    = Byte.MIN_VALUE;
    public static final byte  CHAR_MAX    = Byte.MAX_VALUE;
    public static final byte  UCHAR_MIN   = 0;
    public static final byte  UCHAR_MAX   = -1;
    public static final long  SHORT_MIN   = Foreign.shortSize() == 8 ? Long.MIN_VALUE : Short.MAX_VALUE;
    public static final long  SHORT_MAX   = Foreign.shortSize() == 8 ? Long.MAX_VALUE : Short.MAX_VALUE;
    public static final long  USHORT_MIN  = 0;
    public static final long  INT_MIN     = Foreign.intSize() == 8 ? Long.MIN_VALUE : Integer.MAX_VALUE;
    public static final long  INT_MAX     = Foreign.intSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE;
    public static final long  UINT_MIN    = 0;
    public static final long  LONG_MIN    = Foreign.longSize() == 8 ? Long.MIN_VALUE : Integer.MAX_VALUE;
    public static final long  LONG_MAX    = Foreign.longSize() == 8 ? Long.MAX_VALUE : Integer.MAX_VALUE;
    public static final long  ULONG_MIN   = 0;
    public static final long  ADDRESS_MIN = 0;

    private Limits() {
        throw new AssertionError("No multiffi.Limits instances for you!");
    }

}
