package com.bendb.thrifty.compiler;

import com.bendb.thrifty.compiler.spi.TypeProcessor;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.io.Serializable;

/**
 * An example {@link TypeProcessor} that implements {@link Serializable}
 * on all types.
 */
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
