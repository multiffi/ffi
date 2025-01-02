package io.github.multiffi;

import com.sun.jna.Structure;

@Structure.FieldOrder({"dwLowDateTime", "dwHighDateTime"})
public class FILETIME extends Structure {

    public int dwLowDateTime;
    public int dwHighDateTime;

    public FILETIME() {
        super();
    }

    public static class ByReference extends FILETIME implements Structure.ByReference {
    }

    public static class ByValue extends FILETIME implements Structure.ByValue {
    }

}
