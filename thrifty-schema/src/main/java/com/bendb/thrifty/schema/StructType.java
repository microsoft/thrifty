/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.AnnotationElement;
import com.bendb.thrifty.schema.parser.FieldElement;
import com.bendb.thrifty.schema.parser.StructElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

public class StructType extends Named {
    private final StructElement element;
    private final ThriftType type;
    private final ImmutableList<Field> fields;
    private final ImmutableMap<String, String> annotations;

    StructType(
            StructElement element,
            ThriftType type,
            Map<NamespaceScope, String> namespaces,
            FieldNamingPolicy fieldNamingPolicy) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
        for (FieldElement fieldElement : element.fields()) {
            fieldsBuilder.add(new Field(fieldElement, fieldNamingPolicy));
        }
        this.fields = fieldsBuilder.build();

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    @Override
    public ThriftType type() {
        return type;
    }

    @Override
    public String documentation() {
        return element.documentation();
    }

    @Override
    public Location location() {
        return element.location();
    }

    public ImmutableList<Field> fields() {
        return fields;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public boolean isStruct() {
        return element.type() == StructElement.Type.STRUCT;
    }

    public boolean isUnion() {
        return element.type() == StructElement.Type.UNION;
    }

    public boolean isException() {
        return element.type() == StructElement.Type.EXCEPTION;
    }

    void link(Linker linker) {
        for (Field field : fields) {
            field.link(linker);
        }
    }

    void validate(Linker linker) {
        for (Field field : fields) {
            field.validate(linker);
        }

        Map<Integer, Field> fieldsById = new LinkedHashMap<>(fields.size());
        for (Field field : fields) {
            Field dupe = fieldsById.put(field.id(), field);
            if (dupe != null) {
                linker.addError(
                        "Duplicate field IDs: " + field.name() + " and " + dupe.name()
                        + " both have the same ID (" + field.id() + ")");
            }

            if (isUnion() && field.required()) {
                linker.addError("Unions may not have required fields: " + field.name());
            }
        }
    }
}
