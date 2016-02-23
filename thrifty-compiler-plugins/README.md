Compiler Plugin SPI
-------------------

Thrifty exposes a post-processing interface here called `TypeProcessor`.
Using the standard Java SPI, you can modify the classes generated from Thrift IDL
by implementing this interface and making it available in the compiler's classpath.

Example
-------

Thrifty generated classes are not `Serializable` by default.  If you wanted to change
that, you could modify the compiler - or, you could implement a TypeProcessor to do it
for you, much more easily:

```java
public class SerializableTypeProcessor implements TypeProcessor {
    @Override
    public TypeSpec process(TypeSpec type) {
        TypeSpec.Builder builder = type.toBuilder();

        builder.addSuperinterface(Serializable.class);
        builder.addField(FieldSpec.builder(long.class, "serialVersionUID")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", -1)
                .build());

        return builder.build();
    }
}
```

Usage
-----

The minimal project to implement this processor looks like the following:

src/
  - main/
    - java/
      - com/
        - corp/
          - SerializableTypeProcessor.java
    - resources/
      - META-INF/
        - services/
          - com.microsoft.thrifty.compiler.spi.TypeProcessor

The file under META-INF is how Java services are identified.  The file should
contain the full name of your processor class, in this example:

```
com.corp.SerializableTypeProcessor
```

Once this project is built into a JAR, you can include it in the classpath on the command line
like so:

```bash
java -cp thrifty-compiler.jar:your-processor.jar com.microsoft.thrifty.compiler.ThriftyCompiler # compiler args as usual
```

This is a little wordy.  An easier but less-flexible approach could be to build an uberjar including
the compiler along with your processor.  See `thrifty-example-postprocessor/build.gradle` for one way to do it.