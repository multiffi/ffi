package multiffi.ffi;

import java.lang.reflect.Method;

public interface CallOptionVisitor {

    long visitAddress(Method method);
    int visitFirstVarArgIndex(Method method);
    ForeignType visitReturnType(Method method);
    ForeignType[] visitParameterTypes(Method method);
    CallOption[] visitCallOptions(Method method);

}
