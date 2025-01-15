/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package io.github.multiffi.ffi;

import multiffi.ffi.Memory;

import java.util.Arrays;

/**
 * An off-heap memory stack.
 *
 * <p>This class should be used in a thread-local manner for stack allocations.</p>
 * <p>This class is not thread-safe.</p>
 */
public class MemoryStack implements AutoCloseable {

    private final long address;
    private final long size;
    private long pointer;
    private long[] frames;
    private int frameIndex;

    /**
     * Creates a new {@code MemoryStack} with the default size.
     *
     * <p>In the initial state, there is no active stack frame. The {@link #push} method must be used before any allocations.</p>
     */
    public MemoryStack() {
        this(1024 * 1024);
    }

    /**
     * Creates a new {@code MemoryStack} with the specified size.
     *
     * <p>In the initial state, there is no active stack frame. The {@link #push} method must be used before any allocations.</p>
     *
     * @param size the maximum number of bytes that may be allocated on the stack
     */
    public MemoryStack(long size) {
        this(Memory.allocate(size), size);
    }

    /**
     * Creates a new {@code MemoryStack} backed by the specified memory region.
     *
     * <p>In the initial state, there is no active stack frame. The {@link #push} method must be used before any allocations.</p>
     *
     * @param address the backing memory address
     * @param size the backing memory size
     */
    public MemoryStack(long address, long size) {
        this.address = address;
        if (size < 0) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(size));
        this.size = size;
        this.pointer = size;
        this.frames = new long[8];
    }

    /**
     * Stores the current stack pointer and pushes a new frame to the stack.
     *
     * <p>This method should be called when entering a method, before doing any stack allocations. When exiting a method, call the {@link #pop} method to
     * restore the previous stack frame.</p>
     *
     * <p>Pairs of push/pop calls may be nested. Care must be taken to:</p>
     * <ul>
     * <li>match every push with a pop</li>
     * <li>not call pop before push has been called at least once</li>
     * <li>not nest push calls to more than the maximum supported depth</li>
     * </ul>
     *
     * @return this stack
     */
    public MemoryStack push() {
        if (frameIndex == frames.length) frames = Arrays.copyOf(frames, frames.length * 3 / 2);
        frames[frameIndex ++] = pointer;
        return this;
    }

    /**
     * Pops the current stack frame and moves the stack pointer to the end of the previous stack frame.
     *
     * @return this stack
     */
    public MemoryStack pop() {
        frameIndex --;
        if (frameIndex < 0) throw new IndexOutOfBoundsException("Stack underflow");
        pointer = frames[frameIndex];
        return this;
    }

    /**
     * Calls {@link #pop} on this {@code MemoryStack}.
     *
     *
     * <p>This method should not be used directly. It is called automatically when the {@code MemoryStack} is used as a resource in a try-with-resources
     * statement.</p>
     */
    @Override
    public void close() {
        pop();
    }

    /**
     * Returns the address of the backing off-heap memory.
     *
     * <p>The stack grows "downwards", so the bottom of the stack is at {@code address + size}, while the top is at {@code address}.</p>
     */
    public long address() {
        return address;
    }

    /**
     * Returns the top of the stack.
     */
    public long top() {
        return address;
    }

    /**
     * Returns the bottom of the stack.
     */
    public long bottom() {
        return address + size;
    }

    /**
     * Returns the size of the backing off-heap memory.
     *
     * <p>This is the maximum number of bytes that may be allocated on the stack.</p>
     */
    public long size() {
        return size;
    }

    /**
     * Returns the current frame index.
     *
     * <p>This is the current number of nested {@link #push} calls.</p>
     */
    public int frame() {
        return frameIndex;
    }

    /**
     * Returns the memory address at the current stack pointer.
     */
    public long current() {
        return address + (pointer & 0xFFFFFFFFL);
    }

    /**
     * Returns the remaining space of this stack in bytes.
     * @return the remaining space
     */
    public long remaining() {
        return pointer;
    }

    /**
     * Returns the current stack pointer.
     *
     * <p>The stack grows "downwards", so when the stack is empty {@code pointer} is equal to {@code size}. On every allocation {@code pointer} is reduced by
     * the allocated size (after alignment) and {@code address + pointer} points to the first byte of the last allocation.</p>
     *
     * <p>Effectively, this method returns how many more bytes may be allocated on the stack.</p>
     */
    public long position() {
        return pointer;
    }

    /**
     * Sets the current stack pointer.
     *
     * <p>This method directly manipulates the stack pointer. Using it irresponsibly may break the internal state of the stack. It should only be used in rare
     * cases or in auto-generated code.</p>
     */
    public void position(long newPosition) {
        if (newPosition < 0 || size < newPosition) throw new IndexOutOfBoundsException("Index out of range: " + Long.toUnsignedString(newPosition));

        this.pointer = newPosition;
    }

    /**
     * Allocates a block of {@code size} bytes of memory on the stack. The content of the newly allocated block of memory is not initialized, remaining with
     * indeterminate values.
     *
     * @param size the allocation size
     *
     * @return the memory address on the stack for the requested allocation
     */
    public long allocate(long size) {
        long address = Math.subtractExact(this.address + pointer, size);

        long pointer = address - this.address;
        if (pointer < 0) throw new StackOverflowError(
                String.format("Out of stack space: tried to allocate %d bytes but only %d bytes free to use.", size, this.pointer));
        else this.pointer = pointer;

        return address;
    }

    /**
     * Allocates a block of memory on the stack for an array of {@code count} elements, each of them {@code size} bytes long, and initializes all its bits to
     * zero.
     *
     * @param count the number of elements to allocate
     * @param size the size of each element
     *
     * @return the memory address on the stack for the requested allocation
     */
    public long allocateInitialized(long count, long size) {
        size = Math.multiplyExact(count, size);
        long address = allocate(size);
        Memory.fill(address, 0, size);
        return address;
    }

    /**
     * Allocates a block of {@code size} bytes of memory on the stack. The content of the newly allocated block of memory is not initialized, remaining with
     * indeterminate values.
     *
     * @param alignment the required alignment
     * @param size      the allocation size
     *
     * @return the memory address on the stack for the requested allocation
     */
    public long allocateAligned(long size, long alignment) {
        // Align address to the specified alignment
        long address = (this.address + pointer - size) & -alignment;

        long pointer = address - this.address;
        if (pointer < 0) throw new StackOverflowError(
                String.format("Out of stack space: tried to allocate %d bytes but only %d bytes free to use.", size, this.pointer));
        else this.pointer = pointer;

        return address;
    }

    /**
     * Allocates a block of memory on the stack for an array of {@code count} elements, each of them {@code size} bytes long, and initializes all its bits to
     * zero.
     *
     * @param count the number of elements to allocate
     * @param size the size of each element
     * @param alignment the required element alignment
     *
     * @return the memory address on the stack for the requested allocation
     */
    public long allocateInitializedAligned(long count, long size, long alignment) {
        size = Math.multiplyExact(count, size);
        long address = allocateAligned(size, alignment);
        Memory.fill(address, 0, size);
        return address;
    }

}
