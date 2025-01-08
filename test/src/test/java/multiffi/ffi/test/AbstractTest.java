package multiffi.ffi.test;

import multiffi.ffi.CallOption;
import multiffi.ffi.CompoundType;
import multiffi.ffi.Foreign;
import multiffi.ffi.ForeignType;
import multiffi.ffi.FunctionHandle;
import multiffi.ffi.MemoryHandle;
import multiffi.ffi.ScalarType;
import multiffi.ffi.SimpleCallOptionVisitor;
import multiffi.ffi.StandardCallOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractTest {

    protected interface CLibrary {
        int abs(int value);
        double sin(double value);
        long fopen(long filename, long mode);
        long fprintf(long fp, long format, Object... args);
    }

    protected volatile CLibrary libc;
    protected volatile CompoundType pack96;

    @BeforeEach
    public void beforeEach() {
        if (libc == null) {
            libc = Foreign.downcallProxy(CLibrary.class, new SimpleCallOptionVisitor() {
                @Override
                public ForeignType visitReturnType(Method method) {
                    if (method.getName().equals("fopen")) return ScalarType.ADDRESS;
                    else if (method.getName().equals("fprintf")) return ScalarType.INT;
                    else return super.visitReturnType(method);
                }
                @Override
                public ForeignType[] visitParameterTypes(Method method) {
                    if (method.getName().equals("fopen")) return new ForeignType[] { ScalarType.ADDRESS, ScalarType.ADDRESS };
                    else if (method.getName().equals("fprintf")) return new ForeignType[] { ScalarType.ADDRESS, ScalarType.ADDRESS };
                    else return super.visitParameterTypes(method);
                }
                @Override
                public CallOption[] visitCallOptions(Method method) {
                    if (method.getName().equals("fprintf")) return new CallOption[] { StandardCallOption.DYNCALL };
                    else return super.visitCallOptions(method);
                }
            });
        }
        if (pack96 == null) pack96 = CompoundType.ofStruct(ScalarType.INT32, ScalarType.INT32, ScalarType.INT32);
    }

    @Test
    public void absTest() {
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 1000000; i ++) {
            int number = random.nextInt(Integer.MIN_VALUE, 0);
            Assertions.assertEquals(Math.abs(number), libc.abs(number));
        }
    }

    @Test
    public void sinTest() {
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 1000000; i ++) {
            double number = random.nextDouble();
            double expected = Math.sin(number);
            Assertions.assertEquals(expected, libc.sin(number), Math.ulp(expected));
        }
    }

    protected static final String NULL_FILE_NAME = OS.current() == OS.WINDOWS ? "NUL" : "/dev/null";
    protected volatile long fpNull = 0;

    @Test
    public void fprintfTest() {
        if (fpNull == 0) {
            try (MemoryHandle hFilename = MemoryHandle.allocateDirect(NULL_FILE_NAME);
                 MemoryHandle hMode = MemoryHandle.allocateDirect("wb")) {
                fpNull = libc.fopen(hFilename.address(), hMode.address());
                Assertions.assertNotEquals(0, fpNull);
            }
        }
        Random random = ThreadLocalRandom.current();
        try (MemoryHandle hFormat = MemoryHandle.allocateDirect("%d + %d = %d\n")) {
            int a = random.nextInt();
            int b = random.nextInt();
            Assertions.assertEquals(String.format("%d + %d = %d\n", a, b, a + b).length(),
                    libc.fprintf(fpNull, hFormat.address(), a, b, a + b));
        }
    }

    public static MemoryHandle stub(MemoryHandle pack96) {
        return pack96;
    }

    @Test
    public void stubTest() throws Throwable {
        long compoundMethod = Foreign.upcallStub(AbstractTest.class, AbstractTest.class.getDeclaredMethod("stub", MemoryHandle.class), pack96, pack96);
        FunctionHandle functionHandle = Foreign.downcallHandle(compoundMethod, pack96, pack96);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        try (MemoryHandle heap = MemoryHandle.allocate(pack96); MemoryHandle direct = MemoryHandle.allocateDirect(pack96)) {
            double rand = random.nextDouble();
            heap.setDouble(0, rand);
            direct.setDouble(0, rand);
            Assertions.assertEquals(0, functionHandle.invokeCompound(direct, heap).compareTo(heap));
            Assertions.assertEquals(0, functionHandle.invokeCompound(heap, direct).compareTo(direct));
        }
    }

}
