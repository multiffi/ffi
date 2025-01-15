package io.github.multiffi.ffi;

public final class StackAllocator {

    private StackAllocator() {
        throw new AssertionError("No io.github.multiffi.ffi.StackAllocator instances for you!");
    }

    private static long parseMemoryBytes(String memoryString) {
        long multiplier;
        char unitChar = memoryString.charAt(memoryString.length() - 1);
        if (unitChar == 'k' || unitChar == 'K') multiplier = 1024;
        else if (unitChar == 'm' || unitChar == 'M') multiplier = 1024 * 1024;
        else if (unitChar == 'g' || unitChar == 'G') multiplier = 1024 * 1024 * 1024;
        else if (unitChar == 't' || unitChar == 'T') multiplier = 1024L * 1024L * 1024L * 1024L;
        else multiplier = 1;
        if (multiplier > 1) memoryString = memoryString.substring(0, memoryString.length() - 1);
        return Math.multiplyExact(multiplier, Long.parseLong(memoryString));
    }
    private static final long STACK_SIZE;
    static {
        long value;
        try {
            value = parseMemoryBytes(System.getProperty("multiffi.allocator.stackSize"));
        }
        catch (Throwable e) {
            value = -1;
        }
        STACK_SIZE = value;
    }
    private static final ThreadLocal<MemoryStack> STACK_THREAD_LOCAL = new ThreadLocal<MemoryStack>() {
        @Override
        protected MemoryStack initialValue() {
            return STACK_SIZE == -1 ? new MemoryStack() : new MemoryStack(STACK_SIZE);
        }
    };

    public static void push() {
        STACK_THREAD_LOCAL.get().push();
    }

    public static void pop() {
        STACK_THREAD_LOCAL.get().pop();
    }

    public static long allocate(long size) {
        return STACK_THREAD_LOCAL.get().allocate(size);
    }

    public static long allocateInitialized(long count, long size) {
        return STACK_THREAD_LOCAL.get().allocateInitialized(count, size);
    }

    public static long allocateAligned(long size, long alignment) {
        return STACK_THREAD_LOCAL.get().allocateAligned(size, alignment);
    }

    public static long allocateInitializedAligned(long count, long size, long alignment) {
        return STACK_THREAD_LOCAL.get().allocateInitializedAligned(count, size, alignment);
    }

}
