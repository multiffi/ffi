# Multiffi FFI
Unified solution for Java's most popular native call APIs/libraries: 
[FFM](https://openjdk.org/jeps/454), 
[JNA](https://github.com/java-native-access/jna), 
[JNADirect](https://github.com/java-native-access/jna/blob/master/www/DirectMapping.md), 
[JNR](https://github.com/jnr/jnr-ffi), 
[JFFI](https://github.com/jnr/jffi), 
with simple and clear API.

This project has such main goals:
- Interoperate with code (native functions and closures) and data (memory not managed by JVM) outside JRE without write/generate and compile any JNI code
- Compatible with JDK 8+ and Android SDK 16+ with multiple backends

## Compatibility

### Java Runtime Environment
JREs except Java SE and Android are not supported.

| Module            | JDK | Android SDK | Description           |
|-------------------|-----|-------------|-----------------------|
| multiffi-ffi-core | 8+  | 16+         | Core API              |
| multiffi-ffi-ffm  | 22+ |             | FFM Backend           |
| multiffi-ffi-jna  | 8+  | 16+         | JNA/JNADirect Backend |
| multiffi-ffi-jnr  | 8+  | 16+         | JNR/JFFI Backend      |

### FFI Calling Options
Not all calling options supported by all backends.

| Module           | DynCall | StdCall | Critical | Trivial | SaveErrno |
|------------------|---------|---------|----------|---------|-----------|
| multiffi-ffi-ffm | ✔       |         | ✔        | ✔       | ✔         |
| multiffi-ffi-jna | ✔       | ✔       |          |         | ✔         |
| multiffi-ffi-jnr | ✔       | ✔       |          |         | ✔         |

## Installing
### Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.github.multiffi</groupId>
        <artifactId>multiffi-ffi-core</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.multiffi</groupId>
        <artifactId>multiffi-ffi-ffm</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.multiffi</groupId>
        <artifactId>multiffi-ffi-jna</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.multiffi</groupId>
        <artifactId>multiffi-ffi-jnr</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```
### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.multiffi:multiffi-ffi-core:1.0.0'
    implementation 'io.github.multiffi:multiffi-ffi-ffm:1.0.0'
    implementation 'io.github.multiffi:multiffi-ffi-jna:1.0.0'
    implementation 'io.github.multiffi:multiffi-ffi-jnr:1.0.0'
}
```

## Build
### Prerequisites
- JDK 22+
- Gradle 8.10

### Example (UNIX Shell)
```shell
cd ${PROJECT_ROOT}
./gradlew clean jar
```

## License
[BSD 3-Clause](/LICENSE)