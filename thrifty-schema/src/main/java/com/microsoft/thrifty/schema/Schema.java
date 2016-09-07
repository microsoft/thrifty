/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;

import java.util.NoSuchElementException;

/**
 * Encapsulates all types, values, and services defined in a set of Thrift
 * files.
 *
 * Strictly speaking, this is a lossy representation - the original filesystem
 * structure of the source Programs is not preserved.  As such, this isn't a
 * suitable representation from which to generate C++ code, or any other
 * module-based language for the matter.  But as we're only concerned with Java
 * here, it's perfectly convenient.
 */
public class Schema {
    private final ImmutableList<StructType> structs;
    private final ImmutableList<StructType> unions;
    private final ImmutableList<StructType> exceptions;
    private final ImmutableList<EnumType> enums;
    private final ImmutableList<Constant> constants;
    private final ImmutableList<Typedef> typedefs;
    private final ImmutableList<Service> services;

    Schema(Iterable<Program> programs) {
        ImmutableList.Builder<StructType> structs    = ImmutableList.builder();
        ImmutableList.Builder<StructType> unions     = ImmutableList.builder();
        ImmutableList.Builder<StructType> exceptions = ImmutableList.builder();
        ImmutableList.Builder<EnumType>   enums      = ImmutableList.builder();
        ImmutableList.Builder<Constant>   constants  = ImmutableList.builder();
        ImmutableList.Builder<Typedef>    typedefs   = ImmutableList.builder();
        ImmutableList.Builder<Service>    services   = ImmutableList.builder();

        for (Program program : programs) {
            structs.addAll(program.structs());
            unions.addAll(program.unions());
            exceptions.addAll(program.exceptions());
            enums.addAll(program.enums());
            constants.addAll(program.constants());
            typedefs.addAll(program.typedefs());
            services.addAll(program.services());
        }

        this.structs = structs.build();
        this.unions = unions.build();
        this.exceptions = exceptions.build();
        this.enums = enums.build();
        this.constants = constants.build();
        this.typedefs = typedefs.build();
        this.services = services.build();
    }

    private Schema(Builder builder) {
        this.structs = builder.structs;
        this.unions = builder.unions;
        this.exceptions = builder.exceptions;
        this.enums = builder.enums;
        this.constants = builder.constants;
        this.typedefs = builder.typedefs;
        this.services = builder.services;
    }

    public ImmutableList<StructType> structs() {
        return structs;
    }

    public ImmutableList<StructType> unions() {
        return unions;
    }

    public ImmutableList<StructType> exceptions() {
        return exceptions;
    }

    public ImmutableList<EnumType> enums() {
        return enums;
    }

    public ImmutableList<Constant> constants() {
        return constants;
    }

    public ImmutableList<Typedef> typedefs() {
        return typedefs;
    }

    public ImmutableList<Service> services() {
        return services;
    }

    public EnumType findEnumByType(ThriftType type) {
        for (EnumType enumType : enums) {
            if (enumType.type().equals(type)) {
                return enumType;
            }
        }
        throw new NoSuchElementException("No enum type matching " + type.name());
    }

    public Builder toBuilder() {
        return new Builder(structs, unions, exceptions, enums, constants, typedefs, services);
    }

    public static final class Builder {
        private ImmutableList<StructType> structs;
        private ImmutableList<StructType> unions;
        private ImmutableList<StructType> exceptions;
        private ImmutableList<EnumType> enums;
        private ImmutableList<Constant> constants;
        private ImmutableList<Typedef> typedefs;
        private ImmutableList<Service> services;

        Builder(ImmutableList<StructType> structs,
                ImmutableList<StructType> unions,
                ImmutableList<StructType> exceptions,
                ImmutableList<EnumType> enums,
                ImmutableList<Constant> constants,
                ImmutableList<Typedef> typedefs,
                ImmutableList<Service> services) {
            this.structs = structs;
            this.unions = unions;
            this.exceptions = exceptions;
            this.enums = enums;
            this.constants = constants;
            this.typedefs = typedefs;
            this.services = services;
        }

        public Builder setStructs(ImmutableList<StructType> structs) {
            this.structs = structs;
            return this;
        }

        public Builder setUnions(ImmutableList<StructType> unions) {
            this.unions = unions;
            return this;
        }

        public Builder setExceptions(ImmutableList<StructType> exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        public Builder setEnums(ImmutableList<EnumType> enums) {
            this.enums = enums;
            return this;
        }

        public Builder setConstants(ImmutableList<Constant> constants) {
            this.constants = constants;
            return this;
        }

        public Builder setTypedefs(ImmutableList<Typedef> typedefs) {
            this.typedefs = typedefs;
            return this;
        }

        public Builder setServices(ImmutableList<Service> services) {
            this.services = services;
            return this;
        }

        public Schema build() {
            return new Schema(this);
        }
    }
}
