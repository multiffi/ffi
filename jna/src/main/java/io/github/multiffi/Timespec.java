package io.github.multiffi;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

@Structure.FieldOrder({"tv_sec", "tv_nsec"})
public class Timespec extends Structure {

    public NativeLong tv_sec;
    public NativeLong tv_nsec;

    public Timespec() {
        super();
    }

    public static class ByReference extends Timespec implements Structure.ByReference {
    }

    public static class ByValue extends Timespec implements Structure.ByValue {
    }

}
