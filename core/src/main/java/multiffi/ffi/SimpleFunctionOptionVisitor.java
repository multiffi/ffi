package multiffi.ffi;

import io.github.multiffi.ffi.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleFunctionOptionVisitor implements FunctionOptionVisitor {

    protected final Map<String, CompoundType> typeMap;

    public SimpleFunctionOptionVisitor() {
        this(null);
    }

    public SimpleFunctionOptionVisitor(Map<String, CompoundType> typeMap) {
        this.typeMap = typeMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(typeMap);
    }

    @Override
    public long visitAddress(Method method) {
        RedirectTo redirectTo = method.getAnnotation(RedirectTo.class);
        return Foreign.getSymbolAddress(redirectTo == null ? method.getName() : (redirectTo.value().isEmpty() ? method.getName() : redirectTo.value()));
    }

    @Override
    public int visitFirstVarArgIndex(Method method) {
        FirstVarArgIndex firstVarArgIndex = method.getAnnotation(FirstVarArgIndex.class);
        return firstVarArgIndex == null ? -1 : (firstVarArgIndex.value() < 0 ? -1 : firstVarArgIndex.value());
    }

    private ForeignType toForeignType(Class<?> type, MarshalType marshalType) {
        String marshalTypeName = marshalType == null ? "" : marshalType.value();
        if (type == void.class) return null;
        else if (type == boolean.class) return ScalarType.BOOLEAN;
        else if (type == byte.class) return ScalarType.INT8;
        else if (type == char.class) return ScalarType.UTF16;
        else if (type == short.class) return ScalarType.INT16;
        else if (type == int.class) {
            if ("wchar_t".equalsIgnoreCase(marshalTypeName)) return ScalarType.WCHAR;
            else return ScalarType.INT32;
        }
        else if (type == long.class) {
            if (marshalTypeName.matches("(const\\s+)?([a-z]|[A-Z]|_)([0-9]|[a-z]|[A-Z]|_)+\\s*\\*")) return ScalarType.ADDRESS;
            else if ("size_t".equalsIgnoreCase(marshalTypeName)) return ScalarType.SIZE;
            else if ("long".equalsIgnoreCase(marshalTypeName)) return ScalarType.LONG;
            else if ("int".equalsIgnoreCase(marshalTypeName)) return ScalarType.INT;
            else if ("short".equalsIgnoreCase(marshalTypeName)) return ScalarType.SHORT;
            else return ScalarType.INT64;
        }
        else if (type == float.class) return ScalarType.FLOAT;
        else if (type == double.class) return ScalarType.DOUBLE;
        else if (type == MemoryHandle.class) {
            CompoundType compoundType = typeMap.get(marshalTypeName);
            if (compoundType != null) return compoundType;
        }
        throw new IllegalArgumentException(type + " not supported as mapping type");
    }

    @Override
    public ForeignType visitReturnType(Method method) {
        return toForeignType(method.getReturnType(), method.getAnnotation(MarshalType.class));
    }

    @Override
    public ForeignType[] visitParameterTypes(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) return Util.EMPTY_FOREIGN_TYPE_ARRAY;
        int parameterTypeCount = parameters[parameters.length - 1].getType().isArray() ? parameters.length - 1 : parameters.length;
        ForeignType[] parameterForeignTypes = new ForeignType[parameterTypeCount];
        for (int i = 0; i < parameterTypeCount; i ++) {
            parameterForeignTypes[i] = toForeignType(parameters[i].getType(), parameters[i].getAnnotation(MarshalType.class));
        }
        return parameterForeignTypes;
    }

    private static final CallOption[] DYNCALL_CALL_OPTION_ARRAY = new CallOption[] { StandardCallOption.DYNCALL };
    @Override
    public CallOption[] visitCallOptions(Method method) {
        boolean dyncall = method.getParameterCount() > 0 && method.getParameterTypes()[method.getParameterCount() - 1].isArray();
        CallOptions callOptions = method.getAnnotation(CallOptions.class);
        String[] callOptionStrings = callOptions == null ? null : callOptions.value();
        if (callOptionStrings != null) {
            if (callOptionStrings.length != 0 && !(callOptionStrings.length == 1 && callOptionStrings[0].isEmpty())) {
                Set<CallOption> callOptionSet = new HashSet<>(callOptionStrings.length);
                for (String callOptionString : callOptionStrings) {
                    if ("dyncall".equalsIgnoreCase(callOptionString)) callOptionSet.add(StandardCallOption.DYNCALL);
                    else if ("stdcall".equalsIgnoreCase(callOptionString)) callOptionSet.add(StandardCallOption.STDCALL);
                    else if ("critical".equalsIgnoreCase(callOptionString)) callOptionSet.add(StandardCallOption.CRITICAL);
                    else if ("trivial".equalsIgnoreCase(callOptionString)) callOptionSet.add(StandardCallOption.TRIVIAL);
                    else if ("saveErrno".equalsIgnoreCase(callOptionString)) callOptionSet.add(StandardCallOption.SAVE_ERRNO);
                }
                if (dyncall) callOptionSet.add(StandardCallOption.DYNCALL);
                return callOptionSet.toArray(Util.EMPTY_CALL_OPTION_ARRAY);
            }
        }
        return dyncall ? DYNCALL_CALL_OPTION_ARRAY : Util.EMPTY_CALL_OPTION_ARRAY;
    }

}
