package io.github.multiffi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import multiffi.MemoryHandle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public abstract class JNACompound extends Structure implements Structure.ByValue {

    @FieldOrder("array")
    public static class Bytes extends JNACompound {
        public byte[] array;
        public Bytes(byte[] array) {
            super();
            this.array = array;
        }
        public Bytes(Pointer memory, int size) {
            super(Objects.requireNonNull(memory));
            this.array = memory.getByteArray(0, size);
        }
    }

    @FieldOrder("array")
    public static class Chars extends JNACompound {
        public char[] array;
        public Chars(char[] array) {
            super();
            this.array = array;
        }
    }

    @FieldOrder("array")
    public static class Shorts extends JNACompound {
        public short[] array;
        public Shorts(short[] array) {
            super();
            this.array = array;
        }
    }

    @FieldOrder("array")
    public static class Ints extends JNACompound {
        public int[] array;
        public Ints(int[] array) {
            super();
            this.array = array;
        }
    }

    @FieldOrder("array")
    public static class Longs extends JNACompound {
        public long[] array;
        public Longs(long[] array) {
            super();
            this.array = array;
        }
    }

    @FieldOrder("array")
    public static class Floats extends JNACompound {
        public float[] array;
        public Floats(float[] array) {
            super();
            this.array = array;
        }
    }

    @FieldOrder("array")
    public static class Doubles extends JNACompound {
        public double[] array;
        public Doubles(double[] array) {
            super();
            this.array = array;
        }
    }

    public static JNACompound getInstance(MemoryHandle memoryHandle) {
        if (memoryHandle == null) return null;
        else {
            long size = memoryHandle.size();
            if (size < 0 || size > (Integer.MAX_VALUE - 8)) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
            int length = (int) size;
            if (memoryHandle.isDirect()) return new Bytes(new Pointer(memoryHandle.address()), length);
            else {
                Object array = memoryHandle.array();
                if (array instanceof byte[]) return new Bytes((byte[]) array);
                else if (array instanceof char[]) return new Chars((char[]) array);
                else if (array instanceof short[]) return new Shorts((short[]) array);
                else if (array instanceof int[]) return new Ints((int[]) array);
                else if (array instanceof long[]) return new Longs((long[]) array);
                else if (array instanceof float[]) return new Floats((float[]) array);
                else if (array instanceof double[]) return new Doubles((double[]) array);
                else throw new IllegalStateException("Unexpected exception");
            }
        }
    }

    @FieldOrder("array")
    public static class VariableLength extends JNACompound {
        public byte[] array;
        public VariableLength() {
            this(deque().pop());
        }
        public VariableLength(int size) {
            super();
            this.array = new byte[size];
        }
        private static final ThreadLocal<Deque<Integer>> SIZE_DEQUE_THREAD_LOCAL = new ThreadLocal<Deque<Integer>>() {
            @Override
            protected Deque<Integer> initialValue() {
                return new ArrayDeque<>();
            }
        };
        public static Deque<Integer> deque() {
            return SIZE_DEQUE_THREAD_LOCAL.get();
        }
    }

    public JNACompound() {
        super(ALIGN_NONE);
    }

    public JNACompound(Pointer memory) {
        super(memory, ALIGN_NONE);
    }

}
