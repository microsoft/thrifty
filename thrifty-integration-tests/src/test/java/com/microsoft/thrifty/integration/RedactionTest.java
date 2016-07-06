package com.microsoft.thrifty.integration;

import com.microsoft.thrifty.integration.gen.HasCommentBasedRedaction;
import com.microsoft.thrifty.integration.gen.HasRedaction;
import com.microsoft.thrifty.integration.gen.ObfuscatedCollections;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

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

        assertThat(oc.toString(), containsString("numz=List<Integer>(size=3)"));
    }

    @Test
    public void obfuscatedMap() {
        ObfuscatedCollections oc = new ObfuscatedCollections.Builder()
                .stringz(Collections.singletonMap("foo", "bar"))
                .build();

        assertThat(oc.toString(), containsString("stringz=Map<String, String>(size=1)"));
    }
}
