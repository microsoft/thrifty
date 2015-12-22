/*
 * Copyright (C) 2015 Benjamin Bader
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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FieldNamingPolicyTest {
    @Test
    public void defaultNamesAreUnaltered() {
        FieldNamingPolicy policy = FieldNamingPolicy.DEFAULT;

        assertThat(policy.apply("SSLFlag"), is("SSLFlag"));
        assertThat(policy.apply("MyField"), is("MyField"));
    }

    @Test
    public void javaPolicyCamelCasesNames() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        assertThat(policy.apply("MyField"), is("myField"));
        assertThat(policy.apply("X"), is("x"));
        assertThat(policy.apply("abcde"), is("abcde"));
    }

    @Test
    public void javaPolicyPreservesAcronyms() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        assertThat(policy.apply("OAuthToken"), is("OAuthToken"));
        assertThat(policy.apply("SSLFlag"), is("SSLFlag"));
    }
}