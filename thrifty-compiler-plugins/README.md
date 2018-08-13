Compiler Plugin SPI
-------------------

Thrifty exposes two post-processing interfaces: `TypeProcessor`, and `KotlinTypeProcessor`.
Using the standard Java SPI, you can modify the classes generated from Thrift IDL
by implementing one of these interfaces and making it available in the compiler's classpath.
The interface you implement depends on the target language, Java or Kotlin, respectively.

Java Example
------------

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

Kotlin Example
--------------

Kotlin data classes (which Thrifty-generated structs are) are also not `Serializable` by default.
As in the Java example above, you can change this by providing a `KotlinTypeProcessor`:

```kotlin
class SerializableKotlinProcessor : KotlinTypeProcessor {
  override fun process(spec: TypeSpec): TypeSpec? {
    return spec.toBuilder().run {
      addSuperinterface(Serializable::class)
      
      // Static fields in Kotlin go in a companion object;
      // we'll assume here that `spec` does not already
      // have one.
      val companionType = TypeSpec.companionObjectBuilder()
          .addProperty(PropertySpec.builder("serialVersionUID", Long::class)
              .addModifiers(KModifier.PRIVATE, KModifier.CONST) // const vals in companions are static
              .initializer("%L", -1L)
              .build())
          .build()
      
      addType(companionType)
      build()
    }
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
          - com.microsoft.thrifty.compiler.spi.TypeProcessor // For a Java processor
          - com.microsoft.thrifty.compiler.spi.KotlinTypeProcessor // for a Kotlin processor

The file under META-INF is how Java services are identified.  The file should
contain the full name of your processor class:

For Java:
```
com.corp.SerializableTypeProcessor
```

For Kotlin:
```
com.corp.SerializableKotlinProcessor
```

Once this project is built into a JAR, you can include it in the classpath on the command line
like so:

```bash
java -cp thrifty-compiler.jar:your-processor.jar com.microsoft.thrifty.compiler.ThriftyCompiler # compiler args as usual
```

This is a little wordy.  An easier but less-flexible approach could be to build an uberjar including
the compiler along with your processor.  See `thrifty-example-postprocessor/build.gradle` for one way to do it.