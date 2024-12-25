package multiffi;

public final class ScalarType extends ForeignType {

    /**
     * Java {@code boolean}; C {@code _Bool}; C++ {@code bool}
     */
    public static final ScalarType BOOLEAN = new ScalarType(boolean.class, 1);
    /**
     * Java {@code char}; Windows {@code wchar_t} {@code WCHAR}
     */
    public static final ScalarType UTF16 = new ScalarType(char.class, 2);
    /**
     * Java {@code byte}; C/C++ {@code char} {@code int8_t}
     */
    public static final ScalarType INT8 = new ScalarType(byte.class, 1);
    /**
     * Java {@code byte}; C/C++ {@code char} {@code int8_t}
     */
    public static final ScalarType CHAR = new ScalarType(byte.class, 1);
    /**
     * C/C++ {@code wchar_t}
     */
    public static final ScalarType WCHAR = new ScalarType(Foreign.wcharSize() == 4 ? int.class : char.class, Foreign.wcharSize());
    /**
     * Java {@code short}; C/C++ {@code int16_t}
     */
    public static final ScalarType INT16 = new ScalarType(short.class, 2);
    /**
     * Java {@code int}; C/C++ {@code int32_t}
     */
    public static final ScalarType INT32 = new ScalarType(int.class, 4);
    /**
     * Java {@code long}; C/C++ {@code int64_t}
     */
    public static final ScalarType INT64 = new ScalarType(long.class, 8);
    /**
     * C/C++ {@code void *}
     */
    public static final ScalarType ADDRESS = new ScalarType(Foreign.addressSize() == 8 ? long.class : int.class, Foreign.addressSize());
    /**
     * C/C++ {@code size_t}
     */
    public static final ScalarType SIZE = new ScalarType(Foreign.addressSize() == 8 ? long.class : int.class, Foreign.addressSize());
    /**
     * C/C++ {@code long} {@code long long}
     */
    public static final ScalarType LONG = new ScalarType(Foreign.longSize() == 8 ? long.class : int.class, Foreign.longSize());
    /**
     * C/C++ {@code int} {@code long int}
     */
    public static final ScalarType INT = new ScalarType(Foreign.intSize() == 8 ? long.class : int.class, Foreign.intSize());
    /**
     * C/C++ {@code short}
     */
    public static final ScalarType SHORT = new ScalarType(Foreign.shortSize() == 8 ? long.class : short.class, Foreign.shortSize());
    /**
     * Java/C/C++ {@code float}
     */
    public static final ScalarType FLOAT = new ScalarType(float.class, 4);
    /**
     * Java/C/C++ {@code double}
     */
    public static final ScalarType DOUBLE = new ScalarType(double.class, 8);

    private final Class<?> carrier;
    private final long size;

    private ScalarType(Class<?> carrier, long size) {
        this.carrier = carrier;
        this.size = size;
    }

    @Override
    public Class<?> carrier() {
        return carrier;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public boolean isStruct() {
        return false;
    }

    @Override
    public boolean isUnion() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public CompoundElement getElement(int index) throws IndexOutOfBoundsException {
        return Util.EMPTY_COMPOUND_ELEMENT_ARRAY[index];
    }

    @Override
    public CompoundElement[] getElements() {
        return Util.EMPTY_COMPOUND_ELEMENT_ARRAY;
    }

    @Override
    public ForeignType getComponentType() {
        return null;
    }

}
