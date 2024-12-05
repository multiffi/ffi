package io.github.multiffi;

import multiffi.CallOption;
import multiffi.ErrnoException;
import multiffi.ForeignType;
import multiffi.FunctionPointer;
import multiffi.MemoryBlock;
import multiffi.StandardCallOption;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.multiffi.FFMForeignProvider.BLOCK_TO_SEGMENT;
import static io.github.multiffi.FFMForeignProvider.ERRNO_THREAD_LOCAL;
import static io.github.multiffi.FFMForeignProvider.IS_WINDOWS;
import static io.github.multiffi.FFMForeignProvider.toMemoryLayout;

public class FFMFunctionPointer extends FunctionPointer {

    private static final Linker LINKER = Linker.nativeLinker();

    private final long address;
    private final List<ForeignType> parameterTypes;
    private final ForeignType returnType;
    private final MethodHandle methodHandle;
    private final boolean saveErrno;
    private final boolean throwErrno;

    private static final Linker.Option[] EMPTY_OPTION_ARRAY = new Linker.Option[0];
    public FFMFunctionPointer(long address, int firstVarArg, ForeignType returnType, ForeignType[] parameterTypes, CallOption... options) {
        this.address = address;
        boolean saveErrno = false;
        boolean throwErrno = false;
        boolean critical = false;
        Set<Linker.Option> linkerOptions = new HashSet<>(3);
        for (CallOption option : options) {
            switch (option) {
                case StandardCallOption.THROW_ERRNO:
                    throwErrno = true;
                case StandardCallOption.SAVE_ERRNO:
                    saveErrno = true;
                    linkerOptions.add(Linker.Option.captureCallState(IS_WINDOWS ? "GetLastError" : "errno"));
                    break;
                case ExtendedCallOption.CRITICAL:
                    critical = true;
                    linkerOptions.add(Linker.Option.critical(true));
                    break;
                case StandardCallOption.STDCALL:
                    break;
                default:
                    throw new IllegalArgumentException(option + " not supported");
            }
        }
        if (firstVarArg >= 0) linkerOptions.add(Linker.Option.firstVariadicArg(firstVarArg));
        this.saveErrno = saveErrno;
        this.throwErrno = throwErrno;
        if (saveErrno && critical) throw new IllegalArgumentException("SAVE_ERRNO or THROW_ERRNO can not be combined with CRITICAL");
        MemoryLayout returnLayout = returnType == ForeignType.VOID ? null : toMemoryLayout(returnType);
        MemoryLayout[] parameterLayouts = new MemoryLayout[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            parameterLayouts[i] = toMemoryLayout(parameterTypes[i]);
        }
        boolean addReturnMemoryParameter = returnType != null && returnType.isCompound();
        List<ForeignType> parameterTypeList = new ArrayList<>(parameterTypes.length + (addReturnMemoryParameter ? 1 : 0));
        parameterTypeList.addAll(Arrays.asList(parameterTypes));
        if (addReturnMemoryParameter) parameterTypeList.addFirst(returnType);
        this.returnType = returnType;
        this.parameterTypes = Collections.unmodifiableList(parameterTypeList);
        MethodHandle methodHandle = LINKER.downcallHandle(MemorySegment.ofAddress(address), returnType == ForeignType.VOID ?
                FunctionDescriptor.ofVoid(parameterLayouts) : FunctionDescriptor.of(returnLayout, parameterLayouts),
                linkerOptions.toArray(EMPTY_OPTION_ARRAY));
        for (int i = (addReturnMemoryParameter ? 1 : 0); i < parameterTypeList.size(); i ++) {
            if (parameterTypeList.get(i).isCompound()) methodHandle = MethodHandles
                    .filterArguments(methodHandle, i + (saveErrno ? 1 : 0), BLOCK_TO_SEGMENT);
        }
        this.methodHandle = methodHandle;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public List<ForeignType> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public ForeignType getReturnType() {
        return returnType;
    }

    @Override
    public boolean isStdCall() {
        return false;
    }

    @Override
    public void invokeVoid(Object... args) {
        invokeScalar(args);
    }

    @Override
    public boolean invokeBoolean(Object... args) {
        return (boolean) invokeScalar(args);
    }

    @Override
    public byte invokeInt8(Object... args) {
        return (byte) invokeScalar(args);
    }

    @Override
    public char invokeUTF16(Object... args) {
        return (char) invokeScalar(args);
    }

    @Override
    public short invokeInt16(Object... args) {
        return (short) invokeScalar(args);
    }

    @Override
    public int invokeInt32(Object... args) {
        return (int) invokeScalar(args);
    }

    @Override
    public long invokeInt64(Object... args) {
        return (long) invokeScalar(args);
    }

    @Override
    public float invokeFloat(Object... args) {
        return (float) invokeScalar(args);
    }

    @Override
    public double invokeDouble(Object... args) {
        return (double) invokeScalar(args);
    }

    @Override
    public long invokeAddress(Object... args) {
        return (long) invokeScalar(args);
    }

    @Override
    public MemoryBlock invokeCompound(Object... args) {
        MemoryBlock result = (MemoryBlock) args[0];
        try {
            if (saveErrno) {
                Object[] arguments = new Object[args.length + 1];
                arguments[0] = SegmentAllocator.slicingAllocator((MemorySegment) BLOCK_TO_SEGMENT.invokeExact(result));
                arguments[1] = ERRNO_THREAD_LOCAL.get();
                System.arraycopy(args, 1, arguments, 2, args.length - 1);
                methodHandle.invokeWithArguments(arguments);
                if (throwErrno) throw new ErrnoException();
            }
            else {
                args[0] = SegmentAllocator.slicingAllocator((MemorySegment) BLOCK_TO_SEGMENT.invokeExact(result));
                methodHandle.invokeWithArguments(args);
            }
            return result;
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Object invokeScalar(Object... args) {
        try {
            if (saveErrno) {
                Object result;
                Object[] arguments = new Object[args.length + 1];
                arguments[0] = ERRNO_THREAD_LOCAL.get();
                System.arraycopy(args, 0, arguments, 1, args.length);
                result = methodHandle.invokeWithArguments(arguments);
                if (throwErrno) throw new ErrnoException();
                return result;
            }
            else  return methodHandle.invokeWithArguments(args);
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object invoke(Object... args) {
        if (returnType == null || returnType.isScalar()) return invokeScalar(args);
        else return invokeCompound(args);
    }

}
