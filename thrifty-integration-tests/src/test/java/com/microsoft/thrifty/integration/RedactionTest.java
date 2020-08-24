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
package com.microsoft.thrifty.integration;

import com.microsoft.thrifty.integration.gen.HasCommentBasedRedaction;
import com.microsoft.thrifty.integration.gen.HasObfuscation;
import com.microsoft.thrifty.integration.gen.HasRedaction;
import com.microsoft.thrifty.integration.gen.ObfuscatedCollections;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

public class RedactionTest {
    @Test
    public void redaction() {
        HasRedaction hr = new HasRedaction.Builder()
                .one("value-one")
                .two("should-not-appear")
                .three("value-three")  // expe
                .build();

        assertThat(hr.toString(), containsString("one=value-one"));
        assertThat(hr.toString(), not(containsString("should-not-appear")));
        assertThat(hr.two, is("should-not-appear"));
    }

    @Test
    public void obfuscation() {
        HasRedaction hr = new HasRedaction.Builder()
                .one("value-one")
                .two("value-two")
                .three("value-three")
                .build();

        assertThat(hr.toString(), containsString("three=6A39B242"));
        assertThat(hr.three, is("value-three"));
    }

    @Test
    public void commentBasedRedaction() {
        HasCommentBasedRedaction hcbr = new HasCommentBasedRedaction.Builder()
                .foo("bar")
                .build();

        assertThat(hcbr.toString(), is("HasCommentBasedRedaction{foo=<REDACTED>}"));
    }

    @Test
    public void obfuscatedList() {
        ObfuscatedCollections oc = new ObfuscatedCollections.Builder()
                .numz(Arrays.asList(1, 2, 3))
                .build();

        assertThat(oc.toString(), containsString("numz=list<i32>(size=3)"));
    }

    @Test
    public void obfuscatedMap() {
        ObfuscatedCollections oc = new ObfuscatedCollections.Builder()
                .stringz(Collections.singletonMap("foo", "bar"))
                .build();

        assertThat(oc.toString(), containsString("stringz=map<string, string>(size=1)"));
    }

    @Test
    public void obfuscatedString() {
        HasObfuscation ho = new HasObfuscation.Builder().build();
        assertThat(ho.toString(), is("HasObfuscation{ssn=null}"));

        ho = new HasObfuscation.Builder().ssn("123-45-6789").build();
        assertThat(ho.toString(), is("HasObfuscation{ssn=1E1DB4B3}"));
    }
}
