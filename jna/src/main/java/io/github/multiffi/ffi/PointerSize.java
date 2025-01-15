package io.github.multiffi.ffi;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

public class PointerSize extends IntegerType {

    private static final long serialVersionUID = 1999091870711168424L;

    public static final int SIZE = Native.SIZE_T_SIZE;

    public PointerSize() {
        this(0);
    }

    public PointerSize(long value) {
        super(SIZE, value);
    }

}
