package multiffi.ffi;

import java.lang.reflect.Method;

public class SimpleCallOptionVisitor implements CallOptionVisitor {

    @Override
    public long visitAddress(Method method) {
        return Foreign.getSymbolAddress(method.getName());
    }

    @Override
    public int visitFirstVarArgIndex(Method method) {
        return -1;
    }

    @Override
    public ForeignType visitReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) return null;
        else if (returnType == boolean.class) return ScalarType.BOOLEAN;
        else if (returnType == byte.class) return ScalarType.INT8;
        else if (returnType == char.class) return ScalarType.UTF16;
        else if (returnType == short.class) return ScalarType.INT16;
        else if (returnType == int.class) return ScalarType.INT32;
        else if (returnType == long.class) return ScalarType.INT64;
        else if (returnType == float.class) return ScalarType.FLOAT;
        else if (returnType == double.class) return ScalarType.DOUBLE;
        else throw new IllegalArgumentException(returnType + " not supported as mapping type");
    }
    @Override
    public ForeignType[] visitParameterTypes(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        ForeignType[] parameterForeignTypes = new ForeignType[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i ++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == boolean.class) parameterForeignTypes[i] = ScalarType.BOOLEAN;
            else if (parameterType == byte.class) parameterForeignTypes[i] = ScalarType.INT8;
            else if (parameterType == char.class) parameterForeignTypes[i] = ScalarType.UTF16;
            else if (parameterType == short.class) parameterForeignTypes[i] = ScalarType.INT16;
            else if (parameterType == int.class) parameterForeignTypes[i] = ScalarType.INT32;
            else if (parameterType == long.class) parameterForeignTypes[i] = ScalarType.INT64;
            else if (parameterType == float.class) parameterForeignTypes[i] = ScalarType.FLOAT;
            else if (parameterType == double.class) parameterForeignTypes[i] = ScalarType.DOUBLE;
            else throw new IllegalArgumentException(parameterType + " not supported as mapping type");
        }
        return parameterForeignTypes;
    }

    @Override
    public CallOption[] visitCallOptions(Method method) {
        return Util.EMPTY_CALL_OPTION_ARRAY;
    }

}
